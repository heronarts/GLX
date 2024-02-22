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
import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.component.UIContextMenu;
import heronarts.lx.LXLoopTask;
import heronarts.lx.clipboard.LXClipboardItem;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class UIObject extends UIEventHandler implements LXLoopTask {

  private static class ParameterListener {
    private final LXListenableParameter parameter;
    private final LXParameterListener listener;

    private ParameterListener(LXListenableParameter parameter, LXParameterListener listener) {
      this.parameter = parameter;
      this.listener = listener;
    }
  }

  UI ui = null;

  public final BooleanParameter visible = new BooleanParameter("Visible", true);

  final List<UIObject> mutableChildren = new CopyOnWriteArrayList<UIObject>();
  protected final List<UIObject> children = Collections.unmodifiableList(this.mutableChildren);

  private final List<ParameterListener> parameterListeners = new ArrayList<ParameterListener>();

  UIObject parent = null;

  UIObject focusedChild = null;

  UIObject pressedChild = null;
  UIObject overChild = null;

  private boolean consumeMousePress = false;

  protected boolean hasFocus = false;

  private final List<LXLoopTask> loopTasks = new ArrayList<LXLoopTask>();

  private String debugId = "";

  protected UIObject() {
    addListener(this.visible, (p) -> {
      if (!this.visible.isOn()) {
        blur();
      }
    });
  }

  public UIObject setDebugId(String debugId) {
    this.debugId = debugId;
    return this;
  }

  @Override
  public String toString() {
    return super.toString() + " " + debugId;
  }

  /**
   * Adds a parameter listener which will automatically be unregistered when this UIObject is disposed
   *
   * @param parameter Parameter to listen to
   * @param listener Parameter listener
   * @return this
   */
  public UIObject addListener(LXListenableParameter parameter, LXParameterListener listener) {
    return addListener(parameter, listener, false);
  }

  /**
   * Adds a parameter listener which will automatically be unregistered when this UIObject is disposed
   *
   * @param parameter Parameter to listen to
   * @param listener Parameter listener
   * @param fire Whether to fire listener immediately upon registration
   * @return this
   */
  public UIObject addListener(LXListenableParameter parameter, LXParameterListener listener, boolean fire) {
    parameter.addListener(listener, fire);
    this.parameterListeners.add(new ParameterListener(parameter, listener));
    return this;
  }

  private boolean disposed = false;

  public void dispose() {
    if (this.disposed) {
      throw new IllegalStateException("Cannot dispose UIObject twice: " + this);
    }
    this.disposed = true;
    for (ParameterListener parameterListener : this.parameterListeners) {
      parameterListener.parameter.removeListener(parameterListener.listener);
    }
    this.parameterListeners.clear();
    this.visible.dispose();
    for (UIObject child : this.children) {
      child.dispose();
    }
    this.mutableChildren.clear();
  }

  protected UI getUI() {
    return this.ui;
  }

  protected GLX getLX() {
    return this.ui.lx;
  }

  protected void requireUIThread() {
    if (Thread.currentThread() != UI.thread) {
      throw new IllegalStateException("Method may only be called on UI thread");
    }
  }

  /**
   * Add a task to be performed on every loop of the UI engine.
   *
   * @param loopTask Task to be performed on every UI frame
   * @return this
   */
  public UIObject addLoopTask(LXLoopTask loopTask) {
    this.loopTasks.add(loopTask);
    return this;
  }

  /**
   * Remove a task from the UI engine
   *
   * @param loopTask Task to be removed from work list
   * @return this
   */
  public UIObject removeLoopTask(LXLoopTask loopTask) {
    this.loopTasks.remove(loopTask);
    return this;
  }

  /**
   * Processes all the loop tasks in this object
   */
  @Override
  public final void loop(double deltaMs) {
    if (isVisible()) {
      for (LXLoopTask loopTask : this.loopTasks) {
        loopTask.loop(deltaMs);
      }
      for (UIObject child : this.mutableChildren) {
        child.loop(deltaMs);
      }
    }
  }

  /**
   * Internal method to track the UI that this is a part of
   *
   * @param ui UI context
   */
  void setUI(UI ui) {
    this.ui = ui;
    for (UIObject child : this.mutableChildren) {
      child.setUI(ui);
    }
  }

  /**
   * Subclasses may access the object that is containing this one
   *
   * @return Parent object
   */
  protected UIObject getParent() {
    return this.parent;
  }

  /**
   * Whether the given point is contained by this object
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @return True if the object contains this point
   */
  protected boolean contains(float x, float y) {
    float xp = x - getX();
    float yp = y - getY();
    float width = getWidth();
    float height = getHeight();
    return
      (xp >= 0 && xp < width) &&
      (yp >= 0 && yp < height);
  }

  public float getX() {
    return 0;
  }

  public float getY() {
    return 0;
  }

  public abstract float getWidth();

  public abstract float getHeight();

  private String description = null;

  public UIObject setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Objects are encouraged to override this method providing a helpful String displayed to the user explaining
   * the function of this UI component. If no help is available, return null rather than an empty String.
   *
   * @return Helpful contextual string explaining function of this element
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Whether this object is visible.
   *
   * @return True if this object is being displayed
   */
  public boolean isVisible() {
    return this.visible.isOn();
  }

  /**
   * Toggle visible state of this component
   *
   * @return this
   */
  public UIObject toggleVisible() {
    setVisible(!isVisible());
    return this;
  }

  /**
   * Set whether this object is visible
   *
   * @param visible Whether the object is visible
   * @return this
   */
  public UIObject setVisible(boolean visible) {
    this.visible.setValue(visible);
    return this;
  }

  /**
   * Toggles whether this object always consumes mouse press events it receives
   *
   * @param consumeMousePress Whether to always consume mouse press events by default
   * @return this
   */
  public UIObject setConsumeMousePress(boolean consumeMousePress) {
    this.consumeMousePress = consumeMousePress;
    return this;
  }

  /**
   * Whether this object has focus
   *
   * @return true or false
   */
  public boolean hasFocus() {
    return this.hasFocus;
  }

  /**
   * Whether this object has direct focus, meaning that no
   * child element is focused
   *
   * @return true or false
   */
  public boolean hasDirectFocus() {
    return hasFocus() && (this.focusedChild == null);
  }

  /**
   * Gets which immediate child of this object is focused, may be null. Child
   * may also have focused children.
   *
   * @return immediate child of this object which has focus
   */
  public UIObject getFocusedChild() {
    return this.focusedChild;
  }

  /**
   * Focuses on this object, giving focus to everything above
   * and whatever was previously focused below.
   *
   * @param event Event that caused the focus
   * @return this
   */
  public UIObject focus(Event event) {
    if (event == null) {
      throw new IllegalArgumentException("Focus requires non-null event, use Event.NONE if none available");
    }
    if (this.focusedChild != null) {
      this.focusedChild.blur();
    }
    _focusParents(event);
    return this;
  }

  private void _focusParents(Event event) {
    if (this.parent != null) {
      if (this.parent.focusedChild != this) {
        if (this.parent.focusedChild != null) {
          this.parent.focusedChild.blur();
        }
        this.parent.focusedChild = this;
      }
      this.parent._focusParents(event);
    }
    if (!this.hasFocus) {
      this.hasFocus = true;
      _onFocus(event);
    }
  }

  private void _onFocus(Event event) {
    onFocus(event);
    if (this instanceof UI2dComponent) {
      ((UI2dComponent) this).redraw();
    }
  }

  /**
   * Blur this object. Blurs its children from the bottom of
   * the tree up.
   *
   * @return this
   */
  public UIObject blur() {
    if (this.hasFocus) {
      for (UIObject child : this.mutableChildren) {
        child.blur();
      }
      if (this.parent != null) {
        if (this.parent.focusedChild == this) {
          this.parent.focusedChild = null;
        }
      }
      this.hasFocus = false;
      onBlur();
      if (this instanceof UI2dComponent) {
        ((UI2dComponent)this).redraw();
      }
    }
    return this;
  }

  /**
   * Brings this object to the front of its container.
   *
   * @return this
   */
  public UIObject bringToFront() {
    if (this.parent == null) {
      throw new IllegalStateException("Cannot bring to front when not in any container");
    }
    this.parent.mutableChildren.remove(this);
    this.parent.mutableChildren.add(this);
    return this;
  }

  /**
   * Returns true if this is a MIDI control target that has been selected for
   * mapping. It is highlighted while the system waits for a MIDI event to map.
   *
   * @return true if this is a MIDI control target that has been selected for
   * mapping. It is highlighted while the system waits for a MIDI event to map.
   */
  boolean isControlTarget() {
    return this.ui.getControlTarget() == this;
  }

  /**
   * Returns true if this is a trigger source that has been selected for trigger
   * mapping. It is highlighted while the user selects a trigger target.
   *
   * @return true if this is a trigger source that has been selected for trigger
   * mapping. It is highlighted while the user selects a trigger target.
   */
  boolean isTriggerSource() {
    return
      this.ui.triggerTargetMapping &&
      (this == this.ui.getTriggerSource());
  }

  /**
   * Returns true if the UI is in a trigger source mapping state and this element
   * is an eligible trigger source. It is highlighted if so.
   *
   * @return true if the UI is in a trigger source mapping state and this element
   * is an eligible trigger source. It is highlighted if so.
   */
  boolean isTriggerSourceMapping() {
    return
      this.ui.triggerSourceMapping &&
      (this instanceof UITriggerSource) &&
      ((UITriggerSource) this).getTriggerSource() != null;
  }

  /**
   * Returns true if the UI is in trigger target mapping state and this element
   * is a valid, selectable trigger target.
   *
   * @return true if the UI is in trigger target mapping state and this element
   * is a valid, selectable trigger target.
   */
  boolean isTriggerTargetMapping() {
    if (this.ui.triggerTargetMapping && (this instanceof UITriggerTarget)) {
      BooleanParameter target = ((UITriggerTarget) this).getTriggerTarget();
      return
        (target != null) &&
        this.ui.modulationEngine.isValidTarget(target) &&
        !target.isDescendant(this.ui.getTriggerSourceComponent());
    }
    return false;
  }

  /**
   * Returns true if the UI is in modulation target mapping mode and this element
   * is the modulation source that is being mapped.
   *
   * @return true if the UI is in modulation target mapping mode and this element
   * is the modulation source that is being mapped.
   */
  boolean isModulationSource() {
    if (this.ui.modulationTargetMapping) {
      UIModulationSource modulationSource = this.ui.getModulationSource();
      if (modulationSource == null) {
        return false;
      }
      if (this == modulationSource) {
        return true;
      }
      if (this instanceof UIModulationSource) {
        if (((UIModulationSource) this).getModulationSource() == modulationSource.getModulationSource()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the UI is in modulation source mapping mode and this element
   * is a valid modulation source.
   *
   * @return true if the UI is in modulation source mapping mode and this element
   * is a valid modulation source.
   */
  boolean isModulationSourceMapping() {
    return
      this.ui.modulationSourceMapping && (
        ((this instanceof UIModulationSource) && ((UIModulationSource) this).getModulationSource() != null) ||
        ((this instanceof UITriggerSource) && ((UITriggerSource) this).getTriggerSource() != null)
      );
  }

  /**
   * Returns true if the UI is in modulation target mapping mode and this element
   * is a valid modulation target.
   *
   * @return true if the UI is in modulation target mapping mode and this element
   * is a valid modulation target.
   */
  boolean isModulationTargetMapping() {
    if (this.ui.modulationTargetMapping && (this instanceof UIModulationTarget)) {
      LXCompoundModulation.Target target = ((UIModulationTarget) this).getModulationTarget();
      return (target != null) && this.ui.modulationEngine.isValidTarget(target);
    }
    return false;
  }

  boolean isModulationHighlight() {
    if (this.ui.highlightParameterModulation != null) {
      LXParameter target = this.ui.highlightParameterModulation.getTarget();
      LXParameter thisParameter = null;
      if (this instanceof UITriggerTarget) {
        thisParameter = ((UITriggerTarget) this).getTriggerTarget();
      } else if (this instanceof UIModulationTarget) {
        thisParameter = ((UIModulationTarget) this).getModulationTarget();
      }
      return target == thisParameter;
    }
    return false;
  }

  /**
   * Returns true if the UI is in MIDI mapping mode and this element is a valid
   * control target that could be selected to receive MIDI input.
   *
   * @return true if the UI is in MIDI mapping mode and this element is a valid
   * control target that could be selected to receive MIDI input.
   */
  boolean isMidiMapping() {
    return
      this.ui.midiMapping &&
      (this instanceof UIControlTarget) &&
      ((UIControlTarget) this).getControlTarget() != null;
  }

  void resize(UI ui) {
    this.onUIResize(ui);
    for (UIObject child : this.mutableChildren) {
      child.resize(ui);
    }
  }

  /**
   * Subclasses may override this method to handle resize events on the global UI.
   * Called on the UI thread, only happens if ui.setResizable(true) has been called.
   *
   * @param ui The UI object
   */
  protected void onUIResize(UI ui) {}

  UI2dContainer dragging = null;

  void mousePressed(MouseEvent mouseEvent, float mx, float my) {
    this.dragging = null;

    if (isMidiMapping()) {
      this.ui.setControlTarget((UIControlTarget) this);
      return;
    } else if (isModulationSourceMapping()) {
      if (this instanceof UIModulationSource) {
        this.ui.mapModulationSource((UIModulationSource) this);
      } else if (this instanceof UITriggerSource) {
        this.ui.mapTriggerSource((UITriggerSource) this);
      } else {
        throw new IllegalStateException("isModulationSourceMapping() was true but the element is not a modulation or trigger source: " + this);
      }
      return;
    } else if (isModulationTargetMapping() && !isModulationSource()) {
      LXNormalizedParameter source = this.ui.getModulationSource().getModulationSource();
      LXCompoundModulation.Target target = ((UIModulationTarget) this).getModulationTarget();
      if (source != null && target != null) {
        getLX().command.perform(new LXCommand.Modulation.AddModulation(this.ui.modulationEngine, source, target));
      }
      this.ui.mapModulationOff();
      return;
    } else if (isTriggerSourceMapping()) {
      this.ui.mapTriggerSource((UITriggerSource) this);
      return;
    } else if (isTriggerTargetMapping() && !isTriggerSource()) {
      BooleanParameter source = this.ui.getTriggerSource().getTriggerSource();
      BooleanParameter target = ((UITriggerTarget)this).getTriggerTarget();
      if (source != null && target != null) {
        getLX().command.perform(new LXCommand.Modulation.AddTrigger(this.ui.modulationEngine, source, target));
      }
      this.ui.mapModulationOff();
      return;
    }

    // Find child to press on
    for (int i = this.mutableChildren.size() - 1; i >= 0; --i) {
      UIObject child = this.mutableChildren.get(i);
      if (child.isVisible() && child.contains(mx, my)) {
        child.mousePressed(mouseEvent, mx - child.getX(), my - child.getY());
        this.pressedChild = child;
        break;
      }
    }

    // Show a right-click context menu, if no child has, and if we're eligible
    if (!mouseEvent.isContextMenuConsumed() && this instanceof UIContextActions && (mouseEvent.getButton() == MouseEvent.BUTTON_RIGHT)) {
      UIContextActions contextParent = (UIContextActions) this;
      List<UIContextActions.Action> contextActions = contextParent.getContextActions();
      if (contextActions != null && contextActions.size() > 0) {
        mouseEvent.consumeContextMenu();
        getUI().showContextOverlay(
          new UIContextMenu(mx, my, UIContextMenu.DEFAULT_WIDTH, 0)
          .setActions(contextActions.toArray(new UIContextActions.Action[0]))
          .setPosition(this, (int) mx, (int) my)
        );
      }
    }

    // If mouse press was consumed by a child, don't handle it ourselves
    if (!mouseEvent.isConsumed()) {
      if (!hasFocus() && (this instanceof UIMouseFocus)) {
        focus(mouseEvent);
      }
      if (!mouseEvent.isContextMenuConsumed()) {
        onMousePressed(mouseEvent, mx, my);
      }
      if (!mouseEvent.isConsumed() && mouseEvent.isButton(MouseEvent.BUTTON_LEFT) && (this instanceof UI2dComponent.UIDragReorder)) {
        UI2dComponent.UIDragReorder drag = (UI2dComponent.UIDragReorder) this;
        if (drag.isValidDragPosition(mx, my)) {
          UI2dContainer container = ((UI2dComponent) this).getContainer();
          if ((container != null) && container.hasDragToReorder()) {
            this.dragging = container;
          }
        }
      }
    }

    // Finally, if we're set to always consume mouse presses, eat this
    if (this.consumeMousePress) {
      mouseEvent.consume();
    }

  }

  void mouseReleased(MouseEvent mouseEvent, float mx, float my) {
    if (this.pressedChild != null) {
      this.pressedChild.mouseReleased(
        mouseEvent,
        mx - this.pressedChild.getX(),
        my - this.pressedChild.getY()
      );
      this.pressedChild = null;
    }
    onMouseReleased(mouseEvent, mx, my);

    // Check for case where we mouse-dragged outside of an element, now we've released
    if (this.overChild != null && !this.overChild.contains(mx, my)) {
      this.overChild.mouseOut(mouseEvent);
      this.overChild = null;
    }

    if (this.dragging != null) {
      this.dragging.dragChild(this, mx, my, true);
      this.dragging = null;
    }
  }

  void mouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (isMidiMapping() || isModulationTargetMapping()) {
      return;
    }
    if (this.pressedChild != null) {
      this.pressedChild.mouseDragged(
        mouseEvent,
        mx - this.pressedChild.getX(),
        my - this.pressedChild.getY(),
        dx,
        dy
      );
    }
    if (!mouseEvent.isConsumed()) {
      onMouseDragged(mouseEvent, mx, my, dx, dy);
    }
    if (this.dragging != null) {
      mouseEvent.consume();
      this.dragging.dragChild(this, mx, my, false);
    }
  }

  void mouseMoved(MouseEvent mouseEvent, float mx, float my) {
    boolean overAnyChild = false;
    for (int i = this.mutableChildren.size() - 1; i >= 0; --i) {
      UIObject child = this.mutableChildren.get(i);
      if (child.isVisible() && child.contains(mx, my)) {
        overAnyChild = true;
        if (child != this.overChild) {
          if (this.overChild != null) {
            this.overChild.mouseOut(mouseEvent);
          }
          this.overChild = child;
          child.mouseOver(mouseEvent);
        }
        child.mouseMoved(mouseEvent, mx - child.getX(), my - child.getY());
        break;
      }
    }
    if (!overAnyChild && (this.overChild != null)) {
      this.overChild.mouseOut(mouseEvent);
      this.overChild = null;
    }
    onMouseMoved(mouseEvent, mx, my);
  }

  private String setDescription;

  void mouseOver(MouseEvent mouseEvent) {
    this.setDescription = getDescription();
    if (this.setDescription != null) {
      getUI().setMouseoverHelpText(this.setDescription);
    }
    onMouseOver(mouseEvent);
  }

  void mouseOut(MouseEvent mouseEvent) {
    if (this.setDescription != null) {
      getUI().clearMouseoverHelpText();
      this.setDescription = null;
    }
    if (this.overChild != null) {
      this.overChild.mouseOut(mouseEvent);
      this.overChild = null;
    }
    onMouseOut(mouseEvent);
  }

  void mouseScroll(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    for (int i = this.mutableChildren.size() - 1; i >= 0; --i) {
      UIObject child = this.mutableChildren.get(i);
      if (child.isVisible() && child.contains(mx, my)) {
        child.mouseScroll(mouseEvent, mx - child.getX(), my - child.getY(), dx, dy);
        break;
      }
    }
    if (!mouseEvent.isConsumed()) {
      onMouseScroll(mouseEvent, mx, my, dx, dy);
    }
  }

  void keyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    // First delegate to focused child elements which may handle this event
    if (this.focusedChild != null) {
      UIObject delegate = this.focusedChild;
      delegate.keyPressed(keyEvent, keyChar, keyCode);
    }

    // Next, check for copy/paste/duplicate actions
    if (!keyEvent.isConsumed()) {
      if (keyEvent.isMetaDown() || keyEvent.isControlDown()) {
        if (keyCode == KeyEvent.VK_C && this instanceof UICopy) {
          LXClipboardItem item = ((UICopy) this).onCopy();
          if (item != null) {
            keyEvent.consume();
            this.ui.lx.clipboard.setItem(item);
          }
        } else if (keyCode == KeyEvent.VK_V && this instanceof UIPaste) {
          LXClipboardItem item = this.ui.lx.clipboard.getItem();
          if (item != null) {
            ((UIPaste) this).onPaste(item);
            keyEvent.consume();
          }
        } else if (keyCode == KeyEvent.VK_D && this instanceof UIDuplicate) {
          ((UIDuplicate) this).onDuplicate(keyEvent);
        }
      }
    }

    // Finally, delegate to custom handler
    if (!keyEvent.isConsumed()) {
      onKeyPressed(keyEvent, keyChar, keyCode);
    }

    // Escape key blurs items with key focus
    if (!keyEvent.isConsumed() && !keyEvent.isBlurConsumed() && (keyCode == KeyEvent.VK_ESCAPE) && this instanceof UIKeyFocus) {
      keyEvent.consumeBlur();
      blur();
    }
  }

  void keyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.focusedChild != null) {
      UIObject delegate = this.focusedChild;
      delegate.keyReleased(keyEvent, keyChar, keyCode);
    }
    if (!keyEvent.isConsumed()) {
      onKeyReleased(keyEvent, keyChar, keyCode);
    }
  }

  /**
   * Subclasses override when element is focused
   *
   * @param event Event that caused the focus to occur
   */
  protected void onFocus(Event event) {
  }

  /**
   * Subclasses override when element loses focus
   */
  protected void onBlur() {
  }
}
