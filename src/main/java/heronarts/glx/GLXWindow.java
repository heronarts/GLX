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

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import heronarts.lx.LXPreferences;
import heronarts.lx.utils.LXUtils;

/**
 * The GLXWindow class takes care of running a windowed application using GLFW.
 * This *must* to be run on the main thread (on Mac the JVM will have to be started
 * with -XstartOnFirstThread to ensure this).
 *
 * This class therefore *owns* the main thread, and its main() method is in charge
 * of the overall program lifecycle.
 */
public class GLXWindow {

  public interface Delegate {
    public void setClipboardText(GLXWindow window, String clipboardText);
    public void onWindowClose(GLXWindow window);
    public void onZoomChanged(GLXWindow window, float uiZoom);
    public void onContentScaleChanged(GLXWindow window, float contentScaleX, float contentScaleY);
    public void onFramebufferSizeChanged(GLXWindow window, float framebufferWidth, float framebufferHeight);
    public void onDropFile(GLXWindow window, String fileName);
    public void onShutdown(GLXWindow window);
  }

  public enum MouseCursor {
    ARROW(GLFW_ARROW_CURSOR),
    HAND(GLFW_HAND_CURSOR),
    HRESIZE(GLFW_HRESIZE_CURSOR),
    VRESIZE(GLFW_VRESIZE_CURSOR),
    MAGNIFYING_GLASS("magnifying.png", 4, 4),
    LEFT_BRACE("left-brace.png", 2, 7),
    RIGHT_BRACE("right-brace.png", 2, 7),
    START_MARKER("start-marker.png", 1, 4),
    END_MARKER("end-marker.png", 8, 4),
    CLIP_PLAY("clip-play.png", 1, 5);

    private final int glfwShape;
    private final String resourceName;
    private final int xhot, yhot;
    private ByteBuffer stbiBuffer;
    private GLFWImage glfwImage;
    private long handle;

    private MouseCursor(int glfwShape) {
      this.glfwShape = glfwShape;
      this.resourceName = null;
      this.xhot = this.yhot = 0;
    }

    private MouseCursor(String resourceName) {
      this(resourceName, 0, 0);
    }

    private MouseCursor(String resourceName, int xhot, int yhot) {
      this.glfwShape = -1;
      this.resourceName = resourceName;
      this.xhot = xhot;
      this.yhot = yhot;
    }

    private void initialize() {
      if (this.resourceName != null) {
        this.glfwImage = GLFWImage.create();
        ByteBuffer buffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
          buffer = GLXUtils.loadResource("cursors/" + this.resourceName);

          IntBuffer width = stack.mallocInt(1);
          IntBuffer height = stack.mallocInt(1);
          IntBuffer components = stack.mallocInt(1);

          this.stbiBuffer = STBImage.stbi_load_from_memory(buffer, width, height, components, STBImage.STBI_rgb_alpha);
          this.glfwImage.set(width.get(), height.get(), this.stbiBuffer);
          this.handle = glfwCreateCursor(this.glfwImage, this.xhot, this.yhot);

        } catch (Exception x) {
          GLX.error(x, "Cannot load mouse cursor: " + this.resourceName);
        } finally {
          if (buffer != null) {
            MemoryUtil.memFree(buffer);
          }
        }

      } else {
        this.handle = glfwCreateStandardCursor(this.glfwShape);
      }
    }

