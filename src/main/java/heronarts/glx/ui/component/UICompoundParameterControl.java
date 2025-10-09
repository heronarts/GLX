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
import heronarts.glx.event.Event;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.modulation.UIModulationEditor;

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
    private boolean enabled = false;

    private ModulationRedrawListener(LXCompoundModulation modulation) {
      this.modulation = modulation;
      this.modulation.range.addListener(this);
      this.modulation.polarity.addListener(this);
      this.modulation.enabled.addListener(this);
      this.modulation.color.addListener(this);
    }

    private void enable() {
      // We register listeners for all modulations, but only enable actual redraw
      // calls for those that have actually been shown by devices on-screen (e.g.
      // if there are so many that a knob/slider only shows the first few, no need
      // to redraw for the ones not displayed)
      this.enabled = true;
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      if (this.enabled) {
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

  protected void enableModulationRedraw(LXCompoundModulation modulation) {
    ModulationRedrawListener listener = this.modulationRedrawListeners.get(modulation);
    if (listener != null) {
      listener.enable();
    } else {
      LX.error(new IllegalStateException(getClass().getSimpleName() + " cannot enable modulation redraw for unregistered modulation: " + modulation));
    }
  }

  public static void addModulationContextActions(LX lx, UI2dComponent control, List<UIContextActions.Action> actions, LXCompoundModulation.Target target) {
    final List<? extends LXCompoundModulation> modulations = target.getModulations();
    if (!modulations.isEmpty()) {
      actions.add(UIContextActions.createAction("Remove Modulation", ui -> {
        ui.lx.command.perform(new LXCommand.Modulation.RemoveModulations(target));
      }));
      if (!lx.engine.performanceMode.isOn()) {
        for (LXCompoundModulation modulation : modulations) {
          if (modulation.scope == lx.engine.modulation) {
            actions.add(UIContextActions.createAction("Show Modulation", ui -> {
              ui.setHighlightModulationTarget(target);
            }));
            break;
          }
        }
      }
      if (modulations.size() > 0) {
        actions.add(UIContextActions.createAction("Edit Modulations", ui -> {
          final UIModulationEditor editor = new UIModulationEditor(ui, target);
          ui.showContextOverlay(editor, control, UI.Position.TOP_RIGHT.margin(-10, -10), UI.Position.TOP_LEFT.margin(-10, -10));
          editor.focus(Event.NONE);
        }));
      }
    }
  }

  @Override
  public List<UIContextActions.Action> getContextActions() {
    List<UIContextActions.Action> actions = super.getContextActions();
    if (this.parameter instanceof LXCompoundModulation.Target modulationTarget) {
      addModulationContextActions(getLX(), this, actions, modulationTarget);
    }
    return actions;
  }

  @Override
  public void dispose() {
    if (this.parameter instanceof LXCompoundModulation.Target modulationTarget) {
      modulationTarget.removeModulationListener(this.modulationListener);
    }
    clearModulationRedrawListeners();
    super.dispose();
  }
}
