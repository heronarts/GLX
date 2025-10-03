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

import java.util.List;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.LXColor;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.utils.LXUtils;

public class UISlider extends UICompoundParameterControl implements UIFocus {

  public enum Direction {
    HORIZONTAL, VERTICAL
  };

  private final Direction direction;

  private static final int HANDLE_SIZE = 6;
  private static final int PADDING = 2;
  private static final int GROOVE_MIN = 4;
  private static final int GROOVE_MAX = 6;

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

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    // mod refers to the current, possibly-modulated value of the control's parameter.
    // base is the unmodulated, base value of that parameter.
    // If unmodulated, these will be equal
    final float base = (float) getBaseNormalized();
    final float mod = (float) getCompoundNormalized();
    final boolean isModulated = (base != mod);

    final boolean isHorizontal = this.direction == Direction.HORIZONTAL;
    final boolean isBipolar = this.polarity == LXParameter.Polarity.BIPOLAR;

    final int IH_MAX = 11;

    final float barX, barY, barLength, groove, iw;
    final int ih;

    if (isHorizontal) {
      groove = LXUtils.constrainf(Math.round(this.handleHeight / 4), GROOVE_MIN, GROOVE_MAX);
      barLength = this.width - 2*PADDING;
      barX = barY = PADDING;
      ih = (int) LXUtils.min(IH_MAX, this.handleHeight - barY - groove - PADDING);
    } else {
      groove = LXUtils.constrainf(Math.round(this.width / 2), GROOVE_MIN, GROOVE_MAX);
      barX = (this.width > 24) ? .5f * (this.width - groove) : PADDING;
      barY = PADDING;
      barLength = this.handleHeight - 2*PADDING;
      ih = (int) LXUtils.min(IH_MAX, this.width - PADDING - barX - groove);
    }

    iw = ih * .55f;
    final float centerX = barX + barLength*.5f;
    final float centerY = barY + barLength*.5f;

    // Modulations!
    if (this.parameter instanceof LXCompoundModulation.Target compound) {
      // Note: the UI thread is separate from the engine thread, modulations could in theory change
      // *while* we are rendering here. So explicitly get a UI thread copy
      final List<LXCompoundModulation> uiModulations = compound.getUIThreadModulations();
      for (int i = 0; i < uiModulations.size() && i < 3; ++i) {
        final LXCompoundModulation modulation = uiModulations.get(i);
        int modColor = ui.theme.controlDisabledColor.get();
        int modColorInv = modColor;
        if (isEnabled() && modulation.enabled.isOn()) {
          modColor = modulation.color.getColor();
          modColorInv = LXColor.hsb(LXColor.h(modColor), 50, 75);
        }
        vg.strokeWidth(2);

        final float mw = barLength * modulation.range.getValuef();
        if (isHorizontal) {
          final float my = barY + groove + 1 + 2*i;
          if (my >= this.handleHeight - 1) {
            break;
          }
          final float baseValueX = barX + barLength * base;
          vg.beginPath();
          vg.strokeColor(modColor);
          if (Math.abs(mw) < 1) {
            // Ensure some minimal visible color
            vg.line(baseValueX - 1, my, baseValueX + 1, my);
            vg.stroke();
          } else {
            float xf = LXUtils.constrainf(baseValueX + mw, barX, barX + barLength);
            vg.line(baseValueX, my, xf, my);
            vg.stroke();
            if (modulation.getPolarity() == LXParameter.Polarity.BIPOLAR) {
              vg.strokeColor(modColorInv);
              xf = LXUtils.constrainf(baseValueX - mw, barX, barX + barLength);
              vg.beginPath();
              vg.line(baseValueX, my, xf, my);
              vg.stroke();
            }
          }
        } else {
          final float mx = barX + groove + 1 + 2*i;
          if (mx >= this.width) {
            break;
          }
          final float baseValueY = barY + barLength * (1-base);
          vg.strokeColor(modColor);
          vg.beginPath();
          if (Math.abs(mw) < 1) {
            // Ensure some minimal visible color
            vg.line(mx, baseValueY - 1, mx, baseValueY + 1);
            vg.stroke();
          } else {
            float yf = LXUtils.constrainf(baseValueY - mw, PADDING, PADDING + barLength);
            vg.line(mx, baseValueY, mx, yf);
            vg.stroke();
            if (modulation.getPolarity() == LXParameter.Polarity.BIPOLAR) {
              vg.strokeColor(modColorInv);
              yf = LXUtils.constrainf(baseValueY + mw, PADDING, PADDING + barLength);
              vg.beginPath();
              vg.line(mx, baseValueY, mx, yf);
              vg.stroke();
            }
          }
        }
        enableModulationRedraw(modulation);
      }
      vg.strokeWidth(1);
    }

