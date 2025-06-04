/**
 * Copyright 2019- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.glx;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.bgfx.BGFX.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWayland;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

public class BGFXEngine {

  /**
   * Marker interface for resources that need allocation and freeing
   * on the BGFX thread. Can be enforced by GLX.assertBgfx... family
   * of methods.
   */
  public interface Resource {
    public void dispose();
  }

  private final GLX glx;

  final Thread thread;

  final AtomicBoolean resizeFramebuffer = new AtomicBoolean(false);
  final AtomicBoolean resizeUI = new AtomicBoolean(false);

  volatile boolean hasFailed = false;
  volatile boolean shutdown = false;

  final boolean zZeroToOne;
  final int renderer;
  final int format;

  BGFXEngine(GLX glx) {
    this.glx = glx;

    // Note the purpose of this thread
    this.thread = Thread.currentThread();
    this.thread.setName("BGFX Render Thread");

    try (MemoryStack stack = MemoryStack.stackPush()) {
      final int renderer = this.glx.flags.useOpenGL ?
        org.lwjgl.bgfx.BGFX.BGFX_RENDERER_TYPE_OPENGL :
        org.lwjgl.bgfx.BGFX.BGFX_RENDERER_TYPE_COUNT;

      final BGFXInit init = BGFXInit.malloc(stack);
      bgfx_init_ctor(init);
      init
        .type(renderer)
        .vendorId(BGFX_PCI_ID_NONE)
        .deviceId((short) 0)
        .resolution(res -> res
          .width(this.glx.window.getFrameBufferWidth())
          .height(this.glx.window.getFrameBufferHeight())
          .reset(BGFX_RESET_VSYNC));
      switch (Platform.get()) {
        case LINUX, FREEBSD -> {
          if (glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) {
            init.platformData()
              .ndt(GLFWNativeWayland.glfwGetWaylandDisplay())
              .nwh(GLFWNativeWayland.glfwGetWaylandWindow(this.glx.window.handle))
              .type(BGFX_NATIVE_WINDOW_HANDLE_TYPE_WAYLAND);
          } else {
            init.platformData()
              .ndt(GLFWNativeX11.glfwGetX11Display())
              .nwh(GLFWNativeX11.glfwGetX11Window(this.glx.window.handle));
          }
        }
        case MACOSX -> init.platformData().nwh(GLFWNativeCocoa.glfwGetCocoaWindow(this.glx.window.handle));
        case WINDOWS -> init.platformData().nwh(GLFWNativeWin32.glfwGetWin32Window(this.glx.window.handle));
      }
      if (!bgfx_init(init)) {
        throw new RuntimeException("Error initializing bgfx renderer");
      }
      this.format = init.resolution().format();
    }

    this.renderer = bgfx_get_renderer_type();
    final String rendererName = bgfx_get_renderer_name(this.renderer);
    if ("NULL".equals(rendererName)) {
      throw new RuntimeException("Error identifying bgfx renderer");
    }
    GLX.log("BGFX renderer: " + rendererName);

    this.zZeroToOne = !bgfx_get_caps().homogeneousDepth();
  }

  public int getRenderer() {
    return this.renderer;
  }

  public boolean isOpenGL() {
    return this.renderer == BGFX_RENDERER_TYPE_OPENGL;
  }

  void mainLoop() {

    final int FRAME_PERF_LOG = 300;
    long before = System.currentTimeMillis();
    long now;
    int frameCount = 0;
    long drawNanos = 0;

    // Keep rendering until we're asked to dispose
    while (!this.shutdown) {

      if (this.hasFailed) {
        // Just wait to be told to dispose
        synchronized (this) {
          try {
            wait();
          } catch (InterruptedException ix) {}
        }
        continue;
      }

      // Dispose of queued graphics resources
      _disposeQueue();

      // Window size changed, reset backing framebuffer
      if (this.resizeFramebuffer.getAndSet(false)) {
        bgfx_reset(
          this.glx.window.getFrameBufferWidth(),
          this.glx.window.getFrameBufferHeight(),
          BGFX_RESET_VSYNC,
          this.format
        );
        this.glx.ui.resize();
        this.glx.ui.redraw();
      }

      // Resize the UI if it changed
      if (this.resizeUI.getAndSet(false)) {
        this.glx.ui.resize();
        this.glx.ui.redraw();
      }

      long drawStart = System.nanoTime();
      try {
        draw();
      } catch (Throwable x) {
        GLX.error(x, "UI THREAD FAILURE: Unhandled error in BGFXEngine.draw(): " + x.getLocalizedMessage());
        this.glx.fail(x);

        // The above should have set a UI failure window to be drawn...
        // Take one last whack at re-drawing. This may very well fail and
        // throw an uncaught error or exception, so be it.
        try {
          draw();
        } catch (Throwable ignored) {
          // Yeah, we thought that may happen.
        }

        this.hasFailed = true;
      }
      drawNanos += (System.nanoTime() - drawStart);
      if (!this.hasFailed && (++frameCount == FRAME_PERF_LOG)) {
        frameCount = 0;
        now = System.currentTimeMillis();
        if (this.glx.flagUIDebug) {
          GLX.log("UI thread healthy, running at: " + FRAME_PERF_LOG * 1000f / (now - before) + "fps, average draw time: " + (drawNanos / FRAME_PERF_LOG / 1000) + "us");
        }
        before = now;
        drawNanos = 0;
      }
    }
  }

  final List<BGFXEngine.Resource> threadSafeDisposeQueue = Collections.synchronizedList(new ArrayList<>());
  private final List<BGFXEngine.Resource> bgfxThreadDisposeQueue = new ArrayList<>();

  private void draw() {
    // Copy the latest engine-rendered LED frame
    this.glx.engine.copyFrameThreadSafe(this.glx.uiFrame);
    this.glx.ui.draw();
    bgfx_frame(false);
  }

  private void _disposeQueue() {
    synchronized (this.threadSafeDisposeQueue) {
      this.bgfxThreadDisposeQueue.addAll(this.threadSafeDisposeQueue);
      this.threadSafeDisposeQueue.clear();
    }
    this.bgfxThreadDisposeQueue.forEach(r -> r.dispose());
    this.bgfxThreadDisposeQueue.clear();
  }

  void dispose() {
    GLX.log("Disposing BGFXEngine...");
    _disposeQueue();
    bgfx_shutdown();
  }

}
