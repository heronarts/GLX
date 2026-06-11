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

import static org.lwjgl.bgfx.BGFX.BGFX_INVALID_HANDLE;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWayland;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import heronarts.lx.LXPreferences;
import heronarts.lx.LXPreferences.WindowSettings;
import heronarts.lx.utils.LXUtils;
import heronarts.glx.ui.UI;

/**
 * The WindowEngine class takes care of running a windowed application using GLFW.
 * This *must* to be run on the main thread (on Mac the JVM will have to be started
 * with -XstartOnFirstThread to ensure this).
 *
 * This class therefore *owns* the main thread, and its main() method is in charge
 * of the overall program lifecycle.
 */
public class WindowEngine {

  public interface Delegate {
    public void setClipboardText(WindowEngine windowEngine, String clipboardText);
    public void onWindowClose(WindowEngine windowEngine, Window window);
    public void onZoomChanged(WindowEngine windowEngine, float uiZoom);
    public void onContentScaleChanged(WindowEngine windowEngine, Window window, float contentScaleX, float contentScaleY);
    public void onFramebufferSizeChanged(WindowEngine windowEngine, Window window, float framebufferWidth, float framebufferHeight);
    public void onDropFile(WindowEngine windowEngine, String fileName);
    public void onShutdown(WindowEngine windowEngine);
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

  private static final int MIN_WINDOW_WIDTH_MAIN = 820;
  private static final int MIN_WINDOW_HEIGHT_MAIN = 480;

  private static final int MIN_WINDOW_WIDTH_ALT = 200;
  private static final int MIN_WINDOW_HEIGHT_ALT = 200;

  private static final int DEFAULT_WINDOW_WIDTH = 1280;
  private static final int DEFAULT_WINDOW_HEIGHT = 720;

  private Delegate delegate;

  private final Thread thread;

  // Current monitors
  private MonitorConfiguration monitorConfig;

  // Current windows
  public final MainWindow mainWindow;
  public final AltWindow altWindow;

  private volatile boolean showAltWindow;
  private final AtomicBoolean needsAltVisibilityUpdate = new AtomicBoolean(true);

  private volatile MouseCursor mouseCursor = null;
  private final AtomicBoolean needsCursorUpdate = new AtomicBoolean(false);

  private volatile MouseCursor mouseCursorAlt = null;
  private final AtomicBoolean needsCursorUpdateAlt = new AtomicBoolean(false);

  private float uiZoom = 1;

  private boolean ignoreClipboardError = false;
  private final AtomicBoolean setWindowSizeLimits = new AtomicBoolean(true);

  final InputDispatch inputDispatch = new InputDispatch(this);

  private final CountDownLatch isReady = new CountDownLatch(1);

  public final GLX.Flags flags;
  public final LXPreferences preferences;

  public WindowEngine(GLX.Flags flags) {
    this.thread = Thread.currentThread();
    this.flags = flags;
    this.preferences = new LXPreferences(flags);

    // Get initial window size from preferences
    if (flags.loadPreferences) {
      this.preferences.loadWindowSettings();
    }

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

    // Read size and position of all monitors
    refreshMonitors();

    // Create windows
    this.mainWindow = new MainWindow();
    this.altWindow = new AltWindow();

    // Set UI Zoom bounds based upon content scaling
    _updateUIZoomRange();

    // Initialize standard mouse cursors
    for (MouseCursor cursor : MouseCursor.values()) {
      cursor.initialize();
    }
  }

  public Window getWindow(long handle) {
    if (handle == this.altWindow.handle) {
      return this.altWindow;
    }
    if (handle == this.mainWindow.handle) {
      return this.mainWindow;
    }
    GLX.error("No GLX Window exists for handle: " + handle);
    return null;
  }

