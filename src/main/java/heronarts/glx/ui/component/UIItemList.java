/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.clipboard.LXTextValue;
import heronarts.lx.utils.LXUtils;

/**
 * An ItemList is a scrollable list of elements with a focus state
 * and action handling when the elements are focused or clicked on.
 */
public interface UIItemList {

  public interface Listener {
    public void onItemFocused(Item item);
    public void onItemActivated(Item item);
    public void onItemDeactivated(Item item);
  }

  /**
   * Interface to which items in the list must conform
   */
  public static abstract class Item {

    protected boolean hidden = false;

    private boolean isVisible() {
      if (this.hidden) {
        return false;
      }
      Section section = getSection();
      return (section == null) || section.expanded;
    }

    /**
     * Whether this item is in a special active state
     *
     * @return If this item is active
     */
    public boolean isActive() {
      return false;
    }

    /**
     * Whether the item is checked, applies only if checkbox mode set on the list.
     *
     * @return If this item is checked
     */
    public boolean isChecked() {
      return false;
    }

    /**
     * Active background color for this item
     *
     * @param ui UI context
     * @return Background color
     */
    public int getActiveColor(UI ui) {
      return ui.theme.controlBackgroundColor.get();
    }

    /**
     * String label that displays on this item
     *
     * @return Label for the item
     */
    public abstract String getLabel();

    /**
     * Action handler, invoked when item is activated
     */
    public void onActivate() {}

    /**
     * Action handler invoked when item is checked
     *
     * @param checked If checked
     */
    public void onCheck(boolean checked) {
      throw new UnsupportedOperationException("Item does not implement checkbox operation");
    }

    /**
     * Action handler, invoked when item is deactivated. Only applies when setMomentary(true)
     */
    public void onDeactivate() {}

    /**
     * Action handler, invoked when an item is renamed. Only applies when setRenamable(true)
     *
     * @param name New name for item
     */
    public void onRename(String name) {
      throw new UnsupportedOperationException("Item does not implement renaming");
    }

    /**
     * Action handler, invoked when an item is reordered. Only applies to the item that the action
     * was taken upon, not other items affected by the reordering. Only applies when setReorderable(true)
     *
     * @param order New position for item
     */
    public void onReorder(int order) {
      throw new UnsupportedOperationException("Item does not implement reordering");
    }

    /**
     * Action handler, invoked when item is deleted
     */
    public void onDelete() {}

    /**
     * Action handler, invoked when item is focused
     */
    public void onFocus() {}

    /**
     * Section that this item belongs to
     *
     * @return section or null
     */
    public Section getSection() {
      return null;
    }

    /**
     * Dispose of the item
     */
    public void dispose() {}
  }

  /**
   * A section is an item in the list that indents the items beneath it.
   */
  public static class Section extends Item {

    private final List<Item> items = new ArrayList<Item>();

    private boolean expanded = true;

    private final String label;

    protected Section() {
      this(null);
    }

    public Section(String label) {
      this.label = label;
    }

    @Override
    public String getLabel() {
      return this.label;
    }

    private void addItem(Item item) {
      this.items.add(item);
    }

    private void removeItem(Item item) {
      this.items.remove(item);
    }

    private void toggle() {
      this.expanded = !this.expanded;
    }

    @Override
    public final Section getSection() {
      // We're not supporting nested sections!
      return null;
    }

    @Override
    public void onActivate() {
      this.expanded = true;
    }

  }

  public static class Impl {

    private static final int PADDING = 2;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_MARGIN = 2;
    private static final int ROW_SPACING = ROW_HEIGHT + ROW_MARGIN;
    private static final int CHECKBOX_SIZE = 8;
    private static final int SECTION_CHEVRON_WIDTH = 14;

    private final LX lx;

    private final UI2dContainer list;

    private final List<Item> items = new CopyOnWriteArrayList<Item>();

    private final List<Listener> listeners = new ArrayList<Listener>();

    private int focusIndex = -1;

    private boolean singleClickActivate = false;

    private boolean isMomentary = false;

    private boolean isRenamable = false;

    private boolean isReorderable = false;

    private boolean isDeletable = false;

    private boolean showCheckboxes = false;

    private boolean renaming = false;

    private UIInputBox.EditState editState = new UIInputBox.EditState();

    private boolean scrolling = false;

    private Item dragging = null;
    private float dragY = -1;

    private boolean mouseChevronPress = false;

    private int mouseActivate = -1;

    private int keyActivate = -1;

    private int controlSurfaceFocusIndex = -1;
    private int controlSurfaceFocusLength = -1;
    private UIColor controlSurfaceFocusColor = UIColor.NONE;

    private String filter = null;

    private Impl(UI ui, UI2dContainer list) {
      this.lx = ui.lx;
      this.list = list;
      list.setBackgroundColor(ui.theme.listBackgroundColor);;
      list.setBorderColor(ui.theme.listBorderColor);;
      list.setBorderRounding(4);

      // Animate cursor while editing
      list.addLoopTask(deltaMs -> {
        if (this.renaming) {
          this.editState.animateCursor(deltaMs);
          list.redraw();
        }
      });
    }

    private void addListener(Listener listener) {
      Objects.requireNonNull(listener, "May not add null UIItemList.Listener");
      if (this.listeners.contains(listener)) {
        throw new IllegalStateException("May not add duplicate UIItemList.Listener: " + listener);
      }
      this.listeners.add(listener);
    }

    private void removeListener(Listener listener) {
      if (!this.listeners.contains(listener)) {
        throw new IllegalStateException("May not remove non-registered UIItemList.Listener: " + listener);
      }
      this.listeners.remove(listener);
    }

