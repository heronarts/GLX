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

package heronarts.glx.ui.component;

import java.util.ArrayList;
import java.util.List;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.LXColor;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.utils.LXUtils;

public class UISlider extends UICompoundParameterControl implements UIFocus {

  public enum Direction {
    HORIZONTAL, VERTICAL
  };

  private final Direction direction;

  private static final int HANDLE_SIZE = 6;
  private static final int HANDLE_ROUNDING = 2;
  private static final int PADDING = 2;
  private static final int GROOVE = 4;

  private float handleHeight;

  private boolean hasFillColor = false;

  private UIColor fillColor = UIColor.NONE;

  public UISlider(float w, LXListenableNormalizedParameter parameter) {
    this(w, DEFAULT_HEIGHT, parameter);
  }

  public UISlider(float w, float h, LXListenableNormalizedParameter parameter) {
    this(Direction.HORIZONTAL, w, h, parameter);
  }

  public UISlider(Direction direction, float w, float h, LXListenableNormalizedParameter parameter) {
    this(direction, 0, 0, w, h, parameter);
  }

  public UISlider(Direction direction, float x, float y, float w, float h, LXListenableNormalizedParameter parameter) {
    this(direction, x, y, w, h);
    setParameter(parameter);
  }

  public UISlider() {
    this(0, 0, 0, 0);
  }

  public UISlider(float x, float y, float w, float h) {
    this(Direction.HORIZONTAL, x, y, w, h);
  }

  public UISlider(Direction direction, float x, float y, float w, float h) {
    super(x, y, w, h);
    this.keyEditable = true;
    this.direction = direction;
    this.handleHeight = h;
  }


  @Override
  protected void onResize() {
    this.handleHeight = this.height -
      (isShowLabel() ? (LABEL_MARGIN + LABEL_HEIGHT) : 0);
  }

  public UISlider resetFillColor() {
    if (this.hasFillColor) {
      this.hasFillColor = false;
      redraw();
    }
    return this;
  }

  public UISlider setFillColor(int fillColor) {
    return setFillColor(new UIColor(fillColor));
  }

  public UISlider setFillColor(UIColor fillColor) {
    if (!this.hasFillColor || (this.fillColor != fillColor)) {
      this.hasFillColor = true;
      this.fillColor = fillColor;
      redraw();
    }
    return this;
  }

  private final List<LXCompoundModulation> uiModulations = new ArrayList<LXCompoundModulation>();

