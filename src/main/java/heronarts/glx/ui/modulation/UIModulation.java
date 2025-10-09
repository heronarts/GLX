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

package heronarts.glx.ui.modulation;

import java.util.ArrayList;
import java.util.List;

import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITheme;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UIParameterLabel;
import heronarts.glx.ui.component.UISlider;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LXComponent;
import heronarts.lx.command.LXCommand;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.modulation.LXParameterModulation;
import heronarts.lx.modulation.LXTriggerModulation;
import heronarts.lx.modulator.LXTriggerSource;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameterListener;

public abstract class UIModulation extends UI2dContainer implements UIFocus, UIContextActions {

  protected class UIMappingLabel extends UIParameterLabel {

    private boolean showSourceLabel = false;
    private boolean targetMode = false;
    private final LXParameterModulation modulation;
    private final VGraphics.Image icon;
    private final float iconPadding;

    private UIMappingLabel(LXParameterModulation modulation, VGraphics.Image icon, float iconPadding) {
      super(0, PADDING, UIModulation.this.width, 12);
      setRoot(modulation.getLX().engine.mixer);
      setParameter(modulation.target);
      this.modulation = modulation;
      this.icon = icon;
      this.iconPadding = iconPadding;
      addListener(modulation.enabled, this.redraw);
    }

    private void setShowSource() {
      this.showSourceLabel = true;
      if (this.modulation.source instanceof LXComponent source) {
        addListener(source.label, this.redraw);
      }
    }

    private void setEditorMode() {
      this.showSourceLabel = false;
      this.targetMode = true;
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      final UITheme.Color fontColor = modulation.enabled.isOn() ? ui.theme.labelColor : ui.theme.controlDisabledTextColor;
      final UITheme.Color tintColor = modulation.enabled.isOn() ? ui.theme.controlActiveTextColor : ui.theme.controlDisabledTextColor;

      vg.fontFace(ui.theme.getLabelFont());

      float tx = 0;
      if (this.showSourceLabel) {
        tx = PADDING;
        final String source = this.modulation.source.getLabel();
        vg.beginPath();
        vg.fillColor(fontColor);
        vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
        vg.text(tx, this.height / 2, source);
        vg.fill();
        tx += vg.textWidth(source);
      }
      tx += this.iconPadding;

      this.icon.setTint(tintColor);
      vg.beginPath();
      vg.image(this.icon, tx, -1.5f);
      vg.fill();
      this.icon.noTint();
      tx += this.icon.width;

      final String str = this.targetMode ?
        this.modulation.source.getCanonicalLabel(ui.lx.engine.mixer) :
        clipTextToWidth(vg, getLabel(), this.width - PADDING - tx);

      vg.beginPath();
      vg.fillColor(fontColor);
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
      vg.text(tx, this.height / 2, str);
      vg.fill();

    }

  }


  protected static final int PADDING = 4;

  public final LXParameterModulation modulation;
  protected final UI ui;

  protected UIModulation(final UI ui, final LXParameterModulation modulation, float x, float y, float w, float h) {
    super(x, y, w, h);
    this.modulation = modulation;
    this.ui = ui;
    setBackgroundColor(ui.theme.listItemBackgroundColor);
    setFocusBackgroundColor(ui.theme.listItemFocusedBackgroundColor);
  }

  protected abstract void remove();

  protected int getModulationColor(UI ui) {
    return this.modulation.enabled.isOn() ?
      this.modulation.color.getColor() :
      ui.theme.controlDisabledColor.get();
  }

  @Override
  public void drawFocus(UI ui, VGraphics vg) {
    vg.beginPath();
    vg.strokeColor(getModulationColor(ui));
    vg.line(.5f, 0, .5f, this.height);
    vg.stroke();
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (keyEvent.isDelete()) {
      keyEvent.consume();
      remove();
    }
    if (keyEvent.isEnter()) {
      keyEvent.consume();
      this.modulation.enabled.toggle();
    }
  }

  @Override
  protected void onFocus(Event event) {
    super.onFocus(event);
    getUI().setHighlightParameterModulation(this.modulation);
  }

  @Override
  protected void onBlur() {
    super.onBlur();
    getUI().setHighlightParameterModulation(null);
  }

  private final UIContextActions.Action actionEnable = new UIContextActions.Action("Enable") {
    @Override
    public void onContextAction(UI ui) {
      modulation.enabled.toggle();
    }
  };

  public List<UIContextActions.Action> getContextActions() {
    List<UIContextActions.Action> actions = new ArrayList<UIContextActions.Action>();
    actionEnable.setLabel(this.modulation.enabled.isOn() ? "Disable" : "Enable");
    actions.add(actionEnable);
    return actions;
  }

  public static class Trigger extends UIModulation {

    private static final int HEIGHT = 12 + 2 * PADDING;
    private static final int MODE_HEIGHT = 30 + 2 * PADDING;

    private final LXTriggerModulation trigger;
    private final UIMappingLabel label;

    public Trigger(UI ui, LXComponent context, final LXTriggerModulation trigger, float x, float y, float w) {
      super(ui, trigger, x, y, w, HEIGHT);
      this.trigger = trigger;
      this.label = new UIMappingLabel(trigger, ui.theme.iconTriggerSource, 0);

      BooleanParameter trigSource = null;
      if (context instanceof LXTriggerSource triggerSource) {
        trigSource = triggerSource.getTriggerSource();
      }
      if ((this.modulation.source != context) && (this.modulation.source != trigSource)) {
        this.label.setShowSource();
      }
      addChildren(this.label);

      final EnumParameter<?> mode = trigger.getModeParameter();
      if (mode != null) {
        setHeight(MODE_HEIGHT);
        new UIDropMenu(PADDING, PADDING+16, this.width - 2*PADDING, 14, mode)
        .addToContainer(this);
      }
    }

    @Override
    protected void remove() {
      this.ui.lx.command.perform(new LXCommand.Modulation.RemoveTrigger(this.ui.lx.engine.modulation, this.trigger));
    }

  }

  public static class Compound extends UIModulation {

    private static final int HEIGHT = 34 + 2 * PADDING;
    protected final LXCompoundModulation modulation;
    private final UIMappingLabel label;

    public Compound(final UI ui, final LXCompoundModulation modulation, float x, float y, float w) {
      this(ui, null, modulation, x, y, w);
    }

    public Compound(final UI ui, LXComponent context, final LXCompoundModulation modulation, float x, float y, float w) {
      super(ui, modulation, x, y, w, HEIGHT);

      this.modulation = modulation;
      this.label = new UIMappingLabel(modulation, ui.theme.iconMap, 3);
      if (context == null) {
        this.label.setEditorMode();
      } else if (modulation.source != context) {
        this.label.setShowSource();
      }

      final UISlider slider = (UISlider)
        new UISlider(2 * PADDING + 26, PADDING + 16, width - 3 * PADDING - 26, 16)
        .setFillColor(getModulationColor(ui))
        .setShowLabel(false)
        .setParameter(modulation.range);

      addChildren(
        this.label,
        slider,
        new UIButton(PADDING + 2, PADDING + 18, 24, 12).setParameter(modulation.polarity)
      );

      LXParameterListener updateColor = p -> {
        slider.setFillColor(getModulationColor(ui));
        redraw();
      };
      addListener(modulation.clr, updateColor);
      addListener(modulation.enabled, updateColor);
    }

    @Override
    protected void remove() {
      this.ui.lx.command.perform(new LXCommand.Modulation.RemoveModulation(this.ui.lx.engine.modulation, this.modulation));
    }
  }
}
