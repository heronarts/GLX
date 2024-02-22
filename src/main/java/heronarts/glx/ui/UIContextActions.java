/**
 * Copyright 2018- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui;

import java.util.List;

import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;

public interface UIContextActions {

  public static abstract class Action {

    private String label;

    public Action(String label) {
      this.label = label;
    }

    public String getLabel() {
      return this.label;
    }

    public String getDescription() {
      return null;
    }

    public Action setLabel(String label) {
      this.label = label;
      return this;
    }

    @Override
    public String toString() {
      return this.label;
    }

    public abstract void onContextAction(UI ui);

    public static class ToggleParameter extends Action {

      private final String onLabel, offLabel;
      private final BooleanParameter parameter;

      public ToggleParameter(BooleanParameter parameter) {
        this(parameter, "Toggle " + parameter.getLabel());
      }

      public ToggleParameter(BooleanParameter parameter, String label) {
        this(parameter, label, label);
      }

      public ToggleParameter(BooleanParameter parameter, String onLabel, String offLabel) {
        super(parameter.isOn() ? onLabel : offLabel);
        this.onLabel = onLabel;
        this.offLabel = offLabel;
        this.parameter = parameter;
      }

      @Override
      public String getLabel() {
        return this.parameter.isOn() ? this.onLabel : this.offLabel;
      }

      @Override
      public void onContextAction(UI ui) {
        ui.lx.command.perform(new LXCommand.Parameter.Toggle(this.parameter));
      }
    }

    public static class ResetParameter extends Action {

      private final LXParameter parameter;

      public ResetParameter(LXParameter parameter) {
        super("Reset value");
        this.parameter = parameter;
      }

      @Override
      public void onContextAction(UI ui) {
        ui.lx.command.perform(new LXCommand.Parameter.Reset(this.parameter));
      }
    }

    public static class CopyOscAddress extends Action {
      private final String oscAddress;

      public CopyOscAddress(String oscAddress) {
        super("Copy OSC address");
        this.oscAddress = oscAddress;
      }

      @Override
      public void onContextAction(UI ui) {
        ui.lx.setSystemClipboardString(oscAddress);
      }
    }
  }

  /**
   * Returns a list of context actions that should be shown for this item
   *
   * @return List of context actions
   */
  public List<Action> getContextActions();

}