  private void refreshMonitors() {
    GLX.log("Refreshing monitors...");
    List<Monitor> list = new ArrayList<>();
    try (MemoryStack stack = MemoryStack.stackPush()) {
      final long primaryMonitor = glfwGetPrimaryMonitor();
      if (primaryMonitor == NULL) {
        GLX.error("Running on a system with no monitor, is this intended?");
      } else {
        PointerBuffer monitors = glfwGetMonitors();
        if (monitors != null) {
          for (int i = 0; i < monitors.limit(); i++) {
            long handle = monitors.get(i);
            if (handle == NULL) {
              continue;
            }
            final boolean isPrimary = (handle == primaryMonitor);
            final String label = "Monitor " + (i + 1);
            Monitor m = new Monitor(handle, isPrimary, label);
            if (!m.hasError()) {
              GLX.log("  " + label + ": " + m + (isPrimary ? "  *Primary" : ""));
              list.add(m);
            }
          }
        }
      }
    }

    // Create monitor configuration
    MonitorConfiguration monitorConfig = new MonitorConfiguration(list);
    if (this.monitorConfig == null || !this.monitorConfig.equals(monitorConfig)) {
      this.monitorConfig = new MonitorConfiguration(list);
    }
  }

  private void assertMainThread() {
    if (Thread.currentThread() != this.thread) {
      throw new IllegalThreadStateException("WindowEngine method may only be called from main thread");
    }
  }

  private void _updateUIZoom(float uiScale) {
    this.uiZoom = uiScale;
    this.mainWindow.updateUIZoom(uiScale);
    this.altWindow.updateUIZoom(uiScale);
    this.setWindowSizeLimits.set(true);
    if (this.delegate != null) {
      this.delegate.onZoomChanged(this, uiScale);
    }
  }

  private void _updateUIZoomRange() {
    this.preferences.uiZoom.setRange((int) Math.ceil(100 / this.mainWindow.systemContentScaleX), 201);
  }

  void setDelegate(Delegate delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("WindowEngine.setDelegate() may not be passed null");
    }
    if (this.delegate != null) {
      throw new IllegalStateException("WindowEngine.setDelegate() may only be called once");
    }
    this.delegate = delegate;
  }

  public float getUIZoom() {
    return this.uiZoom;
  }

  public void showAltWindow(boolean visible) {
    this.showAltWindow = visible;
    this.needsAltVisibilityUpdate.set(true);
  }

  protected void setShouldClose(boolean shouldClose) {
    glfwSetWindowShouldClose(this.mainWindow.handle, shouldClose);
  }

  protected void setWindowSize(int windowWidth, int windowHeight) {
    assertMainThread();
    glfwSetWindowSize(this.mainWindow.handle, windowWidth, windowHeight);
  }

  public void setMouseCursor(UI.Window window, MouseCursor mouseCursor) {
    switch (window) {
      case MAIN -> {
        if (this.mouseCursor != mouseCursor) {
          this.mouseCursor = mouseCursor;
          this.needsCursorUpdate.set(true);
        }
      }
      case ALT -> {
        if (this.mouseCursorAlt != mouseCursor) {
          this.mouseCursorAlt = mouseCursor;
          this.needsCursorUpdateAlt.set(true);
        }
      }
    }
  }

  private String _getSystemClipboardString = null;
  private volatile String _setSystemClipboardString = null;

  void setSystemClipboardString(String str) {
    this._setSystemClipboardString = str;
  }

  public void main() {
    GLX.log("WindowEngine.main() awaiting GLX boostrap...");
    try {
      while (true) {
        // NB: this poll call seems to be *necessary* to kick GLFW and get bgfx_init() to return! (on MacOS at least)
        glfwPollEvents();
        if (this.isReady.await(16, TimeUnit.MILLISECONDS)) {
          break;
        }
      }
    } catch (InterruptedException ix) {
      GLX.error(ix, "WindowEngine.main() interrupted awaiting BGFX initialization");
    }

    if (this.delegate == null) {
      throw new IllegalStateException("WindowEngine cannot continue past bootstrapping with no GLX delegate set");
    }

    GLX.log("GLX boostrap complete, WindowEngine running event loop...");
    eventLoop();

    GLX.log("WindowEngine closed, shutting down...");
    shutdown();
  }

  void start() {
    this.isReady.countDown();
  }

