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

import java.util.ArrayList;
import java.util.List;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UIControlTarget;
import heronarts.lx.LX;
import heronarts.lx.command.LXCommand;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;

public class UIButtonGroup extends UI2dContainer implements UIControlTarget, UIContextActions {

  private final static int DEFAULT_BUTTON_MARGIN = 4;

  private final DiscreteParameter parameter;

  public final UIButton[] buttons;

  private boolean internalStateUpdate = false;

  public UIButtonGroup(DiscreteParameter parameter, float x, float y, float w, float h) {
    this(parameter, x, y, w, h, false);
  }

  public UIButtonGroup(final DiscreteParameter parameter, float x, float y, float w, float h, final boolean hideFirst) {
    super(x, y, w, h);
    setLayout(UI2dContainer.Layout.HORIZONTAL, DEFAULT_BUTTON_MARGIN);

    this.parameter = parameter;
    final int range = parameter.getRange();

    this.buttons = new UIButton[range];
    final LX lx = this.parameter.getParent().getLX();
    final String[] options = this.parameter.getOptions();

    final int numButtons = range - (hideFirst ? 1 : 0);
    final int buttonWidth = (int) (w - (numButtons-1) * DEFAULT_BUTTON_MARGIN) / numButtons;

    for (int i = hideFirst ? 1 : 0; i < range; ++i) {
      final int index = i;
      this.buttons[i] = (UIButton) new UIButton(0, 0, buttonWidth, h) {
        @Override
        public void onToggle(boolean enabled) {
          if (!internalStateUpdate) {
            if (enabled) {
              if (this.useCommandEngine) {
                lx.command.perform(new LXCommand.Parameter.SetIndex(parameter, index));
              } else {
                parameter.setIndex(index);
              }
            } else if (hideFirst) {
              if (this.useCommandEngine) {
                lx.command.perform(new LXCommand.Parameter.SetIndex(parameter, 0));
              } else {
                parameter.setIndex(0);
              }
            }
          }
        }
      }
      .setLabel(options[index])
      .addToContainer(this);
    }

    // Initialize and follow button state
    addListener(parameter, p -> {
      this.internalStateUpdate = true;
      final int activeIndex = parameter.getBaseIndex();
      for (int i = 0; i < this.buttons.length; ++i) {
        // NOTE: if hideFirst was true index 0 will be null
        if (this.buttons[i] != null) {
          this.buttons[i].setActive(i == activeIndex);
        }
      }
      this.internalStateUpdate = false;
    }, true);
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  @Override
  public LXNormalizedParameter getControlTarget() {
    return getMappableParameter(this.parameter);
  }

  @Override
  public List<Action> getContextActions() {
    String oscAddress = LXOscEngine.getOscAddress(this.parameter);
    if (oscAddress != null) {
      List<Action> list = new ArrayList<Action>();
      list.add(new UIContextActions.Action.CopyOscAddress(oscAddress));
    }
    return null;
  }

}
