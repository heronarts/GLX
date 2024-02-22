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

import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.command.LXCommand;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.vg.VGraphics;

public class UIDynamicColorPicker extends UIColorPicker implements UIFocus {

  private final LXDynamicColor dynamicColor;

  public UIDynamicColorPicker(LXDynamicColor dynamicColor) {
    this(0, 0, 16, 16, dynamicColor);
  }

  public UIDynamicColorPicker(float x, float y, float w, float h, LXDynamicColor dynamicColor) {
    super(x, y, w, h, dynamicColor.primary, true);
    setBorderRounding(4);
    this.dynamicColor = dynamicColor;
    addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
      @Override
      protected void run() {
        setDrawColor(dynamicColor.getColor());
      }
    });
    setFocusCorners(false);
  }

  @Override
  protected void drawFocus(UI ui, VGraphics vg) {
    vg.beginPath();
    vg.strokeColor(ui.theme.controlActiveTextColor);
    vg.strokeWidth(1);
    vgRoundedRect(vg, .5f, .5f, this.width-1, this.height-1);
    vg.stroke();
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (keyEvent.isDelete()) {
      keyEvent.consume();
      if (this.dynamicColor.getIndex() > 0) {
        hideOverlay();
        getLX().command.perform(new LXCommand.Palette.RemoveColor(this.dynamicColor));
      }
    }
    super.onKeyPressed(keyEvent, keyChar, keyCode);
  }

  @Override
  protected UIColorOverlay buildColorOverlay(UI ui) {
    return new UIDynamicColorOverlay(ui);
  }

  class UIDynamicColorOverlay extends UIColorPicker.UIColorOverlay {

    private final UI2dComponent
      blendMode,
      primaryColorSelector,
      secondaryColorSelector,
      arrowLabel,
      period;

    UIDynamicColorOverlay(UI ui) {
      super(ui, 38);

      // Horizontal break
      new UI2dComponent(12, 140, 220, 1) {}
      .setBorderColor(ui.theme.controlBorderColor)
      .addToContainer(this);

      UI2dContainer controls = UI2dContainer.newHorizontalContainer(16, 4);
      controls.setPosition(12, 148);

      new UIButton(48, 16, dynamicColor.mode)
      .addToContainer(controls);

      this.blendMode =
        new UIButton(44, dynamicColor.blendMode)
        .setEnumFormatter(ep -> {
          Object e = ep.getEnum();
          if (e == GradientUtils.BlendMode.HSVM) {
            return "Min";
          } else if (e == GradientUtils.BlendMode.HSVCW) {
            return "CW";
          } else if (e == GradientUtils.BlendMode.HSVCCW) {
            return "CCW";
          }
          return e.toString();
        })
        .addToContainer(controls);

      this.primaryColorSelector = new UIColorSelector(dynamicColor.primary)
      .addToContainer(controls);

      this.arrowLabel =
        new UILabel(16, 16, "↔")
        .setTextAlignment(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE)
        .setMargin(0, -4)
        .addToContainer(controls);

      this.secondaryColorSelector = new UIColorSelector(dynamicColor.secondary)
      .addToContainer(controls);

      this.period = new UIDoubleBox(48, dynamicColor.period)
      .setNormalizedMouseEditing(false)
      .setShiftMultiplier(60)
      .setProgressIndicator(new UIDoubleBox.ProgressIndicator() {
        @Override
        public boolean hasProgress() {
          return true;
        }
        @Override
        public double getProgress() {
          return dynamicColor.getBasis();
        }
      })
      .addToContainer(controls);

      new UIDynamicColorIndicator()
      .addToContainer(controls);

      focusColor(dynamicColor.primary);
      controls.addToContainer(this);

      addListener(dynamicColor.mode, p -> { setMode(); }, true);
    }

    private void setMode() {
      boolean isFixed = dynamicColor.mode.getEnum() == LXDynamicColor.Mode.FIXED;
      boolean isOscillate = dynamicColor.mode.getEnum() == LXDynamicColor.Mode.OSCILLATE;
      this.period.setVisible(!isFixed);
      this.blendMode.setVisible(isOscillate);
      this.primaryColorSelector.setVisible(isOscillate);
      this.arrowLabel.setVisible(isOscillate);
      this.secondaryColorSelector.setVisible(isOscillate);
      if (!isOscillate) {
        focusColor(dynamicColor.primary);
      }
    }

    void focusColor(ColorParameter color) {
      setColor(color);
      if (color == dynamicColor.primary) {
        this.primaryColorSelector.setBorderWeight(2);
        this.primaryColorSelector.setBorderColor(UI.get().theme.controlActiveTextColor);
        this.secondaryColorSelector.setBorder(false);
      } else {
        this.secondaryColorSelector.setBorderWeight(2);
        this.secondaryColorSelector.setBorderColor(UI.get().theme.controlActiveTextColor);
        this.primaryColorSelector.setBorder(false);
      }
    }

    private class UIColorSelector extends UI2dComponent {

      private final ColorParameter color;

      public UIColorSelector(ColorParameter color) {
        super(0, 0, 16, 16);
        this.color = color;
        setBackgroundColor(color.getColor());
        addListener(color, p -> { setBackgroundColor(color.getColor()); });
      }

      @Override
      public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
        focusColor(this.color);
        super.onMousePressed(mouseEvent, mx, my);
      }
    }

    private class UIDynamicColorIndicator extends UI2dComponent {
      public UIDynamicColorIndicator() {
        super(0, 0, 16, 16);
        setBackgroundColor(dynamicColor.getColor());

        addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
          @Override
          protected void run() {
            setBackgroundColor(dynamicColor.getColor());
          }
        });
      }
    }
  }


}
