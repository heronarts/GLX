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

import static org.lwjgl.util.tinyfd.TinyFileDialogs.*;

import java.io.File;
import java.io.IOException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import heronarts.glx.shader.Phong;
import heronarts.glx.shader.Tex2d;
import heronarts.glx.shader.Text3d;
import heronarts.glx.shader.UniformFill;
import heronarts.glx.shader.VertexFill;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIDialogBox;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXClassLoader;
import heronarts.lx.LXEngine;
import heronarts.lx.clipboard.LXTextValue;
import heronarts.lx.model.LXModel;

public class GLX extends LX {

  public final class Programs {

    public final Tex2d tex2d;
    public final UniformFill uniformFill;
    public final VertexFill vertexFill;
    public final Phong phong;
    public final Text3d text3d;

    public Programs() {
      this.tex2d = new Tex2d(GLX.this);
      this.uniformFill = new UniformFill(GLX.this);
      this.vertexFill = new VertexFill(GLX.this);
      this.phong = new Phong(GLX.this);
      this.text3d = new Text3d(GLX.this);
    }

    public void dispose() {
      this.tex2d.dispose();
      this.uniformFill.dispose();
      this.vertexFill.dispose();
      this.phong.dispose();
      this.text3d.dispose();
    }
  }

  /**
   * Flags that control the behavior of the application
   */
  public static class Flags extends LX.Flags {
    public int windowWidth = -1;
    public int windowHeight = -1;
    public boolean windowResizable = true;
    public String windowTitle = "GLX";
    public boolean useOpenGL = false;
  }

  public final Flags flags;

  /**
   * The window that runs this application
   */
  public final GLXWindow window;

  /**
   * BGFX rendering engine
   */
  public final BGFXEngine bgfx;

  /**
   * The Vector Graphics implementation
   */
  public final VGraphics vg;

  /**
   * Publicly accessible, globally reusable shader programs.
   */
  public final Programs program;

  /**
   * The UI stack
   */
  public final UI ui;

  public final LXEngine.Frame uiFrame;

  boolean flagUIDebug = false;

  protected GLX(GLXWindow window) throws IOException {
    this(window, window.flags);
  }

  protected GLX(GLXWindow window, Flags flags) throws IOException {
    this(window, flags, null);
  }

  protected GLX(GLXWindow window, Flags flags, LXModel model) throws IOException {
    super(window.preferences, flags, model);
    this.window = window;
    this.flags = flags;

    // Register ourselves as delegate for window events
    this.window.setDelegate(new WindowDelegate());
    this.window.inputDispatch.setGLX(this);

    // Construct the BGFX instance
    this.bgfx = new BGFXEngine(this);
    this.program = new Programs();
    this.vg = new VGraphics(this);

    // Build the application UI
    this.ui = buildUI();

    // Initialize LED frame buffer for the UI
    this.uiFrame = new LXEngine.Frame(this);
    this.engine.getFrameNonThreadSafe(this.uiFrame);
  }

  private class WindowDelegate implements GLXWindow.Delegate {

    @Override
    public void setClipboardText(GLXWindow window, String clipboardText) {
      clipboard.setItem(new LXTextValue(clipboardText), false);
    }

    @Override
    public void onWindowClose(GLXWindow window) {
      if (!bgfx.hasFailed) {
        window.setShouldClose(false);
        // Confirm that we really want to do it
        confirmChangesSaved("quit", () -> window.setShouldClose(true));
      }
    }

    @Override
    public void onZoomChanged(GLXWindow window, float uiZoom) {
      vg.notifyContentScaleChanged();
      bgfx.resizeUI.set(true);
    }

    @Override
    public void onContentScaleChanged(GLXWindow window, float contentScaleX, float contentScaleY) {
      bgfx.resizeUI.set(true);
    }

    @Override
    public void onFramebufferSizeChanged(GLXWindow window, float framebufferWidth, float framebufferHeight) {
      bgfx.resizeFramebuffer.set(true);
    }

