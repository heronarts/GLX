/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui;

import heronarts.glx.GLX;
import heronarts.glx.View;
import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.component.UIContextMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.LXMappingEngine;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiMapping;
import heronarts.lx.modulation.LXModulationEngine;
import heronarts.lx.modulation.LXParameterModulation;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.MutableParameter;
import heronarts.lx.parameter.StringParameter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.bgfx.BGFX.*;


/**
 * Top-level container for all overlay UI elements.
 */
public class UI {

  public enum CoordinateSystem {
    LEFT_HANDED,
    RIGHT_HANDED;
  }

  private static UI instance = null;
  public static Thread thread;

  private class UIRoot extends UIObject implements UIContainer {

    private final View viewClear;
    private final View view2d;

    private UIRoot() {
      this.ui = UI.this;

      this.viewClear = new View(this.ui.lx);
      this.viewClear.setClearColor(0x000000ff);
      this.viewClear.setScreenOrtho();

      this.view2d = new View(this.ui.lx);
      this.view2d.setClearFlags(BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL);
      this.view2d.setClearColor(0);
      this.view2d.setScreenOrtho();
    }

    protected void resize() {
      this.viewClear.setRect(
        0,
        0,
        lx.getFrameBufferWidth(),
        lx.getFrameBufferHeight()
      );
      this.viewClear.setScreenOrtho();

      this.view2d.setRect(
        0,
        0,
        lx.getFrameBufferWidth(),
        lx.getFrameBufferHeight()
      );
      this.view2d.setScreenOrtho();

    }

    /**
     * Returns the width of the UI, in UI-domain pixels
     *
     * @return UI width, in UI-coordinate space
     */
    @Override
    public float getWidth() {
      return this.ui.lx.getUIWidth();
    }

    /**
     * Returns the height of the UI, in UI-domain pixels
     *
     * @return UI height, in UI-coordinate space
     */
    @Override
    public float getHeight() {
      return this.ui.lx.getUIHeight();
    }

    @Override
    public UIObject getContentTarget() {
      return this;
    }

    @Override
    public float getContentWidth() {
      return getWidth();
    }

    @Override
    public float getContentHeight() {
      return getHeight();
    }

    private boolean isErrorDialog(UI2dComponent component) {
      if (component instanceof UIDialogBox) {
        return ((UIDialogBox) component).isError();
      }
      return false;
    }

    @Override
    void mousePressed(MouseEvent mouseEvent, float mx, float my) {
      // If a context menu is open, we'll want to close it on mouse-press
      // unless the mouse-press is within the context menu itself
      boolean hideContextOverlay = false;
      if (contextMenuOverlay.overlayContent != null) {
        hideContextOverlay = true;
        contextMenuOverlay.mousePressed = false;
      }
      super.mousePressed(mouseEvent, mx, my);

      // Note: check
      if (hideContextOverlay && !mouseEvent.isContextMenuConsumed() && !contextMenuOverlay.mousePressed && !isErrorDialog(contextMenuOverlay.overlayContent)) {
        hideContextOverlay();
      }
    }

    @Override
    protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
      if (topLevelKeyEventHandler != null) {
        topLevelKeyEventHandler.onKeyPressed(keyEvent, keyChar, keyCode);
      }
      if (!keyEvent.isConsumed()) {
        if (keyCode == KeyEvent.VK_Z && (keyEvent.isMetaDown() || keyEvent.isControlDown())) {
          if (keyEvent.isShiftDown()) {
            lx.command.redo();
          } else {
            lx.command.undo();
          }
        } else if (keyCode == KeyEvent.VK_TAB) {
          if (keyEvent.isShiftDown()) {
            focusPrev(keyEvent);
          } else {
            focusNext(keyEvent);
          }
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
          hideContextOverlay();
        }
      }
    }

