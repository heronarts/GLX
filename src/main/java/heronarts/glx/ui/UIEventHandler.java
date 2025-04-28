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

import heronarts.glx.event.GamepadEvent;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;

public abstract class UIEventHandler {

  /**
   * Subclasses override to receive mouse events
   *
   * @param mouseEvent Mouse event
   * @param mx x-coordinate
   * @param my y-coordinate
   */
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
  }

  /**
   * Subclasses override to receive mouse events
   *
   * @param mouseEvent Mouse event
   * @param mx x-coordinate
   * @param my y-coordinate
   */
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
  }


  /**
   * Subclasses override to receive mouse events
   *
   * @param mouseEvent Mouse event
   * @param mx x-coordinate
   * @param my y-coordinate
   * @param dx movement in x
   * @param dy movement in y
   */
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
  }

  /**
   * Subclasses override to receive mouse events
   *
   * @param mouseEvent Mouse event
   * @param mx x-coordinate
   * @param my y-coordinate
   */
  protected void onMouseMoved(MouseEvent mouseEvent, float mx, float my) {
  }

  /**
   * Subclasses override to receive events when mouse moves over this object
   *
   * @param mouseEvent Mouse Event
   */
  protected void onMouseOver(MouseEvent mouseEvent) {
  }

  /**
   * Subclasses override to receive events when mouse moves out of this object
   *
   * @param mouseEvent Mouse Event
   */
  protected void onMouseOut(MouseEvent mouseEvent) {
  }

  /**
   * Subclasses override to receive mouse events
   *
   * @param mouseEvent Mouse event
   * @param mx x-coordinate
   * @param my y-coordinate
   * @param dx Amount of horizontal scroll
   * @param dy Amount of vertical scroll
   */
  protected void onMouseScroll(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
  }

  /**
   * Subclasses override to receive key events
   *
   * @param keyEvent Key event
   * @param keyChar Key character
   * @param keyCode Key code value
   */
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
  }

  /**
   * Subclasses override to receive key events
   *
   * @param keyEvent Key event
   * @param keyChar Key character
   * @param keyCode Key code value
   */
  protected void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
  }

  protected void onGamepadButtonPressed(GamepadEvent gamepadEvent, int button) {
  }

  protected void onGamepadButtonReleased(GamepadEvent gamepadEvent, int button) {
  }

  protected void onGamepadAxisChanged(GamepadEvent gamepadEvent, int axis, float value) {
  }

}
