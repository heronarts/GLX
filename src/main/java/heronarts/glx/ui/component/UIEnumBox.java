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
import heronarts.lx.parameter.CompoundEnumParameter;
import heronarts.lx.parameter.EnumParameter;

public class UIEnumBox extends UIIntegerBox {

  public UIEnumBox() {
    this(0, 0, 0, 0);
  }

  public UIEnumBox(float w, EnumParameter<?> parameter) {
    this(0, 0, w, DEFAULT_HEIGHT, parameter);
  }

  public UIEnumBox(float w, CompoundEnumParameter<?> parameter) {
    this(0, 0, w, DEFAULT_HEIGHT, parameter);
  }

  public UIEnumBox(float x, float y, float w, float h) {
    this(x, y, w, h, (EnumParameter<?>) null);
  }

  public UIEnumBox(float x, float y, float w, float h, EnumParameter<?> parameter) {
    super(x, y, w, h);
    disableImmediateEdit();
    if (parameter != null) {
      setParameter(parameter);
    }
  }

  public UIEnumBox(float x, float y, float w, float h, CompoundEnumParameter<?> parameter) {
    super(x, y, w, h);
    disableImmediateEdit();
    if (parameter != null) {
      setParameter(parameter);
    }
  }

  @Override
  public String getValueString() {
    if (this.parameter != null) {
      return this.parameter.getBaseOption();
    }
    return super.getValueString();
  }

  private String numericEntry = "";

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.enabled) {
      if (keyEvent.isEnter() || (keyCode == KeyEvent.VK_SPACE)) {
        keyEvent.consume();
        if (this.numericEntry.isEmpty()) {
          incrementValue(keyEvent);
        } else {
          try {
            setValue(Integer.parseInt(this.numericEntry) - 1);
          } catch (Exception x) {}
        }
        this.numericEntry = "";
        return;
      } else if (keyEvent.isDigit()) {
        this.numericEntry = this.numericEntry + keyChar;
        keyEvent.consume();
        return;
      }
    }

    // Not handled above
    this.numericEntry = "";
    super.onKeyPressed(keyEvent, keyChar, keyCode);
  }
}
