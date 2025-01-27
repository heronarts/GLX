/**
 * Copyright 2024- Mark C. Slee, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package heronarts.glx.ui;

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI2dScrollInterface.ScrollChange;
import heronarts.glx.ui.vg.VGraphics;

/**
 * A wrapper container to place a scrollable pane with an inset and an optional
 * scroll bar drawn in the margin between the outer bounds and the inset.
 */
public class UI2dScrollPane extends UI2dContainer {

  public static final int DEFAULT_INSET = 8;
  public static final int DEFAULT_PADDING = 6;
  public static final int DEFAULT_BORDER_ROUNDING = 4;

  public final UI2dScrollContainer scrollContent;

  // Insets are the space around the entire pane with application background
  // color over which scroll bars are drawn
  private float
    insetTop = DEFAULT_INSET,
    insetRight = DEFAULT_INSET,
    insetBottom = DEFAULT_INSET,
    insetLeft = DEFAULT_INSET;

  // Padding is a rounded box around the scrollable content, typically with same
  // background color as the scroll content itself
  private float
    paddingTop = DEFAULT_PADDING,
    paddingRight = DEFAULT_PADDING,
    paddingBottom = DEFAULT_PADDING,
    paddingLeft = DEFAULT_PADDING;

  private UIColor contentBackgroundColor;

  private final ScrollBar verticalScrollBar;

  private final ScrollBar horizontalScrollBar;

  public enum VerticalScrollBarPosition {
    NONE,
    LEFT,
    RIGHT;
  }

  public enum HorizontalScrollBarPosition {
    NONE,
    TOP,
    BOTTOM;
  }

  private VerticalScrollBarPosition verticalScrollBarPosition = VerticalScrollBarPosition.NONE;

  private HorizontalScrollBarPosition horizontalScrollBarPosition = HorizontalScrollBarPosition.NONE;

  public UI2dScrollPane(UI ui, float x, float y, float w, float h) {
    super(x, y, w, h);
    setBackgroundColor(ui.theme.paneBackgroundColor);
    setBorderRounding(DEFAULT_BORDER_ROUNDING);
    this.contentBackgroundColor = ui.theme.paneInsetColor;
    this.scrollContent = new UI2dScrollContainer(ui, 0, 0, w, h) {
      @Override
      public void onResize() {
        super.onResize();
        if (hasDynamicHeight()) {
          UI2dScrollPane.this.setHeight(this.height + insetTop + insetBottom + paddingTop + paddingBottom);
        }
        if (hasDynamicWidth()) {
          UI2dScrollPane.this.setWidth(this.width + insetLeft + insetRight + paddingLeft + paddingRight);
        }
      }
    };
    this.scrollContent.setBackgroundColor(this.contentBackgroundColor);
    this.verticalScrollBar = new ScrollBar(this.scrollContent, ScrollBar.Orientation.VERTICAL, 0, 0, 0, 0);
    this.horizontalScrollBar = new ScrollBar(this.scrollContent, ScrollBar.Orientation.HORIZONTAL, 0, 0, 0, 0);
    addChildren(this.verticalScrollBar, this.horizontalScrollBar);
    setContentTarget(this.scrollContent);
  }

  private void _setContentPosition() {
    this.scrollContent.setPosition(
      this.insetLeft + this.paddingLeft,
      this.insetTop + this.paddingTop,
      this.width - this.insetLeft - this.paddingLeft - this.insetRight - this.paddingRight,
      this.height - this.insetTop - this.paddingTop - this.insetBottom - this.paddingBottom
    );
    _setHorizontalScrollBarPosition();
    _setVerticalScrollBarPosition();
  }

  @Override
  protected void onResize() {
    _setContentPosition();
  }

  public UI2dScrollPane setContentBackgroundColor(UIColor color) {
    if (this.contentBackgroundColor != color) {
      this.contentBackgroundColor = color;
      this.scrollContent.setBackgroundColor(this.contentBackgroundColor);
      redraw();
    }
    return this;
  }

  public UI2dScrollPane setContentInset(float inset) {
    return setContentInset(inset, inset);
  }

  public UI2dScrollPane setContentInset(float insetY, float insetX) {
    return setContentInset(insetY, insetX, insetY, insetX);
  }

  public UI2dScrollPane setContentInset(float insetTop, float insetRight, float insetBottom, float insetLeft) {
    boolean redraw = false;
    if (this.insetTop != insetTop) {
      this.insetTop = insetTop;
      redraw = true;
    }
    if (this.insetRight != insetRight) {
      this.insetRight = insetRight;
      redraw = true;
    }
    if (this.insetBottom != insetBottom) {
      this.insetBottom = insetBottom;
      redraw = true;
    }
    if (this.insetLeft != insetLeft) {
      this.insetLeft = insetLeft;
      redraw = true;
    }
    if (redraw) {
      _setContentPosition();
    }
    return this;
  }

