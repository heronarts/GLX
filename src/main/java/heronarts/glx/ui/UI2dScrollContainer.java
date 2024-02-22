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

import heronarts.glx.event.MouseEvent;
import heronarts.lx.utils.LXUtils;

public class UI2dScrollContainer extends UI2dContainer implements UI2dScrollInterface {

  private boolean dynamicHeight = false;
  private float maxHeight = -1;

  private boolean dynamicWidth = false;
  private float maxWidth = -1;

  // Dimensions of the "virtual" content that can be scrolled within
  // the actual bounds of this context
  private float scrollWidth;
  private float scrollHeight;

  private boolean horizontalScrollingEnabled = false;
  private boolean verticalScrollingEnabled = true;

  public UI2dScrollContainer(UI ui, float x, float y, float w, float h) {
    super(x, y, w, h);
    this.scrollWidth = w;
    this.scrollHeight = h;
  }

  /**
   * Sets a maximum width on the scroll container. Resize or dynamic layout operations
   * up to this size will actually resize the container and texture itself. But past that point,
   * scroll operation occurs.
   *
   * @param maxWidth Maximum width before scrolling kicks in
   * @return this
   */
  public UI2dScrollContainer setMaxWidth(float maxWidth) {
    return setMaxWidth(maxWidth, false);
  }

  /**
   * Sets a maximum width on the scroll container. Resize or dynamic layout operations
   * up to this size will actually resize the container and texture itself. But past that point,
   * scroll operation occurs.
   *
   * @param maxWidth Maximum width before scrolling kicks in
   * @param reflow Reflow on this call
   * @return this
   */
  public UI2dScrollContainer setMaxWidth(float maxWidth, boolean reflow) {
    this.dynamicWidth = maxWidth > 0;
    this.maxWidth = maxWidth;
    if (reflow) {
      reflow();
    }
    return this;
  }

  /**
   * Sets a maximum height on the scroll container. Resize or dynamic layout operations
   * up to this size will actually resize the container and texture itself. But past that point,
   * scroll operation occurs.
   *
   * @param maxHeight Maximum height before scrolling kicks in
   * @return this
   */
  public UI2dScrollContainer setMaxHeight(float maxHeight) {
    return setMaxHeight(maxHeight, false);
  }

  /**
   * Sets a maximum height on the scroll container. Resize or dynamic layout operations
   * up to this size will actually resize the container and texture itself. But past that point,
   * scroll operation occurs.
   *
   * @param maxHeight Maximum height before scrolling kicks in
   * @param reflow Reflow on this call
   * @return this
   */
  public UI2dScrollContainer setMaxHeight(float maxHeight, boolean reflow) {
    this.dynamicHeight = maxHeight > 0;
    this.maxHeight = maxHeight;
    if (reflow) {
      reflow();
    }
    return this;
  }

  @Override
  public UI2dContainer setContentSize(float w, float h) {
    // Explicitly do not invoke super here!
    if (this.dynamicWidth || this.dynamicHeight) {
      setSize(
        this.dynamicWidth ? Math.min(this.maxWidth, w) : this.width,
        this.dynamicHeight ? Math.min(this.maxHeight, h) : this.height
      );
    }
    return setScrollSize(w, h);
  }

  /**
   * Sets the size of the scrolled content, which could potentially be larger
   * than the actual size of this element itself
   *
   * @param scrollWidth Width of scrollable virtual pane
   * @param scrollHeight Height of scrollable virtual pane
   * @return
   */
  @Override
  public UI2dScrollContainer setScrollSize(float scrollWidth, float scrollHeight) {
    if ((this.scrollWidth != scrollWidth) || (this.scrollHeight != scrollHeight)) {
      this.scrollWidth = scrollWidth;
      this.scrollHeight = scrollHeight;
      rescroll();
    }
    return this;
  }

  /**
   * Gets the total height of scrollable content, which could be larger than the size
   * of the container itself if vertical scrolling is occurring.
   */
  @Override
  public float getScrollHeight() {
    return this.scrollHeight;
  }

