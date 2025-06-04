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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWayland;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import heronarts.glx.shader.Phong;
import heronarts.glx.shader.Tex2d;
import heronarts.glx.shader.UniformFill;
import heronarts.glx.shader.VertexFill;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.vg.VGraphics;

public class BGFXEngine {

  static class RenderThread extends Thread {

    private final GLX glx;

    BGFXEngine bgfx;

    final CountDownLatch didInitialize = new CountDownLatch(1);

    final AtomicBoolean resizeFramebuffer = new AtomicBoolean(false);
    final AtomicBoolean resizeUI = new AtomicBoolean(false);

    final CountDownLatch buildUI = new CountDownLatch(1);
    final CountDownLatch didBuildUI = new CountDownLatch(1);

    final CountDownLatch run = new CountDownLatch(1);

    private volatile boolean shutdown = false;

    boolean hasFailed;

    final List<Runnable> threadSafeDisposeQueue = Collections.synchronizedList(new ArrayList<>());

    UI ui;

    RenderThread(GLX glx) {
      super("BGFX Render Thread");
      this.glx = glx;
    }

    // Called by GLX main thread, signals to the RenderThread to clean everything
    // up and close down BGFX. Blocks until finished, at which point main thread
    // can destroy the GLFW window.
    void shutdown() {
      if (Thread.currentThread() == this) {
        throw new IllegalThreadStateException(getName() + " may not call shutdown() on itself");
      }
      this.shutdown = true;
      synchronized (this) { notify(); }
      try {
        join();
      } catch (InterruptedException ix) {
        GLX.error(ix, "Interrupted awaiting BGFX shutdown");
      }
    }

    @Override
    public void run() {
      GLX.log("Starting " + getName() + "...");

      try {
        // Setup BGFX
        this.bgfx = new BGFXEngine(this.glx);
        this.didInitialize.countDown();

        // Build UI
        try {
          this.buildUI.await();
        } catch (InterruptedException ix) {
          GLX.error(ix, getName() + " interrupted before buildUI");
          return;
        }
        this.ui = this.glx.buildUI();
        this.didBuildUI.countDown();

        // Wait for main render loop signal
        try {
          this.run.await();
        } catch (InterruptedException ix) {
          GLX.error(ix, getName() + " interrupted before begin");
          return;
        }

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
          disposeQueue();

          // Window size changed, reset backing framebuffer
          if (this.resizeFramebuffer.getAndSet(false)) {
            bgfx_reset(this.glx.frameBufferWidth, this.glx.frameBufferHeight, BGFX_RESET_VSYNC, this.bgfx.format);
            this.ui.resize();
            this.ui.redraw();
          }

          // Resize the UI if it changed
          if (this.resizeUI.getAndSet(false)) {
            this.ui.resize();
            this.ui.redraw();
          }

          long drawStart = System.nanoTime();
          try {
            this.bgfx.draw();
          } catch (Throwable x) {
            GLX.error(x, "UI THREAD FAILURE: Unhandled error in BGFXEngine.draw(): " + x.getLocalizedMessage());
            this.glx.fail(x);

            // The above should have set a UI failure window to be drawn...
            // Take one last whack at re-drawing. This may very well fail and
            // throw an uncaught error or exception, so be it.
            try {
              this.bgfx.draw();
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

        // We're ka-put, shut it all down.
        dispose();

      } catch (Throwable x) {
        GLX.error(x, "BGFX Thread Failure: " + x.getMessage());
        // TODO(bgfx): crash out clean as possible
      }

      GLX.log(getName() + " finished.");
    }

    private final List<Runnable> bgfxThreadDisposeQueue = new ArrayList<>();

    private void disposeQueue() {
      synchronized (this.threadSafeDisposeQueue) {
        this.bgfxThreadDisposeQueue.addAll(this.threadSafeDisposeQueue);
        this.threadSafeDisposeQueue.clear();
      }
      this.bgfxThreadDisposeQueue.forEach(r -> r.run());
      this.bgfxThreadDisposeQueue.clear();
    }

    private void dispose() {
      // Last ditch any bgfx resources queued for removal
      disposeQueue();

      // Stop the LX engine
      GLX.log("Stopping LX engine...");
      this.glx.engine.stop();

      // NOTE: destroy the whole UI first, rip down all the listeners
      // before disposing of the engine itself. Done on the BGFX thread
      // to properly dispose of BGFX resources.
      GLX.log("Disposing of GLX UI...");
      this.glx.ui.dispose();
      GLX.log("GLX UI disposed.");

      // Clean up the LX instance
      GLX.log("Disposing of GLX instance...");
      this.glx.dispose();
      GLX.log("GLX instance disposed.");

      // Dispose of BGFX
      GLX.log("Disposing of BGFX engine...");
      this.bgfx.dispose();
    }

  }

  public final class Programs {

    public final Tex2d tex2d;
    public final UniformFill uniformFill;
    public final VertexFill vertexFill;
    public final Phong phong;

    public Programs(BGFXEngine bgfx) {
      this.tex2d = new Tex2d(bgfx);
      this.uniformFill = new UniformFill(bgfx);
      this.vertexFill = new VertexFill(bgfx);
      this.phong = new Phong(bgfx);
    }

    public void dispose() {
      this.tex2d.dispose();
      this.uniformFill.dispose();
      this.vertexFill.dispose();
      this.phong.dispose();
    }
  }

  public final GLX glx;

  final boolean zZeroToOne;
  final int renderer;
  final int format;
  final Programs program;
  final VGraphics vg;

  private BGFXEngine(GLX glx) {
    this.glx = glx;
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
          .width(this.glx.frameBufferWidth)
          .height(this.glx.frameBufferHeight)
          .reset(BGFX_RESET_VSYNC));
      switch (Platform.get()) {
        case LINUX, FREEBSD -> {
          if (glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) {
            init.platformData()
              .ndt(GLFWNativeWayland.glfwGetWaylandDisplay())
              .nwh(GLFWNativeWayland.glfwGetWaylandWindow(this.glx.window))
              .type(BGFX_NATIVE_WINDOW_HANDLE_TYPE_WAYLAND);
          } else {
            init.platformData()
              .ndt(GLFWNativeX11.glfwGetX11Display())
              .nwh(GLFWNativeX11.glfwGetX11Window(this.glx.window));
          }
        }
        case MACOSX -> init.platformData().nwh(GLFWNativeCocoa.glfwGetCocoaWindow(this.glx.window));
        case WINDOWS -> init.platformData().nwh(GLFWNativeWin32.glfwGetWin32Window(this.glx.window));
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
    GLX.log("Using BGFX renderer: " + rendererName);
    this.zZeroToOne = !bgfx_get_caps().homogeneousDepth();
    this.program = new Programs(this);
    this.vg = new VGraphics(glx);
  }

  public int getRenderer() {
    return this.renderer;
  }

  public boolean isOpenGL() {
    return this.renderer == BGFX_RENDERER_TYPE_OPENGL;
  }

  private void draw() {
    // Copy the latest engine-rendered LED frame
    this.glx.engine.copyFrameThreadSafe(this.glx.uiFrame);
    this.glx.ui.draw();
    bgfx_frame(false);
  }

  private void dispose() {
    this.program.dispose();
    bgfx_shutdown();
  }

}
