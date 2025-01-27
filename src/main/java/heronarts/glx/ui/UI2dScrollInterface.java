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

public interface UI2dScrollInterface {

  public static class ScrollChange {
    public final boolean x;
    public final boolean y;
    public final boolean width;
    public final boolean height;

    public ScrollChange(boolean x, boolean y, boolean width, boolean height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }

    public static final ScrollChange X = new ScrollChange(true, false, false, false);
    public static final ScrollChange Y = new ScrollChange(false, true, false, false);
    public static final ScrollChange WIDTH = new ScrollChange(false, false, true, false);
    public static final ScrollChange HEIGHT = new ScrollChange(false, false, false, true);
  }

  public interface ScrollListener {
    public void onScrollChange(UI2dScrollInterface scroll, ScrollChange change);
  }

  public UI2dScrollInterface addScrollListener(ScrollListener listener);

  public UI2dScrollInterface removeScrollListener(ScrollListener listener);

  public float getWidth();

  public float getHeight();

  public float getScrollHeight();

  public UI2dScrollInterface setScrollHeight(float scrollHeight);

  public float getScrollWidth();

  public UI2dScrollInterface setScrollWidth(float scrollWidth);

  public UI2dScrollInterface setScrollSize(float scrollWidth, float scrollHeight);

  public float getScrollX();

  public float getScrollY();

  public UI2dScrollInterface setScrollX(float scrollX);

  public UI2dScrollInterface setScrollY(float scrollY);

  public default boolean hasScrollX() {
    return getScrollWidth() > getWidth();
  }

  public default boolean hasScrollY() {
    return getScrollHeight() > getHeight();
  }

  public default boolean hasScroll() {
    return hasScrollX() || hasScrollY();
  }

}
