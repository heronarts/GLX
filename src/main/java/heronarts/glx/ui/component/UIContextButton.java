/**
 * Copyright 2018- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;

public class UIContextButton extends UI2dComponent implements UIFocus {

  private String label = "";
  private final UI2dComponent contextMenu;
  private float contextMenuWidth = -1;
  private Direction direction = Direction.DOWN;
  private HorizontalDirection horizontalDirection = HorizontalDirection.RIGHT;
  private VGraphics.Image icon = null;
  private float iconOffsetX = 0, iconOffsetY = 0;


  /**
   * Direction that a context menu opens from a button
   */
  public enum Direction {
    /**
     * Menu opens beneath the button
     */
    DOWN,

    /**
     * Menu opens upwards from bottom of button
     */
    UP
  };

  public enum HorizontalDirection {
    /**
     * Menu opens to the right of the button
     */
    RIGHT,

    /**
     * Menu opens to the left of the button
     */
    LEFT
  };

  public UIContextButton(UI ui, float x, float y, float w, float h) {
    this(ui, x, y, w, h, new UIContextMenu(0, 0, UIContextMenu.DEFAULT_WIDTH, 0));
    this.contextMenuWidth = -1;
  }

  public UIContextButton(UI ui, float x, float y, float w, float h, UI2dComponent contextMenu) {
    super(x, y, w, h);
    setBorderColor(ui.theme.controlBorderColor);
    setFontColor(ui.theme.controlTextColor);
    setBackgroundColor(ui.theme.controlBackgroundColor);
    this.contextMenu = contextMenu;
    this.contextMenuWidth = contextMenu.getWidth();
  }

  public UIContextButton setIcon(VGraphics.Image icon) {
    this.icon = icon;
    return this;
  }

  public UIContextButton setIconOffset(float iconOffsetX, float iconOffsetY) {
    this.iconOffsetX = iconOffsetX;
    this.iconOffsetY = iconOffsetY;
    return this;
  }

  /**
   * Accessor for the underlying context menu object
   *
   * @return Context menu object opened by this button
   */
  public UI2dComponent getContextMenu() {
    return this.contextMenu;
  }

  /**
   * Sets the width of the context menu opened by this button
   *
   * @param contextMenuWidth Width of context menu
   * @return this
   */
  public UIContextButton setContextMenuWidth(float contextMenuWidth) {
    this.contextMenuWidth = contextMenuWidth;
    return this;
  }

  /**
   * Sets an array of actions that will be shown in the context menu that opens when
   * the button is clicked
   *
   * @param contextActions Set of context actions shown when button is clicked
   * @return this
   */
  public UIContextButton setContextActions(UIContextActions.Action[] contextActions) {
    if (!(this.contextMenu instanceof UIContextMenu)) {
      throw new IllegalStateException("May not set context actions on custom UIContextButton");
    }
    ((UIContextMenu) this.contextMenu).setActions(contextActions);
    return this;
  }

  /**
   * Sets the label visible on the button
   *
   * @param label Label
   * @return this
   */
  public UIContextButton setLabel(String label) {
    if (!this.label.equals(label)) {
      this.label = label;
      redraw();
    }
    return this;
  }

  /**
   * Sets the direction in which the context menu opens
   *
   * @param direction Direction to open
   * @return this
   */
  public UIContextButton setDirection(Direction direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Sets the direction in which the context menu opens
   *
   * @param horizontalDirection Direction to open
   * @return this
   */
  public UIContextButton setHorizontalDirection(HorizontalDirection horizontalDirection) {
    this.horizontalDirection = horizontalDirection;
    return this;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    if (this.icon != null) {
      UIColor iconTint = this.mouseDown ? ui.theme.controlActiveTextColor : getFontColor();
      this.icon.setTint(iconTint);
      vg.beginPath();
      vg.image(
        this.icon,
        this.iconOffsetX + this.width/2 - this.icon.width/2,
        this.iconOffsetY + this.height/2 - this.icon.height/2
      );
      vg.fill();
      this.icon.noTint();
    } else if ((this.label != null) && (this.label.length() > 0)) {
      UIColor fontColor = this.mouseDown ? ui.theme.controlActiveTextColor : getFontColor();
      vg.fillColor(fontColor);
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      if (this.textAlignVertical == VGraphics.Align.MIDDLE) {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
        vg.text(this.width / 2 + this.textOffsetX, this.height / 2 + this.textOffsetY, this.label);
        vg.fill();
      } else {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER);
        vg.text(this.width / 2 + this.textOffsetX, (int) (this.height * .75) + this.textOffsetY, this.label);
        vg.fill();
      }
    }
  }

  private boolean mouseDown = false;

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    mouseEvent.consume();
    showMenu();
    this.mouseDown = true;
    redraw();
  }

  @Override
  public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    this.mouseDown = false;
    redraw();
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.contextMenu.isVisible()) {
      keyEvent.consume();
      if (this.contextMenu instanceof UIContextMenu) {
        ((UIContextMenu) this.contextMenu).onKeyPressed(keyEvent, keyChar, keyCode);
      }
    } else if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      keyEvent.consume();
      showMenu();
    }
  }

  private void showMenu() {
    this.contextMenu.setWidth((this.contextMenuWidth > 0) ? this.contextMenuWidth : Math.max(UIContextMenu.DEFAULT_WIDTH, this.width));
    float hOffset = 0;
    if (this.horizontalDirection == HorizontalDirection.LEFT) {
      hOffset = this.width - this.contextMenu.getWidth();
    }

    switch (this.direction) {
    case DOWN:
      this.contextMenu.setPosition(this, hOffset, this.height);
      break;
    case UP:
      this.contextMenu.setPosition(this, hOffset, -this.contextMenu.getHeight());
      break;
    }
    if (this.contextMenu instanceof UIContextMenu) {
      ((UIContextMenu) this.contextMenu).setHighlight(0);
    }
    getUI().showContextOverlay(this.contextMenu);
  }

  @Override
  public void dispose() {
    UI.get().clearContextOverlay(this.contextMenu);
    this.contextMenu.dispose();
    super.dispose();
  }
}
