package heronarts.glx.ui.component;

import java.util.Objects;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIControlTarget;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITriggerSource;
import heronarts.glx.ui.UITriggerTarget;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;

public class UICheckbox extends UIParameterComponent implements UIControlTarget, UITriggerSource, UITriggerTarget, UIFocus {

  public final static int DEFAULT_WIDTH = 10;
  public final static int DEFAULT_HEIGHT = 10;

  protected boolean active = false;
  protected boolean isMomentary = false;

  private boolean triggerable = false;

  protected boolean enabled = true;

  private BooleanParameter parameter = null;

  private final LXParameterListener parameterListener = (p) -> {
    setActive(this.parameter.isOn(), false);
  };

  public UICheckbox() {
    this(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public UICheckbox(float w, BooleanParameter p) {
    this(0, 0, w, w, p);
  }

  public UICheckbox(float x, float y) {
    this(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public UICheckbox(float x, float y, BooleanParameter p) {
    this(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, p);
  }

  public UICheckbox(float x, float y, float w, float h) {
    this(x, y, w, h, null);
  }

  public UICheckbox(float x, float y, float w, float h, BooleanParameter p) {
    super(x, y, w, h);
    setParameter(p);
  }

  public UICheckbox setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      redraw();
    }
    return this;
  }

  public UICheckbox setTriggerable(boolean triggerable) {
    this.triggerable = triggerable;
    return this;
  }

  @Override
  public String getDescription() {
    if (this.parameter != null) {
      return UIParameterControl.getDescription(this.parameter);
    }
    return super.getDescription();
  }

  @Override
  public LXListenableNormalizedParameter getParameter() {
    return this.parameter;
  }

  public UICheckbox removeParameter() {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    this.parameter = null;
    return this;
  }

  public UICheckbox setParameter(BooleanParameter parameter) {
    Objects.requireNonNull(parameter, "Cannot set null UICheckbox.setParameter() - use removeParameter() instead");
    if (parameter != this.parameter) {
      removeParameter();
      if (parameter != null) {
        this.parameter = parameter;
        this.parameter.addListener(this.parameterListener);
        setMomentary(this.parameter.getMode() == BooleanParameter.Mode.MOMENTARY);
        setActive(this.parameter.isOn(), false);
      }
    }
    return this;
  }

  public UICheckbox setMomentary(boolean momentary) {
    this.isMomentary = momentary;
    return this;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    // A lighter gray background color when the button is disabled, or it's engaged
    // with a mouse press but the mouse has moved off the active button
    UIColor color = this.enabled ? ui.theme.controlTextColor : ui.theme.controlDisabledColor;

    vg.beginPath();
    vg.strokeColor(color);
    vg.rect(1.5f, 1.5f, this.width-3, this.height-3);
    vg.stroke();

    if (this.active) {
      vg.beginPath();
      vg.fillColor(color);
      vg.rect(3, 3, this.width - 6, this.height - 6);
      vg.fill();
    }
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (this.enabled) {
      setActive(this.isMomentary ? true : !this.active);
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    if (this.enabled) {
      if (this.isMomentary) {
        setActive(false);
      }
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      keyEvent.consume();
      if (this.enabled) {
        setActive(this.isMomentary ? true : !this.active);
      }
    }
  }

  @Override
  protected void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
    if ((keyCode == KeyEvent.VK_SPACE) || keyEvent.isEnter()) {
      keyEvent.consume();
      if (this.enabled && this.isMomentary) {
        setActive(false);
      }
    }
  }

  public boolean isActive() {
    return this.active;
  }

  public UICheckbox setActive(boolean active) {
    return setActive(active, true);
  }

  protected UICheckbox setActive(boolean active, boolean pushToParameter) {
    if (this.active != active) {
      this.active = active;
      if (pushToParameter) {
        if (this.parameter != null) {
          if (this.isMomentary) {
            this.parameter.setValue(active);
          } else {
            getLX().command.perform(new LXCommand.Parameter.SetNormalized(this.parameter, active));
          }
        }
      }
      onToggle(active);
      redraw();
    }
    return this;
  }

  public UICheckbox toggle() {
    return setActive(!this.active);
  }

  /**
   * Subclasses may override this to handle changes to the button's state
   *
   * @param active Whether button is active
   */
  protected void onToggle(boolean active) {
  }


  @Override
  public LXNormalizedParameter getControlTarget() {
    if (isMappable()) {
      if (this.parameter != null) {
        if (this.parameter.getParent() != null) {
          return this.parameter;
        }
      } else {
        return getTriggerParameter();
      }
    }
    return null;
  }

  @Override
  public BooleanParameter getTriggerSource() {
    return this.triggerable ? getTriggerParameter() : null;
  }

  @Override
  public BooleanParameter getTriggerTarget() {
    return this.triggerable ? getTriggerParameter() : null;
  }

  private BooleanParameter getTriggerParameter() {
    if (this.parameter != null && this.parameter.isMappable() && this.parameter.getParent() != null) {
      return this.parameter;
    }
    return null;
  }

  @Override
  public void dispose() {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    super.dispose();
  }

}
