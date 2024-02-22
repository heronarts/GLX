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
import heronarts.glx.ui.UIModulationTarget;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;

public class UIIntegerBox extends UINumberBox implements UIControlTarget, UIModulationTarget {

  private int minValue = 0;
  private int maxValue = Integer.MAX_VALUE;
  private int value = 0;
  private boolean wrappable = true;
  protected DiscreteParameter parameter = null;

  private final LXParameterListener parameterListener = p -> {
    setValue(this.parameter.getBaseValuei(), false);
  };

  public UIIntegerBox() {
    this(0, 0, 0, 0);
  }

  public UIIntegerBox(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UIIntegerBox(float w, DiscreteParameter parameter) {
    this(w, DEFAULT_HEIGHT, parameter);
  }

  public UIIntegerBox(float w, float h, DiscreteParameter parameter) {
    this(0, 0, w, h, parameter);
  }

  public UIIntegerBox(float x, float y, float w, float h, DiscreteParameter parameter) {
    this(x, y, w, h);
    setParameter(parameter);
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  public DiscreteParameter getParameter() {
    return this.parameter;
  }

  public UIIntegerBox setParameter(final DiscreteParameter parameter) {
    super.setModulationTarget(parameter);
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    this.parameter = parameter;
    if (parameter != null) {
      this.minValue = parameter.getMinValue();
      this.maxValue = parameter.getMaxValue();
      this.value = parameter.getBaseValuei();
      this.parameter.addListener(this.parameterListener);
    }
    redraw();
    return this;
  }

  /**
   * Sets whether the box is wrappable, only applies when there is not a parameter
   * set.
   *
   * @param wrappable Whether box is wrappable when no parameter set
   * @return This
   */
  public UIIntegerBox setWrappable(boolean wrappable) {
    this.wrappable = wrappable;
    return this;
  }

  /**
   * Sets the range of the input box, inclusive
   *
   * @param minValue Minimum value (inclusive)
   * @param maxValue Maximum value (inclusive)
   * @return this
   */
  public UIIntegerBox setRange(int minValue, int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
    setValue(LXUtils.constrain(this.value, minValue, maxValue));
    return this;
  }

  private int getMinValue() {
    if (this.parameter != null) {
      return this.parameter.getMinValue();
    }
    return this.minValue;
  }

  private int getMaxValue() {
    if (this.parameter != null) {
      return this.parameter.getMaxValue();
    }
    return this.maxValue;
  }

  @Override
  protected double getFillWidthNormalized() {
    if (this.parameter != null) {
      return this.parameter.getNormalized();
    }
    int min = getMinValue();
    int max = getMaxValue();

    return (this.value - min) / (double) (max - min);
  }

  public int getValue() {
    return this.value;
  }

  @Override
  public String getValueString() {
    if (this.parameter != null) {
      return this.parameter.getFormatter().format(this.value);
    }
    return Integer.toString(this.value);
  }

  public UIIntegerBox setValue(int value) {
    return setValue(value, true);
  }

  protected UIIntegerBox setValue(int value, boolean pushToParameter) {
    if (this.value == value) {
      return this;
    }
    final int min = getMinValue();
    final int max = getMaxValue();
    final boolean wrappable = (this.parameter == null) ? this.wrappable : this.parameter.isWrappable();
    if (wrappable) {
      final int range = (max - min + 1);
      if (value < min) {
        value = max + ((value - min) % range);
      } else if (value > max) {
        value = min + ((value - min) % range);
      }
    } else {
      value = LXUtils.constrain(value, min, max);
    }
    if (this.value != value) {
      this.value = value;
      if ((this.parameter != null) && pushToParameter) {
        if (this.useCommandEngine) {
          getLX().command.perform(new LXCommand.Parameter.SetValue(this.parameter, this.value));
        } else {
          this.parameter.setValue(this.value);
        }
      }
      onValueChange(this.value);
      redraw();
    }
    return this;
  }

  /**
   * Subclasses may override to handle value changes
   *
   * @param value New value being set
   */
  protected void onValueChange(int value) {}

  @Override
  protected void saveEditBuffer(String editBuffer) {
    try {
      setValue(Integer.parseInt(editBuffer));
    } catch (NumberFormatException nfx) {}
  }

  @Override
  protected boolean isValidCharacter(char keyChar) {
    return (keyChar >= '0' && keyChar <= '9') || (keyChar == '-');
  }

  private int getIncrement(Event inputEvent) {
    int increment = 1;
    if (inputEvent.isShiftDown()) {
      if (this.hasShiftMultiplier) {
        increment *= this.shiftMultiplier;
      } else if (this.parameter != null) {
        increment = Math.max(1, this.parameter.getRange() / 10);
      } else {
        increment *= 10;
      }
    }
    return increment;
  }

  @Override
  protected void incrementValue(KeyEvent keyEvent) {
    keyEvent.consume();
    setValue(this.value + getIncrement(keyEvent));
  }

  @Override
  protected void decrementValue(KeyEvent keyEvent) {
    keyEvent.consume();
    setValue(this.value - getIncrement(keyEvent));
  }

  @Override
  protected void incrementMouseValue(MouseEvent mouseEvent, int offset) {
    setValue(this.value + getIncrement(mouseEvent) * offset);
  }

  @Override
  public LXNormalizedParameter getControlTarget() {
    return getMappableParameter(this.parameter);
  }

  @Override
  public LXCompoundModulation.Target getModulationTarget() {
    if (this.parameter instanceof LXCompoundModulation.Target) {
      return (LXCompoundModulation.Target) getMappableParameter(this.parameter);
    }
    return null;
  }

  @Override
  public void dispose() {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    super.dispose();
  }

}
