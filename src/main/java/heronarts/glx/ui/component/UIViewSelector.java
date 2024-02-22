/**
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
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

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.structure.view.LXViewDefinition;
import heronarts.lx.structure.view.LXViewEngine;

public class UIViewSelector extends UIDropMenu {

  public static final int DEVICE_HEIGHT = 12;
  public static final int DEVICE_WIDTH = 16;
  public static final int WIDTH = 40;
  public static final int HEIGHT = 16;

  private final LXViewEngine.Selector viewSelector;

  private boolean deviceMode = false;

  public UIViewSelector(UI ui, float x, float y, LXViewEngine.Selector viewSelector) {
    super(x, y, WIDTH, HEIGHT, viewSelector);
    setBackground(false);
    setBorder(false);
    setDirection(UIDropMenu.Direction.UP);
    setMenuWidth(74);

    this.viewSelector = viewSelector;
  }

  public UIViewSelector setDeviceMode(boolean deviceMode) {
    this.deviceMode = deviceMode;
    if (this.deviceMode) {
      setSize(DEVICE_WIDTH, DEVICE_HEIGHT);
    } else {
      setSize(WIDTH, HEIGHT);
    }
    return this;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    final LXViewDefinition view = this.viewSelector.getObject();

    final int maskColor = (view == null) ? 0 :
      UIColor.mask(view.modulationColor.getColor(), view.enabled.isOn() ? 0xff : 0x66);

    ui.theme.iconView.setTint(
      (view == null) ?
        ui.theme.controlFillColor.get() :
        (this.deviceMode ? maskColor : ui.theme.primaryColor.get())
    );
    vg.beginPath();
    vg.image(ui.theme.iconView, 0, this.deviceMode ? -2 : 0);
    vg.fill();
    ui.theme.iconView.noTint();

    if (!this.deviceMode && (view != null)) {
      vg.beginPath();
      vg.fillColor(maskColor);
      vg.rect(18, 5, 6, 6);
      vg.fill();
    }
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.viewSelector);
  }

}