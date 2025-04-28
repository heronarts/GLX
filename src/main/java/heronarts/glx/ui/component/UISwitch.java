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
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITriggerSource;
import heronarts.glx.ui.UITriggerTarget;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.LXColor;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.utils.LXUtils;

public class UISwitch extends UIParameterControl implements UIFocus, UITriggerTarget, UITriggerSource {

  public final static int SWITCH_MARGIN = UIKnob.KNOB_MARGIN;
  public final static int SWITCH_SIZE = UIKnob.KNOB_SIZE;
  public final static int WIDTH = SWITCH_SIZE + 2*SWITCH_MARGIN;

  private final static double TRIGGER_DURATION_MS = UIIndicator.DEFAULT_TIMER_MS;

  protected boolean isMomentary = false;

  private boolean momentaryPressValid = false;
  private boolean momentaryPressEngaged = false;
  private double triggerTimeoutMs = 0;

  public UISwitch(LXNormalizedParameter parameter) {
    this(0, 0, parameter);
  }

  public UISwitch(float x, float y, LXNormalizedParameter parameter) {
    this(x, y);
    setParameter(parameter);
  }

  public UISwitch() {
    this(0, 0);
  }

  public UISwitch(float x, float y) {
    super(x, y, WIDTH, SWITCH_SIZE);
    this.keyEditable = true;
    addLoopTask(deltaMs -> {
      if (this.triggerTimeoutMs > 0) {
        this.triggerTimeoutMs = LXUtils.max(0, this.triggerTimeoutMs - deltaMs);
        redraw();
      }
    });
  }

  public UISwitch setMomentary(boolean momentary) {
    this.isMomentary = momentary;
    return this;
  }

  @Override
  public UIParameterControl setParameter(LXNormalizedParameter parameter) {
    if (!(parameter instanceof BooleanParameter)) {
      throw new IllegalArgumentException("UISwitch may only take BooleanParameter");
    }
    super.setParameter(parameter);
    setMomentary(getBooleanParameter().getMode() == BooleanParameter.Mode.MOMENTARY);
    return this;
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter instanceof TriggerParameter) {
      if (((TriggerParameter) parameter).isOn() && isVisible(true)) {
        this.triggerTimeoutMs = TRIGGER_DURATION_MS;
      }
    }
    super.onParameterChanged(parameter);
  }

  private BooleanParameter getBooleanParameter() {
    return (BooleanParameter) this.parameter;
  }

  @Override
  public BooleanParameter getTriggerTarget() {
    return isMappable() ? getTriggerParameter() : null;
  }

  @Override
  public BooleanParameter getTriggerSource() {
    return isMappable() ? getTriggerParameter() : null;
  }

  private BooleanParameter getTriggerParameter() {
    if (this.parameter != null && this.parameter.getParent() != null) {
      return getBooleanParameter();
    }
    return null;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    vg.strokeColor(ui.theme.controlBorderColor);
    if (isEnabled() && (this.parameter != null)) {
      if (isTriggerParameter()) {
        vg.fillColor(LXColor.lerp(
          ui.theme.controlBackgroundColor.get(),
          ui.theme.primaryColor.get(),
          this.triggerTimeoutMs / TRIGGER_DURATION_MS
        ));
      } else {
        vg.fillColor(this.momentaryPressEngaged ?
          (this.momentaryPressValid ? ui.theme.primaryColor : ui.theme.controlDisabledColor) :
          ((this.parameter.getValue() > 0) ? ui.theme.primaryColor : ui.theme.controlBackgroundColor)
        );
      }
    } else {
      vg.fillColor(ui.theme.controlDisabledColor);
    }
    vg.beginPath();
    vg.rect(SWITCH_MARGIN, 0, SWITCH_SIZE, SWITCH_SIZE);
    vg.fill();
    vg.stroke();

    super.onDraw(ui, vg);
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    super.onKeyPressed(keyEvent, keyChar, keyCode);
    if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      if (this.parameter != null) {
        keyEvent.consume();
        this.momentaryPressValid = this.isMomentary;
        this.momentaryPressEngaged = this.isMomentary;
        if (this.isMomentary) {
          getBooleanParameter().setValue(true);
        } else {
          if (this.useCommandEngine) {
            getLX().command.perform(new LXCommand.Parameter.Toggle(getBooleanParameter()));
          } else {
            getBooleanParameter().toggle();
          }
        }
      }
    }
  }

  @Override
  protected void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
    super.onKeyReleased(keyEvent, keyChar, keyCode);
    if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      if ((this.parameter != null) && this.isMomentary) {
        keyEvent.consume();
        getBooleanParameter().setValue(false);
      }
    }
    if (this.momentaryPressEngaged) {
      this.momentaryPressEngaged = false;
      redraw();
    }
  }

  private boolean isOnSwitch(float mx, float my) {
    return
      (mx >= SWITCH_MARGIN) &&
      (mx < SWITCH_SIZE + SWITCH_MARGIN) &&
      (my >= 0) &&
      (my < SWITCH_SIZE);
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    if (this.parameter != null && isOnSwitch(mx, my)) {
      this.momentaryPressValid = this.isMomentary;
      this.momentaryPressEngaged = this.isMomentary;
      if (this.isMomentary) {
        getBooleanParameter().setValue(true);
      } else {
        if (this.useCommandEngine) {
          getLX().command.perform(new LXCommand.Parameter.Toggle(getBooleanParameter()));
        } else {
          getBooleanParameter().toggle();
        }
      }
    }
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (isEnabled() && this.momentaryPressEngaged) {
      mouseEvent.consume();
      boolean mouseDownMomentary = isOnSwitch(mx, my);
      if (mouseDownMomentary != this.momentaryPressValid) {
        this.momentaryPressValid = mouseDownMomentary;
        redraw();
      }
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.onMouseReleased(mouseEvent, mx, my);
    if (this.isMomentary && (this.parameter != null) && isOnSwitch(mx, my)) {
      getBooleanParameter().setValue(false);
    }
    if (this.momentaryPressEngaged) {
      this.momentaryPressEngaged = false;
      redraw();
    }
  }

}
