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
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.vg.VGraphics;

public class UIContextMenu extends UI2dContainer {

  private static final float DEFAULT_ROW_HEIGHT = 18;
  public static final float DEFAULT_WIDTH = 120;

  private UIContextActions.Action[] actions = new UIContextActions.Action[0];

  private int highlight = -1;
  private float rowHeight = DEFAULT_ROW_HEIGHT;
  private float padding = 0;
  private float maxScrollHeight = -1;
  private float contentHeight = 0;
  private float scrollHeight = -1;

  public UIContextMenu(float x, float y, float w, float h) {
    super(x, y, w, h);
    setVisible(false);
    setBackgroundColor(UI.get().theme.contextBackgroundColor);
    setBorderColor(UI.get().theme.contextBorderColor);
  }

  public float getPadding() {
    return this.padding;
  }

  @Override
  public UIContextMenu setPadding(float padding) {
    if (this.padding != padding) {
      this.padding = padding;
      updateSize();
    }
    return this;
  }

  public UIContextMenu setRowHeight(float rowHeight) {
    if (this.rowHeight != rowHeight) {
      this.rowHeight = rowHeight;
      updateSize();
    }
    return this;
  }

  public UIContextMenu setMaxHeight(float maxHeight) {
    this.maxScrollHeight = maxHeight;
    updateSize();
    return this;
  }

  public UIContextMenu setActions(UIContextActions.Action[] actions) {
    this.actions = actions;
    updateSize();
    return this;
  }

  private void updateSize() {
    this.contentHeight = this.actions.length * this.rowHeight + 2 * this.padding + 2;
    setHeight(this.contentHeight);
    if ((this.maxScrollHeight > 0) && (this.contentHeight > this.maxScrollHeight)) {
      this.scrollHeight = this.maxScrollHeight;
    } else {
      this.scrollHeight = this.contentHeight;
    }
  }

  @Override
  public float getScrollHeight() {
    return this.scrollHeight;
  }

  public UIContextMenu setHighlight(int highlight) {
    if (this.highlight != highlight) {
      if (highlight >= 0 && highlight < this.actions.length) {
        this.highlight = highlight;
        String description = this.actions[highlight].getDescription();
        if (description != null) {
          getUI().contextualHelpText.setValue(description);
        }
      } else {
        this.highlight = -1;
      }
      redraw();
    }
    return this;
  }

  @Override
  protected void drawBackground(UI ui, VGraphics vg) {
    vg.beginPath();
    vg.fillColor(getBackgroundColor());
    if (this.padding > 0) {
      vgRoundedRect(vg, this.padding, this.padding, this.width - 2 * this.padding, this.height - 2*this.padding);
    } else {
      vgRoundedRect(vg);
    }
    vg.fill();
  }

  @Override
  protected void drawBorder(UI ui, VGraphics vg) {
    // No-op, handled by UI.UIContextOverlay
  }

  /**
   * Subclasses may override to draw some other kind of drop menu
   *
   * @param ui UI context
   * @param vg PGraphics context
   */
  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    drawItems(ui, vg, this.width, this.padding);
  }

  private void drawItems(UI ui, VGraphics vg, float width, float padding) {

    if (this.highlight >= 0) {
      vg.beginPath();
      vg.rect(padding + 2, padding + 2 + this.highlight * this.rowHeight, width - 2 * padding - 4, this.rowHeight - 2, 2);
      vg.fillColor(ui.theme.contextHighlightColor);
      vg.fill();
    }

    float yp = 1;
    for (UIContextActions.Action action : this.actions) {
      vg.beginPath();
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      vg.fillColor(ui.theme.controlTextColor);
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
      vg.text(padding + 4, padding + yp + this.rowHeight / 2, clipTextToWidth(vg, action.getLabel(), width - 6 - 2 * padding));
      vg.fill();
      yp += this.rowHeight;
    }
  }

  private double lastKeyTime = -1;
  private String searchString = "";

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (keyCode == KeyEvent.VK_UP) {
      keyEvent.consume();
      setHighlight((this.highlight + this.actions.length - 1) % this.actions.length);
    } else if (keyCode == KeyEvent.VK_DOWN) {
      keyEvent.consume();
      setHighlight((this.highlight + 1) % this.actions.length);
    } else if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      keyEvent.consume();
      if (this.highlight >= 0) {
        this.actions[this.highlight].onContextAction(getUI());
      }
      getUI().clearContextOverlay(this);
    } else if (keyCode == KeyEvent.VK_ESCAPE) {
      keyEvent.consume();
      getUI().clearContextOverlay(this);
    } else if (!keyEvent.isCommand() && UITextBox.isValidTextCharacter(keyChar)) {
      keyEvent.consume();
      final double keyTime = keyEvent.getTime();
      if (keyTime - this.lastKeyTime > 1.) {
        this.searchString = "";
      }
      this.lastKeyTime = keyEvent.getTime();
      this.searchString = this.searchString + keyChar;
      int i = 0;
      for (UIContextActions.Action action : this.actions) {
        if (action.getLabel().regionMatches(true, 0, this.searchString, 0, this.searchString.length())) {
          setHighlight(i);
          break;
        }
        ++i;
      }
    }
  }

  @Override
  public void onMouseOut(MouseEvent mouseEvent) {
    setHighlight(-1);
  }

  @Override
  public void onMouseMoved(MouseEvent mouseEvent, float x, float y) {
    setHighlight((int) ((y - this.padding - 1) / this.rowHeight));
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float x, float y) {
    int index = (int) ((y - this.padding - 1) / this.rowHeight);
    if (index >= 0 && index < this.actions.length) {
      this.actions[index].onContextAction(getUI());
    }
    getUI().clearContextOverlay(this);
  }

}