    private void focusNext(int increment) {
      int index = this.focusIndex + increment;
      while (index >= 0 && index < this.items.size()) {
        if (this.items.get(index).isVisible()) {
          setFocusIndex(index);
          return;
        }
        index += increment;
      }
    }

    private float getFocusYOffset(int focusIndex) {
      float offset = 0;
      int i = 0;
      for (Item item : this.items) {
        if (i++ >= focusIndex) {
          break;
        }
        if (item.isVisible()) {
          offset += ROW_SPACING;
        }
      }
      return offset;
    }

    private void setFocusIndex(int focusIndex) {
      setFocusIndex(focusIndex, true);
    }

    private void setFocusIndex(int focusIndex, boolean scroll) {
      focusIndex = LXUtils.constrain(focusIndex, -1, this.items.size() - 1);
      if (this.focusIndex != focusIndex) {
        this.renaming = false;
        cancelDragging();
        if ((focusIndex >= 0) && scroll && this.list instanceof ScrollList) {
          ScrollList scrollList = (ScrollList) this.list;
          final float yOffset = getFocusYOffset(focusIndex);
          final float yDelta = yOffset + scrollList.getScrollY();
          if (yDelta < 0) {
            scrollList.setScrollY(-yOffset);
          } else if (yDelta >= list.getHeight() - ROW_SPACING) {
            scrollList.setScrollY(list.getHeight() - ROW_SPACING - ROW_MARGIN - yOffset);
          }
        }
        this.focusIndex = focusIndex;
        if (this.focusIndex >= 0) {
          Item item = this.items.get(this.focusIndex);
          item.onFocus();
          for (Listener listener : this.listeners) {
            listener.onItemFocused(item);
          }
        }
        this.list.redraw();
      }
    }

    private int getFocusedIndex() {
      return this.focusIndex;
    }

    /**
     * Retrieves the currently focused item in the list.
     *
     * @return Focused item, or null if none is focused
     */
    private UIItemList.Item getFocusedItem() {
      if (this.focusIndex >= 0 && this.focusIndex < this.items.size()) {
        return this.items.get(this.focusIndex);
      }
      return null;
    }

    private void recomputeContentHeight() {
      int itemCount = 0;
      for (Item item : this.items) {
        if (item.isVisible()) {
          ++itemCount;
        }
      }
      setContentHeight(ROW_SPACING * itemCount + ROW_MARGIN);
    }

    private void addSection(Section section) {
      addItem(section);
    }

    /**
     * Adds an item to the list
     *
     * @param item Item to remove
     * @return this
     */
    private void addItem(int index, Item item) {
      Section section = item.getSection();
      if (section != null) {
        throw new IllegalArgumentException("Cannot specify index when adding item to section");
      }
      this.items.add(index, item);
      recomputeContentHeight();
      this.list.redraw();
    }

    /**
     * Adds an item to the list
     *
     * @param item Item to remove
     * @return this
     */
    private void addItem(Item item) {
      Section section = item.getSection();
      if (section != null) {
        section.addItem(item);
        int sectionIndex = this.items.indexOf(section);
        this.items.add(sectionIndex + section.items.size(), item);
      } else {
        this.items.add(item);
      }
      recomputeContentHeight();
      this.list.redraw();
    }

    /**
     * Removes an item from the list
     *
     * @param item Item to remove
     * @return this
     */
    private void removeItem(Item item) {
      Section section = item.getSection();
      if (section != null) {
        section.removeItem(item);
      }

      int itemIndex = this.items.indexOf(item);
      if (itemIndex < 0) {
        throw new IllegalArgumentException("Item is not in UIItemList: " + item);
      }
      this.items.remove(itemIndex);
      if (this.focusIndex >= this.items.size()) {
        setFocusIndex(items.size() - 1);
      } else if (this.focusIndex >= 0) {
        Item focusItem = this.items.get(this.focusIndex);
        focusItem.onFocus();
        for (Listener listener : this.listeners) {
          listener.onItemFocused(focusItem);
        }
      }
      recomputeContentHeight();
      cancelDragging();
      this.list.redraw();
    }

    private void moveItem(Item item, int index) {
      this.items.remove(item);
      this.items.add(index, item);
      cancelDragging();
      this.list.redraw();
    }

    /**
     * Sets the items in the list and redraws it
     *
     * @param items Items
     * @return this
     */
    private void setItems(List<? extends Item> items) {
      cancelDragging();
      this.items.clear();
      for (Item item : items) {
        Section section = item.getSection();
        if (section != null) {
          section.addItem(item);
        }
      }
      this.items.addAll(items);
      if (this.focusIndex >= items.size()) {
        setFocusIndex(items.size() - 1);
      } else if (this.focusIndex >= 0) {
        Item item = this.items.get(this.focusIndex);
        item.onFocus();
        for (Listener listener : this.listeners) {
          listener.onItemFocused(item);
        }
      }
      setContentHeight(ROW_SPACING * items.size() + ROW_MARGIN);
      this.list.redraw();
    }

    private void clearItems() {
      cancelDragging();
      this.items.clear();
      setContentHeight(ROW_MARGIN);
      this.list.redraw();
    }

    private void setSingleClickActivate(boolean singleClickActivate) {
      this.singleClickActivate = singleClickActivate;
    }

    private void setShowCheckboxes(boolean showCheckboxes) {
      if (this.showCheckboxes != showCheckboxes) {
        this.showCheckboxes = showCheckboxes;
        this.list.redraw();
      }
    }