  public UI2dScrollPane setContentInsetTop(float insetTop) {
    return setContentInset(insetTop, this.insetRight, this.insetBottom, this.insetLeft);
  }

  public UI2dScrollPane setContentInsetRight(float insetTop) {
    return setContentInset(this.insetTop, insetRight, this.insetBottom, this.insetLeft);
  }

  public UI2dScrollPane setContentInsetBottom(float insetBottom) {
    return setContentInset(this.insetTop, this.insetRight, insetBottom, this.insetLeft);
  }

  public UI2dScrollPane setContentInsetLeft(float insetLeft) {
    return setContentInset(this.insetTop, this.insetRight, this.insetBottom, insetLeft);
  }

  @Override
  public UI2dScrollPane setPadding(float padding) {
    return setPadding(padding, padding);
  }

  @Override
  public UI2dScrollPane setPadding(float paddingY, float paddingX) {
    return setPadding(paddingY, paddingX, paddingY, paddingX);
  }

  public UI2dScrollPane setPaddingTop(float paddingTop) {
    return setPadding(paddingTop, this.paddingRight, this.paddingBottom, this.paddingLeft);
  }

  public UI2dScrollPane setPaddingRight(float paddingRight) {
    return setPadding(this.paddingTop, paddingRight, this.paddingBottom, this.paddingLeft);
  }

  public UI2dScrollPane setPaddingBottom(float paddingBottom) {
    return setPadding(this.paddingTop, this.paddingRight, paddingBottom, this.paddingLeft);
  }

  public UI2dScrollPane setPaddingLeft(float paddingLeft) {
    return setPadding(this.paddingTop, this.paddingRight, this.paddingBottom, paddingLeft);
  }

  @Override
  public UI2dScrollPane setPadding(float paddingTop, float paddingRight, float paddingBottom, float paddingLeft) {
    boolean redraw = false;
    if (this.paddingTop != paddingTop) {
      this.paddingTop = paddingTop;
      redraw = true;
    }
    if (this.paddingRight != paddingRight) {
      this.paddingRight = paddingRight;
      redraw = true;
    }
    if (this.paddingBottom != paddingBottom) {
      this.paddingBottom = paddingBottom;
      redraw = true;
    }
    if (this.paddingLeft != paddingLeft) {
      this.paddingLeft = paddingLeft;
      redraw = true;
    }
    if (redraw) {
      _setContentPosition();
    }
    return this;
  }

  public float getScrollX() {
    return this.scrollContent.getScrollX();
  }

  public float getScrollY() {
    return this.scrollContent.getScrollY();
  }

  public UI2dScrollPane setScrollX(float scrollX) {
    this.scrollContent.setScrollX(scrollX);
    return this;
  }

  public UI2dScrollPane setScrollY(float scrollY) {
    this.scrollContent.setScrollY(scrollY);
    return this;
  }

  public UI2dScrollPane setVerticalScrollBarPosition(VerticalScrollBarPosition verticalScrollBarPosition) {
    if (this.verticalScrollBarPosition != verticalScrollBarPosition) {
      this.verticalScrollBarPosition = verticalScrollBarPosition;
      _setVerticalScrollBarPosition();
    }
    return this;
  }

  private void _setVerticalScrollBarPosition() {
    switch (this.verticalScrollBarPosition) {
    case NONE:
      break;
    case LEFT:
      this.verticalScrollBar.setPosition(
        0,
        this.insetTop + this.paddingTop,
        this.insetLeft,
        this.height - this.insetBottom - this.paddingBottom - this.insetTop - this.paddingTop
      );
      break;
    case RIGHT:
      this.verticalScrollBar.setPosition(
        this.width - this.insetRight,
        this.insetTop + this.paddingTop,
        this.insetRight,
        this.height - this.insetBottom - this.paddingBottom - this.insetTop - this.paddingTop
      );
      break;
    }
    this.verticalScrollBar.setVisible(this.verticalScrollBarPosition != VerticalScrollBarPosition.NONE);
  }

  public UI2dScrollPane setHorizontalScrollBarPosition(HorizontalScrollBarPosition horizontalScrollBarPosition) {
    if (this.horizontalScrollBarPosition != horizontalScrollBarPosition) {
      this.horizontalScrollBarPosition = horizontalScrollBarPosition;
      _setHorizontalScrollBarPosition();
    }
    return this;
  }

  private void _setHorizontalScrollBarPosition() {
    switch (this.horizontalScrollBarPosition) {
    case NONE:
      break;
    case TOP:
      this.horizontalScrollBar.setPosition(
        this.insetLeft + this.paddingLeft,
        0,
        this.width - this.insetRight - this.paddingRight - this.insetLeft - this.paddingLeft,
        this.insetTop
      );
      break;
    case BOTTOM:
      this.horizontalScrollBar.setPosition(
        this.insetLeft + this.paddingLeft,
        this.height - this.insetBottom,
        this.width - this.insetRight - this.paddingRight - this.insetLeft - this.paddingLeft,
        this.insetBottom
      );
      break;
    }
    this.horizontalScrollBar.setVisible(this.horizontalScrollBarPosition != HorizontalScrollBarPosition.NONE);
  }

