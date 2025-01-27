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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  private final List<ScrollListener> listeners = new ArrayList<>();

  public UI2dScrollContainer addScrollListener(ScrollListener listener) {
    Objects.requireNonNull(listener, "May not add null ScrollListener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("May not add duplicate ScrollListener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public UI2dScrollContainer removeScrollListener(ScrollListener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("May not remove non-registered ScrollListener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }

  public boolean hasDynamicHeight() {
    return this.dynamicHeight;
  }

  public boolean hasDynamicWidth() {
    return this.dynamicWidth;
  }

  public float getMaxHeight() {
    return this.maxHeight;
  }

  public float getMaxWidth() {
    return this.maxWidth;
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
    boolean widthChange = false, heightChange = false;
    if (this.scrollWidth != scrollWidth) {
      widthChange = true;
      this.scrollWidth = scrollWidth;
    }
    if (this.scrollHeight != scrollHeight) {
      heightChange = true;
      this.scrollHeight = scrollWidth;
    }
    if (widthChange || heightChange) {
      rescroll(widthChange, heightChange);
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
      rescroll(false, true);
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
      rescroll(true, false);
    }
    return this;
  }

  public boolean getHorizontalScrollingEnabled() {
    return this.horizontalScrollingEnabled;
  }

  public boolean getVerticalScrollingEnabled() {
    return this.verticalScrollingEnabled;
  }

  public UI2dScrollContainer setHorizontalScrollingEnabled(boolean horizontalScrollingEnabled) {
    this.horizontalScrollingEnabled = horizontalScrollingEnabled;
    return this;
  }

  public UI2dScrollContainer setVerticalScrollingEnabled(boolean verticalScrollingEnabled) {
    this.verticalScrollingEnabled = verticalScrollingEnabled;
    return this;
  }

  @Override
  protected void onResize() {
    super.onResize();
    rescroll(true, true);
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

  @Deprecated
  /**
   * @deprecated Use onScrollChange(x, y)
   */
  protected void onScrollChange() {}

  protected void onScrollChange(ScrollChange scrollChange) {}

  private void fireScrollChange(ScrollChange scrollChange) {
    onScrollChange();
    onScrollChange(scrollChange);
    for (ScrollListener listener : this.listeners) {
      listener.onScrollChange(this, scrollChange);
    }
  }

  @Override
  public UI2dScrollContainer setScrollX(float scrollX) {
    scrollX = LXUtils.constrainf(scrollX, minScrollX(), 0);
    if (this.scrollX != scrollX) {
      this.scrollX = scrollX;
      fireScrollChange(ScrollChange.X);
      redraw();
    }
    return this;
  }

  @Override
  public UI2dScrollContainer setScrollY(float scrollY) {
    scrollY = LXUtils.constrainf(scrollY, minScrollY(), 0);
    if (this.scrollY != scrollY) {
      this.scrollY = scrollY;
      fireScrollChange(ScrollChange.Y);
      redraw();
    }
    return this;
  }

  private void rescroll(boolean widthChange, boolean heightChange) {
    float minScrollX = minScrollX();
    float minScrollY = minScrollY();
    boolean xChange = false, yChange = false;
    if (this.scrollX < minScrollX) {
      xChange = true;
      this.scrollX = Math.max(this.scrollX, minScrollX);
    }
    if (this.scrollY < minScrollY) {
      yChange = true;
      this.scrollY = Math.max(this.scrollY, minScrollY);
    }
    if (xChange || yChange) {
      redraw();
    }
    fireScrollChange(new ScrollChange(xChange, yChange, widthChange, heightChange));
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
      if (hasScrollX()) {
        // Holding shift can side-scroll using the Y scroll
        if (mouseEvent.isShiftDown()) {
          if (!mouseEvent.isScrollYConsumed() && (dy != 0)) {
            mouseEvent.consumeScrollY();
            setScrollX(this.scrollX + dy);
          }
        } else {
          if (!mouseEvent.isScrollXConsumed() && (dx != 0)) {
            mouseEvent.consumeScrollX();
            setScrollX(this.scrollX - dx);
          }
        }
      }
    }
    if (this.verticalScrollingEnabled) {
      if (hasScrollY() && !mouseEvent.isScrollYConsumed() && (dy != 0)) {
        mouseEvent.consumeScrollY();
        setScrollY(this.scrollY + dy);
      }
    }
  }
}