  private void eventLoop() {
    // Okay now we're into the real polling loop!
    while (!glfwWindowShouldClose(this.mainWindow.handle)) {
      // Update window visibility
      if (this.needsAltVisibilityUpdate.compareAndSet(true, false)) {
        if (this.showAltWindow) {
          this.altWindow.show();
        } else {
          this.altWindow.hide();
        }
      }

      // Update window size limits
      if (this.setWindowSizeLimits.compareAndSet(true, false)) {
        this.mainWindow.setSizeLimits();
        this.altWindow.setSizeLimits();
      }

      // Poll for input events
      this.inputDispatch.poll();

      // Update mouse cursor if needed
      if (this.needsCursorUpdate.compareAndSet(true, false)) {
        final MouseCursor mc = this.mouseCursor;
        glfwSetCursor(this.mainWindow.handle, (mc != null) ? mc.handle : 0);
      }
      if (this.needsCursorUpdateAlt.compareAndSet(true, false)) {
        final MouseCursor mc = this.mouseCursorAlt;
        glfwSetCursor(this.altWindow.handle, (mc != null) ? mc.handle : 0);
      }

      // Copy something to the clipboard
      final String copyToClipboard = this._setSystemClipboardString;
      if (copyToClipboard != null) {
        glfwSetClipboardString(this.mainWindow.handle, copyToClipboard);
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

    // Free the window callbacks and destroy the windows
    GLX.log("Destroying main thread GLFW windows...");
    this.altWindow.destroy();
    this.mainWindow.destroy();

    // Terminate GLFW and free the error callback
    glfwTerminate();
    glfwSetErrorCallback(null).free();

    // The program *should* end now, if not it means we hung a thread somewhere...
    GLX.log("Done with main thread, GLX shutdown complete. Thanks for playing. <3");
  }

  /**
   * Represents a single window in the application
   */
  public abstract class Window extends LXPreferences.WindowSettings {

    // GLFW handle
    final long handle;

    // Preferences key for saving
    private final LXPreferences.Window key;

    // BGFX base view id
    public final short baseViewId;

    // Specs for a monitor previously used with this window
    // JKB note: Not yet implemented
    private boolean hasSavedMonitor = false;
    private int lastDisplayWidth = -1;
    private int lastDisplayHeight = -1;

    // Current monitor used by this window
    Monitor monitor = null;

    // Scale-related variables

    int frameBufferWidth = 0;
    int frameBufferHeight = 0;

    float systemContentScaleX = 1;
    float systemContentScaleY = 1;

    float cursorScaleX = 1;
    float cursorScaleY = 1;

    float uiWidth = 0;
    float uiHeight = 0;

    private Window(LXPreferences.Window key, short baseViewId, String title) {
      super(key);
      setSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);

      this.key = key;
      this.baseViewId = baseViewId;
      GLX.log("Creating window " + this.key);

      // Initialize size & position from preferences and current monitors
      locate();

      // Create GLFW window
      this.handle = create(title);

      // Detect content scale from framebuffer size and window size
      initContentScale();

      // Register GLFW callbacks
      registerCallbacks();
    }

    public final boolean isMain() {
      return this == mainWindow;
    }

    public final boolean isAlt() {
      return this == altWindow;
    }

    public long getNativeHandle() {
      return switch (Platform.get()) {
        case LINUX, FREEBSD ->
          (glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) ?
            GLFWNativeWayland.glfwGetWaylandWindow(this.handle) :
            GLFWNativeX11.glfwGetX11Window(this.handle);
        case MACOSX -> GLFWNativeCocoa.glfwGetCocoaWindow(this.handle);
        case WINDOWS -> GLFWNativeWin32.glfwGetWin32Window(this.handle);
      };
    }

    private void locate() {
      // Retrieve window size and position from preferences
      WindowSettings settings = preferences.getWindowSettings(this.key);
      if (settings == null) {
        GLX.error("Failed to load window settings for window " + this.key);
      } else {
        if (settings.hasPosition()) {
          setPosition(settings.getX(), settings.getY());
        }
        if (settings.hasSize()) {
          setSize(settings.getWidth(), settings.getHeight());
        }
        GLX.log("  Settings: " + toString());
      }

      this.monitor = null;
      if (hasPosition()) {
        // Attempt to place window at last known position

        // Find monitor that contains our upper left corner
        for (Monitor m : monitorConfig.monitors) {
          if (m.contains(getX(), getY())) {
            this.monitor = m;
            GLX.log("  Matched window position to " + this.monitor.label);
            break;
          }
        }

        // If our position no longer falls on a monitor, find a monitor that closely matches the dimensions of our last monitor
        // JKB note: Not yet implemented. Would need to save/reload monitor in preferences.
        if (this.monitor == null && this.hasSavedMonitor) {
          for (Monitor m : monitorConfig.monitors) {
            if (m.getWidth() == this.lastDisplayWidth && m.getHeight() == this.lastDisplayHeight) {
              this.monitor = m;
              GLX.log("  Matched window position to similar monitor: " + this.monitor.label);
              break;
            }
          }
        }

        // Ensure initial window is fully contained within the monitor work area
        if (this.monitor != null) {
          if (exceeds(this.monitor)) {
            constrain(this.monitor);
            GLX.log("    ..modified to fit on monitor: " + toString());
          }
        }

      } else {
        // Default: center the window on the primary monitor
        if (!monitorConfig.monitors.isEmpty()) {
          this.monitor = monitorConfig.monitors.getFirst();
          int width = LXUtils.min(getWidth(), this.monitor.getWidth());
          int height = LXUtils.min(getHeight(), this.monitor.getHeight());
          setSize(width, height);
          int x = (this.monitor.getWidth() - width) / 2;
          int y = (this.monitor.getHeight() - height) / 2;
          setPosition(x, y);
          GLX.log("  Using default window location");
        }
      }
    }

    private long create(String title) {
      GLX.log("  createWindow: size(" + getWidth() + "x" + getHeight() + ")");
      long handle = glfwCreateWindow(
        getWidth(),
        getHeight(),
        title,
        NULL,
        NULL
      );
      if (handle == NULL) {
        throw new RuntimeException("Failed to create the GLFW window");
      }

      try (MemoryStack stack = MemoryStack.stackPush()) {
        // Note: if a target monitor was found, size+position have already been adjusted to be compatible

        // Set window position on the virtual screen (which might be on a different monitor)
        GLX.log("  setWindowPos: " + getX() + "," + getY());
        glfwSetWindowPos(handle, getX(), getY());

        // Determine if window size was shrunk during creation
        IntBuffer xSize = stack.mallocInt(1);
        IntBuffer ySize = stack.mallocInt(1);
        glfwGetWindowSize(handle, xSize, ySize);
        if (getWidth() != xSize.get(0) || getHeight() != ySize.get(0)) {

          // Attempt to restore desired window dimensions, now that we're on the right monitor
          glfwSetWindowSize(handle, getWidth(), getHeight());

          // Check the window size again, hopefully it changed
          glfwGetWindowSize(handle, xSize, ySize);
          int gX = xSize.get(0);
          int gY = ySize.get(0);
          if (getWidth() != gX || getHeight() != gY) {
            if (gX != NULL && gY != NULL) {
              setSize(gX, gY);
            }
            GLX.log("    desired size not available, new size: " + getWidth() + "x" + getHeight());
          }
        }
      }

      return handle;
    }

    private void initContentScale() {
      // Detect window/framebuffer sizes and content scale
      try (MemoryStack stack = MemoryStack.stackPush()) {

        // The window size is in terms of "OS window size" - best thought of
        // as an abstract setting which may or may not exactly correspond to
        // pixels (e.g. a Mac retina display may have 2x as many pixels)

        // NOTE: apparently been observed in the wild that the window may end up too big to fit,
        // (email exchange w/ jkbelcher june 4 2025), check again here after setting position
        // that it's been fixed?
        // JKB note 11-20-25: likely this is fixed now that we're checking each monitor

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
        GLX.log("  systemContentScale: " + this.systemContentScaleX + "x" + this.systemContentScaleY);

        // See what is in the framebuffer. A retina Mac probably supplies
        // 2x the dimensions on framebuffer relative to window.
        IntBuffer xSize = stack.mallocInt(1);
        IntBuffer ySize = stack.mallocInt(1);
        glfwGetFramebufferSize(this.handle, xSize, ySize);
        this.frameBufferWidth = xSize.get(0);
        this.frameBufferHeight = ySize.get(0);
        GLX.log("  framebufferSize: " + this.frameBufferWidth + "x" + this.frameBufferHeight);

        // Okay, let's figure out how many "virtual pixels" the GLX UI should
        // be. Note that on a Mac with 2x retina display, contentScale will be
        // 2, but the framebuffer will have dimensions twice that of the window.
        // So we should end up with uiWidth/uiHeight matching the window.
        // But on Windows it's a different situation, if contentScale > 100%
        // then we're going to "scale down" our number of UI pixels and draw them
        // into a larger framebuffer.
        this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / uiZoom;
        this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / uiZoom;
        GLX.log("  uiSize: " + this.uiWidth + "x" + this.uiHeight);

        // To make things even trickier... keep in mind that the OS specifies cursor
        // movement relative to its window size. We need to scale those onto our
        // virtual UI window size.
        this.cursorScaleX = this.uiWidth / getWidth();
        this.cursorScaleY = this.uiHeight / getHeight();
        GLX.log("  cursorScale: " + this.cursorScaleX + "x" + this.cursorScaleY);
      }
    }

    private void registerCallbacks() {
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
            inputDispatch.onFocus(this,xPos.get(0) * this.cursorScaleX, yPos.get(0) * this.cursorScaleY);
          }
        }
      });

      glfwSetWindowCloseCallback(this.handle, (window) -> {
        if (delegate != null) {
          delegate.onWindowClose(WindowEngine.this, this);
        }
      });

      glfwSetWindowSizeCallback(this.handle, (window, width, height) -> {
        // NOTE(mcslee): This call should *follow* a call from glfwSetFramebufferSizeCallback, the window
        // properties change after the underlying framebuffer
        setSize(width, height);
        this.cursorScaleX = this.uiWidth / getWidth();
        this.cursorScaleY = this.uiHeight / getHeight();
        try (MemoryStack stack = MemoryStack.stackPush()) {
          // NOTE(mcslee): need to grab the new window position here as well! If a top or left
          // corner of the window is used for a drag-resize operation, then the window's X or Y
          // position can change without a glfwSetWindowPosCallback being invoked from a window
          // move operation
          IntBuffer xPos = stack.mallocInt(1);
          IntBuffer yPos = stack.mallocInt(1);
          glfwGetWindowPos(this.handle, xPos, yPos);
          setPosition(xPos.get(), yPos.get());
        }
        preferences.setWindowSettings(this.key, getWidth(), getHeight(), getX(), getY());
      });

      glfwSetWindowPosCallback(this.handle, (window, x, y) -> {
        setPosition(x, y);
        preferences.setWindowPosition(this.key, x, y);
      });

      glfwSetWindowContentScaleCallback(this.handle, (window, contentScaleX, contentScaleY) -> {
        this.systemContentScaleX = contentScaleX;
        this.systemContentScaleY = contentScaleY;
        this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / uiZoom;
        this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / uiZoom;
        this.cursorScaleX = this.uiWidth / getWidth();
        this.cursorScaleY = this.uiHeight / getHeight();
        _updateUIZoomRange();
        if (delegate != null) {
          delegate.onContentScaleChanged(WindowEngine.this, this, contentScaleX, contentScaleY);
        }
      });

      glfwSetFramebufferSizeCallback(this.handle, (window, width, height) -> {
        this.frameBufferWidth = width;
        this.frameBufferHeight = height;
        this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / uiZoom;
        this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / uiZoom;
        this.cursorScaleX = this.uiWidth / getWidth();
        this.cursorScaleY = this.uiHeight / getHeight();
        if (delegate != null) {
          delegate.onFramebufferSizeChanged(WindowEngine.this, this, width, height);
        }
      });

      glfwSetDropCallback(this.handle, (window, count, names) -> {
        if (count == 1) {
          if (delegate != null) {
            delegate.onDropFile(WindowEngine.this, GLFWDropCallback.getName(names, 0));
          }
        }
      });

      // Register input dispatching callbacks
      glfwSetKeyCallback(this.handle, inputDispatch::glfwKeyCallback);
      glfwSetCharCallback(this.handle, inputDispatch::glfwCharCallback);
      glfwSetCursorPosCallback(this.handle, inputDispatch::glfwCursorPosCallback);
      glfwSetMouseButtonCallback(this.handle, inputDispatch::glfwMouseButtonCallback);
      glfwSetScrollCallback(this.handle, inputDispatch::glfwScrollCallback);
    }

    void setSizeLimits() {
      final int minWindowWidth = (int) (getMinWidth() / this.cursorScaleX);
      final int minWindowHeight = (int) (getMinHeight() / this.cursorScaleY);
      glfwSetWindowSizeLimits(this.handle, minWindowWidth, minWindowHeight, GLFW_DONT_CARE, GLFW_DONT_CARE);
      if (getWidth() < minWindowWidth || getHeight() < minWindowHeight) {
        glfwSetWindowSize(
          this.handle,
          LXUtils.max(getWidth(), minWindowWidth),
          LXUtils.max(getHeight(), minWindowHeight)
        );
      }
    }

    abstract protected int getMinWidth();

    abstract protected int getMinHeight();

    void updateUIZoom(float uiZoom) {
      this.uiWidth = this.frameBufferWidth / this.systemContentScaleX / uiZoom;
      this.uiHeight = this.frameBufferHeight / this.systemContentScaleY / uiZoom;
      this.cursorScaleX = this.uiWidth / getWidth();
      this.cursorScaleY = this.uiHeight / getHeight();
    }

    public long getHandle() {
      return this.handle;
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
      return this.systemContentScaleX * uiZoom;
    }

    public float getUIContentScaleY() {
      return this.systemContentScaleY * uiZoom;
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

    public abstract short getFrameBuffer();

    protected void destroy() {
      GLX.log("  destroying window " + this.key);
      glfwFreeCallbacks(this.handle);
      glfwDestroyWindow(this.handle);
    }

  }

  // Base viewIds for each window
  private static final short BASE_VIEW_ID_MAIN = (short) 0;
  private static final short BASE_VIEW_ID_ALT = (short) 100;

  public class MainWindow extends Window {

    private MainWindow() {
      super(LXPreferences.Window.MAIN, BASE_VIEW_ID_MAIN, flags.windowTitle);
    }

    @Override
    protected int getMinWidth() {
      return MIN_WINDOW_WIDTH_MAIN;
    }

    @Override
    protected int getMinHeight() {
      return MIN_WINDOW_HEIGHT_MAIN;
    }

    @Override
    public short getFrameBuffer() {
      return BGFX_INVALID_HANDLE;
    }

  }

  public class AltWindow extends Window {

    private AltWindow() {
      super(LXPreferences.Window.ALT, BASE_VIEW_ID_ALT, "Timeline");
      // Hide until we are loaded and confirmed visible
      hide();
    }

    @Override
    protected int getMinWidth() {
      return MIN_WINDOW_WIDTH_ALT;
    }

    @Override
    protected int getMinHeight() {
      return MIN_WINDOW_HEIGHT_ALT;
    }

    private void show() {
      assertMainThread();
      glfwShowWindow(this.handle);
    }

    private void hide() {
      assertMainThread();
      glfwHideWindow(this.handle);
    }

    // BGFX frame buffer for secondary window
    private short frameBufferHandle = BGFX_INVALID_HANDLE;

    public AltWindow setFrameBuffer(short framebuffer) {
      this.frameBufferHandle = framebuffer;
      return this;
    }

    @Override
    public short getFrameBuffer() {
      return this.frameBufferHandle;
    }
  }
}
