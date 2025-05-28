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
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIDialogBox;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXClassLoader;
import heronarts.lx.LXEngine;
import heronarts.lx.clipboard.LXTextValue;
import heronarts.lx.model.LXModel;
import heronarts.lx.utils.LXUtils;

public class GLX extends LX {

  private static final int MIN_WINDOW_WIDTH = 820;
  private static final int MIN_WINDOW_HEIGHT = 480;

  private static final int DEFAULT_WINDOW_WIDTH = 1280;
  private static final int DEFAULT_WINDOW_HEIGHT = 720;

  long window;

  private volatile MouseCursor mouseCursor = null;
  private volatile boolean needsCursorUpdate = false;

  private int displayX = -1;
  private int displayY = -1;
  private int displayWidth = -1;
  private int displayHeight = -1;
  private int windowWidth = DEFAULT_WINDOW_WIDTH;
  private int windowHeight = DEFAULT_WINDOW_HEIGHT;
  private int windowPosX = -1;
  private int windowPosY = -1;

  int frameBufferWidth = 0;
  int frameBufferHeight = 0;

  private float uiWidth = 0;
  private float uiHeight = 0;

  boolean flagUIDebug = false;

  float systemContentScaleX = 1;
  float systemContentScaleY = 1;

  float uiZoom = 1;

  float cursorScaleX = 1;
  float cursorScaleY = 1;

  final boolean zZeroToOne;

  public final VGraphics vg;

  private final InputDispatch inputDispatch = new InputDispatch(this);

  public final UI ui;
  public final LXEngine.Frame uiFrame;

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

  /**
   * Publicly accessible, globally reusable shader programs.
   */
  public final BGFXEngine.Programs program;

  public static class Flags extends LX.Flags {
    public int windowWidth = -1;
    public int windowHeight = -1;
    public boolean windowResizable = true;
    public String windowTitle = "GLX";
    public boolean useOpenGL = false;
  }

  public final Flags flags;

  public final BGFXEngine.RenderThread bgfxThread;
  public final BGFXEngine bgfx;

  protected GLX(Flags flags) throws IOException {
    this(flags, null);
  }

  protected GLX(Flags flags, LXModel model) throws IOException {
    super(flags, model);
    this.flags = flags;

    // Get initial window size from preferences
    int preferenceWidth = this.preferences.getWindowWidth();
    int preferenceHeight = this.preferences.getWindowHeight();
    if (preferenceWidth > 0 && preferenceHeight > 0) {
      this.windowWidth = preferenceWidth;
      this.windowHeight = preferenceHeight;
    } else if (this.flags.windowWidth > 0 && this.flags.windowHeight > 0) {
      this.windowWidth = this.flags.windowWidth;
      this.windowHeight = this.flags.windowHeight;
    }
    this.windowPosX = this.preferences.getWindowPosX();
    this.windowPosY = this.preferences.getWindowPosY();

    // GLFW initialization
    initializeWindow();

    // BGFX initialization
    this.bgfxThread = new BGFXEngine.RenderThread(this);
    this.bgfxThread.start();

    // Get BGFX instance and core state
    GLX.log("GLX Thread awaiting initialized");
    try {
      while (true) {
        // NB: this poll is necessary to kick GLFW and get bgfx_init() to return!
        glfwPollEvents();
        if (this.bgfxThread.didInitialize.await(16, TimeUnit.MILLISECONDS)) {
          break;
        }
      }
    } catch (InterruptedException ix) {
      GLX.error(ix, "GLX interrupted awaiting BGFX initialization");
    }
    this.bgfx = this.bgfxThread.bgfx;
    this.zZeroToOne = this.bgfx.zZeroToOne;
    this.program = this.bgfx.program;
    this.vg = this.bgfx.vg;

    // Have the BGFX build the UI
    this.bgfxThread.buildUI.countDown();
    try {
      this.bgfxThread.didBuildUI.await();
    } catch (InterruptedException ix) {
      GLX.error(ix, "GLX interrupted awaiting BGFX to buildUI");
    }
    this.ui = this.bgfxThread.ui;

    // Initialize LED frame buffer for the UI
    this.uiFrame = new LXEngine.Frame(this);
    this.engine.getFrameNonThreadSafe(this.uiFrame);
  }

