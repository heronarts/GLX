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

import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIModulationSource;
import heronarts.glx.ui.UIModulationTarget;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;

public class UIDoubleBox extends UINumberBox implements UIControlTarget, UIModulationSource, UIModulationTarget {

  private double minValue = 0;
  private double maxValue = Double.MAX_VALUE;
  private double value = 0;
  private BoundedParameter parameter = null;

  private boolean normalizedMouseEditing = true;

  private final LXParameterListener parameterListener = p -> { setValue(p); };

  public UIDoubleBox() {
    this(0, 0, 0, 0);
  }

  public UIDoubleBox(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UIDoubleBox(float w, BoundedParameter parameter) {
    this(0, 0, w, parameter);
  }

  public UIDoubleBox(float w, float h, BoundedParameter parameter) {
    this(0, 0, w, h, parameter);
  }

  public UIDoubleBox(float x, float y, float w, BoundedParameter parameter) {
    this(x, y, w, DEFAULT_HEIGHT, parameter);
  }

  public UIDoubleBox(float x, float y, float w, float h, BoundedParameter parameter) {
    this(x, y, w, h);
    setParameter(parameter);
  }

  public UIDoubleBox setNormalizedMouseEditing(boolean normalizedMouseEditing) {
    this.normalizedMouseEditing = normalizedMouseEditing;
    return this;
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  public BoundedParameter getParameter() {
    return this.parameter;
  }

  public UIDoubleBox setParameter(final BoundedParameter parameter) {
    super.setModulationTarget(parameter);
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    this.parameter = parameter;
    if (parameter != null) {
      this.minValue = parameter.range.min;
      this.maxValue = parameter.range.max;
      this.parameter.addListener(this.parameterListener);
      setValue(parameter);
    } else {
      this.value = 0;
    }
    return this;
  }

  public UIDoubleBox setRange(double minValue, double maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
    setValue(LXUtils.constrain(this.value, minValue, maxValue));
    return this;
  }

  protected double getBaseNormalized() {
    if (this.parameter != null) {
      return this.parameter.getBaseNormalized();
    }
    return (this.value - this.minValue) / (this.maxValue - this.minValue);
  }

  public UIDoubleBox setNormalized(double normalized) {
    if (this.parameter != null) {
      setNormalizedCommand(normalized);
    } else {
      setValue(this.minValue + normalized * (this.maxValue - this.minValue));
    }
    return this;
  }

  @Override
  protected double getFillWidthNormalized() {
    return getBaseNormalized();
  }

  public double getValue() {
    return this.value;
  }

  protected UIDoubleBox setValue(LXParameter p) {
    return setValue(p.getBaseValue(), false);
  }

  public UIDoubleBox setValue(double value) {
    return setValue(value, true);
  }

  protected UIDoubleBox setValue(double value, boolean pushToParameter) {
    if (this.value == value) {
      return this;
    }

    // Check for wrappable params
    boolean wrappable = (this.parameter != null) && this.parameter.isWrappable();
    if (wrappable) {
      final double range = this.maxValue - this.minValue;
      if (value < this.minValue) {
        value = this.maxValue + ((value - this.minValue) % range);
      } else if (value > this.maxValue) {
        value = this.minValue + ((value - this.minValue) % range);
      }
    } else {
      value = LXUtils.constrain(value, this.minValue, this.maxValue);
    }

    if (this.value != value) {
      this.value = value;
      if (this.parameter != null && pushToParameter) {
        setValueCommand(value);
      }
      onValueChange(this.value);
      redraw();
    }
    return this;
  }

  @Override
  protected String getValueString() {
    if (this.parameter != null) {
      return this.parameter.getFormatter().format(this.value);
    }
    return LXParameter.Units.NONE.format(this.value);
  }

  /**
   * Invoked when value changes, subclasses may override to handle.
   *
   * @param value New value that is being set
   */
  protected /* abstract */ void onValueChange(double value) {}

  @Override
  protected void saveEditBuffer(String editBuffer) {
    try {
      setValue((this.parameter != null) ?
        this.parameter.getUnits().parseDouble(editBuffer) :
        Double.parseDouble(editBuffer)
      );
    } catch (NumberFormatException nfx) {}
  }

  public static boolean isValidInputCharacter(char keyChar) {
    return (keyChar >= '0' && keyChar <= '9') || (keyChar == '.') || (keyChar == '-') || (keyChar == ':');
  }

  @Override
  protected boolean isValidCharacter(char keyChar) {
    return isValidInputCharacter(keyChar);
  }

  private LXParameter.Units getUnits() {
    if (this.parameter != null) {
      return this.parameter.getUnits();
    }
    return LXParameter.Units.NONE;
  }

  private double getBaseIncrement() {
    double range = this.maxValue - this.minValue;
    if (this.parameter != null) {
      range = Math.abs(this.parameter.range.max - this.parameter.range.min);
    }
    switch (getUnits()) {
    case MILLISECONDS:
      if (range > 10000) {
        return 1000;
      } else if (range > 1000) {
        return 10;
      }
      return 1;
    default:
      return (range > 100) ? 1 : (range / 100.);
    }
  }

  private double getIncrement(Event inputEvent) {
    double increment = getBaseIncrement();
    if (inputEvent.isShiftDown()) {
      if (this.hasShiftMultiplier) {
        increment *= this.shiftMultiplier;
      } else if (this.parameter != null) {
        increment = (float) (this.parameter.getRange() / 10.);
      } else {
        increment *= .1;
      }
    }
    return increment;
  }

  @Override
  protected void decrementValue(KeyEvent keyEvent) {
    keyEvent.consume();
    setValue(getValue() - getIncrement(keyEvent));
  }

  @Override
  protected void incrementValue(KeyEvent keyEvent) {
    keyEvent.consume();
    setValue(getValue() + getIncrement(keyEvent));
  }

  @Override
  protected void incrementMouseValue(MouseEvent mouseEvent, int offset) {
    setValue(this.value + offset * getIncrement(mouseEvent));
  }

  @Override
  public LXNormalizedParameter getControlTarget() {
    return getMappableParameter();
  }

  @Override
  public LXNormalizedParameter getModulationSource() {
    return getMappableParameter();
  }

  @Override
  public LXCompoundModulation.Target getModulationTarget() {
    if (this.parameter instanceof LXCompoundModulation.Target) {
      return (LXCompoundModulation.Target) getMappableParameter();
    }
    return null;
  }

  private LXNormalizedParameter getMappableParameter() {
    return getMappableParameter(this.parameter);
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (this.enabled && this.editable && !this.editing && this.normalizedMouseEditing && this.parameter != null) {
      mouseEvent.consume();
      float delta = dy / 100.f;
      if (mouseEvent.isShiftDown()) {
        delta /= 10;
      }
      setNormalized(LXUtils.constrain(getBaseNormalized() - delta, 0, 1));
    } else {
      super.onMouseDragged(mouseEvent, mx, my, dx, dy);
    }
  }

  @Override
  public void dispose() {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    super.dispose();
  }

}