    private void setRenamable(boolean isRenamable) {
      this.isRenamable = isRenamable;
    }

    private boolean isRenaming() {
      return this.renaming;
    }

    private String getRenameBuffer() {
      return this.editState.getRange();
    }

    private void renameAppend(String append) {
      if (this.renaming && !append.isEmpty()) {
        this.editState.append(append);
        this.list.redraw();
      }
    }

    private void setMomentary(boolean momentary) {
      this.isMomentary = momentary;
    }

    private void setReorderable(boolean isReorderable) {
      this.isReorderable = isReorderable;
    }

    private void setDeletable(boolean isDeletable) {
      this.isDeletable = isDeletable;
    }

    private void setFilter(String filter) {
      if (filter != null) {
        filter = filter.toLowerCase();
      }
      if (this.filter != filter) {
        this.filter = filter;
        boolean changed = false;
        for (Item item : this.items) {
          if (!(item instanceof Section)) {
            boolean hidden = (filter != null) && !item.getLabel().toLowerCase().contains(filter);
            if (hidden != item.hidden) {
              item.hidden = hidden;
              changed = true;
            }
          }
        }
        for (Item item : this.items) {
          if (item instanceof Section) {
            Section section = (Section) item;
            boolean sectionHidden = true;
            for (Item sectionItem : section.items) {
              if (!sectionItem.hidden) {
                sectionHidden = false;
                break;
              }
            }
            if (sectionHidden != section.hidden) {
              section.hidden = sectionHidden;
              changed = true;
            }
          }
        }

        if (changed) {
          setFilterFocusIndex();
          recomputeContentHeight();
          this.list.redraw();
        }
      }
    }

    private void setFilterFocusIndex() {
      boolean resetFocus = true;
      if (this.focusIndex >= 0 && this.focusIndex < this.items.size()) {
        resetFocus = !this.items.get(this.focusIndex).isVisible();
      }
      if (resetFocus) {
        int focusIndex = -1;
        int i = 0;
        for (Item item : this.items) {
          if (!(item instanceof Section) && item.isVisible()) {
            focusIndex = i;
            break;
          }
          ++i;
        }
        setFocusIndex(focusIndex);
      }
    }

    private void setControlSurfaceFocus(int index, int length, UIColor color) {
      this.controlSurfaceFocusIndex = index;
      this.controlSurfaceFocusLength = length;
      this.controlSurfaceFocusColor = color;
      this.list.redraw();
    }

    private void activate() {
      if (this.focusIndex >= 0) {
        Item item = this.items.get(this.focusIndex);
        if (item instanceof Section) {
          ((Section) item).toggle();
          recomputeContentHeight();
          this.list.redraw();
        } else {
          item.onActivate();
          for (Listener listener : this.listeners) {
            listener.onItemActivated(item);
          }
        }
      }
    }

    private void delete() {
      if (this.focusIndex >= 0) {
        this.items.get(this.focusIndex).onDelete();
      }
    }

    private void check() {
      if (this.focusIndex >= 0) {
        Item item = this.items.get(this.focusIndex);
        if (!(item instanceof Section)) {
          item.onCheck(!item.isChecked());
          this.list.redraw();
        }
      }
    }

    private float getScrollY() {
      if (this.list instanceof ScrollList) {
        return ((ScrollList) this.list).getScrollY();
      }
      return 0;
    }

    private float getScrollHeight() {
      if (this.list instanceof ScrollList) {
        return ((ScrollList) this.list).getScrollHeight();
      }
      return this.list.getHeight();
    }

    private void setContentHeight(float height) {
      this.list.setContentHeight(height);
    }

    private void setScrollY(float scrollY) {
      if (this.list instanceof ScrollList) {
        ((ScrollList) this.list).setScrollY(scrollY);
      }
    }

    private float getHeight() {
      return this.list.getHeight();
    }

    private float getWidth() {
      return this.list.getWidth();
    }

    private float getRowWidth() {
      return (getScrollHeight() > this.list.getHeight()) ? this.list.getWidth() - SCROLL_BAR_WIDTH - PADDING : this.list.getWidth();
    }

    private int getVisibleFocusIndex() {
      int counter = 0;
      int visibleIndex = -1;
      for (Item item : this.items) {
        if (item.isVisible()) {
          ++visibleIndex;
        }
        if (counter++ >= this.focusIndex) {
          break;
        }
      }
      return visibleIndex;
    }

    private void drawFocus(UI ui, VGraphics vg) {
      int visibleFocusIndex = getVisibleFocusIndex();
      if (visibleFocusIndex >= 0) {
        final boolean hasScroll = getScrollHeight() > getHeight();
        if (hasScroll) {
          vg.scissorPush(1, 1, getWidth()-2, getHeight() - 2);
        }
        float yp = ROW_MARGIN + getScrollY() + ROW_SPACING * visibleFocusIndex;
        UI2dComponent.drawFocusCorners(ui, vg, ui.theme.focusColor.get(), PADDING, yp, getRowWidth() - 2*PADDING, ROW_HEIGHT, 2);
        if (hasScroll) {
          vg.scissorPop();
        }
      }
    }

    private void onDraw(UI ui, VGraphics vg) {
      final float scrollY = getScrollY();
      final float height = getHeight();
      final float scrollHeight = getScrollHeight();
      final boolean hasScroll = scrollHeight > height;

      if (hasScroll) {
        vg.scissorPush(1, 1, getWidth()-2, height-2);
      }
      vg.fontFace(ui.theme.getControlFont());
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);

      int index = 0;
      float yp = ROW_MARGIN + scrollY;

      final float rowWidth = getRowWidth();

