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
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.utils.LXUtils;

public abstract class UIColorSlider extends UI2dComponent implements UIFocus, UIControlTarget {

  protected final ColorParameter color;
  protected final LXNormalizedParameter parameter;

  protected UIColorSlider(LXNormalizedParameter parameter, float x, float y, float w, float h) {
    this(null, parameter, x, y, w, h);
  }

  protected UIColorSlider(ColorParameter color, LXNormalizedParameter parameter, float x, float y, float w, float h) {
    super(x, y, w, h);
    this.color = color;
    this.parameter = parameter;
    setBorderColor(UI.get().theme.controlBorderColor);
    if (color != null) {
      addListener(color, this.redraw);
    }
  }

  protected float getBaseHuef() {
    if (this.color != null) {
      return this.color.hue.getBaseValuef();
    }
    return 0f;
  }

  protected float getBaseSaturationf() {
    if (this.color != null) {
      return this.color.saturation.getBaseValuef();
    }
    return 0f;
  }

  protected float getBaseBrightnessf() {
    if (this.color != null) {
      return this.color.brightness.getBaseValuef();
    }
    return 100f;
  }

  private void updateParameter(float mx, float my) {
    this.parameter.setNormalized(1. - my / (this.height-1));
  }

  private double getBaseNormalized() {
    return this.parameter.getBaseNormalized();
  }

  protected void drawValue(UI ui, VGraphics vg) {
    int y = (int) LXUtils.constrainf((this.height - 2) - (float) getBaseNormalized() * (this.height-3), 1, this.height - 2);

    vg.fillColor(0xff111111);

    vg.beginPath();
    vg.moveTo(1, y-.5f);
    vg.lineTo(1, y+1.5f);
    vg.lineTo(2, y+.5f);
    vg.fill();

    vg.beginPath();
    vg.moveTo(this.width-1, y-.5f);
    vg.lineTo(this.width-1, y+1.5f);
    vg.lineTo(this.width-2, y+.5f);
    vg.fill();

    vg.strokeColor(0x66ffffff);
    vg.beginPath();
    vg.line(2, y+.5f, this.width-2, y+.5f);
    vg.stroke();
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    double amt = .025;
    if (keyEvent.isShiftDown()) {
      amt = .1;
    }
    if (keyCode == KeyEvent.VK_DOWN) {
      this.parameter.setNormalized(getBaseNormalized() - amt);
    } else if (keyCode == KeyEvent.VK_UP) {
      this.parameter.setNormalized(getBaseNormalized() + amt);
    }
  }

  @Override
  public void onMousePressed(MouseEvent MouseEvent, float mx, float my) {
    updateParameter(mx, my);
  }

  @Override
  public void onMouseDragged(MouseEvent MouseEvent, float mx, float my, float dx, float dy) {
    updateParameter(mx, my);
  }

  @Override
  public LXNormalizedParameter getControlTarget() {
    return this.parameter;
  }

  public static class Hue extends UIColorSlider {
    public Hue(ColorParameter color, float x, float y, float w, float h) {
      super(color, color.hue, x, y, w, h);
    }

    public Hue(LXNormalizedParameter hue, float x, float y, float w, float h) {
      super(hue, x, y, w, h);
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      final int stops = 6;
      for (int  i = 0; i < stops; ++i) {
        int sy = Math.round(i * this.height / stops);
        int ey = Math.round((i+1) * this.height / stops);
        vg.fillLinearGradient(0, sy, 0, ey, LX.hsb(360 - i * 360 / stops, 100, 100), LX.hsb(360 - (i+1) * 360 / stops, 100, 100));
        vg.beginPath();
        vg.rect(0, sy, this.width, ey - sy);
        vg.fill();
      }
      drawValue(ui, vg);
    }

  }

  public static class Saturation extends UIColorSlider {
    public Saturation(ColorParameter color, float x, float y, float w, float h) {
      super(color, color.saturation, x, y, w, h);
    }

    public Saturation(LXNormalizedParameter saturation, float x, float y, float w, float h) {
      super(saturation, x, y, w, h);
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      int sColor = LX.hsb(getBaseHuef(), 100, getBaseBrightnessf());
      int eColor = LX.hsb(getBaseHuef(), 0, getBaseBrightnessf());
      vg.fillLinearGradient(0, 0, 0, this.height, sColor, eColor);
      vg.beginPath();
      vg.rect(0, 0, this.width, this.height);
      vg.fill();
      drawValue(ui, vg);
    }

  }

  public static class Brightness extends UIColorSlider {
    public Brightness(ColorParameter color, float x, float y, float w, float h) {
      super(color, color.brightness, x, y, w, h);
    }

    public Brightness(LXNormalizedParameter brightness, float x, float y, float w, float h) {
      super(brightness, x, y, w, h);
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      int sColor = LX.hsb(getBaseHuef(), getBaseSaturationf(), 100);
      int eColor = LX.hsb(getBaseHuef(), getBaseSaturationf(), 0);
      vg.fillLinearGradient(0, 0, 0, this.height, sColor, eColor);
      vg.beginPath();
      vg.rect(0, 0, this.width, this.height);
      vg.fill();
      drawValue(ui, vg);
    }
  }

}
