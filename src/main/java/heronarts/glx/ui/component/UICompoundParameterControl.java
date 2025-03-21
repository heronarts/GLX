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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.LX;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UITimerTask;

public class UICompoundParameterControl extends UIParameterControl {
  private double lastParameterValue = 0;

  private final UITimerTask checkRedrawTask = new UITimerTask(30, UITimerTask.Mode.FPS) {
    @Override
    public void run() {
      double parameterValue = getCompoundNormalized();
      if (parameterValue != lastParameterValue) {
        redraw();
      }
      lastParameterValue = parameterValue;
    }
  };

  private class ModulationRedrawListener implements LXParameterListener {

    private final LXCompoundModulation modulation;
    private boolean registered = false;

    private ModulationRedrawListener(LXCompoundModulation modulation) {
      this.modulation = modulation;
      this.modulation.range.addListener(this);
      this.modulation.polarity.addListener(this);
      this.modulation.enabled.addListener(this);
      this.modulation.color.addListener(this);
    }

    private void register() {
      this.registered = true;
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      if (this.registered) {
        redraw();
      }
    }

    private void dispose() {
      this.modulation.range.removeListener(this);
      this.modulation.polarity.removeListener(this);
      this.modulation.enabled.removeListener(this);
      this.modulation.color.removeListener(this);
    }
  }

  private final Map<LXCompoundModulation, ModulationRedrawListener> modulationRedrawListeners = new HashMap<>();

  private final LXCompoundModulation.Listener modulationListener = new LXCompoundModulation.Listener() {

    @Override
    public void modulationAdded(LXCompoundModulation.Target parameter, LXCompoundModulation modulation) {
      ModulationRedrawListener existing = modulationRedrawListeners.get(modulation);
      if (existing != null) {
        LX.error(new IllegalStateException("Cannot add redundant LXCompoundModulation to " + UICompoundParameterControl.this.getClass().getSimpleName() + ": " + this + " -> " + modulation));
      } else {
        modulationRedrawListeners.put(modulation, new ModulationRedrawListener(modulation));
      }
    }

    @Override
    public void modulationRemoved(LXCompoundModulation.Target parameter, LXCompoundModulation modulation) {
      ModulationRedrawListener listener = modulationRedrawListeners.remove(modulation);
      if (listener != null) {
        listener.dispose();
      }
    }
  };

  protected UICompoundParameterControl(float x, float y, float w, float h) {
    super(x, y, w, h);
    addLoopTask(this.checkRedrawTask);
  }

  private void clearModulationRedrawListeners() {
    for (ModulationRedrawListener modulation : this.modulationRedrawListeners.values()) {
      modulation.dispose();
    }
    this.modulationRedrawListeners.clear();
  }

  @Override
  public UIParameterControl setParameter(LXNormalizedParameter parameter) {
    if (this.parameter instanceof LXCompoundModulation.Target) {
      ((LXCompoundModulation.Target) this.parameter).removeModulationListener(this.modulationListener);
    }
    clearModulationRedrawListeners();
    super.setParameter(parameter);
    if (parameter instanceof LXCompoundModulation.Target target) {
      target.getModulations().forEach(modulation -> this.modulationListener.modulationAdded(target, modulation));
      ((LXCompoundModulation.Target) parameter).addModulationListener(this.modulationListener);
    }
    return this;
  }

  protected double getCompoundNormalized() {
    if (this.parameter != null) {
      return this.parameter.getNormalized();
    }
    return 0;
  }

  protected void registerModulation(LXCompoundModulation modulation) {
    ModulationRedrawListener listener = this.modulationRedrawListeners.get(modulation);
    if (listener != null) {
      listener.register();
    } else {
      LX.error(new IllegalStateException(getClass().getSimpleName() + " cannot register modulation with no listener: " + modulation));
    }
  }

  public static void addModulationContextActions(LX lx, List<UIContextActions.Action> actions, LXCompoundModulation.Target target) {
    final List<? extends LXCompoundModulation> modulations = target.getModulations();
    if (!modulations.isEmpty()) {
      actions.add(new UIContextActions.Action("Remove Modulation") {
        @Override
        public void onContextAction(UI ui) {
          ui.lx.command.perform(new LXCommand.Modulation.RemoveModulations(target));
        }
      });
      if (!lx.engine.performanceMode.isOn()) {
        for (LXCompoundModulation modulation : modulations) {
          if (modulation.scope == lx.engine.modulation) {
            actions.add(new UIContextActions.Action("Show Modulation") {
              @Override
              public void onContextAction(UI ui) {
                ui.setHighlightModulationTarget(target);
              }
            });
            break;
          }
        }
      }
    }
  }

  @Override
  public List<UIContextActions.Action> getContextActions() {
    List<UIContextActions.Action> actions = super.getContextActions();
    if (this.parameter instanceof LXCompoundModulation.Target) {
      addModulationContextActions(getLX(), actions, (LXCompoundModulation.Target) this.parameter);
    }
    return actions;
  }

  @Override
  public void dispose() {
    if (this.parameter instanceof LXCompoundModulation.Target) {
      ((LXCompoundModulation.Target) this.parameter).removeModulationListener(this.modulationListener);
    }
    clearModulationRedrawListeners();
    super.dispose();
  }
}
