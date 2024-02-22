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

package heronarts.glx.ui;

public class UIColor {

  public static final UIColor BLACK = new UIColor(0xff000000);
  public static final UIColor WHITE = new UIColor(0xffffffff);
  public static final UIColor NONE = new UIColor(0x00000000);

  protected int argb;

  public UIColor(UIColor that) {
    this(that.argb);
  }

  public UIColor(int argb) {
    this.argb = argb;
  }

  public int get() {
    return this.argb;
  }

  public int maskf(float alpha) {
    return mask((int) (0xff * alpha));
  }

  public int mask(int alpha) {
    return UIColor.mask(this.argb, alpha);
  }

  public static int mask(int argb, int alpha) {
    return (alpha << 24) | (0xffffff & argb);
  }

}