    final boolean editable = isEnabled() && isEditable();

    final int baseColor;
    final int modColor;
    if (editable) {
      baseColor = this.hasFillColor ? this.fillColor.get() : ui.theme.primaryColor.get();
      modColor = getModulatedValueColor(baseColor);
    } else {
      baseColor = modColor = ui.theme.controlDisabledValueColor.get();
    }

    vg.strokeWidth(1);

    // Dark background value groove
    vg.beginPath();
    vg.fillColor(editable ? ui.theme.controlFillColor : ui.theme.controlDisabledFillColor);
    if (isHorizontal) {
      vg.rect(barX, barY, barLength, groove);
    } else {
      vg.rect(barX, barY, groove, barLength);
    }
    vg.fill();

    // Base value fill
    final float fillSize = barLength * (isBipolar ? Math.abs(.5f - base) : base);
    if (fillSize > 0) {
      vg.fillColor(baseColor);
      vg.beginPath();
      if (isHorizontal) {
        if (isBipolar) {
          vg.rect(barX + barLength * Math.min(.5f, base), barY, fillSize, groove);
        } else {
          vg.rect(barX, barY, barLength * base, groove);
        }
      } else {
        if (isBipolar) {
          vg.rect(barX, barY + barLength * (1 - Math.max(.5f, base)), groove, fillSize);
        } else {
          vg.rect(barX, barY + barLength * (1-base), groove, fillSize);
        }
      }
      vg.fill();
    }

    // Modulated value fill
    if (isModulated) {
      vg.fillColor(modColor);
      vg.beginPath();
      if (isHorizontal) {
        if (isBipolar) {
          vg.rect(barX + barLength * Math.min(mod, base), barY, barLength * Math.abs(base - mod), groove);
        } else {
          vg.rect(barX + barLength * Math.min(base, mod), barY, barLength * Math.abs(base - mod), groove);
        }
      } else {
        if (isBipolar) {
          vg.rect(barX, barY + barLength * (1 - Math.max(base, mod)), groove, barLength * Math.abs(base-mod));
        } else {
          vg.rect(barX, barY + barLength * (1 - Math.max(base, mod)), groove, barLength * Math.abs(base-mod));
        }
      }
      vg.fill();
    }

    // Center notch if bipolar
    if (isBipolar) {
      vg.strokeColor(ui.theme.controlDetentColor);
      vg.beginPath();
      if (isHorizontal) {
        vg.line(centerX, barY, centerX, barY + groove);
      } else {
        vg.line(barX, centerY, barX + groove, centerY);
      }
      vg.stroke();
    }

    // Triangle Indicator
    final float is = ih / iw;
    vg.fillColor(ui.theme.controlHandleColor);
    vg.strokeColor(ui.theme.controlHandleBorderColor);
    vg.beginPath();
    if (isHorizontal) {
      final float indicatorX = barX + barLength * base;
      final float yTop = barY + groove + 1;
      final float yBottom = barY + groove + ih;
      vg.moveTo(indicatorX, yTop);
      final float ryint = yTop + (this.width - PADDING - indicatorX) * is;
      if (ryint < yBottom) {
        vg.lineTo(this.width-PADDING, ryint);
        vg.lineTo(this.width-PADDING, yBottom);
      } else {
        vg.lineTo(indicatorX + iw, yBottom);
      }
      final float lyint = yTop + (indicatorX - PADDING) * is;
      if (lyint < yBottom) {
        vg.lineTo(PADDING, yBottom);
        vg.lineTo(PADDING, lyint);
      } else {
        vg.lineTo(indicatorX - iw, yBottom);
      }
    } else {
      final float indicatorY = barY + barLength * (1-base);
      final float xLeft = barX + groove + 1;
      final float xRight = barX + groove + ih;

      vg.moveTo(xLeft, indicatorY);

      final float CLIP = 1.5f;

      final float txint = xLeft + (indicatorY - CLIP) * is;
      if (txint < xRight) {
        vg.lineTo(txint, CLIP);
        vg.lineTo(xRight, CLIP);
      } else {
        vg.lineTo(xRight, indicatorY - iw);
      }

      final float bxint = xLeft + (this.handleHeight - CLIP - indicatorY) * is;
      if (bxint < xRight) {
        vg.lineTo(xRight, this.handleHeight-CLIP);
        vg.lineTo(bxint, this.handleHeight-CLIP);
      } else {
        vg.lineTo(xRight, indicatorY + iw);
      }

    }
    vg.closePath();
    vg.fill();
    vg.stroke();

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
