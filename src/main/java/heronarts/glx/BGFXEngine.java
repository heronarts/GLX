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
import org.lwjgl.glfw.GLFWNativeWayland;
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

  public static class ResourceException extends RuntimeException {
    private static final long serialVersionUID = -3148578246159355331L;

    public ResourceException(String message) {
      super(message);
    }
  }

  public interface Buffer {

    public interface Vertex extends Buffer {
      public void setVertexBuffer(int stream);
    }

    public interface Index extends Buffer {
      public void setIndexBuffer();
    }
  }

  private final GLX glx;

  final Thread thread;

  final AtomicBoolean resizeFramebuffer = new AtomicBoolean(false);
  final AtomicBoolean resizeFramebufferAlt = new AtomicBoolean(false);
  final AtomicBoolean resizeUI = new AtomicBoolean(false);
  final AtomicBoolean resizeUIAlt = new AtomicBoolean(false);

  volatile boolean hasFailed = false;
  volatile boolean shutdown = false;

  final boolean zZeroToOne;
  final int renderer;
  final int format;

  BGFXEngine(GLX glx, WindowEngine windowEngine) {
    this.glx = glx;

    // Note the purpose of this thread
    this.thread = Thread.currentThread();
    this.thread.setName("BGFX Render Thread");

    try (MemoryStack stack = MemoryStack.stackPush()) {
      final int renderer = this.glx.flags.useOpenGL ?
        org.lwjgl.bgfx.BGFX.BGFX_RENDERER_TYPE_OPENGL :
        org.lwjgl.bgfx.BGFX.BGFX_RENDERER_TYPE_COUNT;

      // Set BGFX initialization parameters
      final BGFXInit init = BGFXInit.malloc(stack);
      bgfx_init_ctor(init);
      init
        .type(renderer)
        .vendorId(BGFX_PCI_ID_NONE)
        .deviceId((short) 0)
        .resolution(res -> res
          .width(glx.windowEngine.mainWindow.getFrameBufferWidth())
          .height(glx.windowEngine.mainWindow.getFrameBufferHeight())
          .reset(BGFX_RESET_VSYNC));

      // Set BGFX platform-specific parameters
      switch (Platform.get()) {
        case LINUX, FREEBSD -> {
          if (glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) {
            init.platformData()
              .ndt(GLFWNativeWayland.glfwGetWaylandDisplay())
              .type(BGFX_NATIVE_WINDOW_HANDLE_TYPE_WAYLAND);
          } else {
            init.platformData()
              .ndt(GLFWNativeX11.glfwGetX11Display());
          }
        }
        default -> {}
      }
      init.platformData().nwh(glx.windowEngine.mainWindow.getNativeHandle());

      // Initialize BGFX
      if (!bgfx_init(init)) {
        throw new RuntimeException("Error initializing bgfx renderer");
      }
      this.format = init.resolution().formatColor();

      // Create a framebuffer for the Alt window
      createFrameBufferAlt();
    }

    this.renderer = bgfx_get_renderer_type();
    final String rendererName = bgfx_get_renderer_name(this.renderer);
    if ("NULL".equals(rendererName)) {
      throw new RuntimeException("Error identifying bgfx renderer");
    }
    GLX.log("BGFX renderer: " + rendererName);

    this.zZeroToOne = !bgfx_get_caps().homogeneousDepth();
  }

  private void destroyFrameBufferAlt() {
    final short handle = this.glx.windowEngine.altWindow.getFrameBuffer();
    if (handle != BGFX_INVALID_HANDLE) {
      bgfx_destroy_frame_buffer(handle);
      this.glx.windowEngine.altWindow.setFrameBuffer(BGFX_INVALID_HANDLE);
    }
  }

  private void createFrameBufferAlt() {
    destroyFrameBufferAlt();
    final WindowEngine.Window window = this.glx.windowEngine.altWindow;
    final short framebuffer = bgfx_create_frame_buffer_from_nwh(
      window.getNativeHandle(),
      window.getFrameBufferWidth(),
      window.getFrameBufferHeight(),
      BGFX_TEXTURE_FORMAT_RGBA8,
      BGFX_TEXTURE_FORMAT_COUNT // No depth buffer
    );
    if (framebuffer == BGFX_INVALID_HANDLE) {
      throw new RuntimeException("Could not create framebuffer for window: " + window);
    }
    this.glx.windowEngine.altWindow.setFrameBuffer(framebuffer);
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

      // Main window size changed, reset backing framebuffer
      if (this.resizeFramebuffer.getAndSet(false)) {
        bgfx_reset(
          this.glx.windowEngine.mainWindow.getFrameBufferWidth(),
          this.glx.windowEngine.mainWindow.getFrameBufferHeight(),
          BGFX_RESET_VSYNC,
          this.format
        );
        this.glx.ui.resize();
        this.glx.ui.redraw();
      }

      // Alt window size changed, dispose and re-create backing framebuffer
      if (this.resizeFramebufferAlt.getAndSet(false)) {
        createFrameBufferAlt();
        this.glx.ui.resizeAlt();
        this.glx.ui.redrawAlt();
      }

      // Resize the UI if it changed (Main window)
      if (this.resizeUI.getAndSet(false)) {
        this.glx.ui.resize();
        this.glx.ui.redraw();
      }

      // Resize the UI if it changed (Alt window)
      if (this.resizeUIAlt.getAndSet(false)) {
        this.glx.ui.resizeAlt();
        this.glx.ui.redrawAlt();
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

    // Dummy frames on exit to ensure bgfx memory validity
    bgfx_frame(false);
    bgfx_frame(false);
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
    destroyFrameBufferAlt();
    _disposeQueue();
    bgfx_shutdown();
  }

}
