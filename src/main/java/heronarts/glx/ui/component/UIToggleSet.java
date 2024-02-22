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

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;

public class UIToggleSet extends UIParameterComponent implements UIFocus, UIControlTarget, LXParameterListener {

  private String[] options = null;

  private int[] boundaries = null;

  private int selectedIndex = -1;

  private boolean evenSpacing = false;

  private DiscreteParameter parameter = null;

  private UIColor activeColor = UIColor.NONE;
  private boolean hasActiveColor = false;

  public UIToggleSet() {
    this(0, 0, 0, 0);
  }

  public UIToggleSet(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UIToggleSet(float w, float h, DiscreteParameter parameter) {
    this(0, 0, w, h, parameter);
  }

  public UIToggleSet(float x, float y, float w, float h, DiscreteParameter parameter) {
    this(x, y, w, h);
    setParameter(parameter);
  }

  public UIToggleSet setActiveColor(int activeColor) {
    return setActiveColor(new UIColor(activeColor));
  }

  public UIToggleSet setActiveColor(UIColor activeColor) {
    if (!this.hasActiveColor || (this.activeColor != activeColor)) {
      this.hasActiveColor = true;
      this.activeColor = activeColor;
      redraw();
    }
    return this;
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  protected void onResize() {
    computeBoundaries();
  }

  public UIToggleSet setOptions(String[] options) {
    if (this.options != options) {
      this.options = options;
      this.selectedIndex = 0;
      this.boundaries = new int[options.length];
      computeBoundaries();
      redraw();
    }
    return this;
  }

  @Override
  public DiscreteParameter getParameter() {
    return this.parameter;
  }

  public UIToggleSet setParameter(DiscreteParameter parameter) {
    if (this.parameter != parameter) {
      if (this.parameter != null) {
        this.parameter.removeListener(this);
      }
      this.parameter = parameter;
      if (this.parameter != null) {
        this.parameter.addListener(this);
        String[] options = this.parameter.getOptions();
        if (options == null) {
          options = new String[this.parameter.getRange()];
          for (int i = 0; i < options.length; ++i) {
            options[i] = Integer.toString(this.parameter.getMinValue() + i);
          }
        }
        setOptions(options);
        setSelectedIndex(this.parameter.getBaseIndex(), false);
      }
    }
    return this;
  }

  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.parameter) {
      setSelectedIndex(this.parameter.getBaseIndex(), false);
    }
  }

  private void computeBoundaries() {
    if (this.boundaries == null) {
      return;
    }
    if (this.evenSpacing) {
      for (int i = 0; i < this.boundaries.length; ++i) {
        this.boundaries[i] = (int) ((i + 1) * (this.width-1) / this.boundaries.length);
      }
    } else {
      int totalLength = 0;
      for (String s : this.options) {
        totalLength += s.length();
      }
      int lengthSoFar = 0;
      for (int i = 0; i < this.options.length; ++i) {
        lengthSoFar += this.options[i].length();
        this.boundaries[i] = (int) (lengthSoFar * (this.width-1) / totalLength);
      }
    }
  }

  public UIToggleSet setEvenSpacing() {
    return setEvenSpacing(true);
  }

  public UIToggleSet setEvenSpacing(boolean evenSpacing) {
    if (this.evenSpacing != evenSpacing) {
      this.evenSpacing = evenSpacing;
      computeBoundaries();
      redraw();
    }
    return this;
  }

  /**
   * Gets the index of the currently selected value in the toggle set
   *
   * @return currently selected index
   */
  public int getSelectedIndex() {
    return this.selectedIndex;
  }

  /**
   * Get the currently selected option in the toggle set
   *
   * @return Currently selected value
   */
  public String getSelectedOption() {
    return this.options[this.selectedIndex];
  }

  /**
   * Sets the value of the control to the given value in the toggle set
   *
   * @param option String value, must be one of the options in the toggle set
   * @return this
   */
  public UIToggleSet setSelectedOption(String option) {
    for (int i = 0; i < this.options.length; ++i) {
      if (this.options[i].equals(option)) {
        return setSelectedIndex(i);
      }
    }

    // That string doesn't exist
    String optStr = "{";
    for (String str : this.options) {
      optStr += str + ",";
    }
    optStr = optStr.substring(0, optStr.length() - 1) + "}";
    throw new IllegalArgumentException("Not a valid option in UIToggleSet: "
        + option + " " + optStr);
  }

  /**
   * Sets the selected index in the toggle set
   *
   * @param index Index in the toggle set, from 0 to range-1
   * @return this
   */
  public UIToggleSet setSelectedIndex(int index) {
    return setSelectedIndex(index, true);
  }

  private UIToggleSet setSelectedIndex(int index, boolean pushToParameter) {
    if (this.selectedIndex != index) {
      if (index < 0 || index >= this.options.length) {
        throw new IllegalArgumentException("Invalid index to setValue(): "
            + index);
      }
      this.selectedIndex = index;
      if (this.parameter != null && pushToParameter) {
        getLX().command.perform(new LXCommand.Parameter.SetValue(this.parameter, this.parameter.getMinValue() + index));
      }
      onToggle(this.selectedIndex);
      redraw();
    }
    return this;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    vg.beginPath();
    vg.fillColor(ui.theme.controlBackgroundColor);
    vg.strokeColor(ui.theme.controlBorderColor);
    vg.rect(.5f, .5f, this.width-1, this.height-1);
    vg.fill();
    vg.stroke();

    // Active item
    if (this.selectedIndex >= 0) {
      int leftBoundary = (this.selectedIndex > 0) ? this.boundaries[this.selectedIndex - 1] : 0;
      int rightBoundary = this.boundaries[this.selectedIndex];
      vg.beginPath();
      vg.fillColor(this.hasActiveColor ? this.activeColor : ui.theme.selectionColor);
      vg.rect(leftBoundary + 2, 2, rightBoundary - leftBoundary - 3, this.height - 4, 2);
      vg.fill();
    }

    // Text labels
    vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
    vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
    vg.beginPath();
    int leftBoundary = 0;
    for (int i = 0; i < this.options.length; ++i) {
      boolean isActive = (i == this.selectedIndex);
      vg.fillColor(isActive ? ui.theme.controlActiveTextColor : ui.theme.controlTextColor);
      vg.text((leftBoundary + this.boundaries[i]) / 2.f, this.height/2 + 1, this.options[i]);
      leftBoundary = this.boundaries[i];
    }
    vg.fill();
  }

  protected void onToggle(int value) {
    onToggle(getSelectedOption());
  }

  protected void onToggle(String option) {
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    for (int i = 0; i < this.boundaries.length; ++i) {
      if (mx < this.boundaries[i]) {
        setSelectedIndex(i);
        break;
      }
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_DOWN)) {
      keyEvent.consume();
      setSelectedIndex(LXUtils.constrain(this.selectedIndex - 1, 0, this.options.length - 1));
    } else if ((keyCode == KeyEvent.VK_RIGHT) || (keyCode == KeyEvent.VK_UP)) {
      keyEvent.consume();
      setSelectedIndex(LXUtils.constrain(this.selectedIndex + 1, 0, this.options.length - 1));
    }
  }

  @Override
  public LXNormalizedParameter getControlTarget() {
    return getMappableParameter(this.parameter);
  }

  @Override
  public void dispose() {
    if (this.parameter != null) {
      this.parameter.removeListener(this);
    }
    super.dispose();
  }

}