  void toggleUIPerformanceDebug() {
    this.flagUIDebug = !this.flagUIDebug;
    log("UI thread performance logging " + (this.flagUIDebug ? "ON" : "OFF"));
  }

  public void run() {

    // Start the LX engine thread
    log("Starting LX Engine...");
    this.engine.setInputDispatch(this.inputDispatch);
    this.engine.start();

    // Start the render thread
    this.bgfxThread.run.countDown();

    // Enter the core event loop
    log("Bootstrap complete, running main loop.");
    eventLoop();

    // Stop the LX engine
    log("Stopping LX engine...");
    this.engine.stop();

    // TODO(mcslee): join the LX engine thread? make sure it's really
    // done before cleaning up the window assets? doesn't seem to be necessary...

    // Dispose the BGFX render thread and UI assets
    this.bgfxThread.dispose.set(true);
    this.bgfxThread.interrupt();
    try {
      this.bgfxThread.didDispose.await();
    } catch (InterruptedException ix) {
      GLX.error(ix, "Interrupted awaiting BGFX shutdown");
    }

    // Clean up after ourselves
    dispose();

    // Free the window callbacks and destroy the window
    glfwFreeCallbacks(this.window);
    glfwDestroyWindow(this.window);

    // Terminate GLFW and free the error callback
    glfwTerminate();
    glfwSetErrorCallback(null).free();

    // The program *should* end now, if not it means we hung a thread somewhere...
    log("Done with main thread, GLX shutdown complete. Thanks for playing. <3");
  }

  /**
   * Subclasses may override to create a custom structured UI
   *
   * @throws IOException if required UI assets could not be loaded
   * @return The instantiated UI object
   */
  protected UI buildUI() throws IOException {
    return new UI(this);
  }

  public int getRenderer() {
    return this.bgfx.getRenderer();
  }

