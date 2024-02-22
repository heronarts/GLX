/**
 * Copyright 2022- Mark C. Slee, Heron Arts LLC
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

import heronarts.glx.ui.UIFocus;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.parameter.LXNormalizedParameter;

public class UIColorControl extends UIColorPicker implements UIFocus {

  public UIColorControl(float x, float y, ColorParameter color) {
    this(x, y, color, null);
  }

  public UIColorControl(float x, float y, ColorParameter color, LXNormalizedParameter subparameter) {
    super(x, y, UIKnob.WIDTH, UIKnob.HEIGHT, color, subparameter);
    setDeviceMode(true);
    setCorner(Corner.TOP_RIGHT);
  }
}