      if (hasScroll) {
        float eligibleHeight = height - 2*PADDING;
        float barHeight = height / scrollHeight * eligibleHeight;
        float barY = PADDING - (eligibleHeight - barHeight) * (getScrollY() / (scrollHeight - height));
        vg.beginPath();
        vg.fillColor(ui.theme.listScrollBarColor);
        vg.rect(getWidth() - PADDING - SCROLL_BAR_WIDTH, barY, SCROLL_BAR_WIDTH, barHeight, 2);
        vg.fill();
      }

      for (Item item : this.items) {
        Section section = item.getSection();
        boolean isSection = item instanceof Section;

        // Skip rendering items that are invisible
        if (!item.isVisible()) {
          ++index;
          continue;
        }

        if (yp <= -ROW_SPACING) {
          // We're not visible yet...
          ++index;
          yp += ROW_SPACING;
          continue;
        } else if (yp > getHeight()) {
          // We're offscreen at this point...
          break;
        }

        boolean renameItem = this.renaming && (this.focusIndex == index);

        int backgroundColor, textColor;
        if (item.isActive()) {
          backgroundColor = item.getActiveColor(ui);
          textColor = ui.theme.listItemFocusedTextColor.get();
        } else {
          backgroundColor = (index == this.focusIndex) ? ui.theme.listItemFocusedBackgroundColor.get() : ui.theme.listItemBackgroundColor.get();
          textColor = isSection ? ui.theme.listSectionTextColor.get() : ((index == this.focusIndex) ? ui.theme.listItemFocusedTextColor.get() : ui.theme.controlTextColor.get());
        }
        vg.beginPath();
        vg.fillColor(backgroundColor);
        vg.rect(PADDING, yp, rowWidth-2*PADDING, ROW_HEIGHT, 4);
        vg.fill();

        int textX = 6;
        if (isSection) {
          vg.beginPath();
          vg.fillColor(ui.theme.listSectionArrowColor);
          if (((Section) item).expanded) {
            vg.moveTo(textX-1, yp + 6);
            vg.lineTo(textX+5, yp + 6);
            vg.lineTo(textX+2, yp + 12);
          } else {
            vg.moveTo(textX, yp + 5);
            vg.lineTo(textX, yp + 11);
            vg.lineTo(textX + 6, yp + 8);
          }
          vg.closePath();
          vg.fill();
          textX += 8;
        } else if (section != null) {
          textX += 14;
        }

        if (this.showCheckboxes && !isSection) {
          vg.beginPath();
          vg.strokeColor(textColor);
          vg.rect(textX + .5f, yp + 4.5f, CHECKBOX_SIZE-1, CHECKBOX_SIZE-1);
          vg.stroke();

          if (item.isChecked()) {
            vg.beginPath();
            vg.fillColor(textColor);
            vg.rect(textX + 2, yp+6, CHECKBOX_SIZE - 4, CHECKBOX_SIZE - 4);
            vg.fill();
          }

          textX += CHECKBOX_SIZE + 4;
        }

        if (renameItem) {
          vg.beginPath();
          vg.fillColor(ui.theme.editTextBackgroundColor);
          vg.rect(textX-2, yp+1, rowWidth - 2*PADDING - textX + 2, ROW_HEIGHT-2, 4);
          vg.fill();

          vg.fillColor(ui.theme.editTextColor);
          UIInputBox.onDrawText(ui, vg, this.editState, this.editState.buffer, true, VGraphics.Align.LEFT, textX - 2, yp - .5f, rowWidth, ROW_HEIGHT, rowWidth - textX - 5);
        } else {
          vg.fillColor(textColor);
          vg.beginPath();
          vg.text(textX, yp + ROW_SPACING/2-.5f, UI2dComponent.clipTextToWidth(vg, item.getLabel(), rowWidth - textX - 2));
          vg.fill();
        }
        yp += ROW_SPACING;

        ++index;
      }

      if ((this.dragging != null) && (this.dragY >= 0)) {
        vg.fillColor(ui.theme.attentionColor);
        vg.beginPath();
        vg.rect(PADDING, LXUtils.constrainf(this.dragY + getScrollY() - .5f, PADDING, getHeight()-PADDING), getRowWidth() - 2*PADDING, 1);
        vg.fill();
      }

      if (this.controlSurfaceFocusIndex >= 0 && this.controlSurfaceFocusLength > 0) {
        vg.strokeColor(this.controlSurfaceFocusColor);
        vg.beginPath();
        vg.rect(
          PADDING - .5f,
          ROW_MARGIN + scrollY + this.controlSurfaceFocusIndex * ROW_SPACING - .5f,
          rowWidth - 2*PADDING + 1,
          Math.min(this.controlSurfaceFocusLength, this.items.size() - this.controlSurfaceFocusIndex) * ROW_SPACING - ROW_MARGIN + 1,
          4
        );
        vg.stroke();
      }

      if (hasScroll) {
        vg.scissorPop();
      }

    }

    private int getMousePressIndex(float my) {
      if ((my % ROW_SPACING) < ROW_MARGIN) {
        // Don't detect clicks on strip between rows
        return -1;
      }
      int visibleIndex = (int) (my / (ROW_SPACING));
      int visibleCounter = 0;
      int itemIndex = 0;
      int mouseIndex = -1;
      for (Item item : this.items) {
        if (item.isVisible()) {
          if (visibleCounter++ >= visibleIndex) {
            mouseIndex = itemIndex;
            break;
          }
        }
        ++itemIndex;
      }
      return mouseIndex;
    }