    private void dispose() {
      glfwDestroyCursor(this.handle);
      if (this.stbiBuffer != null) {
        STBImage.stbi_image_free(this.stbiBuffer);
      }

    }
  };

  private static final int MIN_WINDOW_WIDTH = 820;
  private static final int MIN_WINDOW_HEIGHT = 480;

  private static final int DEFAULT_WINDOW_WIDTH = 1280;
  private static final int DEFAULT_WINDOW_HEIGHT = 720;

  private Delegate delegate;

  private final Thread thread;

  final long handle;

  private volatile MouseCursor mouseCursor = null;
  private final AtomicBoolean needsCursorUpdate = new AtomicBoolean(false);

  private int displayX = -1;
  private int displayY = -1;
  private int displayWidth = -1;
  private int displayHeight = -1;
  private int windowWidth = DEFAULT_WINDOW_WIDTH;
  private int windowHeight = DEFAULT_WINDOW_HEIGHT;
  private int windowPosX = -1;
  private int windowPosY = -1;

  private int frameBufferWidth = 0;
  private int frameBufferHeight = 0;

  private float systemContentScaleX = 1;
  private float systemContentScaleY = 1;

  private float uiZoom = 1;

  private float cursorScaleX = 1;
  private float cursorScaleY = 1;

  private float uiWidth = 0;
  private float uiHeight = 0;

  private boolean ignoreClipboardError = false;
  private final AtomicBoolean setWindowSizeLimits = new AtomicBoolean(true);

  final InputDispatch inputDispatch = new InputDispatch(this);

  private final CountDownLatch isReady = new CountDownLatch(1);

  public final GLX.Flags flags;
  public final LXPreferences preferences;

  public GLXWindow(GLX.Flags flags) {
    this.thread = Thread.currentThread();
    this.flags = flags;
    this.preferences = new LXPreferences(flags);

    // Get initial window size from preferences
    if (flags.loadPreferences) {
      this.preferences.loadWindowSettings();
    }
    final int preferenceWidth = this.preferences.getWindowWidth();
    final int preferenceHeight = this.preferences.getWindowHeight();
    if (preferenceWidth > 0 && preferenceHeight > 0) {
      this.windowWidth = preferenceWidth;
      this.windowHeight = preferenceHeight;
    } else if (flags.windowWidth > 0 && flags.windowHeight > 0) {
      this.windowWidth = flags.windowWidth;
      this.windowHeight = flags.windowHeight;
    }
    this.windowPosX = this.preferences.getWindowPosX();
    this.windowPosY = this.preferences.getWindowPosY();

    glfwSetErrorCallback(new GLFWErrorCallback() {
      private Map<Integer, String> ERROR_CODES =
        APIUtil.apiClassTokens((field, value) -> 0x10000 < value && value < 0x20000, null, GLFW.class);

      @Override
      public void invoke(int error, long description) {
        if (ignoreClipboardError) {
          return;
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(
          ERROR_CODES.get(error) + " error\n" +
          "\tDescription : " + getDescription(description) + "\n" +
          "\tStacktrace  :"
        );

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 4; i < stack.length; ++i) {
          logMessage.append("\n\t\t" + stack[i].toString());
        }

        GLX._error("LWJGL", logMessage.toString());
      }
    });

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit()) {
      throw new RuntimeException("Unable to initialize GLFW");
    }

    // Grab uiZoom from preferences
    this.uiZoom = this.preferences.uiZoom.getValuef() / 100f;
    this.preferences.uiZoom.addListener(p -> {
      _updateUIZoom(this.preferences.uiZoom.getValuef() / 100f);
    });

    // Configure GLFW
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);
    glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);
    glfwWindowHint(GLFW_RESIZABLE, flags.windowResizable ? GLFW_TRUE : GLFW_FALSE);

    // Detect window/framebuffer sizes and content scale
    try (MemoryStack stack = MemoryStack.stackPush()) {
      long primaryMonitor = glfwGetPrimaryMonitor();
      if (primaryMonitor == NULL) {
        GLX.error("Running on a system with no monitor, is this intended?");
      } else {
        IntBuffer xPos = stack.mallocInt(1);
        IntBuffer yPos = stack.mallocInt(1);
        IntBuffer xSize = stack.mallocInt(1);
        IntBuffer ySize = stack.mallocInt(1);
        glfwGetMonitorWorkarea(primaryMonitor, xPos, yPos, xSize, ySize);
        this.displayX = xPos.get();
        this.displayY = yPos.get();
        this.displayWidth = xSize.get();
        this.displayHeight = ySize.get();
      }
      GLX.log("GLXWindow monitorWorkarea: size(" + this.displayWidth + "x" + this.displayHeight + "), pos(x:" + this.displayX + ",y:" + this.displayY + ")");
    }

    // Ensure initial window bounds do not exceed the available display
    this.windowWidth = LXUtils.min(this.windowWidth, this.displayWidth);
    this.windowHeight = LXUtils.min(this.windowHeight, this.displayHeight);

    // Create GLFW window
    GLX.log("GLXWindow createWindow: " + this.windowWidth + "x" + this.windowHeight);
    this.handle = glfwCreateWindow(
      this.windowWidth,
      this.windowHeight,
      flags.windowTitle,
      NULL,
      NULL
    );
    if (this.handle == NULL) {
      throw new RuntimeException("Failed to create the GLFW window");
    }

    // Detect window/framebuffer sizes and content scale
    try (MemoryStack stack = MemoryStack.stackPush()) {

      // NOTE: content scale is different across platforms. On a Retina Mac,
      // content scale will be 2x and the framebuffer will have dimensions
      // that are twice that of the window. On Windows, content-scaling is
      // a setting that might be 125%, 150%, etc. - we'll have to look at
      // the window and framebuffer sizes to figure this all out
      FloatBuffer xScale = stack.mallocFloat(1);
      FloatBuffer yScale = stack.mallocFloat(1);
      glfwGetWindowContentScale(this.handle, xScale, yScale);
      this.systemContentScaleX = xScale.get(0);
      this.systemContentScaleY = yScale.get(0);
      GLX.log("GLXWindow systemContentScale: " + this.systemContentScaleX + "x" + this.systemContentScaleY);

      // The window size is in terms of "OS window size" - best thought of
      // as an abstract setting which may or may not exactly correspond to
      // pixels (e.g. a Mac retina display may have 2x as many pixels)
      IntBuffer xSize = stack.mallocInt(1);
      IntBuffer ySize = stack.mallocInt(1);
      glfwGetWindowSize(this.handle, xSize, ySize);
      this.windowWidth = xSize.get(0);
      this.windowHeight = ySize.get(0);

      // Restore window position if restored from preferences
      if (this.windowPosX >= 0 && this.windowPosY >= 0) {
        this.windowPosX = LXUtils.constrain(this.windowPosX, this.displayX, this.displayX + this.displayWidth - this.windowWidth);
        this.windowPosY = LXUtils.constrain(this.windowPosY, this.displayY, this.displayY + this.displayHeight - this.windowHeight);
        this.windowPosY = 44;
        // this.windowPosY = 0;
        GLX.log("GLXWindow setWindowPos: " + this.windowPosX + "," + this.windowPosY);
        glfwSetWindowPos(this.handle, this.windowPosX, this.windowPosY);

        // NOTE: apparently been observed in the wild that the window may end up too big to fit,
        // (email exchange w/ jkbelcher june 4 2025), check again here after setting position
        // that it's been fixed?
        glfwGetWindowSize(this.handle, xSize, ySize);
        this.windowWidth = xSize.get(0);
        this.windowHeight = ySize.get(0);
      }
      GLX.log("GLXWindow windowSize: " + this.windowWidth + "x" + this.windowHeight);

      // See what is in the framebuffer. A retina Mac probably supplies
      // 2x the dimensions on framebuffer relative to window.
      glfwGetFramebufferSize(this.handle, xSize, ySize);
      this.frameBufferWidth = xSize.get(0);
      this.frameBufferHeight = ySize.get(0);
      GLX.log("GLXWindow framebufferSize: " + this.frameBufferWidth + "x" + this.frameBufferHeight);

      // Okay, let's figure out how many "virtual pixels" the GLX UI should
      // be. Note that on a Mac with 2x retina display, contentScale will be
      // 2, but the framebuffer will have dimensions twice that of the window.
      // So we should end up with uiWidth/uiHeight matching the window.
      // But on Windows it's a different situation, if contentScale > 100%
      // then we're going to "scale down" our number of UI pixels and draw them
      // into a larger framebuffer.
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      GLX.log("GLXWindow uiSize: " + this.uiWidth + "x" + this.uiHeight);

      // To make things even trickier... keep in mind that the OS specifies cursor
      // movement relative to its window size. We need to scale those onto our
      // virtual UI window size.
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      GLX.log("GLXWindow cursorScale: " + this.cursorScaleX + "x" + this.cursorScaleY);

      // Set UI Zoom bounds based upon content scaling
      _updateUIZoomRange();
    }

    glfwSetWindowFocusCallback(this.handle, (window, focused) -> {
      if (focused) {
        // Update the cursor position callback... if the window wasn't focused
        // and the user re-focused it with a click followed by mouse drag, then
        // the CursorPosCallback won't have had a chance to fire yet. So
        // we give it a kick whenever the window refocuses.
        try (MemoryStack stack = MemoryStack.stackPush()) {
          DoubleBuffer xPos = stack.mallocDouble(1);
          DoubleBuffer yPos = stack.mallocDouble(1);
          glfwGetCursorPos(this.handle, xPos, yPos);
          this.inputDispatch.onFocus(xPos.get(0) * this.cursorScaleX, yPos.get(0) * this.cursorScaleY);
        }
      }
    });

    glfwSetWindowCloseCallback(this.handle, (window) -> {
      if (this.delegate != null) {
        this.delegate.onWindowClose(this);
      }
    });

    glfwSetWindowSizeCallback(this.handle, (window, width, height) -> {
      // NOTE(mcslee): This call should *follow* a call from glfwSetFramebufferSizeCallback, the window
      // properties change after the underlying framebuffer
      this.windowWidth = width;
      this.windowHeight = height;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      try (MemoryStack stack = MemoryStack.stackPush()) {
        // NOTE(mcslee): need to grab the new window position here as well! If a top or left
        // corner of the window is used for a drag-resize operation, then the window's X or Y
        // position can change without a glfwSetWindowPosCallback being invoked from a window
        // move operation
        IntBuffer xPos = stack.mallocInt(1);
        IntBuffer yPos = stack.mallocInt(1);
        glfwGetWindowPos(this.handle, xPos, yPos);
        this.windowPosX = xPos.get();
        this.windowPosY = yPos.get();
      }
      this.preferences.setWindowSize(this.windowWidth, this.windowHeight, this.windowPosX, this.windowPosY);
    });

    glfwSetWindowPosCallback(this.handle, (window, x, y) -> {
      this.windowPosX = x;
      this.windowPosY = y;
      this.preferences.setWindowPosition(this.windowPosX, this.windowPosY);
    });

    glfwSetWindowContentScaleCallback(this.handle, (window, contentScaleX, contentScaleY) -> {
      this.systemContentScaleX = contentScaleX;
      this.systemContentScaleY = contentScaleY;
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      _updateUIZoomRange();
      if (this.delegate != null) {
        this.delegate.onContentScaleChanged(this, contentScaleX, contentScaleY);
      }
    });

    glfwSetFramebufferSizeCallback(this.handle, (window, width, height) -> {
      this.frameBufferWidth = width;
      this.frameBufferHeight = height;
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      if (this.delegate != null) {
        this.delegate.onFramebufferSizeChanged(this, width, height);
      }
    });

    glfwSetDropCallback(this.handle, (window, count, names) -> {
      if (count == 1) {
        if (this.delegate != null) {
          this.delegate.onDropFile(this, GLFWDropCallback.getName(names, 0));
        }
      }
    });

    // Register input dispatching callbacks
    glfwSetKeyCallback(this.handle, this.inputDispatch::glfwKeyCallback);
    glfwSetCharCallback(this.handle, this.inputDispatch::glfwCharCallback);
    glfwSetCursorPosCallback(this.handle, this.inputDispatch::glfwCursorPosCallback);
    glfwSetMouseButtonCallback(this.handle, this.inputDispatch::glfwMouseButtonCallback);
    glfwSetScrollCallback(this.handle, this.inputDispatch::glfwScrollCallback);

    // Initialize standard mouse cursors
    for (MouseCursor cursor : MouseCursor.values()) {
      cursor.initialize();
    }
  }

  private void assertMainThread() {
    if (Thread.currentThread() != this.thread) {
      throw new IllegalThreadStateException("GLXWindow method may only be called from main thread");
    }
  }

  private void _updateUIZoom(float uiScale) {
    this.uiZoom = uiScale;
    this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
    this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
    this.cursorScaleX = this.uiWidth / this.windowWidth;
    this.cursorScaleY = this.uiHeight / this.windowHeight;
    this.setWindowSizeLimits.set(true);
    if (this.delegate != null) {
      this.delegate.onZoomChanged(this, uiScale);
    }
  }

  private void _updateUIZoomRange() {
    this.preferences.uiZoom.setRange((int) Math.ceil(100 / this.systemContentScaleX), 201);
  }

  void setDelegate(Delegate delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("GLXWindow.setDelegate() may not be passed null");
    }
    if (this.delegate != null) {
      throw new IllegalStateException("GLXWindow.setDelegate() may only be called once");
    }
    this.delegate = delegate;
  }

  public float getUIWidth() {
    return this.uiWidth;
  }

  public float getUIHeight() {
    return this.uiHeight;
  }

  public int getFrameBufferWidth() {
    return this.frameBufferWidth;
  }

  public int getFrameBufferHeight() {
    return this.frameBufferHeight;
  }

  public float getUIContentScaleX() {
    return this.systemContentScaleX * this.uiZoom;
  }

  public float getUIContentScaleY() {
    return this.systemContentScaleY * this.uiZoom;
  }

  public float getUIZoom() {
    return this.uiZoom;
  }

  public float getSystemContentScaleX() {
    return this.systemContentScaleX;
  }

  public float getSystemContentScaleY() {
    return this.systemContentScaleY;
  }

  float getCursorScaleX() {
    return this.cursorScaleX;
  }

  float getCursorScaleY() {
    return this.cursorScaleY;
  }

  protected void setShouldClose(boolean shouldClose) {
    glfwSetWindowShouldClose(this.handle, shouldClose);
  }

  protected void setWindowSize(int windowWidth, int windowHeight) {
    assertMainThread();
    glfwSetWindowSize(this.handle, windowWidth, windowHeight);
  }

  public void setMouseCursor(MouseCursor mouseCursor) {
    if (this.mouseCursor != mouseCursor) {
      this.mouseCursor = mouseCursor;
      this.needsCursorUpdate.set(true);
    }
  }

  private String _getSystemClipboardString = null;
  private volatile String _setSystemClipboardString = null;

  void setSystemClipboardString(String str) {
    this._setSystemClipboardString = str;
  }

  public void main() {
    GLX.log("GLXWindow.main() awaiting GLX boostrap...");
    try {
      while (true) {
        // NB: this poll call seems to be *necessary* to kick GLFW and get bgfx_init() to return! (on MacOS at least)
        glfwPollEvents();
        if (this.isReady.await(16, TimeUnit.MILLISECONDS)) {
          break;
        }
      }
    } catch (InterruptedException ix) {
      GLX.error(ix, "GLXWindow.main() interrupted awaiting BGFX initialization");
    }

    if (this.delegate == null) {
      throw new IllegalStateException("GLXWindow cannot continue past bootstrapping with no GLX delegate set");
    }

    GLX.log("GLX boostrap complete, GLXWindow running event loop...");
    eventLoop();

    GLX.log("GLXWindow closed, shutting down...");
    shutdown();
  }

  void start() {
    this.isReady.countDown();
  }

  private void eventLoop() {
    // Okay now we're into the real polling loop!
    while (!glfwWindowShouldClose(this.handle)) {
      // Update window size limits
      if (this.setWindowSizeLimits.compareAndSet(true, false)) {
        final int minWindowWidth = (int) (MIN_WINDOW_WIDTH / this.cursorScaleX);
        final int minWindowHeight = (int) (MIN_WINDOW_HEIGHT / this.cursorScaleY);
        glfwSetWindowSizeLimits(this.handle, minWindowWidth, minWindowHeight, GLFW_DONT_CARE, GLFW_DONT_CARE);
        if (this.windowWidth < minWindowWidth || this.windowHeight < minWindowHeight) {
          glfwSetWindowSize(
            this.handle,
            LXUtils.max(this.windowWidth, minWindowWidth),
            LXUtils.max(this.windowHeight, minWindowHeight)
          );
        }
      }

      // Poll for input events
      this.inputDispatch.poll();

      // Update mouse cursor if needed
      if (this.needsCursorUpdate.compareAndSet(true, false)) {
        final MouseCursor mc = this.mouseCursor;
        glfwSetCursor(this.handle, (mc != null) ? mc.handle : 0);
      }

      // Copy something to the clipboard
      final String copyToClipboard = this._setSystemClipboardString;
      if (copyToClipboard != null) {
        glfwSetClipboardString(this.handle, copyToClipboard);
        this._getSystemClipboardString = copyToClipboard;
        this._setSystemClipboardString = null;
      } else {
        this.ignoreClipboardError = true;
        String str = glfwGetClipboardString(NULL);
        this.ignoreClipboardError = false;
        if ((str != null) && !str.equals(this._getSystemClipboardString)) {
          this._getSystemClipboardString = str;
          if (this.delegate != null) {
            this.delegate.setClipboardText(this, str);
          }
        }
      }
    }
  }

  private void shutdown() {
    // Blocks until the LX and BGFX threads are finished...
    this.delegate.onShutdown(this);

    // Dispose of mouse cursors
    for (MouseCursor cursor : MouseCursor.values()) {
      cursor.dispose();
    }

    // Free the window callbacks and destroy the window
    GLX.log("Destroying main thread GLFW window...");
    glfwFreeCallbacks(this.handle);
    glfwDestroyWindow(this.handle);

    // Terminate GLFW and free the error callback
    glfwTerminate();
    glfwSetErrorCallback(null).free();

    // The program *should* end now, if not it means we hung a thread somewhere...
    GLX.log("Done with main thread, GLX shutdown complete. Thanks for playing. <3");
  }
}