  @Override
  @SuppressWarnings("fallthrough")
  protected void onDraw(UI ui, VGraphics vg) {
    // value refers to the current, possibly-modulated value of the control's parameter.
    // base is the unmodulated, base value of that parameter.
    // If unmodulated, these will be equal
    double value = getCompoundNormalized();
    double base = getBaseNormalized();
    boolean modulated = (base != value);
    int baseHandleEdge;
    float grooveDim;
    switch (this.direction) {
    case HORIZONTAL:
      baseHandleEdge = (int) Math.round(PADDING + base * (this.width - 2*PADDING - HANDLE_SIZE));
      grooveDim = this.width - 2*PADDING;
      break;
    default:
    case VERTICAL:
      baseHandleEdge = (int) Math.round(PADDING + (1 - base) * (this.handleHeight - 2*PADDING - HANDLE_SIZE));
      grooveDim = this.handleHeight - 2*PADDING;
      break;
    }
    int baseHandleCenter = baseHandleEdge + 1 + HANDLE_SIZE/2;

    // Modulations!
    if (this.parameter instanceof LXCompoundModulation.Target) {
      LXCompoundModulation.Target compound = (LXCompoundModulation.Target) this.parameter;
      // Note: the UI thread is separate from the engine thread, modulations could in theory change
      // *while* we are rendering here. So we lean on the fact that the parameters use a
      // CopyOnWriteArrayList and shuffle everything into our own ui-thread-local copy here
      this.uiModulations.clear();
      this.uiModulations.addAll(compound.getModulations());
      for (int i = 0; i < this.uiModulations.size() && i < 3; ++i) {
        LXCompoundModulation modulation = this.uiModulations.get(i);
        int modColor = ui.theme.controlDisabledColor.get();
        int modColorInv = modColor;
        if (isEnabled() && modulation.enabled.isOn()) {
          modColor = modulation.color.getColor();
          modColorInv = LXColor.hsb(LXColor.h(modColor), 50, 75);
        }
        vg.strokeWidth(2);
        boolean drawn = false;
        switch (this.direction) {
        case HORIZONTAL:
          float y = this.handleHeight/2 - GROOVE/2 - 2*(i+1);
          if (y > 0) {
            drawn = true;
            float xw = grooveDim * modulation.range.getValuef();
            float xf;
            switch (modulation.getPolarity()) {
            case BIPOLAR:
              vg.strokeColor(modColorInv);
              xf = LXUtils.constrainf(baseHandleCenter - xw, PADDING, PADDING + grooveDim - 1);
              vg.beginPath();
              vg.line(baseHandleCenter, y, xf, y);
              vg.stroke();
              // Pass-thru
            case UNIPOLAR:
              vg.strokeColor(modColor);
              xf = LXUtils.constrainf(baseHandleCenter + xw, PADDING, PADDING + grooveDim - 1);
              vg.beginPath();
              vg.line(baseHandleCenter, y, xf, y);
              vg.stroke();
              break;
            }
          }
          break;
        case VERTICAL:
          float x = this.width/2 + GROOVE/2 + 2*(i+1);
          if (x < this.width-1) {
            drawn = true;
            float yw =  grooveDim * modulation.range.getValuef();
            float yf;
            switch (modulation.getPolarity()) {
            case BIPOLAR:
              vg.strokeColor(modColorInv);
              yf = LXUtils.constrainf(baseHandleCenter + yw, PADDING, PADDING + grooveDim - 1);
              vg.beginPath();
              vg.line(x, baseHandleCenter, x, yf);
              vg.stroke();
              // Pass thru
            case UNIPOLAR:
              vg.strokeColor(modColor);
              yf = LXUtils.constrainf(baseHandleCenter - yw, PADDING, PADDING + grooveDim - 1);
              vg.beginPath();
              vg.line(x, baseHandleCenter, x, yf);
              vg.stroke();
              break;
            }
          }
          break;
        }
        if (drawn) {
          registerModulation(modulation);
        }
      }
    }

    final boolean editable = isEnabled() && isEditable();

    int baseColor;
    int valueColor;
    if (editable) {
      baseColor = this.hasFillColor ? this.fillColor.get() : ui.theme.primaryColor.get();
      valueColor = getModulatedValueColor(baseColor);
    } else {
      int disabled = ui.theme.controlDisabledValueColor.get();
      baseColor = disabled;
      valueColor = disabled;
    }

    vg.strokeWidth(1);
    vg.fillColor(editable ? ui.theme.controlFillColor : ui.theme.controlDisabledFillColor);

    switch (this.direction) {
    case HORIZONTAL:
      // Dark groove
      vg.beginPath();
      vg.rect(PADDING, this.handleHeight / 2 - GROOVE/2, this.width - 2*PADDING, GROOVE);
      vg.fill();

      int baseFillX, fillX, baseFillWidth, fillWidth;
      switch (this.polarity) {
      case BIPOLAR:
        baseFillX = (int) (this.width / 2);
        baseFillWidth = (int) ((base - 0.5) * (this.width - 2*PADDING));
        fillX = baseFillX + baseFillWidth;
        fillWidth = (int) ((value - base)* (this.width - 2*PADDING));
        break;
      default:
      case UNIPOLAR:
        baseFillX = PADDING;
        baseFillWidth = (int) ((this.width - 2*PADDING) * base);
        fillX = baseFillX + baseFillWidth;
        fillWidth = (int) ((this.width - 2*PADDING) * (value - base));
        break;
      }

      float topY = this.handleHeight / 2 - GROOVE/2;

      // Groove value fill
      if (baseFillWidth != 0) {
        vg.fillColor(baseColor);
        vg.beginPath();
        vg.rect(baseFillX, topY, baseFillWidth, GROOVE);
        vg.fill();
      }

      if (modulated){
        if (fillWidth > 0.5f) {
          vg.fillColor(valueColor);
          vg.beginPath();
          vg.rect(fillX, topY, fillWidth, GROOVE);
          vg.fill();
        } else if (fillWidth < -0.5f) {
          vg.fillColor(valueColor);
          vg.beginPath();
          vg.rect(fillX + fillWidth, topY, -fillWidth, GROOVE);
          vg.fill();
        }
      }

      // If we're modulating across the center, draw a small divider
      if ((base > 0.5 && value < 0.5) || (base < 0.5 && value > 0.5)) {
        float centerX = this.width / 2;
        vg.strokeColor(ui.theme.controlFillColor);
        vg.strokeWidth(1);
        vg.beginPath();
        vg.line(centerX, topY, centerX, topY + GROOVE);
        vg.stroke();
      }

      // Handle
      vg.fillColor(ui.theme.controlHandleColor);
      vg.strokeColor(ui.theme.controlBorderColor);
      vg.beginPath();
      vg.rect(baseHandleEdge+.5f, PADDING+.5f, HANDLE_SIZE, this.handleHeight - 2*PADDING, HANDLE_ROUNDING);
      vg.fill();
      vg.stroke();
      break;

    case VERTICAL:
      vg.beginPath();
      vg.rect(this.width / 2 - GROOVE/2, PADDING, GROOVE, this.handleHeight - 2*PADDING);
      vg.fill();
      int baseFillY;
      int fillY;
      int baseFillSize;
      int fillSize;
      switch (this.polarity) {
      case BIPOLAR:
        baseFillY = (int) (this.handleHeight / 2);
        fillY = baseFillY;
        baseFillSize = (int) ((0.5 - base) * (this.handleHeight - 2*PADDING));
        fillSize = (int) ((0.5 - value) * (this.handleHeight - 2*PADDING));
        break;
      default:
      case UNIPOLAR:
        baseFillSize = (int) (base * (this.handleHeight - 2*PADDING));
        baseFillY = (int) (this.handleHeight - PADDING - baseFillSize);
        fillSize = (int) ((value - base) * (this.handleHeight - 2*PADDING));
        fillY = baseFillY - fillSize;
        break;
      }

      if (baseFillSize > 0f) {
        vg.fillColor(baseColor);
        vg.beginPath();
        vg.rect(this.width / 2 - GROOVE/2, baseFillY, GROOVE, baseFillSize);
        vg.fill();
      } else if (baseFillSize < 0f) {
        vg.fillColor(baseColor);
        vg.beginPath();
        vg.rect(this.width / 2 - GROOVE/2, baseFillY + baseFillSize, GROOVE, -baseFillSize);
        vg.fill();
      }

      if (modulated) {
        if (fillSize > 0.5f) {
          vg.fillColor(valueColor);
          vg.beginPath();
          vg.rect(this.width / 2 - GROOVE/2, fillY, GROOVE, fillSize);
          vg.fill();
        } else if (fillSize < -0.5f) {
          vg.fillColor(valueColor);
          vg.beginPath();
          vg.rect(this.width / 2 - GROOVE/2, fillY + fillSize, GROOVE, -fillSize);
          vg.fill();
        }
      }

      vg.beginPath();
      vg.fillColor(ui.theme.controlHandleColor);
      vg.strokeColor(ui.theme.controlBorderColor);
      vg.rect(PADDING+.5f, baseHandleEdge + .5f, this.width - 2*PADDING, HANDLE_SIZE, HANDLE_ROUNDING);
      vg.fill();
      vg.stroke();
      break;
    }

    super.onDraw(ui, vg);
  }