    @Override
    protected void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
      if (topLevelKeyEventHandler != null) {
        topLevelKeyEventHandler.onKeyReleased(keyEvent, keyChar, keyCode);
      }
    }

    private void redraw() {
      for (UIObject child : this.mutableChildren) {
        if (child instanceof UI2dComponent) {
          ((UI2dComponent) child).redraw();
        }
      }
    }

    private UIObject findCurrentFocus() {
      UIObject currentFocus = this;
      while (currentFocus.focusedChild != null) {
        currentFocus = currentFocus.focusedChild;
      }
      return currentFocus;
    }

    private UIObject findNextFocusable() {
      // Identify the deepest focused object
      UIObject focus = findCurrentFocus();

      // Check if it has a child that is eligible for focus
      UIObject focusableChild = findNextFocusableChild(focus, 0);
      if (focusableChild != null) {
        return focusableChild;
      }

      // Work up the tree, trying siblings at each level
      while (focus.parent != null) {
        int focusIndex = focus.parent.mutableChildren.indexOf(focus);
        focusableChild = findNextFocusableChild(focus.parent, focusIndex + 1);
        if (focusableChild != null) {
          return focusableChild;
        }
        focus = focus.parent;
      }

      // We ran out! Loop around from the front...
      return findNextFocusableChild(this, 0);
    }

    private UIObject findNextFocusableChild(UIObject focus, int startIndex) {
      for (int i = startIndex; i < focus.mutableChildren.size(); ++i) {
        UIObject child = focus.mutableChildren.get(i);
        if (child.isVisible()) {
          if (child instanceof UITabFocus) {
            return child;
          }
          UIObject recurse = findNextFocusableChild(child, 0);
          if (recurse != null) {
            return recurse;
          }
        }
      }
      return null;
    }

    private UIObject findPrevFocusable() {
      // Identify the deepest focused object
      UIObject focus = findCurrentFocus();

      // Check its previous siblings, depth-first
      while (focus.parent != null) {
        int focusIndex = focus.parent.mutableChildren.indexOf(focus);
        UIObject focusableChild = findPrevFocusableChild(focus.parent, focusIndex - 1);
        if (focusableChild != null) {
          return focusableChild;
        }
        if (focus.parent instanceof UITabFocus) {
          return focus.parent;
        }
        focus = focus.parent;
      }

      // We failed! Wrap around to the end
      return findPrevFocusableChild(this, this.mutableChildren.size() - 1);
    }

    private UIObject findPrevFocusableChild(UIObject focus, int startIndex) {
      for (int i = startIndex; i >= 0; --i) {
        UIObject child = focus.mutableChildren.get(i);
        if (child.isVisible()) {
          UIObject recurse = findPrevFocusableChild(child, child.mutableChildren.size() - 1);
          if (recurse != null) {
            return recurse;
          }
          if (child instanceof UITabFocus) {
            return child;
          }
        }
      }
      return null;
    }

    // Limit the number of nanovg buffers we'll render in a single pass
    private static final int MAX_NVG_VIEWS_PER_PASS = 30;

    private final List<UIObject> glfwThreadChildren = new ArrayList<UIObject>();
    private final Queue<UI2dContext> renderQueue = new ArrayDeque<UI2dContext>();

    public void draw() {
      // The children array is a CopyOnWriteArrayList. Grab a proper copy
      // of it here and do drawing operations against that, so that we don't
      // have modifications to it in the middle of this draw() operation.
      this.glfwThreadChildren.clear();
      this.glfwThreadChildren.addAll(this.children);

      short viewId = 1;

      // Clear the whole window background to avoid edge-flicker
      this.viewClear.bind(viewId++).touch();

      // If the redraw flag is set, we need to walk all 2d hierarchies and
      // see which contexts need to be redrawn with the vg layer
      if (redrawFlag.compareAndSet(true, false)) {
        // Pre-pass over all 2d objects, set redraw flags on the UI2dComponent
        // objects and append to the list of 2d contexts that need rendering
        for (UIObject child : this.glfwThreadChildren) {
          if (child instanceof UI2dComponent) {
            ((UI2dComponent) child).predraw(this.renderQueue, false);
          }
        }

        // Now we have all of our UI2dContexts ready to go, render all of them
        // as necessary. Note that this is not blitting to the main screen
        // framebuffer, it's rendering the UI2dContexts using NanoVG onto a
        // texture framebuffer owned by the UI2dContext
        UI2dContext context;
        while ((context = this.renderQueue.poll()) != null) {
          context.render(vg, viewId++);
          if (viewId > MAX_NVG_VIEWS_PER_PASS) {
            // We're going to have to get to the rest on the next pass..
            break;
          }
        }
      }

      // Finally, draw everything in the root view. Note that we don't
      // iterate over mutableChildren here because it could have changed. Instead
      // we use the drawList that we compiled above when we were preparing the
      // UI2dContext objects for rendering. We render from back to front,
      // re-binding views as needed
      boolean bind2d = true;
      for (UIObject child : this.glfwThreadChildren) {
        if (child instanceof UI2dContext) {
          if (bind2d) {
            this.view2d.bind(viewId++);
            bind2d = false;
          }
          ((UI2dContext) child).draw(this.ui, this.view2d);
        } else if (child instanceof UI3dContext) {
          UI3dContext context3d = (UI3dContext) child;
          context3d.view.setId(viewId++);
          context3d.draw(this.ui, context3d.view);
          bind2d = true;
        }
      }
    }
  }

  /**
   * Redraw may be called from any thread
   */
  private final AtomicBoolean redrawFlag = new AtomicBoolean(true);
  private final AtomicBoolean disposeFramebufferFlag = new AtomicBoolean(false);

  private final List<VGraphics.Framebuffer> threadSafeDisposeList =
    Collections.synchronizedList(new ArrayList<VGraphics.Framebuffer>());

  public class Profiler {
    public long drawNanos = 0;
  }

  public final Profiler profiler = new Profiler();

  public final GLX lx;
  public final VGraphics vg;

  private UIRoot root;

  public final StringParameter contextualHelpText =
    new StringParameter("Contextual Help")
    .setDescription("Parameter for contextual help messages in the bottom bar");

  protected CoordinateSystem coordinateSystem = CoordinateSystem.LEFT_HANDED;

  private static final long INIT_RUN = -1;
  private long lastMillis = INIT_RUN;

  private UIEventHandler topLevelKeyEventHandler = null;

  private class UIContextOverlay extends UI2dScrollContext {

    private boolean mousePressed = false;

    private UI2dComponent overlayContent = null;

    private UIContextMenu contextMenu = null;

    public UIContextOverlay() {
      super(UI.this, 0, 0, 0, 0);
      this.parent = root;
      setUI(UI.this);
      setBackgroundColor(0);
    }

    private void clearContent(UI2dComponent overlayContent) {
      if (this.overlayContent == overlayContent) {
        setContent(null);
      }
    }

    private void setContent(UI2dComponent overlayContent) {
      if (this.overlayContent != null) {
        this.overlayContent.setVisible(false);
        this.overlayContent.removeFromContainer();
        root.mutableChildren.remove(this);
      }
      this.overlayContent = overlayContent;
      this.contextMenu = null;
      if (overlayContent != null) {
        float contentWidth = overlayContent.getWidth();
        float contentHeight = overlayContent.getHeight();
        if (overlayContent instanceof UIContextMenu) {
          this.contextMenu = (UIContextMenu) overlayContent;
          float scrollHeight = contextMenu.getScrollHeight();
          setSize(contentWidth, scrollHeight);
          setScrollSize(contentWidth, contentHeight);
        } else {
          setScrollSize(contentWidth, contentHeight);
          setSize(contentWidth, contentHeight);
        }

        float x = 0;
        float y = 0;
        UIObject component = overlayContent;
        while (component != root && component != null) {
          x += component.getX();
          y += component.getY();
          if (component instanceof UI2dScrollInterface) {
            UI2dScrollInterface scrollInterface = (UI2dScrollInterface) component;
            x += scrollInterface.getScrollX();
            y += scrollInterface.getScrollY();
          }
          component = component.getParent();
        }
        setPosition(x, y);
        overlayContent.setVisible(true);
        overlayContent.setPosition(0, 0);
        overlayContent.addToContainer(this);
        root.mutableChildren.add(this);
      }
    }

    @Override
    protected void drawBackground(UI ui, VGraphics vg) {
      UIContextMenu contextMenu = this.contextMenu;
      if (contextMenu != null) {
        float padding = contextMenu.getPadding();
        if (padding > 0) {
          vg.beginPath();
          vg.fillColor(ui.theme.deviceFocusedBackgroundColor);
          vgRoundedRect(contextMenu, vg, 0, 0, this.width, this.height);
          vg.fill();
        }
        vg.beginPath();
        vg.fillColor(contextMenu.getBackgroundColor());
        if (padding > 0) {
          vg.roundedRect(padding, padding, this.width - 2 * padding, this.height - 2*padding, 2);
        } else {
          vgRoundedRect(contextMenu, vg, 0, 0, this.width, this.height);
        }
        vg.fill();
      } else {
        super.drawBackground(ui, vg);
      }
    }

    @Override
    public void drawBorder(UI ui, VGraphics vg) {
      UIContextMenu contextMenu = this.contextMenu;
      if (contextMenu != null) {
        float padding = contextMenu.getPadding();
        UI2dComponent component = contextMenu;
        if (padding > 0) {
          // Cap the top and bottom of the scroll zone
          vg.beginPath();
          vg.fillColor(ui.theme.deviceFocusedBackgroundColor);
          vg.rect(padding, 0, this.width - 2*padding, padding);
          vg.rect(padding, this.height-padding, this.width - 2*padding, padding);
          vg.fill();
        }
        if (contextMenu.hasBorder()) {
          final float borderWeight = contextMenu.getBorderWeight();
          final float halfBorderWeight = borderWeight * 0.5f;
          vg.beginPath();
          vg.strokeWidth(borderWeight);
          vg.strokeColor(contextMenu.getBorderColor());
          vgRoundedRect(component, vg, halfBorderWeight, halfBorderWeight, this.width-borderWeight, this.height-borderWeight);
          vg.stroke();
          vg.strokeWidth(1);
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent, float mx, float my) {
      super.mousePressed(mouseEvent, mx, my);
      this.mousePressed = true;
    }
  }

  /**
   * Drop menu overlay object
   */
  private UIContextOverlay contextMenuOverlay;

  /**
   * UI look and feel
   */
  public final UITheme theme;

  boolean midiMapping = false;
  boolean modulationSourceMapping = false;
  boolean modulationTargetMapping = false;
  boolean triggerSourceMapping = false;
  boolean triggerTargetMapping = false;
  LXModulationEngine modulationEngine = null;
  LXParameterModulation highlightParameterModulation = null;
  private LXParameter highlightModulationTarget = null;
  public final MutableParameter highlightModulationTargetChanged = new MutableParameter();

  private UIControlTarget controlTarget = null;
  private UITriggerSource triggerSource = null;
  private UIModulationSource modulationSource = null;

  public UI(final GLX lx) throws IOException {
    if (UI.instance != null) {
      throw new IllegalStateException("May not create multiple instances of UI");
    }

    UI.instance = this;
    UI.thread = Thread.currentThread();

    this.lx = lx;
    this.vg = lx.vg;

    this.theme = new UITheme(this.vg);
    LX.initProfiler.log("GLX: UI: Theme");

    this.root = new UIRoot();
    this.contextMenuOverlay = new UIContextOverlay();
    LX.initProfiler.log("GLX: UI: Root");

    lx.addProjectListener(new LX.ProjectListener() {
      @Override
      public void projectChanged(File file, Change change) {
        switch (change) {
        case TRY:
          contextualHelpText.setValue("Loading project file: " + file.getName());
          break;
        case NEW:
          contextualHelpText.setValue("Created new project");
          break;
        case SAVE:
          contextualHelpText.setValue("Saved project file: " + file.getName());
          break;
        case OPEN:
          contextualHelpText.setValue("Opened project file: " + file.getName());
          break;
        }
      }
    });

    lx.engine.mapping.mode.addListener((p) -> {

      LXMappingEngine.Mode mappingMode = lx.engine.mapping.getMode();

      boolean mappingOff = mappingMode == LXMappingEngine.Mode.OFF;
      this.midiMapping = mappingMode == LXMappingEngine.Mode.MIDI;
      this.modulationSourceMapping = mappingMode == LXMappingEngine.Mode.MODULATION_SOURCE;
      this.modulationTargetMapping = mappingMode == LXMappingEngine.Mode.MODULATION_TARGET;
      this.triggerSourceMapping = mappingMode == LXMappingEngine.Mode.TRIGGER_SOURCE;
      this.triggerTargetMapping = mappingMode == LXMappingEngine.Mode.TRIGGER_TARGET;

      // Clear mapping state when mapping is finished
      if (mappingOff) {
        this.modulationEngine = this.lx.engine.modulation;
        this.controlTarget = null;
        this.modulationSource = null;
        this.triggerSource = null;
      }
      if (this.triggerSourceMapping) {
        this.triggerSource = null;
      }
      if (this.modulationSourceMapping) {
        this.modulationSource = null;
      }

      if (this.midiMapping) {
        this.contextualHelpText.setValue("Click on a control target to MIDI map, eligible controls are highlighted");
      } else if (this.modulationSourceMapping) {
        this.contextualHelpText.setValue("Click on a modulation source, eligible sources are highlighted ");
      } else if (this.modulationTargetMapping) {
        LXNormalizedParameter sourceParameter = modulationSource.getModulationSource();
        if (sourceParameter == null) {
          this.contextualHelpText.setValue("You are somehow mapping a non-existent source parameter, choose a destination");
        } else {
          this.contextualHelpText.setValue("Select a modulation destination for " + sourceParameter.getCanonicalLabel() + ", eligible targets are highlighted");
        }
      } else if (this.triggerSourceMapping) {
        this.contextualHelpText.setValue("Click on a trigger source, eligible sources are highlighted ");
      } else if (this.triggerTargetMapping) {
        this.contextualHelpText.setValue("Select a trigger destination for " + triggerSource.getTriggerSource().getCanonicalLabel() + ", eligible targets are highlighted");
      } else {
        this.contextualHelpText.setValue("");
      }

      this.root.redraw();
    });

    lx.engine.midi.addMappingListener(new LXMidiEngine.MappingListener() {

      @Override
      public void mappingRemoved(LXMidiEngine engine, LXMidiMapping mapping) {
      }

      @Override
      public void mappingAdded(LXMidiEngine engine, LXMidiMapping mapping) {
        if (midiMapping) {
          contextualHelpText.setValue("Successfully mapped MIDI Ch." + (mapping.channel+1) + " " + mapping.getDescription() + " to " + mapping.parameter.getCanonicalLabel());
        }
      }
    });

    lx.statusMessage.addListener(p -> {
      if (!isMapping()) {
        contextualHelpText.setValue(lx.statusMessage.getString());
      }
    });

    lx.errorChanged.addListener(p -> { showError(); }, true);

    lx.failure.addListener((p) -> {
      float width = getWidth() * .8f;
      float height = getHeight() * .8f;
      showContextOverlay(
        new UILabel(getWidth() * .1f, getHeight() * .1f, width, height)
        .setLabel(lx.failure.getString())
        .setBreakLines(true)
        .setPadding(8)
        .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.TOP)
        .setBackgroundColor(this.theme.listBackgroundColor)
        .setBorderColor(this.theme.attentionColor)
        .setBorderWeight(2)
        .setBorderRounding(4)
        .setFontColor(this.theme.attentionColor)
      );
    });

    lx.preferences.uiTheme.addListener(p -> {
      UITheme.Theme theme = null;
      try {
        theme = UITheme.Theme.valueOf(this.lx.preferences.uiTheme.getString());
      } catch (Exception ignored) {}
      if (theme != null) {
        this.theme.setTheme(theme);
        redraw();
      }
    }, true);
  }

  public void showError() {
    final LX.Error error = lx.getError();
    if (error != null) {
      if (error.cause != null) {
        showContextOverlay(new UIDialogBox(
          this,
          error.message,
          new String[] { "Copy Stack Trace", "Okay" },
          new int[] { UIDialogBox.OPTION_WIDTH * 2, UIDialogBox.OPTION_WIDTH },
          new Runnable[] {
            () -> { lx.setSystemClipboardString(error.getStackTrace()); },
            () -> { lx.popError(); }
          }).setError());
      } else {
        showContextOverlay(new UIDialogBox(this, error.message, () -> { lx.popError(); }).setError());
      }
    }
  }

  public LXParameter getHighlightModulationTarget() {
    return this.highlightModulationTarget;
  }

  public UI setHighlightModulationTarget(LXParameter highlightModulationTarget) {
    this.highlightModulationTarget = highlightModulationTarget;
    this.highlightModulationTargetChanged.bang();
    return this;
  }

  public UI setHighlightParameterModulation(LXParameterModulation highlightParameterModulation) {
    if (this.highlightParameterModulation != highlightParameterModulation) {
      this.highlightParameterModulation = highlightParameterModulation;
      this.root.redraw();
    }
    return this;
  }

  public UI setCoordinateSystem(CoordinateSystem coordinateSystem) {
    this.coordinateSystem = coordinateSystem;
    return this;
  }

  public void redraw() {
    this.root.redraw();
  }

  public static UI get() {
    return UI.instance;
  }

  public void focusPrev(Event event) {
    UIObject focusTarget = this.root.findPrevFocusable();
    if (focusTarget != null) {
      focusTarget.focus(event);
    }
  }

  public void focusNext(Event event) {
    UIObject focusTarget = this.root.findNextFocusable();
    if (focusTarget != null) {
      focusTarget.focus(event);
    }
  }

  private boolean isMapping() {
    return this.midiMapping || this.modulationSourceMapping || this.modulationTargetMapping || this.triggerSourceMapping || this.triggerTargetMapping;
  }

  public void setMouseoverHelpText(String helpText) {
    if (!isMapping()) {
      this.contextualHelpText.setValue(helpText);
    }
  }

  void clearMouseoverHelpText() {
    if (!isMapping()) {
      this.contextualHelpText.setValue("");
    }
  }

  /**
   * Sets an object to handle top-level input events
   *
   * @param eventHandler Event handler
   * @return this
   */
  public UI setTopLevelKeyEventHandler(UIEventHandler eventHandler) {
    this.topLevelKeyEventHandler = eventHandler;
    return this;
  }

  UI setControlTarget(UIControlTarget controlTarget) {
    this.lx.engine.mapping.setControlTarget(controlTarget.getControlTarget());
    LXParameter midiParameter = controlTarget.getControlTarget();
    if (midiParameter == null) {
      this.contextualHelpText.setValue("Press a MIDI key or controller to map a non-existent parameter?");
    } else {
      this.contextualHelpText.setValue("Press a MIDI key or controller to map " + midiParameter.getCanonicalLabel());
    }
    if (this.controlTarget != controlTarget) {
      if (this.controlTarget != null) {
        ((UI2dComponent) this.controlTarget).redraw();
      }
      this.controlTarget = controlTarget;
      if (this.controlTarget != null) {
        ((UI2dComponent) this.controlTarget).redraw();
      }
    }
    return this;
  }

  UIControlTarget getControlTarget() {
    return this.controlTarget;
  }

  public UI mapModulationOff() {
    this.lx.engine.mapping.setMode(LXMappingEngine.Mode.OFF);
    return this;
  }

  public UI mapTriggerSource() {
    return mapTriggerSource(this.lx.engine.modulation, null);
  }

  public UI mapTriggerSource(UITriggerSource triggerSource) {
    return mapTriggerSource(this.lx.engine.modulation, triggerSource);
  }

  public UI mapTriggerSource(LXModulationEngine modulationEngine, UITriggerSource triggerSource) {
    this.modulationEngine = modulationEngine;
    this.triggerSource = triggerSource;
    this.lx.engine.mapping.setMode(triggerSource == null ? LXMappingEngine.Mode.TRIGGER_SOURCE : LXMappingEngine.Mode.TRIGGER_TARGET);
    return this;
  }

  UITriggerSource getTriggerSource() {
    return this.triggerSource;
  }

  LXComponent getTriggerSourceComponent() {
    final BooleanParameter source = this.triggerSource.getTriggerSource();
    if (source != null) {
      return source.getParent();
    }
    return null;
  }

  public UI mapModulationSource() {
    return mapModulationSource(this.lx.engine.modulation, null);
  }

  public UI mapModulationSource(UIModulationSource modulationSource) {
    return mapModulationSource(this.lx.engine.modulation, modulationSource);
  }

  public UI mapModulationSource(LXModulationEngine modulationEngine, UIModulationSource modulationSource) {
    this.modulationEngine = modulationEngine;
    this.modulationSource = modulationSource;
    this.lx.engine.mapping.setMode(modulationSource == null ? LXMappingEngine.Mode.MODULATION_SOURCE : LXMappingEngine.Mode.MODULATION_TARGET);
    return this;
  }

  UIModulationSource getModulationSource() {
    return this.modulationSource;
  }

  /**
   * Add a task to be performed on every loop of the UI engine.
   *
   * @param loopTask Task to perform on every UI loop
   * @return this
   */
  public UI addLoopTask(LXLoopTask loopTask) {
    this.root.addLoopTask(loopTask);
    return this;
  }

  /**
   * Remove a task from the UI engine
   *
   * @param loopTask Task to stop performing on every UI loop
   * @return this
   */
  public UI removeLoopTask(LXLoopTask loopTask) {
    this.root.removeLoopTask(loopTask);
    return this;
  }

  /**
   * Add a 2d context to this UI
   *
   * @param layer UI layer
   * @return this
   */
  public UI addLayer(UI2dContext layer) {
    layer.addToContainer(this.root);
    return this;
  }

  /**
   * Remove a 2d context from this UI
   *
   * @param layer UI layer
   * @return this UI
   */
  public UI removeLayer(UI2dContext layer) {
    layer.removeFromContainer();
    return this;
  }

  /**
   * Add a 3d context to this UI
   *
   * @param layer 3d context
   * @return this UI
   */
  public UI addLayer(UI3dContext layer) {
    this.root.mutableChildren.add(layer);
    layer.parent = this.root;
    layer.setUI(this);
    return this;
  }

  public UI removeLayer(UI3dContext layer) {
    if (layer.parent != this.root) {
      throw new IllegalStateException("Cannot remove 3d layer which is not present");
    }
    this.root.mutableChildren.remove(layer);
    layer.parent = null;
    return this;
  }

  /**
   * Brings a layer to the top of the UI stack
   *
   * @param layer UI layer
   * @return this UI
   */
  public UI bringToTop(UI2dContext layer) {
    this.root.mutableChildren.remove(layer);
    this.root.mutableChildren.add(layer);
    return this;
  }

  public UI hideContextOverlay() {
    showContextOverlay(null);
    return this;
  }

  public UI showContextDialogMessage(String message) {
    return showContextOverlay(new UIDialogBox(this, message));
  }

  public UI clearContextOverlay(UI2dComponent contextOverlay) {
    this.contextMenuOverlay.clearContent(contextOverlay);
    return this;
  }

  public UI showContextOverlay(UI2dComponent contextOverlay) {
    this.contextMenuOverlay.setContent(contextOverlay);
    return this;
  }

  void redraw(UI2dComponent component) {
    // Use atomic booleans here to create a memory barrier for the GLFW
    // UI rendering thread to see that the 2d hierarchy needs to be checked
    // for items that need redraw
    component.redrawFlag.set(true);
    this.redrawFlag.set(true);
  }

  void disposeFramebuffer(VGraphics.Framebuffer buffer) {
    this.threadSafeDisposeList.add(buffer);
    this.disposeFramebufferFlag.set(true);
  }

  public float getContentScaleX() {
    return this.lx.getUIContentScaleX();
  }

  public float getContentScaleY() {
    return this.lx.getUIContentScaleY();
  }

  public float getWidth() {
    return this.lx.getUIWidth();
  }

  public float getHeight() {
    return this.lx.getUIHeight();
  }

  public void resize() {
    this.root.resize();
    onResize();
  }

  /**
   * Draws the UI
   */
  public final void draw() {

    beginDraw();

    long drawStart = System.nanoTime();

    long nowMillis = System.currentTimeMillis();
    if (this.lastMillis == INIT_RUN) {
      // Initial frame is arbitrarily 16 milliseconds (~60 fps)
      this.lastMillis = nowMillis - 16;
    }
    double deltaMs = nowMillis - this.lastMillis;
    this.lastMillis = nowMillis;

    // Run loop tasks through the UI tree
    this.root.loop(deltaMs);

    // Draw UIRoot object
    this.root.draw();

    endDraw();

    this.profiler.drawNanos = System.nanoTime() - drawStart;

    // Dispose of any framebuffers that we are done with
    if (this.disposeFramebufferFlag.compareAndSet(true, false)) {
      synchronized (this.threadSafeDisposeList) {
        for (VGraphics.Framebuffer framebuffer : this.threadSafeDisposeList) {
          framebuffer.dispose();
        }
        this.threadSafeDisposeList.clear();
      }
    }
  }

  protected void beginDraw() {
    // Subclasses may override...
  }

  protected void endDraw() {
    // Subclasses may override...
  }

  protected void onResize() {
    // Subclasses may override
  }

  public void mouseEvent(MouseEvent mouseEvent) {
    switch (mouseEvent.getAction()) {
    case SCROLL:
      this.root.mouseScroll(mouseEvent, mouseEvent.x, mouseEvent.y, mouseEvent.dx, mouseEvent.dy);
      return;
    case PRESS:
      this.root.mousePressed(mouseEvent, mouseEvent.x, mouseEvent.y);
      break;
    case RELEASE:
      this.root.mouseReleased(mouseEvent, mouseEvent.x, mouseEvent.y);
      break;
    case DRAG:
      this.root.mouseDragged(mouseEvent, mouseEvent.x, mouseEvent.y, mouseEvent.dx, mouseEvent.dy);
      break;
    case MOVE:
      this.root.mouseMoved(mouseEvent, mouseEvent.x, mouseEvent.y);
      break;
    }
  }

  public void keyEvent(KeyEvent keyEvent) {
    _engineThreadDefaultKeyEvent(keyEvent);

    char keyChar = keyEvent.getKeyChar();
    int keyCode = keyEvent.getKeyCode();
    switch (keyEvent.getAction()) {
    case RELEASE:
      this.root.keyReleased(keyEvent, keyChar, keyCode);
      break;
    case PRESS:
    case REPEAT:
      this.root.keyPressed(keyEvent, keyChar, keyCode);
      break;
    default:
      throw new RuntimeException("Invalid keyEvent type: " + keyEvent.getAction());
    }
  }

  private void _engineThreadDefaultKeyEvent(KeyEvent keyEvent) {
    int keyCode = keyEvent.getKeyCode();
    KeyEvent.Action action = keyEvent.getAction();
    if (action == KeyEvent.Action.PRESS) {
      switch (keyCode) {
      case KeyEvent.VK_S:
        if (keyEvent.isCommand()) {
          if (keyEvent.isShiftDown() || lx.getProject() == null) {
            lx.showSaveProjectDialog();
          } else {
            lx.saveProject();
          }
        }
        break;
      case KeyEvent.VK_O:
        if (keyEvent.isCommand()) {
          lx.showOpenProjectDialog();
        }
        break;
      }
    }
  }

  public void dispose() {
    this.root.dispose();
    this.theme.dispose();
  }
}
