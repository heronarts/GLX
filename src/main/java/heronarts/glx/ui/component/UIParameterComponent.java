package heronarts.glx.ui.component;

import java.util.ArrayList;
import java.util.List;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIContextActions;
import heronarts.lx.command.LXCommand;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;

public abstract class UIParameterComponent extends UI2dComponent implements UIContextActions {

  public static final float DEFAULT_HEIGHT = 16;

  protected boolean useCommandEngine = true;

  private boolean enableContextActions = true;

  protected UIParameterComponent(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public abstract LXParameter getParameter();

  public UIParameterComponent setEnableContextActions(boolean enableContextActions) {
    this.enableContextActions = enableContextActions;
    return this;
  }

  public UIParameterComponent setUseCommandEngine(boolean useCommandEngine) {
    this.useCommandEngine = useCommandEngine;
    return this;
  }

  public String getOscAddress() {
    LXParameter parameter = getParameter();
    if (parameter != null) {
      return LXOscEngine.getOscAddress(parameter);
    }
    return null;
  }

  @Override
  public List<Action> getContextActions() {
    if (!this.enableContextActions) {
      return null;
    }

    List<Action> actions = new ArrayList<Action>();
    LXParameter parameter = getParameter();
    if (parameter != null && !(parameter instanceof BooleanParameter)) {
      actions.add(new UIContextActions.Action.ResetParameter(parameter));
    }
    String oscAddress = getOscAddress();
    if (oscAddress != null) {
      actions.add(new UIContextActions.Action.CopyOscAddress(oscAddress));
    }
    return actions;
  }

  private boolean mouseEditUndoable = false;
  private LXNormalizedParameter mouseEditParameter = null;
  private LXCommand.Parameter.SetNormalized mouseEditCommand = null;
  private LXCommand.Parameter.SetValue mouseEditModulationRangeCommand = null;

  private void resetMouseEdit() {
    this.mouseEditUndoable = false;
    this.mouseEditParameter = null;
    this.mouseEditCommand = null;
    this.mouseEditModulationRangeCommand = null;
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    resetMouseEdit();
    LXParameter parameter = getParameter();
    if (parameter != null && parameter instanceof LXNormalizedParameter) {
      if (this.useCommandEngine) {
        this.mouseEditUndoable = true;
        this.mouseEditParameter = (LXNormalizedParameter) parameter;
      }
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.onMouseReleased(mouseEvent, mx, my);
    resetMouseEdit();
  }

  protected void setModulationRangeCommand(CompoundParameter range, double newValue) {
    this.mouseEditCommand = null;
    if ((this.mouseEditModulationRangeCommand == null) ||
        (this.mouseEditModulationRangeCommand.getParameter() != range)) {
      this.mouseEditModulationRangeCommand = new LXCommand.Parameter.SetValue(range, newValue);
    }
    getLX().command.perform(this.mouseEditModulationRangeCommand.update(newValue));
  }

  protected void setNormalizedCommand(double newValue) {
    if (this.mouseEditUndoable) {
      this.mouseEditModulationRangeCommand = null;
      if (this.mouseEditCommand == null) {
        this.mouseEditCommand = new LXCommand.Parameter.SetNormalized(this.mouseEditParameter);
      }
      getLX().command.perform(this.mouseEditCommand.update(newValue));
    } else {
      LXParameter parameter = getParameter();
      if (parameter != null && parameter instanceof LXNormalizedParameter) {
        if (this.useCommandEngine) {
          getLX().command.perform(new LXCommand.Parameter.SetNormalized((LXNormalizedParameter) parameter, newValue));
        } else {
          ((LXNormalizedParameter) parameter).setNormalized(newValue);
        }
      }
    }

  }

}