/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.DiscreteColorParameter;
import heronarts.lx.utils.LXUtils;

public class UIDiscreteColorBox extends UI2dComponent implements UIFocus {

  private class UIDiscreteColorMenu extends UI2dComponent {

    private final static int SPACING = 4;
    private final static int BOX_SIZE = 10;

    private UIDiscreteColorMenu(UI ui) {
      super(0, 0, 8 * BOX_SIZE + SPACING * 9, 3 * BOX_SIZE + SPACING * 4);
      setBackgroundColor(ui.theme.contextBackgroundColor);
      setBorderColor(ui.theme.controlBorderColor);
    }

    @Override
    public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
      int xi = LXUtils.constrain((int) ((mx - SPACING) / (BOX_SIZE + SPACING)), 0, 8);
      int yi = LXUtils.constrain((int) ((my - SPACING) / (BOX_SIZE + SPACING)), 0, 3);
      parameter.setValue(xi + yi * 8);
      getUI().clearContextOverlay(this);
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      int selectedI = parameter.getValuei();
      for (int i = 0; i < DiscreteColorParameter.COLORS.length; ++i) {
        int x = i % 8;
        int y = i / 8;
        vg.beginPath();
        vg.fillColor(DiscreteColorParameter.COLORS[i]);
        vg.strokeColor(ui.theme.controlActiveTextColor);
        vg.strokeWidth(2);
        vg.rect((x+1) * SPACING + x * 10, (y+1) * SPACING + y * 10, 10, 10);
        vg.fill();
        if (i == selectedI) {
          vg.stroke();
        }
      }
      vg.strokeWidth(1);
    }
  }

  private final UIDiscreteColorMenu colorMenu;

  private final DiscreteColorParameter parameter;

  public UIDiscreteColorBox(UI ui, final DiscreteColorParameter parameter, float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(ui.theme.controlBorderColor);
    setBackgroundColor(parameter.getColor());
    this.parameter = parameter;
    this.colorMenu = new UIDiscreteColorMenu(ui);
    this.colorMenu.setVisible(false);
    addListener(parameter, p -> {
      setBackgroundColor(parameter.getColor());
      this.colorMenu.redraw();
    });
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  private void toggleExpanded() {
    setExpanded(!this.colorMenu.isVisible());
  }

  private void setExpanded(boolean expanded) {
    if (this.colorMenu.isVisible() != expanded) {
      if (expanded) {
        this.colorMenu.setPosition(this, -this.colorMenu.getWidth() + UIDiscreteColorMenu.BOX_SIZE + UIDiscreteColorMenu.SPACING, -UIDiscreteColorMenu.SPACING);
        getUI().showContextOverlay(this.colorMenu);
      } else {
        getUI().clearContextOverlay(this.colorMenu);
      }
    }
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    setExpanded(true);
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (keyCode == KeyEvent.VK_LEFT) {
      keyEvent.consume();
      this.parameter.decrement();
    } else if (keyCode == KeyEvent.VK_RIGHT) {
      keyEvent.consume();
      this.parameter.increment();
    } else if (keyCode == KeyEvent.VK_DOWN) {
      keyEvent.consume();
      this.parameter.increment(8);
    } else if (keyCode == KeyEvent.VK_UP) {
      keyEvent.consume();
      this.parameter.decrement(8);
    } else if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      keyEvent.consume();
      toggleExpanded();
    } else if (keyCode == KeyEvent.VK_ESCAPE) {
      if (this.colorMenu.isVisible()) {
        keyEvent.consume();
        setExpanded(false);
      }
    }
  }
}