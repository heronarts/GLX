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

package heronarts.glx.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import heronarts.glx.GLX;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.utils.LXUtils;

public class UI2dContainer extends UI2dComponent implements UIContainer, Iterable<UIObject> {

  public enum Layout {
    NONE,
    VERTICAL,
    HORIZONTAL,
    VERTICAL_GRID,
    HORIZONTAL_GRID,
    VERTICAL_EVEN,
    HORIZONTAL_EVEN;

    public boolean isHorizontalList() {
      switch (this) {
      case HORIZONTAL:
      case HORIZONTAL_EVEN:
        return true;
      default:
        return false;
      }
    }

    public boolean isVerticalList() {
      switch (this) {
      case VERTICAL:
      case VERTICAL_EVEN:
        return true;
      default:
        return false;
      }
    }

    public boolean canDragReorder() {
      return isHorizontalList() || isVerticalList();
    }
  }

  public enum ArrowKeyFocus {
    NONE,
    VERTICAL,
    HORIZONTAL
  };

  public enum Position {
    TOP,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    MIDDLE,
    MIDDLE_LEFT,
    MIDDLE_CENTER,
    MIDDLE_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    LEFT,
    CENTER,
    RIGHT
  };

  private Layout layout = Layout.NONE;

  ArrowKeyFocus arrowKeyFocus = ArrowKeyFocus.NONE;

  private float topPadding = 0, rightPadding = 0, bottomPadding = 0, leftPadding = 0;

  private float childSpacingX = 0, childSpacingY = 0;

  private float minHeight = 0, minWidth = 0;

  private boolean dragToReorder = false;

  private UI2dContainer contentTarget;

  public static UI2dContainer newHorizontalContainer(float height) {
    return newHorizontalContainer(height, 0, (UI2dComponent[]) null);
  }

  public static UI2dContainer newHorizontalContainer(float height, float childSpacing) {
    return newHorizontalContainer(height, childSpacing, (UI2dComponent[]) null);
  }

  public static UI2dContainer newHorizontalContainer(float height, float childSpacing, UI2dComponent ... children) {
    UI2dContainer container = new UI2dContainer(0, 0, 0, height)
      .setLayout(UI2dContainer.Layout.HORIZONTAL)
      .setChildSpacing(childSpacing);
    if (children != null) {
      container.addChildren(children);
    }
    return container;
  }

  public static UI2dContainer newVerticalContainer(float width) {
    return newVerticalContainer(width, 0);
  }

  public static UI2dContainer newVerticalContainer(float width, float childSpacing) {
    return newVerticalContainer(width, childSpacing, (UI2dComponent[]) null);
  }

  public static UI2dContainer newVerticalContainer(float width, float childSpacing, UI2dComponent ... children) {
    UI2dContainer container = new UI2dContainer(0, 0, width, 0)
      .setLayout(UI2dContainer.Layout.VERTICAL)
      .setChildSpacing(childSpacing);
    if (children != null) {
      for (UI2dComponent child : children) {
        child.addToContainer(container);
      }
    }
    return container;
  }

  public UI2dContainer(float x, float y, float w, float h) {
    super(x, y, w, h);
    this.contentTarget = this;
  }

  public UI2dContainer setPadding(float padding) {
    return setPadding(padding, padding, padding, padding);
  }

  public UI2dContainer setPadding(float yPadding, float xPadding) {
    return setPadding(yPadding, xPadding, yPadding, xPadding);
  }

  public UI2dContainer setPadding(float topPadding, float rightPadding, float bottomPadding, float leftPadding) {
    boolean reflow = false;
    if (this.contentTarget.topPadding != topPadding) {
      this.contentTarget.topPadding = topPadding;
      reflow = true;
    }
    if (this.contentTarget.rightPadding != rightPadding) {
      this.contentTarget.rightPadding = rightPadding;
      reflow = true;
    }
    if (this.contentTarget.bottomPadding != bottomPadding) {
      this.contentTarget.bottomPadding = bottomPadding;
      reflow = true;
    }
    if (this.contentTarget.leftPadding != leftPadding) {
      this.contentTarget.leftPadding = leftPadding;
      reflow = true;
    }
    if (reflow) {
      this.contentTarget.reflow();
    }
    return this;
  }