  private float doubleClickMode = 0;
  private float doubleClickP = 0;

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    float mp, dim;
    boolean isVertical = false;
    switch (this.direction) {
    case VERTICAL:
      mp = my;
      dim = this.handleHeight;
      isVertical = true;
      break;
    default:
    case HORIZONTAL:
      mp = mx;
      dim = this.width;
      break;
    }
    if (mouseEvent.isDoubleClick() && Math.abs(mp - this.doubleClickP) < 3) {
      setNormalized(this.doubleClickMode);
    }
    this.doubleClickP = mp;
    if (mp < dim * .25) {
      this.doubleClickMode = isVertical ? 1 : 0;
    } else if (mp > dim * .75) {
      this.doubleClickMode = isVertical ? 0 : 1;
    } else {
      this.doubleClickMode = 0.5f;
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.onMouseReleased(mouseEvent, mx, my);
    this.editing = false;
  }

  private LXCompoundModulation getModulation(boolean secondary) {
    if (this.parameter != null && this.parameter instanceof LXCompoundModulation.Target) {
      LXCompoundModulation.Target compound = (LXCompoundModulation.Target) this.parameter;
      // NOTE: this event-processing happens on the engine thread, the modulations won't change
      // underneath us, we can access them directly
      final List<LXCompoundModulation> modulations = compound.getModulations();
      int size = modulations.size();
      if (size > 0) {
        if (secondary && (size > 1)) {
          return modulations.get(1);
        } else {
          return modulations.get(0);
        }
      }
    }
    return null;
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    mouseEvent.consume();
    if (!isEnabled() || !isEditable()) {
      return;
    }
    float dv, dim;
    boolean valid;
    switch (this.direction) {
    case VERTICAL:
      dv = -dy;
      dim = this.handleHeight;
      valid = (my > 0 && dy > 0) || (my < dim && dy < 0);
      break;
    default:
    case HORIZONTAL:
      dv = dx;
      dim = this.width;
      valid = (mx > 0 && dx > 0) || (mx < dim && dx < 0);
      break;
    }
    if (valid) {
      float delta = dv / (dim - HANDLE_SIZE - 2*PADDING);
      LXCompoundModulation modulation = getModulation(mouseEvent.isShiftDown());
      if (modulation != null && (mouseEvent.isMetaDown() || mouseEvent.isControlDown())) {
        if (this.useCommandEngine) {
          setModulationRangeCommand(modulation.range, modulation.range.getValue() + delta);
        } else {
          modulation.range.setValue(modulation.range.getValue() + delta);
        }
      } else {
        if (mouseEvent.isShiftDown()) {
          delta /= 10;
        }
        setNormalized(LXUtils.constrain(getBaseNormalized() + delta, 0, 1));
      }
    }
  }

}
