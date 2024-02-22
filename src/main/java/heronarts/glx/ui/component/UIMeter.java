/**
 * Copyright 2022- Mark C. Slee, Heron Arts LLC
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

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIModulationSource;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.LXNormalizedParameter;

public class UIMeter extends UI2dComponent implements UIModulationSource {

  public enum Axis {
    VERTICAL,
    HORIZONTAL
  };

  private LXNormalizedParameter parameter;
  private final Axis axis;

  protected float drawPixels = 0;

  public static UIMeter newHorizontalMeter(UI ui, LXNormalizedParameter parameter, float w, float h) {
    return new UIMeter(ui, parameter, Axis.HORIZONTAL, 0, 0, w, h);
  }

  public static UIMeter newVerticalMeter(UI ui, LXNormalizedParameter parameter, float w, float h) {
    return new UIMeter(ui, parameter, Axis.VERTICAL, 0, 0, w, h);
  }

  public UIMeter(UI ui, LXNormalizedParameter parameter, float x, float y, float w, float h) {
    this(ui, parameter, Axis.VERTICAL, x, y, w, h);
  }

  public UIMeter(UI ui, LXNormalizedParameter parameter, Axis axis, float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(ui.theme.controlBorderColor);
    setBackgroundColor(ui.theme.meterBackgroundColor);

    this.parameter = parameter;
    this.axis = axis;

    addLoopTask((deltaMs) -> {
      float normalized = (this.parameter == null) ? 0 : this.parameter.getNormalizedf();
      float dv = ((axis == Axis.VERTICAL) ? (this.height-2) : (this.width - 2)) * normalized;
      if (dv != this.drawPixels) {
        this.drawPixels = dv;
        redraw();
      }
    });
  }

  public UIMeter setParameter(LXNormalizedParameter parameter) {
    this.parameter = parameter;
    return this;
  }

  @Override
  public String getDescription() {
    return (this.parameter == null) ? "No parameter" : UIParameterControl.getDescription(this.parameter);
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    if (this.drawPixels > 0.5f) {
      vg.fillColor(ui.theme.primaryColor);
      vg.beginPath();
      if (this.axis == Axis.VERTICAL) {
        vg.rect(1, this.height-1-this.drawPixels, this.width-2, this.drawPixels);
      } else {
        vg.rect(1, 1, this.drawPixels, this.height-2);
      }
      vg.fill();
    }
  }

  @Override
  public LXNormalizedParameter getModulationSource() {
    return this.parameter;
  }
}
