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

import java.util.List;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.DiscreteColorParameter;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.modulation.LXCompoundModulation.Target;
import heronarts.lx.parameter.LXParameter;

public abstract class UINumberBox extends UIInputBox {

  protected boolean hasShiftMultiplier = false;
  protected float shiftMultiplier = 1;

  protected UINumberBox() {
    this(0, 0, 0, 0);
  }

  protected UINumberBox(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UINumberBox setFillStyle(FillStyle fillStyle) {
    if (this.fillStyle != fillStyle) {
      this.fillStyle = fillStyle;
      if (this.hasFill) {
        redraw();
      }
    }
    return this;
  }

  public UINumberBox setFill(boolean hasFill) {
    if (this.hasFill != hasFill) {
      this.hasFill = hasFill;
      redraw();
    }
    return this;
  }

  public UINumberBox setFillColor(int fillColor) {
    if (!this.hasFill || (this.fillColor != fillColor)) {
      this.hasFill = true;
      this.fillColor = fillColor;
      redraw();
    }
    return this;
  }

  /**
   * Sets a multiplier by which the amount value changes are modulated
   * when the shift key is down. Either for more precise control or
   * larger jumps, depending on the component.
   *
   * @param shiftMultiplier Amount to multiply by
   * @return this
   */
  public UINumberBox setShiftMultiplier(float shiftMultiplier) {
    this.hasShiftMultiplier = true;
    this.shiftMultiplier = shiftMultiplier;
    return this;
  }

  private LXCompoundModulation.Target modulationTarget = null;
  private DiscreteColorParameter modulationColor = null;

  private void setModulationColor(DiscreteColorParameter modulationColor) {
    if (this.modulationColor != modulationColor) {
      if (this.modulationColor != null) {
        this.modulationColor.removeListener(this.redraw);
      }
      this.modulationColor = modulationColor;
      if (modulationColor != null) {
        this.modulationColor.addListener(this.redraw);
      }
      redraw();
    }
  }

  private void setModulationTargetColor(LXCompoundModulation.Target parameter) {
    final List<LXCompoundModulation> modulations = parameter.getModulations();
    setModulationColor(modulations.isEmpty() ? null : modulations.get(0).color);
  }

  private final LXCompoundModulation.Listener modulationListener = new LXCompoundModulation.Listener() {
    public void modulationAdded(Target parameter, LXCompoundModulation modulation) {
      setModulationTargetColor(parameter);
    }

    public void modulationRemoved(Target parameter, LXCompoundModulation modulation) {
      setModulationTargetColor(parameter);
    }
  };

  protected void setModulationTarget(LXParameter parameter) {
    final LXCompoundModulation.Target target =
      (parameter instanceof LXCompoundModulation.Target) ?
      (LXCompoundModulation.Target) parameter :
       null;
    if (this.modulationTarget == target) {
      return;
    }
    if (this.modulationTarget != null) {
      this.modulationTarget.removeModulationListener(this.modulationListener);
      setModulationColor(null);
    }
    this.modulationTarget = target;
    if (this.modulationTarget != null) {
      this.modulationTarget.addModulationListener(this.modulationListener);
      setModulationTargetColor(this.modulationTarget);
    }
  }

  @Override
  public void drawBorder(UI ui, VGraphics vg) {
    super.drawBorder(ui, vg);

    // Fast and loose thread safety here, grab a final reference, noting
    // that the value may change after this on engine thread (which will
    // trigger another redraw call)
    final DiscreteColorParameter modColor = this.modulationColor;
    if (modColor != null) {
      vg.beginPath();
      vg.strokeColor(modColor.getColor());
      vg.line(1.5f, 1, 1.5f, this.height - 1);
      vg.stroke();
    }
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    LXParameter parameter = getParameter();
    if ((parameter != null) && isEditable() && mouseEvent.isDoubleClick()) {
      if (this.useCommandEngine) {
        getLX().command.perform(new LXCommand.Parameter.Reset(parameter));
      } else {
        parameter.reset();
      }
    }
  }

  @Override
  public List<UIContextActions.Action> getContextActions() {
    final List<UIContextActions.Action> actions = super.getContextActions();
    if ((actions != null) && (this.modulationTarget != null)) {
      UICompoundParameterControl.addModulationContextActions(getLX(), actions, this.modulationTarget);
    }
    return actions;
  }

  @Override
  public void dispose() {
    setModulationTarget(null);
    super.dispose();
  }

}
