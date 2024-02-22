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
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.color.DiscreteColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;

public class UIKnob extends UICompoundParameterControl implements UIFocus {

  public final static int KNOB_MARGIN = 6;
  public final static int KNOB_SIZE = 28;
  public final static int WIDTH = KNOB_SIZE + 2*KNOB_MARGIN;
  public final static int HEIGHT = KNOB_SIZE + LABEL_MARGIN + LABEL_HEIGHT;

  private final static float KNOB_INDENT = .4f;
  private final static float ARC_CENTER_X = WIDTH / 2;
  private final static float ARC_CENTER_Y = KNOB_SIZE / 2 + .5f;
  private final static float ARC_START = (float) LX.HALF_PI + KNOB_INDENT;
  private final static float ARC_RANGE = (float) LX.TWO_PI - 2 * KNOB_INDENT;
  private final static float ARC_END = ARC_START + ARC_RANGE;
  private final static float ARC_MIN = ARC_RANGE / 255.f; // half a MIDI tick

  public UIKnob(LXNormalizedParameter parameter) {
    this(0, 0, parameter);
  }

  public UIKnob(float x, float y, LXNormalizedParameter parameter) {
    this(x, y);
    setParameter(parameter);
  }

  public UIKnob() {
    this(0, 0);
  }

  public UIKnob(float x, float y) {
    this(x, y, WIDTH, KNOB_SIZE);
  }

  public UIKnob(float x, float y, float w, float h) {
    super(x, y, w, h);
    this.keyEditable = true;
  }

  private final List<LXCompoundModulation> uiModulations = new ArrayList<LXCompoundModulation>();

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    // value refers to the current, possibly-modulated value of the control's parameter.
    // base is the unmodulated, base value of that parameter.
    // If unmodulated, these will be equal
    float value = (float) getCompoundNormalized();
    float base = (float) getBaseNormalized();
    float valueEnd = ARC_START + value * ARC_RANGE;
    float baseEnd = ARC_START + base * ARC_RANGE;
    float valueStart;
    switch (this.polarity) {
    case BIPOLAR: valueStart = ARC_START + ARC_RANGE/2; break;
    default: case UNIPOLAR: valueStart = ARC_START; break;
    }

    float arcSize = KNOB_SIZE / 2;

    // Modulations!
    if (this.parameter instanceof LXCompoundModulation.Target) {
      final LXCompoundModulation.Target compound = (LXCompoundModulation.Target) this.parameter;
      // Note: the UI thread is separate from the engine thread, modulations could in theory change
      // *while* we are rendering here. So we lean on the fact that the parameters use a
      // CopyOnWriteArrayList and shuffle everything into our own ui-thread-local copy here
      this.uiModulations.clear();
      this.uiModulations.addAll(compound.getModulations());
      for (int i = this.uiModulations.size() - 1; i >= 0; --i) {
        LXCompoundModulation modulation = this.uiModulations.get(i);
        registerModulation(modulation);

        float modStart, modEnd;
        switch (modulation.getPolarity()) {
        case BIPOLAR:
          modStart = baseEnd - modulation.range.getValuef() * ARC_RANGE;
          modEnd = baseEnd + modulation.range.getValuef() * ARC_RANGE;
          break;
        default:
        case UNIPOLAR:
          modStart = baseEnd;
          modEnd = modStart + modulation.range.getValuef() * ARC_RANGE;
          break;
        }

        // Light ring of value
        DiscreteColorParameter modulationColor = modulation.color;
        int modColor = ui.theme.controlDisabledFillColor.get();
        int modColorInv = modColor;
        if (isEnabled() && modulation.enabled.isOn()) {
          modColor = modulationColor.getColor();
          modColorInv = LXColor.hsb(LXColor.h(modColor), 50, 75);
        }
        boolean hasWrap = false;
        float wrapStart = 0, wrapEnd = 0;
        int wrapColor = modColor;

        if (this.parameter.isWrappable()) {
          if (modStart < ARC_START) {
            hasWrap = true;
            wrapStart = ARC_END;
            wrapEnd = ARC_END + (modStart - ARC_START);
            modStart = ARC_START;
            wrapColor = modColorInv;
          } else if (modStart > ARC_END) {
            hasWrap = true;
            wrapEnd = ARC_START;
            wrapStart = ARC_START + (modStart - ARC_END);
            modStart = ARC_END;
            wrapColor = modColorInv;
          }
          if (modEnd < ARC_START) {
            hasWrap = true;
            wrapStart = ARC_END;
            wrapEnd = ARC_END + (modEnd - ARC_START);
            modEnd = ARC_START;
            wrapColor = modColor;
          } else if (modEnd > ARC_END) {
            hasWrap = true;
            wrapEnd = ARC_START;
            wrapStart = ARC_START + (modEnd - ARC_END);
            modEnd = ARC_END;
            wrapColor = modColor;
          }
        } else {
          modStart = LXUtils.constrainf(modStart, ARC_START, ARC_END);
          modEnd = LXUtils.constrainf(modEnd, ARC_START, ARC_END);
        }
        drawModArc(vg, modulation.getPolarity(), modStart, modEnd, baseEnd, arcSize, modColor, modColorInv);
        if (hasWrap) {
          drawModArc(vg, LXParameter.Polarity.UNIPOLAR, wrapStart, wrapEnd, baseEnd, arcSize, wrapColor, wrapColor);
        }

        arcSize -= 3;
        vg.fillColor(ui.theme.deviceBackgroundColor);
        vg.beginPath();
        vg.circle(ARC_CENTER_X, ARC_CENTER_Y, arcSize);
        vg.fill();
        arcSize -= 1;
        if (arcSize < 6) {
          break;
        }
      }
    }