  public boolean isOpenGL() {
    return this.bgfx.isOpenGL();
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

  public float getUIZoom() {
    return this.uiZoom;
  }

  public float getUIContentScaleX() {
    return this.systemContentScaleX * this.uiZoom;
  }

  public float getUIContentScaleY() {
    return this.systemContentScaleY * this.uiZoom;
  }

  public float getSystemContentScaleX() {
    return this.systemContentScaleX;
  }

  public float getSystemContentScaleY() {
    return this.systemContentScaleY;
  }

  private boolean ignoreClipboardError = false;

  private void initializeWindow() {
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

        LX._error("LWJGL", logMessage.toString());
      }
    });

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit()) {
      throw new RuntimeException("Unable to initialize GLFW");
    }

    // Grab uiZoom from preferences
    this.uiZoom = this.preferences.uiZoom.getValuef() / 100f;
    this.preferences.uiZoom.addListener((p) -> {
      setUIZoom(this.preferences.uiZoom.getValuef() / 100f);
    });

    // Configure GLFW
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);
    glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);
    glfwWindowHint(GLFW_RESIZABLE, this.flags.windowResizable ? GLFW_TRUE : GLFW_FALSE);

    // Detect window/framebuffer sizes and content scale
    try (MemoryStack stack = MemoryStack.stackPush()) {
      long primaryMonitor = glfwGetPrimaryMonitor();
      if (primaryMonitor == NULL) {
        error("Running on a system with no monitor, is this intended?");
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
      log("GLX monitorWorkarea: size(" + this.displayWidth + "x" + this.displayHeight + "), pos(x:" + this.displayX + ",y:" + this.displayY + ")");
    }

    // Ensure initial window bounds do not exceed the available display
    this.windowWidth = LXUtils.min(this.windowWidth, this.displayWidth);
    this.windowHeight = LXUtils.min(this.windowHeight, this.displayHeight);

    // Create GLFW window
    log("GLX createWindow: " + this.windowWidth + "x" + this.windowHeight);
    this.window = glfwCreateWindow(
      this.windowWidth,
      this.windowHeight,
      this.flags.windowTitle,
      NULL,
      NULL
    );
    if (this.window == NULL) {
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
      glfwGetWindowContentScale(this.window, xScale, yScale);
      this.systemContentScaleX = xScale.get(0);
      this.systemContentScaleY = yScale.get(0);
      log("GLX systemContentScale: " + this.systemContentScaleX + "x" + this.systemContentScaleY);

      // The window size is in terms of "OS window size" - best thought of
      // as an abstract setting which may or may not exactly correspond to
      // pixels (e.g. a Mac retina display may have 2x as many pixels)
      IntBuffer xSize = stack.mallocInt(1);
      IntBuffer ySize = stack.mallocInt(1);
      glfwGetWindowSize(this.window, xSize, ySize);
      this.windowWidth = xSize.get(0);
      this.windowHeight = ySize.get(0);
      log("GLX windowSize: " + this.windowWidth + "x" + this.windowHeight);

      // Restore window position if restored from preferences
      if (this.windowPosX >= 0 && this.windowPosY >= 0) {
        this.windowPosX = LXUtils.constrain(this.windowPosX, this.displayX, this.displayX + this.displayWidth - this.windowWidth);
        this.windowPosY = LXUtils.constrain(this.windowPosY, this.displayY, this.displayY + this.displayHeight - this.windowHeight);
        log("GLX setWindowPos: " + this.windowPosX + "," + this.windowPosY);
        glfwSetWindowPos(this.window, this.windowPosX, this.windowPosY);
      }

      // See what is in the framebuffer. A retina Mac probably supplies
      // 2x the dimensions on framebuffer relative to window.
      glfwGetFramebufferSize(this.window, xSize, ySize);
      this.frameBufferWidth = xSize.get(0);
      this.frameBufferHeight = ySize.get(0);
      log("GLX framebufferSize: " + this.frameBufferWidth + "x" + this.frameBufferHeight);

      // Okay, let's figure out how many "virtual pixels" the GLX UI should
      // be. Note that on a Mac with 2x retina display, contentScale will be
      // 2, but the framebuffer will have dimensions twice that of the window.
      // So we should end up with uiWidth/uiHeight matching the window.
      // But on Windows it's a different situation, if contentScale > 100%
      // then we're going to "scale down" our number of UI pixels and draw them
      // into a larger framebuffer.
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      log("GLX uiSize: " + this.uiWidth + "x" + this.uiHeight);

      // To make things even trickier... keep in mind that the OS specifies cursor
      // movement relative to its window size. We need to scale those onto our
      // virtual UI window size.
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      log("GLX cursorScale: " + this.cursorScaleX + "x" + this.cursorScaleY);

      // Set UI Zoom bounds based upon content scaling
      this.preferences.uiZoom.setRange((int) Math.ceil(100 / this.systemContentScaleX), 201);

      // TODO(mcslee): nanovg test
//      this.frameBufferWidth = this.windowWidth;
//      this.frameBufferHeight = this.windowHeight;
//      this.contentScaleX = 1.25f;
//      this.contentScaleY = 1.25f;
//      this.uiWidth = this.frameBufferWidth / this.contentScaleX;
//      this.uiHeight = this.frameBufferHeigh t / this.contentScaleY;
//      this.cursorScaleX = this.uiWidth / this.windowWidth;
//      this.cursorScaleY = this.uiHeight / this.windowHeight;

    }

    glfwSetWindowFocusCallback(this.window, (window, focused) -> {
      if (focused) {
        // Update the cursor position callback... if the window wasn't focused
        // and the user re-focused it with a click followed by mouse drag, then
        // the CursorPosCallback won't have had a chance to fire yet. So
        // we give it a kick whenever the window refocuses.
        try (MemoryStack stack = MemoryStack.stackPush()) {
          DoubleBuffer xPos = stack.mallocDouble(1);
          DoubleBuffer yPos = stack.mallocDouble(1);
          glfwGetCursorPos(this.window, xPos, yPos);
          this.inputDispatch.onFocus(xPos.get(0) * this.cursorScaleX, yPos.get(0) * this.cursorScaleY);
        }
      }
    });

    glfwSetWindowCloseCallback(this.window, (window) -> {
      if (!this.bgfxThread.hasFailed) {
        // Confirm that we really want to do it
        glfwSetWindowShouldClose(this.window, false);
        confirmChangesSaved("quit", () -> {
          glfwSetWindowShouldClose(this.window, true);
        });
      }
    });

    glfwSetWindowSizeCallback(this.window, (window, width, height) -> {
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
        glfwGetWindowPos(this.window, xPos, yPos);
        this.windowPosX = xPos.get();
        this.windowPosY = yPos.get();
      }
      this.preferences.setWindowSize(this.windowWidth, this.windowHeight, this.windowPosX, this.windowPosY);
    });

    glfwSetWindowPosCallback(this.window, (window, x, y) -> {
      this.windowPosX = x;
      this.windowPosY = y;
      this.preferences.setWindowPosition(this.windowPosX, this.windowPosY);
    });

    glfwSetWindowContentScaleCallback(this.window, (window, contentScaleX, contentScaleY) -> {
      this.systemContentScaleX = contentScaleX;
      this.systemContentScaleY = contentScaleY;
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      this.preferences.uiZoom.setRange((int) Math.ceil(100 / this.systemContentScaleX), 201);
      this.bgfxThread.resizeUI.set(true);
    });

    glfwSetFramebufferSizeCallback(this.window, (window, width, height) -> {
      this.frameBufferWidth = width;
      this.frameBufferHeight = height;
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
      this.cursorScaleX = this.uiWidth / this.windowWidth;
      this.cursorScaleY = this.uiHeight / this.windowHeight;
      this.bgfxThread.resizeFramebuffer.set(true);
    });

    glfwSetDropCallback(this.window, (window, count, names) -> {
      if (count == 1) {
        try {
          final File file = new File(GLFWDropCallback.getName(names, 0));
          if (file.exists() && file.isFile()) {
            if (file.getName().endsWith(".lxp")) {
              confirmChangesSaved("open project " + file.getName(), () -> {
                openProject(file);
              });
            } else if (file.getName().endsWith(".jar")) {
              importContentJar(file, true);
            }
          }
        } catch (Exception x) {
          error(x, "Exception on drop-file handler: " + x.getLocalizedMessage());
        }
      }
    });

    // Register input dispatching callbacks
    glfwSetKeyCallback(this.window, this.inputDispatch::glfwKeyCallback);

    glfwSetCharCallback(this.window, this.inputDispatch::glfwCharCallback);
    glfwSetCursorPosCallback(this.window, this.inputDispatch::glfwCursorPosCallback);
    glfwSetMouseButtonCallback(this.window, this.inputDispatch::glfwMouseButtonCallback);
    glfwSetScrollCallback(window, this.inputDispatch::glfwScrollCallback);

    // Initialize standard mouse cursors
    for (MouseCursor cursor : MouseCursor.values()) {
      cursor.initialize();
    }
  }

  private boolean setWindowSizeLimits = true;

  private void setUIZoom(float uiScale) {
    this.uiZoom = uiScale;
    this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / this.uiZoom;
    this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / this.uiZoom;
    this.cursorScaleX = this.uiWidth / this.windowWidth;
    this.cursorScaleY = this.uiHeight / this.windowHeight;
    this.vg.notifyContentScaleChanged();
    this.bgfxThread.resizeUI.set(true);
    this.setWindowSizeLimits = true;
  }

  protected void setWindowSize(int windowWidth, int windowHeight) {
    glfwSetWindowSize(this.window, windowWidth, windowHeight);
  }

  private void eventLoop() {

    while (!glfwWindowShouldClose(this.window)) {

      // Update window size limits
      if (this.setWindowSizeLimits) {
        this.setWindowSizeLimits = false;
        int minWindowWidth = (int) (MIN_WINDOW_WIDTH / this.cursorScaleX);
        int minWindowHeight = (int) (MIN_WINDOW_HEIGHT / this.cursorScaleY);
        glfwSetWindowSizeLimits(this.window, minWindowWidth, minWindowHeight, GLFW_DONT_CARE, GLFW_DONT_CARE);
        if (this.windowWidth < minWindowWidth || this.windowHeight < minWindowHeight) {
          glfwSetWindowSize(
            this.window,
            LXUtils.max(this.windowWidth, minWindowWidth),
            LXUtils.max(this.windowHeight, minWindowHeight)
          );
        }
      }

      // Poll for input events
      this.inputDispatch.poll();

      // Update mouse cursor if needed
      if (this.needsCursorUpdate) {
        final MouseCursor mc = this.mouseCursor;
        glfwSetCursor(this.window, (mc != null) ? mc.handle : 0);
        this.needsCursorUpdate = false;
      }

      // Copy something to the clipboard
      final String copyToClipboard = this._setSystemClipboardString;
      if (copyToClipboard != null) {
        glfwSetClipboardString(this.window, copyToClipboard);
        this._getSystemClipboardString = copyToClipboard;
        this._setSystemClipboardString = null;
      } else {
        this.ignoreClipboardError = true;
        String str = glfwGetClipboardString(NULL);
        this.ignoreClipboardError = false;
        if ((str != null) && !str.equals(this._getSystemClipboardString)) {
          this._getSystemClipboardString = str;
          this.clipboard.setItem(new LXTextValue(str), false);
        }
      }
    }
  }

  @Override
  public void dispose() {
    for (MouseCursor cursor : MouseCursor.values()) {
      cursor.dispose();
    }
    super.dispose();
  }

  public void setMouseCursor(MouseCursor mouseCursor) {
    if (this.mouseCursor != mouseCursor) {
      this.mouseCursor = mouseCursor;
      this.needsCursorUpdate = true;
    }
  }

  public void importContentJar(File file, boolean overwrite) {
    final File destination = new File(getMediaFolder(LX.Media.PACKAGES), file.getName());
    if (destination.exists()) {
      if (overwrite) {
        showConfirmDialog(
          file.getName() + " already exists in package folder, reinstall?",
          () -> { importContentJar(file, destination); }
        );
      } else {
        pushError(null, "Package file already exists: " + destination.getName());
      }
    } else {
      final LXClassLoader.Package existingPackage = this.registry.findPackage(file);
      if (existingPackage != null) {
        if (overwrite) {
          showConfirmDialog(
            existingPackage.getName() + " package already exists with filename " + existingPackage.getFileName() + ", replace?",
            () -> {
              this.registry.uninstallPackage(existingPackage, false);
              importContentJar(file, destination);
            }
          );
        } else {
          pushError(null, "Package already exists: " + existingPackage.getFileName());
        }
      } else {
        importContentJar(file, destination);
      }
    }
  }

  protected void importContentJar(File file, File destination) {
    log("Importing content JAR: " + destination.toString());
    if (this.registry.installPackage(file, true)) {
      this.engine.addTask(() -> {
        String message = "Installed package: " + destination.getName();
        if (this.registry.getClassLoader().hasDuplicateClasses()) {
          message += "\n\nDuplicate classes were found. See log for details.";
        }
        this.ui.contextualHelpText.setValue("New package imported into " + destination.getName());
        this.ui.showContextDialogMessage(message);
      });
    };
  }

  public void reloadContent() {
    this.registry.reloadContent();
    pushStatusMessage("External packages reloaded");
  }

  public void showSaveProjectDialog() {
    File project = getProject();
    if (project == null) {
      project =getMediaFile(LX.Media.PROJECTS, "default.lxp");
    }

    showSaveFileDialog(
      "Save Project",
      "Project File",
      new String[] { "lxp" },
      project.toString(),
      (path) -> { saveProject(new File(path)); }
    );
  }

  public void showOpenProjectDialog() {
    if (this.dialogShowing) {
      return;
    }

    final File project = (getProject() != null) ? getProject() :
      getMediaFile(LX.Media.PROJECTS, "default.lxp");

    confirmChangesSaved("open another project", () -> {
      showOpenFileDialog(
        "Open Project",
        "Project File",
        new String[] { "lxp" },
        project.toString(),
        (path) -> { openProject(new File(path), true); }
      );
    });
  }

  public void showSaveScheduleDialog() {
    showSaveFileDialog(
      "Save Schedule",
      "Schedule File",
      new String[] { "lxs" },
      getMediaFile(LX.Media.PROJECTS, "default.lxs").toString(),
      (path) -> { this.scheduler.saveSchedule(new File(path)); }
    );
  }

  public void showAddScheduleEntryDialog() {
    if (this.dialogShowing) {
      return;
    }
    showOpenFileDialog(
      "Add Project to Schedule",
      "Project File",
      new String[] { "lxp" },
      getMediaFile(LX.Media.PROJECTS, "default.lxp").toString(),
      (path) -> { this.scheduler.addEntry(new File(path)); }
    );
  }

  public void showOpenScheduleDialog() {
    if (this.dialogShowing) {
      return;
    }
    showOpenFileDialog(
      "Open Schedule",
      "Schedule File",
      new String[] { "lxs" },
      getMediaFile(LX.Media.PROJECTS, "default.lxs").toString(),
      (path) -> { this.scheduler.openSchedule(new File(path)); }
    );
  }

  public interface FileDialogCallback {
    public void fileDialogCallback(String path);
  }

  // Prevent stacking up multiple dialogs
  private volatile boolean dialogShowing = false;

  /**
   * Show a save file dialog
   *
   * @param dialogTitle Dialog title
   * @param fileType File type description
   * @param extensions Valid file extensions
   * @param defaultPath Default file path
   * @param success Callback on successful invocation
   */
  public void showSaveFileDialog(String dialogTitle, String fileType, String[] extensions, String defaultPath, FileDialogCallback success) {
    if (this.dialogShowing) {
      return;
    }
    new Thread(() -> {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        PointerBuffer aFilterPatterns = stack.mallocPointer(extensions.length);
        for (String extension : extensions) {
          aFilterPatterns.put(stack.UTF8("*." + extension));
        }
        aFilterPatterns.flip();
        dialogShowing = true;
        String path = tinyfd_saveFileDialog(
          dialogTitle,
          defaultPath,
          aFilterPatterns,
          fileType + " (*." + String.join("/", extensions) + ")"
        );
        dialogShowing = false;
        if (path != null) {
          final int dot = path.lastIndexOf('.');
          final int separator = path.lastIndexOf(File.separatorChar);
          if (dot < 0 || dot < separator) {
            path = path + "." + extensions[0];
          } else if (dot == path.length() - 1) {
            path = path + extensions[0];
          }
          final String finalPath = path;
          engine.addTask(() -> {
            success.fileDialogCallback(finalPath);
          });
        } else {
          pushError("Invalid file name or no file selected, the file was not saved.");
        }
      }
    }, "Save File Dialog").start();
  }

  /**
   * Show an open file dialog
   *
   * @param dialogTitle Dialog title
   * @param fileType File type description
   * @param extensions Valid file extensions
   * @param defaultPath Default file path
   * @param success Callback on successful invocation
   */
  public void showOpenFileDialog(String dialogTitle, String fileType, String[] extensions, String defaultPath, FileDialogCallback success) {
    if (this.dialogShowing) {
      return;
    }
    new Thread(() -> {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        PointerBuffer aFilterPatterns = stack.mallocPointer(extensions.length);
        for (String extension : extensions) {
          aFilterPatterns.put(stack.UTF8("*." + extension));
        }
        aFilterPatterns.flip();
        dialogShowing = true;
        String path = tinyfd_openFileDialog(
          dialogTitle,
          defaultPath,
          aFilterPatterns,
          fileType + " (*." + String.join("/", extensions) + ")",
          false
        );
        dialogShowing = false;
        if (path != null) {
          engine.addTask(() -> {
            success.fileDialogCallback(path);
          });
        }
      }
    }, "Open File Dialog").start();
  }

  @Override
  protected void showConfirmUnsavedProjectDialog(String message, Runnable confirm) {
    showConfirmDialog(
      "Your project has unsaved changes, really " + message + "?",
      confirm
    );
  }

  @Override
  protected void showConfirmUnsavedModelDialog(File file, Runnable confirm) {
    showConfirmDialog(
      "You have modified the imported model file " + file.getName() +", do you want to export the changes you have made to this model?",
      confirm
    );
  }

  @Override
  public void showConfirmDialog(String message, Runnable confirm) {
    this.ui.showContextOverlay(new UIDialogBox(this.ui,
      message,
      new String[] { "No", "Yes" },
      new Runnable[] { null, confirm }
    ));
  }

  private String _getSystemClipboardString = null;
  private String _setSystemClipboardString = null;

  @Override
  public void setSystemClipboardString(String str) {
    this._setSystemClipboardString = str;
  }

  private static final String GLX_PREFIX = "GLX";

  public static void log(String message) {
    LX._log(GLX_PREFIX, message);
  }

  public static void error(Exception x, String message) {
    LX._error(GLX_PREFIX, x, message);
  }

  public static void error(String message) {
    LX._error(GLX_PREFIX, message);
  }

}
