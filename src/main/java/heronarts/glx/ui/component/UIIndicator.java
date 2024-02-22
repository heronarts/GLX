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

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UITheme.Color;
import heronarts.glx.ui.UITriggerSource;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.TriggerParameter;

public class UIIndicator extends UI2dComponent implements UITriggerSource {

  private final BooleanParameter bool;
  private double timeout = 0;

  private static final int DEFAULT_TIMER_MS = 200;

  public boolean timerMode = false;
  public double indicatorTimeMs = -1;

  private boolean triggerable = false;
  private boolean clickable = true;

  private Color indicatorBackgroundColor;
  private final UI ui;

  public UIIndicator(UI ui, final BooleanParameter bool) {
    this(ui, 0, 0, 12, 12, bool);
  }

  public UIIndicator(UI ui, float w, float h, final BooleanParameter bool) {
    this(ui, 0, 0, w, h, bool);
  }

  public UIIndicator(UI ui, float x, float y, float w, float h, final BooleanParameter bool) {
    super(x, y, w, h);
    setBorderRounding(4);
    setDescription(bool.getDescription());
    this.ui = ui;
    this.bool = bool;
    this.indicatorBackgroundColor = ui.theme.controlBackgroundColor;
    addListener(bool, p -> {
      if (bool.isOn()) {
        setBackgroundColor(ui.theme.primaryColor);
        this.timeout = 0;
      } else if (!this.timerMode) {
        setBackgroundColor(this.indicatorBackgroundColor);
      }
    }, true);
    addLoopTask(deltaMs -> {
      if (this.timerMode && (this.timeout < this.indicatorTimeMs)) {
        this.timeout += deltaMs;
        if (this.timeout >= this.indicatorTimeMs) {
          setBackgroundColor(this.indicatorBackgroundColor);
        }
      }
    });
  }

  public UIIndicator setEnabled(boolean enabled) {
    setIndicatorBackgroundColor(enabled ? this.ui.theme.controlBackgroundColor : this.ui.theme.controlDisabledColor);
    return this;
  }

  public UIIndicator setIndicatorBackgroundColor(Color indicatorBackground) {
    if (this.indicatorBackgroundColor != indicatorBackground) {
      setBackgroundColor(this.indicatorBackgroundColor = indicatorBackground);
    }
    return this;
  }

  public UIIndicator setTriggerable(boolean triggerable) {
    this.triggerable = triggerable;
    return this;
  }

  public UIIndicator setClickable(boolean clickable) {
    this.clickable = clickable;
    return this;
  }

  public UIIndicator setIndicatorTime(boolean timerMode) {
    if (timerMode) {
      setIndicatorTime(DEFAULT_TIMER_MS);
    } else {
      this.timerMode = false;
    }
    return this;
  }

  public UIIndicator setIndicatorTime(double indicatorTimeMs) {
    this.timerMode = true;
    this.indicatorTimeMs = indicatorTimeMs;
    return this;
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (!this.clickable) {
      return;
    }
    if (mouseEvent.isCommand()) {
      mouseEvent.consume();
      this.bool.setValue(true);
    }
  }

  @Override
  public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    if (!this.clickable) {
      return;
    }
    if (mouseEvent.isCommand() && !(this.bool instanceof TriggerParameter)) {
      mouseEvent.consume();
      this.bool.setValue(false);
    }
  }

  @Override
  public BooleanParameter getTriggerSource() {
    return this.triggerable ? this.bool : null;
  }
}