  @Override
  protected void drawBackground(UI ui, VGraphics vg) {
    // NOTE: do not use super.drawBackground - disallow border rounding here!
    vg.beginPath();
    vg.fillColor(getBackgroundColor());
    vg.rect(0, 0, this.width, this.height);
    vg.fill();

    // TODO(mcslee): do we need drawBorder to properly clip scrolling aliasing on the content? Doesn't seem to be?
    vg.beginPath();
    vg.fillColor(this.contentBackgroundColor);
    vg.roundedRectVarying(
      this.insetLeft,
      this.insetTop,
      this.width - this.insetLeft - this.insetRight,
      this.height - this.insetTop - this.insetBottom,
      this.borderRoundingTopLeft,
      this.borderRoundingTopRight,
      this.borderRoundingBottomRight,
      this.borderRoundingBottomLeft
    );
    vg.fill();
  }

  @Override
  protected void drawBorder(UI ui, VGraphics vg) {
    super.drawBorder(ui, vg);
  }

  public static class ScrollBar extends UI2dComponent implements UI2dScrollInterface.ScrollListener {

    public enum Orientation {
      VERTICAL,
      HORIZONTAL;

      private boolean isChanged(ScrollChange change) {
        switch (this) {
        case VERTICAL:
          return change.isVertical();
        case HORIZONTAL:
          return change.isHorizontal();
        }
        return false;
      }
    }

    private final UI2dScrollInterface scrollContent;
    private final Orientation orientation;

    private boolean scrolling = false;

    public ScrollBar(UI2dScrollInterface scrollContent, Orientation orientation, float x, float y, float w, float h) {
      super(x, y, w, h);
      this.scrollContent = scrollContent;
      this.scrollContent.addScrollListener(this);
      this.orientation = orientation;
      setVisible(false);
    }

    @Override
    public void onScrollChange(UI2dScrollInterface scroll, ScrollChange change) {
      if (this.orientation.isChanged(change)) {
        redraw();
      }
    }

    @Override
    public void dispose() {
      this.scrollContent.removeScrollListener(this);
      super.dispose();
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      switch (this.orientation){
        case VERTICAL:
          float scrollHeight = this.scrollContent.getScrollHeight();
          float insetHeight = this.scrollContent.getHeight();
          if (scrollHeight > insetHeight) {
            float barHeight = this.height * insetHeight / scrollHeight;
            float barY = (this.height - barHeight) * this.scrollContent.getScrollY() / (insetHeight - scrollHeight);
            vg.beginPath();
            vg.fillColor(ui.theme.paneScrollBarColor);
            vg.rect(2, barY, this.width - 4, barHeight, 1);
            vg.fill();
          }
          break;

        case HORIZONTAL:
          float scrollWidth = this.scrollContent.getScrollWidth();
          float insetWidth = this.scrollContent.getWidth();
          if (scrollWidth > insetWidth) {
            float barWidth = this.height * insetWidth / scrollWidth;
            float barX = (this.width - barWidth) * this.scrollContent.getScrollX() / (insetWidth - scrollWidth);
            vg.beginPath();
            vg.fillColor(ui.theme.paneScrollBarColor);
            vg.rect(barX, 2, barWidth, this.height - 4, 1);
            vg.fill();
          }
          break;
      }
    }

    @Override
    public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
      switch (this.orientation) {
      case VERTICAL:
        this.scrolling = this.scrollContent.getScrollHeight() > this.scrollContent.getHeight();
        break;
      case HORIZONTAL:
        this.scrolling = this.scrollContent.getScrollWidth() > this.scrollContent.getWidth();
        break;
      }
    }

    @Override
    public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
      if (this.scrolling) {
        switch (this.orientation) {
        case VERTICAL:
          float scrollHeight = this.scrollContent.getScrollHeight();
          float insetHeight = this.scrollContent.getHeight();
          float barHeight = this.height * insetHeight / scrollHeight;
          this.scrollContent.setScrollY(this.scrollContent.getScrollY() - dy * (scrollHeight - insetHeight) / (this.height - barHeight));
          break;
        case HORIZONTAL:
          float scrollWidth = this.scrollContent.getScrollWidth();
          float insetWidth = this.scrollContent.getWidth();
          float barWidth = this.width * insetWidth / scrollWidth;
          this.scrollContent.setScrollX(this.scrollContent.getScrollX() - dx * (scrollWidth - insetWidth) / (this.width - barWidth));
          break;
        }
      }
    }
  }

}