    private void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
      this.mouseActivate = -1;
      this.mouseChevronPress = false;
      cancelDragging();

      if (getScrollHeight() > getHeight() && mx >= getRowWidth()) {
        this.scrolling = true;
      } else {
        this.scrolling = false;
        int index = getMousePressIndex(my - getScrollY());
        if (index >= 0) {
          setFocusIndex(index);
          if (this.showCheckboxes && (mx < (5*PADDING + CHECKBOX_SIZE))) {
            if (mx >= 2*PADDING) {
              check();
            }
          } else {
            this.mouseChevronPress = (mx < SECTION_CHEVRON_WIDTH) && (getFocusedItem() instanceof Section);
            if (this.mouseChevronPress) {
              activate();
            } else if (this.singleClickActivate || mouseEvent.isDoubleClick()) {
              activate();
              if (this.isMomentary) {
                this.mouseActivate = this.focusIndex;
              }
            } else if (this.isReorderable) {
              this.dragging = this.items.get(index);
            }
          }
        }
      }
    }

    private void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
      if (this.scrolling) {
        mouseEvent.consume();
        setScrollY(getScrollY() - dy * (getScrollHeight() / getHeight()));
      } else if (this.dragging != null) {
        mouseEvent.consume();
        dragItem(mx, my, false);
      }
    }

    private void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
      this.scrolling = false;
      if (this.mouseActivate >= 0 && this.mouseActivate < this.items.size()) {
        Item item = this.items.get(this.mouseActivate);
        item.onDeactivate();
        for (Listener listener : this.listeners) {
          listener.onItemDeactivated(item);
        }
      }
      this.mouseActivate = -1;
      if (this.dragging != null) {
        dragItem(mx, my, true);
        cancelDragging();
        this.list.redraw();
      }
    }

    private void cancelDragging() {
      this.dragging = null;
      this.dragY = -1;
    }

    private void dragItem(float mx, float my, boolean release) {
      float dragY = -1;
      int targetIndex = -1;
      boolean before = true;

      if (mx >= 0 && mx < getRowWidth() && my >= 0 && my < getHeight()) {
        my -= getScrollY();

        int visibleIndex = (int) (my / ROW_SPACING);
        boolean bottomHalf = (my % ROW_SPACING) > (ROW_MARGIN + ROW_HEIGHT / 2);
        int visibleCounter = 0;

        int itemIndex = 0;
        for (Item item : this.items) {
          if (item.isVisible()) {
            ++visibleCounter;
          }
          if (visibleCounter > visibleIndex) {
            if (this.dragging == item) {
              break;
            }
            targetIndex = itemIndex + (bottomHalf ? 1 : 0);
            dragY = 1 + ROW_SPACING * (visibleCounter - (bottomHalf ? 0 : 1));
            break;
          } else if (this.dragging == item) {
            before = false;
          }
          ++itemIndex;
        }
      }

      if (!before) {
        --targetIndex;
      }

      if (targetIndex >= 0) {
        int dragIndex = this.items.indexOf(this.dragging);
        if (dragIndex == targetIndex) {
          targetIndex = -1;
        }
      }

      if (!release && (targetIndex >= 0)) {
        if (this.dragY != dragY) {
          this.dragY = dragY;
          this.list.redraw();
        }
      } else {
        if (release && (targetIndex >= 0)) {
          this.items.remove(this.dragging);
          this.items.add(targetIndex, this.dragging);
          this.dragging.onReorder(this.focusIndex = targetIndex);
        }
        if (this.dragY != -1) {
          this.dragY = -1;
          this.list.redraw();
        }
      }

    }

    private void onBlur() {
      if (this.renaming) {
        this.renaming = false;
        this.list.redraw();
      }
    }

    private boolean onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
      boolean consume = false;
      if (this.renaming) {
        if (keyCode == KeyEvent.VK_ESCAPE) {
          consume = true;
          this.renaming = false;
          this.list.redraw();
        } else if (keyEvent.isEnter()) {
          consume = true;
          String newName = this.editState.buffer.trim();
          if (newName.length() > 0) {
            this.items.get(this.focusIndex).onRename(newName);
          }
          this.renaming = false;
          this.list.redraw();
        } else if (keyEvent.isDelete()) {
          consume = true;
          if (this.editState.buffer.length() > 0) {
            if (keyEvent.isShiftDown() || keyEvent.isCommand()) {
              this.editState.deleteAll();
            } else {
              this.editState.delete();
            }
            this.list.redraw();
          }
        } else if (keyEvent.isCommand(KeyEvent.VK_X)) {
          final String cut = this.editState.cut();
          if (cut != null) {
            consume = true;
            this.lx.clipboard.setItem(new LXTextValue(cut));
            this.list.redraw();
          }
        } else if (keyEvent.isCommand(KeyEvent.VK_A)) {
          consume = true;
          this.editState.selectAll();
          this.list.redraw();
        } else if (keyCode == KeyEvent.VK_LEFT) {
          consume = true;
          this.editState.cursorLeft(keyEvent);
          this.list.redraw();
        } else if (keyCode == KeyEvent.VK_RIGHT) {
          consume = true;
          this.editState.cursorRight(keyEvent);
          this.list.redraw();
        } else if (UITextBox.isValidTextCharacter(keyChar)) {
          consume = true;
          this.editState.append(keyChar);
          this.list.redraw();
        }
      } else {
        if (keyCode == KeyEvent.VK_UP) {
          consume = true;
          if (this.isReorderable && keyEvent.isCommand()) {
            if (this.focusIndex > 0) {
              Item item = this.items.remove(this.focusIndex);
              this.focusIndex = this.focusIndex - 1;
              this.items.add(this.focusIndex, item);
              item.onReorder(this.focusIndex);
              this.list.redraw();
            }
          } else {
            if (this.focusIndex > 0) {
              focusNext(-1);
              this.list.redraw();
            }
          }
        } else if (keyCode == KeyEvent.VK_DOWN) {
          consume = true;
          if (this.isReorderable && keyEvent.isCommand()) {
            if (this.focusIndex < this.items.size() - 1) {
              Item item = this.items.remove(this.focusIndex);
              this.focusIndex = this.focusIndex + 1;
              this.items.add(this.focusIndex, item);
              item.onReorder(this.focusIndex);
              this.list.redraw();
            }
          } else {
            focusNext(1);
            this.list.redraw();
          }
        } else if (keyEvent.isEnter()) {
          consume = true;
          if (this.isMomentary) {
            this.keyActivate = this.focusIndex;
          }
          activate();
        } else if (keyCode == KeyEvent.VK_SPACE) {
          consume = true;
          if (this.showCheckboxes) {
            check();
          } else {
            if (this.isMomentary) {
              this.keyActivate = this.focusIndex;
            }
            activate();
          }
        } else if (keyEvent.isDelete()) {
          if (this.isDeletable) {
            consume = true;
            delete();
          }
        } else if (keyEvent.isCommand() && !keyEvent.isShiftDown()) {
          if (keyCode == KeyEvent.VK_D) {
            consume = true;
            delete();
          } else if (keyCode == KeyEvent.VK_R) {
            if (this.isRenamable && this.focusIndex >= 0) {
              consume = true;
              this.renaming = true;
              this.editState.init(this.items.get(this.focusIndex).getLabel());
              this.list.redraw();
            }
          }
        }
      }
      return consume;
    }

    private boolean onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
      boolean consume = false;
      if (keyEvent.isEnter() || keyCode == KeyEvent.VK_SPACE) {
        if (this.isMomentary) {
          if (this.keyActivate >= 0 && this.keyActivate < this.items.size()) {
            consume = true;
            Item item = this.items.get(this.keyActivate);
            item.onDeactivate();
            for (Listener listener : this.listeners) {
              listener.onItemDeactivated(item);
            }
          }
          this.keyActivate = -1;
        }
      }
      return consume;
    }

    private void onFocus(Event event) {
      if (this.focusIndex < 0 && this.items.size() > 0) {
        setFocusIndex(0, false);
      }
    }
  }

  public static class ScrollList extends UI2dContainer implements UIItemList, UIFocus {

    private final Impl impl;

    private float scrollY = 0;
    private float scrollHeight = 0;

    public ScrollList(UI ui, int x, int y, int w, int h) {
      super(x, y, w, h);
      setScrollHeight(Impl.ROW_MARGIN);
      this.impl = new Impl(ui, this);
    }

    @Override
    public UI2dContainer setContentSize(float w, float h) {
      setWidth(w);
      return setScrollHeight(h);
    }

    public ScrollList setScrollHeight(float scrollHeight) {
      if (this.scrollHeight != scrollHeight) {
        this.scrollHeight = scrollHeight;
        rescroll();
      }
      return this;
    }

    @Override
    public float getScrollHeight() {
      return this.scrollHeight;
    }

    @Override
    protected void onResize() {
      rescroll();
    }

    public float getScrollY() {
      return this.scrollY;
    }

    public ScrollList setScrollY(float scrollY) {
      float minScrollY = LXUtils.minf(0, getHeight() - getScrollHeight());
      scrollY = LXUtils.constrainf(scrollY, minScrollY, 0);
      if (this.scrollY != scrollY) {
        this.scrollY = scrollY;
        redraw();
      }
      return this;
    }

    private void rescroll() {
      setScrollY(this.scrollY);
      redraw();
    }

    @Override
    public UIItemList setFocusIndex(int focusIndex) {
      this.impl.setFocusIndex(focusIndex);
      return this;
    }

    @Override
    public int getFocusedIndex() {
      return this.impl.getFocusedIndex();
    }

    @Override
    public UIItemList.Item getFocusedItem() {
      return this.impl.getFocusedItem();
    }

    @Override
    public UIItemList addSection(Section section) {
      this.impl.addSection(section);
      return this;
    }

    @Override
    public UIItemList addItem(int index, Item item) {
      this.impl.addItem(index, item);
      return this;
    }

    @Override
    public UIItemList addItem(Item item) {
      this.impl.addItem(item);
      return this;
    }

    @Override
    public UIItemList moveItem(Item item, int index) {
      this.impl.moveItem(item, index);
      return this;
    }

    @Override
    public UIItemList removeItem(Item item) {
      this.impl.removeItem(item);
      return this;
    }

    @Override
    public UIItemList setItems(List<? extends Item> items) {
      this.impl.setItems(items);
      return this;
    }

    @Override
    public UIItemList clearItems() {
      this.impl.clearItems();
      return this;
    }

    @Override
    public List<? extends Item> getItems() {
      return this.impl.items;
    }

    @Override
    public UIItemList setSingleClickActivate(boolean singleClickActivate) {
      this.impl.setSingleClickActivate(singleClickActivate);
      return this;
    }

    @Override
    public UIItemList setShowCheckboxes(boolean showCheckboxes) {
      this.impl.setShowCheckboxes(showCheckboxes);
      return this;
    }

    @Override
    public UIItemList setRenamable(boolean isRenamable) {
      this.impl.setRenamable(isRenamable);
      return this;
    }

    @Override
    public boolean isRenaming() {
      return this.impl.isRenaming();
    }

    @Override
    public String getRenameBuffer() {
      return this.impl.getRenameBuffer();
    }

    @Override
    public void renameAppend(String append) {
      this.impl.renameAppend(append);
    }

    @Override
    public UIItemList setMomentary(boolean momentary) {
      this.impl.setMomentary(momentary);
      return this;
    }

    @Override
    public UIItemList setReorderable(boolean reorderable) {
      this.impl.setReorderable(reorderable);
      return this;
    }

    @Override
    public UIItemList setDeletable(boolean deletable) {
      this.impl.setDeletable(deletable);
      return this;
    }

    @Override
    public UIItemList setFilter(String filter) {
      this.impl.setFilter(filter);
      return this;
    }

    @Override
    public UIItemList setControlSurfaceFocus(int index, int length, UIColor color) {
      this.impl.setControlSurfaceFocus(index, length, color);
      return this;
    }

    @Override
    public UIItemList addListener(Listener listener) {
      this.impl.addListener(listener);
      return this;
    }

    @Override
    public UIItemList removeListener(Listener listener) {
      this.impl.removeListener(listener);
      return this;
    }

    @Override
    public void drawFocus(UI ui, VGraphics vg) {
      this.impl.drawFocus(ui, vg);
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      this.impl.onDraw(ui, vg);
    }

    @Override
    public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
      this.impl.onMouseDragged(mouseEvent, mx, my, dx, dy);
    }

    @Override
    public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
      this.impl.onMousePressed(mouseEvent, mx, my);
    }

    @Override
    public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
      this.impl.onMouseReleased(mouseEvent, mx, my);
    }

    @Override
    public void onBlur() {
      this.impl.onBlur();
    }

    @Override
    public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
      boolean consume = this.impl.onKeyPressed(keyEvent, keyChar, keyCode);
      if (consume) {
        keyEvent.consume();
      }
    }

    @Override
    public void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
      boolean consume = this.impl.onKeyReleased(keyEvent, keyChar, keyCode);
      if (consume) {
        keyEvent.consume();
      }
    }

    @Override
    public void onFocus(Event event) {
      this.impl.onFocus(event);
    }

    @Override
    protected void onMouseScroll(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
      if (getScrollHeight() > getHeight()) {
        mouseEvent.consume();
        setScrollY(this.scrollY + dy);
      }
    }

  }

  public static class BasicList extends UI2dContainer implements UIItemList, UIFocus {

    private final Impl impl;

    public BasicList(UI ui, float x, float y, float w, float h) {
      super(x, y, w, h);
      setContentHeight(Impl.ROW_MARGIN);
      this.impl = new Impl(ui, this);
    }

    @Override
    public UIItemList setFocusIndex(int focusIndex) {
      this.impl.setFocusIndex(focusIndex, true);
      return this;
    }

    @Override
    public int getFocusedIndex() {
      return this.impl.getFocusedIndex();
    }

    @Override
    public UIItemList.Item getFocusedItem() {
      return this.impl.getFocusedItem();
    }

    @Override
    public UIItemList addSection(Section section) {
      this.impl.addSection(section);
      return this;
    }

    @Override
    public UIItemList addItem(int index, Item item) {
      this.impl.addItem(index, item);
      return this;
    }

    @Override
    public UIItemList addItem(Item item) {
      this.impl.addItem(item);
      return this;
    }

    @Override
    public UIItemList moveItem(Item item, int index) {
      this.impl.moveItem(item, index);
      return this;
    }

    @Override
    public UIItemList removeItem(Item item) {
      this.impl.removeItem(item);
      return this;
    }

    @Override
    public UIItemList setItems(List<? extends Item> items) {
      this.impl.setItems(items);
      return this;
    }

    @Override
    public UIItemList clearItems() {
      this.impl.clearItems();
      return this;
    }

    @Override
    public List<? extends Item> getItems() {
      return this.impl.items;
    }

    @Override
    public UIItemList setSingleClickActivate(boolean singleClickActivate) {
      this.impl.setSingleClickActivate(singleClickActivate);
      return this;
    }

    @Override
    public UIItemList setShowCheckboxes(boolean showCheckboxes) {
      this.impl.setShowCheckboxes(showCheckboxes);
      return this;
    }

    @Override
    public UIItemList setRenamable(boolean isRenamable) {
      this.impl.setRenamable(isRenamable);
      return this;
    }

    @Override
    public boolean isRenaming() {
      return this.impl.isRenaming();
    }

    @Override
    public String getRenameBuffer() {
      return this.impl.getRenameBuffer();
    }

    @Override
    public void renameAppend(String append) {
      this.impl.renameAppend(append);
    }

    @Override
    public UIItemList setMomentary(boolean momentary) {
      this.impl.setMomentary(momentary);
      return this;
    }

    @Override
    public UIItemList setReorderable(boolean reorderable) {
      this.impl.setReorderable(reorderable);
      return this;
    }

    @Override
    public UIItemList setDeletable(boolean deletable) {
      this.impl.setDeletable(deletable);
      return this;
    }

    @Override
    public UIItemList setFilter(String filter) {
      this.impl.setFilter(filter);
      return this;
    }

    @Override
    public UIItemList setControlSurfaceFocus(int index, int length, UIColor color) {
      this.impl.setControlSurfaceFocus(index, length, color);
      return this;
    }

    @Override
    public UIItemList addListener(Listener listener) {
      this.impl.addListener(listener);
      return this;
    }

    @Override
    public UIItemList removeListener(Listener listener) {
      this.impl.removeListener(listener);
      return this;
    }

    @Override
    public void drawFocus(UI ui, VGraphics vg) {
      this.impl.drawFocus(ui, vg);
    }

    @Override
    public void onDraw(UI ui, VGraphics vg) {
      this.impl.onDraw(ui, vg);
    }

    @Override
    public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
      this.impl.onMouseDragged(mouseEvent, mx, my, dx, dy);
    }

    @Override
    public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
      this.impl.onMousePressed(mouseEvent, mx, my);
    }

    @Override
    public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
      this.impl.onMouseReleased(mouseEvent, mx, my);
    }

    @Override
    public void onBlur() {
      this.impl.onBlur();
    }

    @Override
    public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
      boolean consume = this.impl.onKeyPressed(keyEvent, keyChar, keyCode);
      if (consume) {
        keyEvent.consume();
      }
    }

    @Override
    public void onKeyReleased(KeyEvent keyEvent, char keyChar, int keyCode) {
      boolean consume = this.impl.onKeyReleased(keyEvent, keyChar, keyCode);
      if (consume) {
        keyEvent.consume();
      }
    }

    @Override
    public void onFocus(Event event) {
      this.impl.onFocus(event);
    }

  }

  /**
   * Sets the index of the focused item in the list. Checks the bounds
   * and adjusts the scroll position if necessary.
   *
   * @param focusIndex Index of item to focus
   * @return this
   */
  public UIItemList setFocusIndex(int focusIndex);

  /**
   * Returns the index of the currently focused item in the list
   *
   * @return Index of focused item
   */
  public int getFocusedIndex();

  /**
   * Retrieves the currently focused item in the list.
   *
   * @return Focused item, or null if none is focused
   */
  public UIItemList.Item getFocusedItem();

  /**
   * Adds an item to the list
   *
   * @param item Item to add
   * @return this
   */
  public UIItemList addItem(Item item);

  /**
   * Adds an item to the list at the given index
   *
   * @param index Index to add the item at
   * @param item Item to add
   * @return this
   */
  public UIItemList addItem(int index, Item item);

  /**
   * Removes an item from the list
   *
   * @param item Item to remove
   * @return this
   */
  public UIItemList removeItem(Item item);

  /**
   * Moves an item to another location in the list
   *
   * @param item Item to move
   * @param index Index to move to
   * @return this
   */
  public UIItemList moveItem(Item item, int index);

  /**
   * Sets the items in the list and redraws it
   *
   * @param items Items
   * @return this
   */
  public UIItemList setItems(List<? extends Item> items);

  /**
   * Clears all items in the list
   *
   * @return this
   */
  public UIItemList clearItems();

  /**
   * Get the items in the list
   *
   * @return list of items
   */
  public List<? extends Item> getItems();

  /**
   * Add a section to the list
   *
   * @param section Section
   * @return this
   */
  public UIItemList addSection(Section section);

  /**
   * Sets whether single-clicks on an item should activate them. Default behavior
   * requires double-click or ENTER keypress
   *
   * @param singleClickActivate Whether to activate on a single click
   * @return this
   */
  public UIItemList setSingleClickActivate(boolean singleClickActivate);

  /**
   * Sets whether a column of checkboxes should be shown on the item list, to the
   * left of the labels. Useful for a secondary selection state.
   *
   * @param showCheckboxes Whether to show checkboxes
   * @return this
   */
  public UIItemList setShowCheckboxes(boolean showCheckboxes);

  /**
   * Sets whether renaming items is allowed
   *
   * @param isRenamable If items may be renamed
   * @return this
   */
  public UIItemList setRenamable(boolean isRenamable);

  /**
   * Whether this control is in the midst of an item rename operation
   *
   * @return True if an item is being renamed
   */
  public boolean isRenaming();

  /**
   * Gets the current value in the rename buffer for copy/paste
   *
   * @return Rename buffer segment
   */
  public String getRenameBuffer();

  /**
   * Appends a string to the active rename buffer
   *
   * @param append Value to append to the rename buffer
   */
  public void renameAppend(String append);

  /**
   * Sets whether the item list is momentary. If so, then clicking on an item
   * or pressing ENTER/SPACE sends a deactivate action after the click ends.
   *
   * @param momentary Whether items are momentary
   * @return this
   */
  public UIItemList setMomentary(boolean momentary);

  /**
   * Sets whether the list is reorderable. If so, then pressing the modifier key
   * with the up or down arrows will reorder the items.
   *
   * @param reorderable Whether items are reorderable
   * @return this
   */
  public UIItemList setReorderable(boolean reorderable);

  /**
   * Sets whether items in the list are deletable. If so, then pressing the
   * delete key will delete the focused item.
   *
   * @param deletable Whether items are deletable
   * @return this
   */
  public UIItemList setDeletable(boolean deletable);

  /**
   * Filter the items in the list by a String, resulting list will only
   * show items that contains the filter string
   *
   * @param filter Filter string
   * @return this
   */
  public UIItemList setFilter(String filter);

  /**
   * Sets a control focus range that is highlighted in the list
   *
   * @param index Start of the surface focus
   * @param length Length of the surface focus block
   * @param color Color to show focus with
   * @return this
   */
  public UIItemList setControlSurfaceFocus(int index, int length, UIColor color);

  /**
   * Adds a listener to receive notifications about list operations
   *
   * @param listener Listener
   * @return this
   */
  public UIItemList addListener(Listener listener);

  /**
   * Removes a listener from receiving notifications about list operations
   *
   * @param listener Listener
   * @return this
   */
  public UIItemList removeListener(Listener listener);

}
