/**
 * Copyright 2025- Justin K. Belcher, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package heronarts.glx.ui;

import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A container capable of adjusting child sizes to fill the available space.
 * Layout can start from top, bottom, left, or right. Each child may be
 * a fixed size *or* a percentage of the remaining space (in the layout orientation).
 * To fill the container, the sum of child fill percentages should equal 100.
 */
public class UIFillContainer extends UI2dContainer {

  public enum Direction {
    TOP_DOWN,
    BOTTOM_UP,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT
  }

  private Direction direction = Direction.TOP_DOWN;
  private boolean fillOpposite = false;

  private final Map<UI2dComponent, Float> fills = new HashMap<>();

  public UIFillContainer(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UIFillContainer setDirection(Direction direction) {
    if (direction != this.direction) {
      this.direction = direction;
      reflow();
    }
    return this;
  }

  public UIFillContainer addChild(UI2dComponent child) {
    child.addToContainer(this);
    return this;
  }

  public UIFillContainer addChild(UI2dComponent child, float fillPercent) {
    child.addToContainer(this);
    setFill(child, fillPercent);
    return this;
  }

  @Override
  protected void childAdded(UI2dComponent child) {
    child.visible.addListener(this.childVisibleChanged);
  }

  @Override
  protected void childRemoved(UI2dComponent child) {
    child.visible.removeListener(this.childVisibleChanged);
    removeFill(child);
  }

  private final LXParameterListener childVisibleChanged = p -> {
    reflow();
  };

  public UIFillContainer setFill(UI2dComponent child, float fillPercent) {
    if (child.getParent() != this) {
      throw new IllegalArgumentException("Can not set fill percentage on object that is not a child of collection");
    }
    this.fills.put(child, fillPercent);
    reflow();
    return this;
  }

  public UIFillContainer removeFill(UI2dComponent child) {
    if (this.fills.containsKey(child)) {
      this.fills.remove(child);
      reflow();
    }
    return this;
  }

  public UIFillContainer setFillOpposite(boolean fillOpposite) {
    if (this.fillOpposite != fillOpposite) {
      this.fillOpposite = fillOpposite;
      reflow();
    }
    return this;
  }

  @Override
  protected void onResize() {
    super.onResize();
    reflow();
  }

  @Override
  protected void onReflow() {
    super.onReflow();

    // Calculate size available for fill children
    float fillSize;
    float childSpacing;
    if (this.direction == Direction.TOP_DOWN || this.direction == Direction.BOTTOM_UP) {
      // Vertical
      fillSize = getContentHeight() - getTopPadding() - getBottomPadding();
      childSpacing  = getChildSpacingY();
      for (UIObject uiObject : this.children) {
        UI2dComponent child = (UI2dComponent) uiObject;
        if (child.isVisible()) {
          fillSize = fillSize - child.getTopMargin() - child.getBottomMargin() - childSpacing;
          if (!this.fills.containsKey(child)) {
            fillSize -= child.getHeight();
          }
        }
      }
    } else {
      // Horizontal
      fillSize = getContentWidth() - getLeftPadding() - getRightPadding();
      childSpacing = getChildSpacingX();
      for (UIObject uiObject : this.children) {
        UI2dComponent child = (UI2dComponent) uiObject;
        if (child.isVisible()) {
          fillSize = fillSize - child.getLeftMargin() - child.getRightMargin() - childSpacing;
          if (!this.fills.containsKey(child)) {
            fillSize -= child.getWidth();
          }
        }
      }
    }

    // There will be one less childSpacing than the number of children
    if (!this.children.isEmpty()) {
      fillSize += childSpacing;
    }
    fillSize = LXUtils.maxf(0f, fillSize);

    // Lay everything out
    switch (this.direction) {
      case TOP_DOWN -> {
        float y = getTopPadding();
        for (UIObject uiObject : this.children) {
          if (uiObject.isVisible()) {
            UI2dComponent child = (UI2dComponent) uiObject;

            float childHeight;
            if (this.fills.containsKey(child)) {
              // Calculate child size
              childHeight = fillSize * (this.fills.get(child) / 100f);
              child.setHeight(childHeight);
            } else {
              childHeight = child.getHeight();
            }
            y += child.getTopMargin();
            child.setY(y);
            y += childHeight + child.getBottomMargin() + childSpacing;

            // Optional horizontal fill
            if (this.fillOpposite) {
              child.setX(getLeftPadding() + child.getLeftMargin());
              child.setWidth(getWidth() - getLeftPadding() - getRightPadding() - child.getLeftMargin() - child.getRightMargin());
            }
          }
        }
      }
      case BOTTOM_UP -> {
        float y = getHeight() - getBottomPadding();
        for (UIObject uiObject : this.children) {
          if (uiObject.isVisible()) {
            UI2dComponent child = (UI2dComponent) uiObject;

            float childHeight;
            if (this.fills.containsKey(child)) {
              // Calculate child size
              childHeight = fillSize * (this.fills.get(child) / 100f);
              child.setHeight(childHeight);
            } else {
              childHeight = child.getHeight();
            }
            y = y - child.getBottomMargin() - childHeight;
            child.setY(y);
            y = y - child.getTopMargin() - childSpacing;

            // Optional horizontal fill
            if (this.fillOpposite) {
              child.setX(getLeftPadding() + child.getLeftMargin());
              child.setWidth(getWidth() - getLeftPadding() - getRightPadding() - child.getLeftMargin() - child.getRightMargin());
            }
          }
        }
      }
      case LEFT_TO_RIGHT -> {
        float x = getLeftPadding();
        for (UIObject uiObject : this.children) {
          if (uiObject.isVisible()) {
            UI2dComponent child = (UI2dComponent) uiObject;

            float childSize;
            if (this.fills.containsKey(child)) {
              // Calculate child size
              childSize = fillSize * (this.fills.get(child) / 100f);
              child.setWidth(childSize);
            } else {
              childSize = child.getWidth();
            }
            x += child.getLeftMargin();
            child.setX(x);
            x += childSize + child.getRightMargin() + childSpacing;

            // Optional vertical fill
            if (this.fillOpposite) {
              child.setY(getTopPadding() + child.getTopMargin());
              child.setHeight(getHeight() - getTopPadding() - getBottomPadding() - child.getTopMargin() - child.getBottomMargin());
            }
          }
        }
      }
      case RIGHT_TO_LEFT -> {
        float x = getWidth() - getRightPadding();
        for (UIObject uiObject : this.children) {
          if (uiObject.isVisible()) {
            UI2dComponent child = (UI2dComponent) uiObject;

            float childSize;
            if (this.fills.containsKey(child)) {
              // Calculate child size
              childSize = fillSize * (this.fills.get(child) / 100f);
              child.setWidth(childSize);
            } else {
              childSize = child.getWidth();
            }
            x = x - child.getRightMargin() - childSize;
            child.setX(x);
            x = x - child.getLeftMargin() - childSpacing;

            // Optional vertical fill
            if (this.fillOpposite) {
              child.setY(getTopPadding() + child.getTopMargin());
              child.setHeight(getHeight() - getTopPadding() - getBottomPadding() - child.getTopMargin() - child.getBottomMargin());
            }
          }
        }
      }
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    this.fills.clear();
  }

}
