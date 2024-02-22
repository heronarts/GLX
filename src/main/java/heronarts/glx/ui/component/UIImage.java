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

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.vg.VGraphics;

public class UIImage extends UI2dComponent {

  private final VGraphics.Image image;

  private boolean hasTint = false;

  private UIColor tint = UIColor.WHITE;

  public UIImage(VGraphics.Image image) {
    this(image, 0, 0);
  }

  public UIImage(VGraphics.Image image, float x, float y) {
    super(x, y, image.width, image.height);
    this.image = image;
  }

  public UIImage setTint(boolean hasTint) {
    if (this.hasTint != hasTint) {
      this.hasTint = hasTint;
      redraw();
    }
    return this;
  }

  public UIImage setTintColor(int tint) {
    return setTintColor(new UIColor(tint));
  }

  public UIImage setTintColor(UIColor tint) {
    if (!this.hasTint || (this.tint != tint)) {
      this.hasTint = true;
      this.tint = tint;
      redraw();
    }
    return this;
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    if (this.hasTint) {
      this.image.setTint(this.tint);
    }
    vg.beginPath();
    vg.image(this.image, 0, 0);
    vg.fill();
    this.image.noTint();
  }

}