  public float getTopPadding() {
    return this.topPadding;
  }

  public float getRightPadding() {
    return this.rightPadding;
  }

  public float getBottomPadding() {
    return this.bottomPadding;
  }

  public float getLeftPadding() {
    return this.leftPadding;
  }

  /**
   * Deprecated. Use {@link #setChildSpacing(float)} instead
   *
   * @param childMargin Child margin
   * @return this
   */
  @Deprecated
  public UI2dContainer setChildMargin(float childMargin) {
    return setChildSpacing(childMargin);
  }

  public UI2dContainer setChildSpacing(float childSpacing) {
    return setChildSpacing(childSpacing, childSpacing);
  }

  public UI2dContainer setChildSpacing(float childSpacingY, float childSpacingX) {
    if ((this.contentTarget.childSpacingX != childSpacingX) || (this.contentTarget.childSpacingY != childSpacingY)) {
      this.contentTarget.childSpacingX = childSpacingX;
      this.contentTarget.childSpacingY = childSpacingY;
      this.contentTarget.reflow();
    }
    return this;
  }

  public UI2dContainer setMinWidth(float minWidth) {
    if (this.contentTarget.minWidth != minWidth) {
      this.contentTarget.minWidth = minWidth;
      reflow();
    }
    return this;
  }

  public UI2dContainer setMinHeight(float minHeight) {
    if (this.contentTarget.minHeight != minHeight) {
      this.contentTarget.minHeight = minHeight;
      reflow();
    }
    return this;
  }

  public UI2dContainer setLayout(Layout layout, float childSpacing) {
    setLayout(layout);
    setChildSpacing(childSpacing);
    return this;
  }

  public UI2dContainer setLayout(Layout layout) {
    if (this.contentTarget.layout != layout) {
      this.contentTarget.layout = layout;
      this.contentTarget.reflow();
      if (this.dragToReorder && !layout.canDragReorder()) {
        GLX.error(new Exception("Container had dragToReorder set but invalid layout later specified: " + this + " " + layout));
        this.dragToReorder = false;
      }
    }
    return this;
  }

  public UI2dContainer setDragToReorder(boolean dragToReorder) {
    if (dragToReorder && !this.contentTarget.layout.canDragReorder()) {
      throw new IllegalStateException("Cannot set dragToReorder on a container with a non-list layout:"  + this);
    }
    this.dragToReorder = dragToReorder;
    return this;
  }

  public boolean hasDragToReorder() {
    return this.dragToReorder;
  }

  public UI2dContainer setArrowKeyFocus(ArrowKeyFocus keyFocus) {
    this.contentTarget.arrowKeyFocus = keyFocus;
    return this;
  }

  public UI2dContainer addChildren(UI2dComponent ... children) {
    for (UI2dComponent child : children) {
      child.addToContainer(this, false);
    }
    UIContainer contentTarget = getContentTarget();
    if (contentTarget instanceof UI2dContainer) {
      ((UI2dContainer) contentTarget).reflow();
    }
    redraw();
    return this;
  }

  private boolean inReflow = false;