  @Override
  public UI2dScrollContainer setScrollHeight(float scrollHeight) {
    if (this.scrollHeight != scrollHeight) {
      this.scrollHeight = scrollHeight;
      rescroll();
    }
    return this;
  }

  /**
   * Gets the total width of scrollable content, which could be larger than the size
   * of the container itself if horizontal scrolling is occurring.
   */
  @Override
  public float getScrollWidth() {
    return this.scrollWidth;
  }

  @Override
  public UI2dScrollContainer setScrollWidth(float scrollWidth) {
    if (this.scrollWidth != scrollWidth) {
      this.scrollWidth = scrollWidth;
      rescroll();
    }
    return this;
  }

  public UI2dScrollContainer setHorizontalScrollingEnabled(boolean horizontalScrollingEnabled) {
    this.horizontalScrollingEnabled = horizontalScrollingEnabled;
    return this;
  }

  public UI2dScrollContainer setVerticalScrollingEnabled(boolean verticalScrollingEnabled) {
    this.verticalScrollingEnabled = verticalScrollingEnabled;
    return this;
  }

  public boolean hasScroll() {
    return
      (getScrollWidth() > getWidth()) ||
      (getScrollHeight() > getHeight());
  }

  @Override
  protected void onResize() {
    super.onResize();
    rescroll();
  }

  private float minScrollX() {
    return Math.min(0, this.width - this.scrollWidth);
  }

  private float minScrollY() {
    return Math.min(0, this.height - this.scrollHeight);
  }

  @Override
  public float getScrollX() {
    return this.scrollX;
  }

  @Override
  public float getScrollY() {
    return this.scrollY;
  }

  protected void onScrollChange() {}

  @Override
  public UI2dScrollContainer setScrollX(float scrollX) {
    scrollX = LXUtils.constrainf(scrollX, minScrollX(), 0);
    if (this.scrollX != scrollX) {
      this.scrollX = scrollX;
      onScrollChange();
      redraw();
    }
    return this;
  }

  @Override
  public UI2dScrollContainer setScrollY(float scrollY) {
    scrollY = LXUtils.constrainf(scrollY, minScrollY(), 0);
    if (this.scrollY != scrollY) {
      this.scrollY = scrollY;
      onScrollChange();
      redraw();
    }
    return this;
  }

  private void rescroll() {
    float minScrollX = minScrollX();
    float minScrollY = minScrollY();
    if ((this.scrollX < minScrollX) || (this.scrollY < minScrollY)) {
      this.scrollX = Math.max(this.scrollX, minScrollX);
      this.scrollY = Math.max(this.scrollY, minScrollY);
      redraw();
    }
    onScrollChange();
  }

  @Override
  void mousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.mousePressed(mouseEvent, mx - this.scrollX, my - this.scrollY);
  }

  @Override
  void mouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.mouseReleased(mouseEvent, mx - this.scrollX, my - this.scrollY);
  }

  @Override
  void mouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    super.mouseDragged(mouseEvent, mx - this.scrollX, my - this.scrollY, dx, dy);
  }

  @Override
  void mouseMoved(MouseEvent mouseEvent, float mx, float my) {
    super.mouseMoved(mouseEvent, mx - this.scrollX, my - this.scrollY);
  }

  @Override
  void mouseScroll(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    super.mouseScroll(mouseEvent, mx - this.scrollX, my - this.scrollY, dx, dy);
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (this.verticalScrollingEnabled) {
      mouseEvent.consume();
      setScrollY(this.scrollY + dy);
    }
    if (this.horizontalScrollingEnabled) {
      mouseEvent.consume();
      setScrollX(this.scrollX + dx);
    }
  }

  @Override
  protected void onMouseScroll(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (this.horizontalScrollingEnabled) {
      if (this.scrollWidth > this.width) {
        mouseEvent.consume();
        setScrollX(this.scrollX + (mouseEvent.isShiftDown() ? dy : -dx));
      }
    }
    if (this.verticalScrollingEnabled) {
      if (this.scrollHeight > this.height) {
        mouseEvent.consume();
        setScrollY(this.scrollY + dy);
      }
    }
  }
}