    @Override
    public void onDropFile(GLXWindow window, String fileName) {
      try {
        final File file = new File(fileName);
        if (file.exists() && file.isFile()) {
          if (file.getName().endsWith(".lxp")) {
            engine.addTask(() -> {
              confirmChangesSaved("open project " + file.getName(), () -> openProject(file));
            });
          } else if (file.getName().endsWith(".jar")) {
            engine.addTask(() -> {
              importContentJar(file, true);
            });
          }
        }
      } catch (Exception x) {
        error(x, "Exception in drop-file handler: " + x.getLocalizedMessage());
      }
    }

    @Override
    public void onShutdown(GLXWindow window) {
      if (Thread.currentThread() == bgfx.thread) {
        throw new IllegalThreadStateException("BGFX thread may not shutdown itself, shutdown should come from GLXWindow");
      }

      // Signal to the BGFX thread that it should shutdown
      bgfx.shutdown = true;

      // Notify in case bgfx has failed and is in wait()
      synchronized (bgfx.thread) { bgfx.thread.notify(); }

      // Ensure the thread has finished
      try {
        bgfx.thread.join();
      } catch (InterruptedException ix) {
        error(ix, "Interrupted awaiting BGFX shutdown");
      }
    }
  }

  void toggleUIPerformanceDebug() {
    this.flagUIDebug = !this.flagUIDebug;
    log("UI thread performance logging " + (this.flagUIDebug ? "ON" : "OFF"));
  }

  public void assertBgfxThreadAllocation(BGFXEngine.Resource resource) {
    assertBgfxThread(resource.getClass().getName() + " must be created on the BGFX thread");
  }

  public void assertBgfxThreadUpdate(BGFXEngine.Resource resource) {
    assertBgfxThread(resource.getClass().getName() + " may only be updated on the BGFX thread");
  }

  public void assertBgfxThreadDispose(BGFXEngine.Resource resource) {
    assertBgfxThread(resource.getClass().getName() + " must be disposed on the BGFX thread");
  }

  public void assertBgfxThread(String error) {
    if (Thread.currentThread() != this.bgfx.thread) {
      throw new IllegalThreadStateException(error);
    }
  }

  /**
   * Returns true if we are on the BGFX thread and can immediately dispose
   * of this resource. Otherwise it is scheduled to run later on the BGFX thread
   * at which point this call will succeed.
   *
   * @param resource Resource
   * @return true if the dispose code should run now
   */
  public boolean bgfxThreadDispose(BGFXEngine.Resource resource) {
    if (Thread.currentThread() != this.bgfx.thread) {
      debug(resource.getClass().getName() + ".dispose() queued to run on BGFX thread");
      this.bgfx.threadSafeDisposeQueue.add(resource);
      return false;
    }
    return true;
  }

  public void run() {

    // Start the LX engine thread
    log("Starting LX Engine...");
    this.engine.setInputDispatch(this.window.inputDispatch);
    this.engine.start();

    // Start the GLFW window main event polling loop
    this.window.start();

    // Enter the core event loop
    log("Running main BGFX loop...");
    this.bgfx.mainLoop();

    // We're ka-put, shut it all down.
    log("Stopping LX engine...");
    this.engine.stop();

    // Clean up the LX instance
    log("Disposing GLX...");
    dispose();
    log("GLX disposed.");

    // Dispose of BGFX graphics assets
    this.vg.dispose();
    this.program.dispose();
    this.bgfx.dispose();
    log(bgfx.thread.getName() + " finished.");
  }

  @Override
  public void dispose() {
    // NOTE: destroy the whole UI first, rip down all the listeners
    // before disposing of the engine itself. Done on the BGFX thread
    // to properly dispose of BGFX resources.
    log("Disposing GLX.UI...");
    this.ui.dispose();
    log("GLX.UI disposed.");

    // Now dispose of LX itself
    super.dispose();
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

  @Override
  public void setSystemClipboardString(String str) {
    this.window.setSystemClipboardString(str);
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

  protected static void _error(String prefix, String message) {
    _log(System.err, null, prefix, message);
  }

}