    final boolean editable = isEnabled() && isEditable();

    // Outer fill
    vg.fillColor(editable ? ui.theme.controlFillColor : ui.theme.controlDisabledFillColor);
    vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, ARC_START, ARC_END);

    // Compute colors for base/value fills
    int baseColor;
    int valueColor;
    if (editable) {
      baseColor = ui.theme.primaryColor.get();
      valueColor = getModulatedValueColor(baseColor);
    } else {
      int disabled = ui.theme.controlDisabledValueColor.get();
      baseColor = disabled;
      valueColor = disabled;
    }

    // Value indication
    if (Math.abs(valueStart - baseEnd) > ARC_MIN) {
      vg.fillColor(baseColor);
      vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.min(valueStart, baseEnd), Math.max(valueStart, baseEnd));
    }

    if (Math.abs(baseEnd - valueEnd) > ARC_MIN) {
      vg.fillColor(valueColor);
      vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.min(baseEnd, valueEnd), Math.max(baseEnd, valueEnd));
    }

    // Center tick mark for bipolar knobs
    if (this.polarity == LXParameter.Polarity.BIPOLAR) {
      vg.strokeColor(ui.theme.controlDetentColor);
      vg.beginPath();
      vg.line(ARC_CENTER_X, ARC_CENTER_Y, ARC_CENTER_X, ARC_CENTER_Y - arcSize);
      vg.stroke();
    }

    // Center dot
    float detent = LXUtils.minf(arcSize - 4, ui.theme.getKnobDetentSize());
    if (detent > 0) {
      vg.fillColor(ui.theme.controlDetentColor);
      vg.beginPath();
      vg.circle(ARC_CENTER_X, ARC_CENTER_Y, detent);
      vg.fill();
    }

    super.onDraw(ui, vg);
  }

  private void drawModArc(VGraphics vg, LXParameter.Polarity polarity, float modStart, float modEnd, float baseEnd, float arcSize, int modColor, int modColorInv) {
    vg.fillColor(modColor);
    switch (polarity) {
    case BIPOLAR:
      if (modEnd >= modStart) {
        vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, baseEnd, Math.min(ARC_END, modEnd+.1f));
        vg.fillColor(modColorInv);
        vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.max(ARC_START, modStart-.1f), baseEnd);
      } else {
        vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.max(ARC_START, modEnd-.1f), baseEnd);
        vg.fillColor(modColorInv);
        vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, baseEnd, Math.min(ARC_END, modStart+.1f));
      }
      break;
    case UNIPOLAR:
      vg.beginPathMoveToArcFill(ARC_CENTER_X, ARC_CENTER_Y, arcSize, Math.max(ARC_START, Math.min(modStart, modEnd)-.1f), Math.min(ARC_END, Math.max(modStart, modEnd)+.1f));
      break;
    }
  }

  private double dragValue;

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);

    this.dragValue = getBaseNormalized();
    if (isEditable() && (this.parameter != null) && (mouseEvent.isDoubleClick())) {
      LXCompoundModulation modulation = getModulation(mouseEvent.isShiftDown());
      if (modulation != null && (mouseEvent.isControlDown() || mouseEvent.isMetaDown())) {
        if (this.useCommandEngine) {
          getLX().command.perform(new LXCommand.Parameter.Reset(modulation.range));
        } else {
          modulation.range.reset();
        }
      } else {
        if (this.useCommandEngine) {
          getLX().command.perform(new LXCommand.Parameter.Reset(this.parameter));
        } else {
          this.parameter.reset();
        }
      }
    }
  }

  private LXCompoundModulation getModulation(boolean secondary) {
    if (this.parameter instanceof LXCompoundModulation.Target) {
      LXCompoundModulation.Target compound = (LXCompoundModulation.Target) this.parameter;
      // NOTE: this event-processing happens on the engine thread, the modulations won't change
      // underneath us, we can access them directly
      final List<LXCompoundModulation> modulations = compound.getModulations();
      int size = modulations.size();
      if (size > 0) {
        if (secondary && (size > 1)) {
          return modulations.get(size-2);
        } else {
          return modulations.get(size-1);
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
    float delta = dy / 100.f;
    LXCompoundModulation modulation = getModulation(mouseEvent.isShiftDown());
    if (modulation != null && (mouseEvent.isMetaDown() || mouseEvent.isControlDown())) {
      if (this.useCommandEngine) {
        setModulationRangeCommand(modulation.range, modulation.range.getValue() - delta);
      } else {
        modulation.range.setValue(modulation.range.getValue() - delta);
      }
    } else {
      if (mouseEvent.isShiftDown()) {
        delta /= 10;
      }
      this.dragValue -= delta;
      if (isWrappable()) {
        if (this.dragValue < 0) {
          this.dragValue = 1 + (this.dragValue % 1.);
        } else if (this.dragValue > 1) {
          this.dragValue = this.dragValue % 1.;
        }
      } else {
        this.dragValue = LXUtils.constrain(this.dragValue, 0, 1);
      }
      setNormalized(this.dragValue);
    }
  }

}