  protected final void reflow() {
    if (this.inReflow) {
      // Prevent re-entrant reflow() calls, we're going to update the positions
      // of many objects, we don't need them to re-notify us each time.
      return;
    }
    this.inReflow = true;

    if (this.layout == Layout.VERTICAL) {
      float y = this.topPadding;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          component.setY(y + component.marginTop);
          y += component.marginTop + component.getHeight() + component.marginBottom + this.childSpacingY;
        }
      }
      y += this.bottomPadding;
      setContentHeight(Math.max(this.minHeight, y - this.childSpacingY));
    } else if (this.layout == Layout.HORIZONTAL) {
      float x = this.leftPadding;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          component.setX(x + component.marginLeft);
          x += component.marginLeft + component.getWidth() + component.marginRight + this.childSpacingX;
        }
      }
      x += this.rightPadding;
      setContentWidth(Math.max(this.minWidth, x - this.childSpacingX));
    } else if (this.layout == Layout.VERTICAL_GRID) {
      float x = this.leftPadding;
      float y = this.topPadding;
      float w = 0;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          if (y + component.marginTop + component.getHeight() > getContentHeight()) {
            x += w + this.childSpacingX;
            y = this.topPadding;
            w = 0;
          }
          component.setPosition(x + component.marginLeft, y + component.marginTop);
          w = Math.max(w, component.getWidth() + component.marginLeft + component.marginRight);
          y += component.marginTop + component.getHeight() + component.marginBottom + this.childSpacingY;
        }
      }
      setContentWidth(Math.max(this.minWidth, x + w + this.rightPadding));
    } else if (this.layout == Layout.HORIZONTAL_GRID) {
      float x = this.leftPadding;
      float y = this.topPadding;
      float h = 0;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          if (x + component.marginLeft + component.getWidth() > getContentWidth()) {
            y += h + this.childSpacingY;
            x = this.leftPadding;
            h = 0;
          }
          component.setPosition(x + component.marginLeft, y + component.marginTop);
          h = Math.max(h, component.marginTop + component.getHeight() + component.marginBottom);
          x += component.marginLeft + component.getWidth() + component.marginRight + this.childSpacingX;
        }
      }
      setContentHeight(Math.max(this.minHeight, y + h + this.bottomPadding));
    } else if (this.layout == Layout.VERTICAL_EVEN) {
      int countVisible = 0;
      for (UIObject child : this) {
        if (child.isVisible()) {
          countVisible++;
        }
      }
      if (countVisible > 0) {
        // Subtract all spaces between children
        float totalChildHeight = getContentHeight() - this.topPadding - this.bottomPadding - (this.childSpacingY * (countVisible - 1));
        if (totalChildHeight > 0) {
          // Divide remaining space evenly among visible children
          float childHeight = totalChildHeight / countVisible;
          float y = this.topPadding;
          for (UIObject child : this) {
            if (child.isVisible()) {
              UI2dComponent component = (UI2dComponent) child;
              component.setPosition(this.leftPadding + component.marginLeft, y + component.marginTop);
              component.setHeight(LXUtils.maxf(0, childHeight - component.marginTop - component.marginBottom));
              y += childHeight + this.childSpacingY;
            }
          }
        }
      }
    } else if (this.layout == Layout.HORIZONTAL_EVEN) {
      int countVisible = 0;
      for (UIObject child : this) {
        if (child.isVisible()) {
          countVisible++;
        }
      }
      if (countVisible > 0) {
        // Subtract all spaces between children
        float totalChildWidth = getContentWidth() - this.leftPadding - this.rightPadding - (this.childSpacingX * (countVisible - 1));
        if (totalChildWidth > 0) {
          // Divide remaining space evenly among visible children
          float childWidth = totalChildWidth / countVisible;
          float x = this.leftPadding;
          for (UIObject child : this) {
            if (child.isVisible()) {
              UI2dComponent component = (UI2dComponent) child;
              component.setPosition(x + component.marginLeft, this.topPadding + component.marginTop);
              component.setWidth(LXUtils.maxf(0, childWidth - component.marginLeft - component.marginRight));
              x += childWidth + this.childSpacingX;
            }
          }
        }
      }
    } else if (this.layout == Layout.NONE) {
      boolean changed = false;
      for (UIObject child : this) {
        if (child.isVisible()) {
          UI2dComponent component = (UI2dComponent) child;
          if (component.containerPosition != null) {
            boolean childChanged = component._setContainerPosition(this, component.containerPosition, false);
            changed = changed || childChanged;
          }
        }
      }
      if (changed) {
        redraw();
      }
    }
    onReflow();
    this.inReflow = false;
  }

  protected void onReflow() {}

  protected UI2dContainer setContentTarget(UI2dContainer contentTarget) {
    if (this.mutableChildren.contains(contentTarget)) {
      throw new IllegalStateException("contentTarget already belongs to container: " + contentTarget);
    }
    this.contentTarget = contentTarget;
    this.mutableChildren.add(contentTarget);
    contentTarget.parent = this;
    contentTarget.setUI(this.ui);
    redraw();
    return this;
  }

  protected UI2dContainer addTopLevelComponent(UI2dComponent child) {
    if (child.parent != null) {
      child.removeFromContainer();
    }
    this.mutableChildren.add(child);
    child.parent = this;
    child.setUI(this.ui);
    redraw();
    return this;
  }

  /**
   * Returns the object that elements are added to when placed in this container.
   * In most cases, it will be "this" - but some elements have special subcontainers.
   *
   * @return Element
   */
  @Override
  public UI2dContainer getContentTarget() {
    return this.contentTarget;
  }

  /**
   * Returns the width of scrolling content. By default this is the same as the width
   * of the container itself, but if the container scrolls then the scroll width may
   * be a larger value.
   *
   * @return Width of scrollable content
   */
  public float getScrollWidth() {
    return getWidth();
  }

  /**
   * Returns the height of scrolling content. By default this is the same as the height
   * of the container itself, but if the container scrolls then the scroll height may
   * be a larger value.
   *
   * @return Height of scrollable content
   */
  public float getScrollHeight() {
    return getHeight();
  }

  @Override
  public float getContentWidth() {
    return getContentTarget().getWidth();
  }

  @Override
  public float getContentHeight() {
    return getContentTarget().getHeight();
  }

  public UI2dContainer setContentWidth(float w) {
    return setContentSize(w, getContentHeight());
  }

  public UI2dContainer setContentHeight(float h) {
    return setContentSize(getContentWidth(), h);
  }

  public UI2dContainer setContentSize(float w, float h) {
    this.contentTarget.setSize(w, h);
    return this;
  }

  public UI2dContainer removeAllChildren() {
    return removeAllChildren(true);
  }

  public UI2dContainer removeAllChildren(boolean dispose) {
    UI2dContainer contentTarget = getContentTarget();
    for (UIObject child : contentTarget.mutableChildren) {
      ((UI2dComponent) child).parent = null;
      if (dispose) {
        child.dispose();
      }
    }
    contentTarget.mutableChildren.clear();
    reflow();
    return this;
  }

  @Override
  public Iterator<UIObject> iterator() {
    return this.contentTarget.mutableChildren.iterator();
  }

  public List<UIObject> getChildren() {
    return this.contentTarget.mutableChildren;
  }

  public UI2dComponent getChild(int i) {
    return (UI2dComponent) this.contentTarget.mutableChildren.get(i);
  }

  private boolean hasSameKeyFocus(UIObject object) {
    return
      (object instanceof UI2dContainer) &&
      this.arrowKeyFocus == ((UI2dContainer)object).arrowKeyFocus &&
      object.isVisible();
  }

  private void keyFocus(KeyEvent keyEvent, int delta) {
    if (this.children.size() > 0) {
      UIObject focusedChild = getFocusedChild();
      if (focusedChild == null) {

        // God damn this is so fugly just to iterate the
        // CopyOnWriteArrayList in reverse
        List<UIObject> test = this.children;
        if (delta < 0) {
          test = new ArrayList<UIObject>(this.mutableChildren);
          Collections.reverse(test);
        }

        for (UIObject object : test) {
          if (object.isVisible() && (object instanceof UIKeyFocus)) {
            object.focus(keyEvent);
            break;
          } else if (hasSameKeyFocus(object)) {
            ((UI2dContainer) object).keyFocus(keyEvent, delta);
            break;
          }
        }
      } else {
        int index = this.children.indexOf(focusedChild);
        while (true) {
          index += delta;
          if (index < 0 || index >= this.children.size()) {
            UI2dComponent sibling = this;
            while (sibling != null) {
              sibling = (index < 0) ? sibling.getPrevSibling() : sibling.getNextSibling();
              if ((sibling instanceof UIKeyFocus) && sibling.isVisible()) {
                sibling.focus(keyEvent);
                break;
              } else if (hasSameKeyFocus(sibling)) {
                ((UI2dContainer) sibling).keyFocus(keyEvent, delta);
                break;
              }
            }
            break;
          }
          UIObject object = this.children.get(index);
          if (object.isVisible() && (object instanceof UIKeyFocus)) {
            object.focus(keyEvent);
            break;
          } else if (hasSameKeyFocus(object)) {
            ((UI2dContainer) object).keyFocus(keyEvent, delta);
            break;
          }
        }
      }
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    super.onKeyPressed(keyEvent, keyChar, keyCode);
    if (!keyEvent.isConsumed()) {
      if (this.arrowKeyFocus == ArrowKeyFocus.VERTICAL) {
        if (keyCode == KeyEvent.VK_UP) {
          keyEvent.consume();
          keyFocus(keyEvent, -1);
        } else if (keyCode == KeyEvent.VK_DOWN) {
          keyEvent.consume();
          keyFocus(keyEvent, 1);
        }
      } else if (this.arrowKeyFocus == ArrowKeyFocus.HORIZONTAL) {
        if (keyCode == KeyEvent.VK_LEFT) {
          keyEvent.consume();
          keyFocus(keyEvent, -1);
        } else if (keyCode == KeyEvent.VK_RIGHT) {
          keyEvent.consume();
          keyFocus(keyEvent, 1);
        }
      }
    }
  }

  private float drawDragIndicator = -1;

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    super.onDraw(ui, vg);
    if (this.drawDragIndicator >= 0) {
      vg.beginPath();
      vg.fillColor(ui.theme.attentionColor);
      if (this.contentTarget.layout.isHorizontalList()) {
        vg.rect(
          this.drawDragIndicator - .5f,
          this.topPadding,
          1,
          this.contentTarget.height - this.topPadding - this.bottomPadding
        );
      } else if (this.contentTarget.layout.isVerticalList()) {
        vg.rect(
          this.leftPadding,
          this.drawDragIndicator - .5f,
          this.contentTarget.width - this.leftPadding - this.rightPadding,
          1
        );
      }
      vg.fill();
    }
  }

  void dragCancel() {
    if (this.drawDragIndicator >= 0) {
      this.drawDragIndicator = -1;
      redraw();
    }
  }

  void dragChild(UIObject drag, float mx, float my, boolean release) {
    mx += drag.getX();
    my += drag.getY();

    final boolean isHorizontal = this.contentTarget.layout.isHorizontalList();
    final boolean isVertical = this.contentTarget.layout.isVerticalList();

    final boolean invalid =
      (isVertical && (mx < 0 || mx > this.contentTarget.getScrollWidth())) ||
      (isHorizontal && (my < 0 || my > this.contentTarget.getScrollHeight()));
    if (invalid) {
      dragCancel();
      return;
    }

    UIObject hover = null;
    int hoverIndex = -1;
    int dragIndex = -1;

    int index = 0;
    for (UIObject child : this.contentTarget) {
      if (child == drag) {
        dragIndex = index;
      } else if (child.isVisible() && (child instanceof UI2dComponent.UIDragReorder)) {
        if (isHorizontal && (mx > child.getX())) {
          hover = child;
          hoverIndex = index;
        } else if (isVertical && (my > child.getY())) {
          hover = child;
          hoverIndex = index;
        }
      }
      ++index;
    }

    float dragPos = -1;
    if ((hover != null) && (hover != drag)) {
      if (isHorizontal) {
        if (mx > hover.getX() + .5f * hover.getWidth()) {
          dragPos = LXUtils.minf(this.contentTarget.getScrollWidth() - .5f, hover.getX() + hover.getWidth() + .5f * this.contentTarget.childSpacingX);
          ++hoverIndex;
        } else {
          dragPos = LXUtils.maxf(.5f, hover.getX() - .5f * this.contentTarget.childSpacingX);
        }
      } else if (isVertical) {
        if (my > hover.getY() + .5f * hover.getHeight()) {
          dragPos = LXUtils.minf(this.contentTarget.getScrollHeight() - .5f, hover.getY() + hover.getHeight() + .5f * this.contentTarget.childSpacingY);
          ++hoverIndex;
        } else {
          dragPos = LXUtils.maxf(.5f, hover.getY() - .5f * this.contentTarget.childSpacingY);
        }
      }
    }

    if (hoverIndex > dragIndex) {
      --hoverIndex;
    }
    if (!release && (hoverIndex != dragIndex)) {
      // Redraw if the drag indicator position has changed
      if (this.drawDragIndicator != dragPos) {
        this.drawDragIndicator = dragPos;
        redraw();
      }
    } else {
      if (release) {
        if ((hoverIndex >= 0) && (hoverIndex != dragIndex)) {
          ((UI2dComponent.UIDragReorder) drag).onDragReorder(this, (UI2dComponent) drag, hoverIndex);
        }
      }
      dragCancel();
    }
  }
}
