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

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.clipboard.LXTextValue;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;

public abstract class UIInputBox extends UIParameterComponent implements UIFocus {

  public interface ProgressIndicator {
    public boolean hasProgress();
    public double getProgress();
  }

  private static final int TEXT_MARGIN = 2;

  protected boolean enabled = true;

  protected boolean editable = true;
  protected boolean mouseEditable = true;

  protected boolean returnKeyEdit = true;
  private boolean immediateEdit = true;
  private boolean immediateAppend = false;

  protected volatile boolean editing = false;

  public static class EditState {
    public String buffer = "";
    public int cursor = -1;
    public int rangeStart = -1;
    public int rangeEnd = -1;
    public double cursorBasis = 1;

    public void animateCursor(double deltaMs) {
      this.cursorBasis = (this.cursorBasis + deltaMs * .002f) % 1.;
    };

    public void init(String value) {
      this.buffer = value;
      setCursor(value.length());
    }

    public String cut() {
      if (this.rangeEnd != this.rangeStart) {

        // Cut the text, put it on the clipboard
        final String cut = this.buffer.substring(this.rangeStart, this.rangeEnd);

        // Range deletion, save the end point
        final int rangeEnd = this.rangeEnd;

        // Move the cursor to front of deletion range
        setCursor(this.rangeStart);
        this.buffer =
          this.buffer.substring(0, this.rangeStart) +
          this.buffer.substring(rangeEnd);

        return cut;
      }

      return null;
    }

    public void selectAll() {
      this.rangeStart = 0;
      this.cursor = this.rangeEnd = this.buffer.length();
    }

    public void deleteAll() {
      this.buffer = "";
      setCursor(0);
    }

    public void delete() {
      if (this.rangeEnd != this.rangeStart) {
        // Range deletion, save the end point
        final int rangeEnd = this.rangeEnd;
        // Move the cursor to front of deletion range
        setCursor(this.rangeStart);
        this.buffer =
          this.buffer.substring(0, this.rangeStart) +
          this.buffer.substring(rangeEnd);
      } else {
        // Single char deletion, move the cursor back one, delete from there
        setCursor(this.cursor - 1);
        this.buffer =
          this.buffer.substring(0, this.cursor) +
          this.buffer.substring(this.cursor + 1);
      }
    }

    public void setRange(int cursor) {
      cursor = LXUtils.constrain(cursor, 0, this.buffer.length());
      if (this.rangeStart == this.rangeEnd) {
        // No range selected, now we're starting a range
        if (cursor > this.rangeStart) {
          this.cursor = this.rangeEnd = cursor;
        } else {
          this.cursor = this.rangeStart = cursor;
        }
      } else if (this.cursor == this.rangeStart) {
        if (cursor < this.rangeEnd) {
          this.cursor = this.rangeStart = cursor;
        } else {
          this.cursor = this.rangeEnd = cursor;
        }
      } else {
        if (cursor > this.rangeStart) {
          this.cursor = this.rangeEnd = cursor;
        } else {
          this.cursor = this.rangeStart = cursor;
        }
      }
      if (this.rangeStart > this.rangeEnd) {
        throw new IllegalStateException("Cannot have editRangeStart(" + rangeStart + ") > editRangeEnd(" + rangeEnd + ")");
      }
    }

    public void setCursor(int cursor) {
      this.rangeStart = this.rangeEnd = this.cursor = LXUtils.constrain(cursor, 0, this.buffer.length());
    }

    public void append(char append) {
      this.buffer =
        this.buffer.substring(0, this.rangeStart) +
        append +
        this.buffer.substring(this.rangeEnd);
      setCursor(this.rangeStart + 1);
    }

    public void append(String append) {
      this.buffer =
        this.buffer.substring(0, this.rangeStart) +
        append +
        this.buffer.substring(this.rangeEnd);
      setCursor(this.rangeStart + append.length());
    }

    public String getRange() {
      if (this.rangeStart != this.rangeEnd) {
        return this.buffer.substring(this.rangeStart, this.rangeEnd);
      }
      return this.buffer;
    }

    public void cursorLeft(KeyEvent keyEvent) {
      int cursor = this.cursor - 1;
      if ((this.rangeStart != this.rangeEnd) && !keyEvent.isShiftDown()) {
        cursor = this.rangeStart;
      }
      if (keyEvent.isCommand()) {
        cursor = 0;
      } else if (keyEvent.isAltDown()) {
        while (cursor > 0 && Character.isLetterOrDigit(this.buffer.charAt(cursor - 1))) {
          --cursor;
        }
      }
      if (keyEvent.isShiftDown()) {
        setRange(cursor);
      } else {
        setCursor(cursor);
      }
    }

    public void cursorRight(KeyEvent keyEvent) {
      int cursor = this.cursor + 1;
      if ((this.rangeStart != this.rangeEnd) && !keyEvent.isShiftDown()) {
        cursor = this.rangeEnd;
      }
      if (keyEvent.isCommand()) {
        cursor = this.buffer.length();
      } else if (keyEvent.isAltDown()) {
        while (cursor < this.buffer.length() && Character.isLetterOrDigit(this.buffer.charAt(cursor))) {
          ++cursor;
        }
      }
      if (keyEvent.isShiftDown()) {
        setRange(cursor);
      } else {
        setCursor(cursor);
      }
    }

  }

  private final EditState editState = new EditState();

  protected boolean hasFill = false;
  protected int fillColor = 0;

  private ProgressIndicator progressMeter;
  private int progressPixels = 0;
  private boolean hasProgressColor = false;
  private UIColor progressColor = UIColor.NONE;

  protected FillStyle fillStyle = FillStyle.UNDERLINE;

  public enum FillStyle {
    UNDERLINE,
    FULL
  };

  protected UIInputBox() {
    this(0, 0, 0, 0);
  }

  protected UIInputBox(float x, float y, float w, float h) {
    super(x, y, w, h);
    setBorderColor(UI.get().theme.controlBorderColor);
    setBackgroundColor(UI.get().theme.controlBackgroundColor);
    setTextAlignment(VGraphics.Align.CENTER);

    // Animate cursor while editing
    addLoopTask(deltaMs -> {
      if (this.editing) {
        this.editState.animateCursor(deltaMs);
        redraw();
      }
    });
  }

  public UIInputBox setProgressColor(boolean hasProgressColor) {
    this.hasProgressColor = hasProgressColor;
    redraw();
    return this;
  }

  public UIInputBox setProgressColor(int progressColor) {
    return setProgressColor(new UIColor(progressColor));
  }

  public UIInputBox setProgressColor(UIColor progressColor) {
    if (!this.hasProgressColor || (this.progressColor != progressColor)) {
      this.hasProgressColor = true;
      this.progressColor = progressColor;
      redraw();
    }
    return this;
  }

  public UIInputBox setProgressIndicator(ProgressIndicator meter) {
    boolean addLoopTask = this.progressMeter == null;
    this.progressMeter = meter;
    if (addLoopTask) {
      addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
        @Override
        public void run() {
          if (progressMeter != null && progressMeter.hasProgress()) {
            int newProgress = (int) (progressMeter.getProgress() * (width-5));
            if (newProgress != progressPixels) {
              progressPixels = newProgress;
              redraw();
            }
          } else {
            if (progressPixels != 0) {
              progressPixels = 0;
              redraw();
            }
          }
        }
      });
    }
    return this;
  }

  public UIInputBox disableReturnKeyEdit() {
    this.returnKeyEdit = false;
    return this;
  }

  public UIInputBox enableReturnKeyEdit() {
    this.returnKeyEdit = true;
    return this;
  }

  public UIInputBox disableImmediateEdit() {
    this.immediateEdit = false;
    return this;
  }

  public UIInputBox enableImmediateEdit() {
    this.immediateEdit = true;
    return this;
  }

  public UIInputBox disableImmediateAppend() {
    this.immediateAppend = false;
    return this;
  }

  public UIInputBox enableImmediateAppend() {
    this.immediateAppend = true;
    return this;
  }

  protected abstract String getValueString();

  protected abstract void saveEditBuffer(String editBuffer);

  protected String getEditBuffer() {
    return this.editState.buffer;
  }

  protected String getEditRange() {
    return this.editState.getRange();
  }

  /**
   * Subclasses may override to handle editing changes
   *
   * @param editBuffer New value being actively edited
   */
  protected /* abstract */ void onEditChange(String editBuffer) {}

  /**
   * Subclasses may override to handle when an edit is finished
   */
  protected /* abstract */ void onEditFinished() {}

  public boolean isEditable() {
    return this.editable;
  }

  public UIInputBox setEditable(boolean editable) {
    if (this.editable != editable) {
      if (this.editing) {
        this.editing = false;
        redraw();
      }
      this.editable = editable;
    }
    return this;
  }

  public UIInputBox setMouseEditable(boolean mouseEditable) {
    this.mouseEditable = mouseEditable;
    return this;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public UIInputBox setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      if (this.editing && !this.enabled) {
        this.editing = false;
      }
      redraw();
    }
    return this;
  }

  public void edit() {
    edit(getInitialEditBufferValue());
  }

  public void edit(String editBufferValue) {
    if (!this.editable) {
      throw new IllegalStateException("May not edit a non-editable UIInputBox");
    }
    if (this.enabled && !this.editing) {
      this.editState.init(editBufferValue);
      this.editing = true;
    }
    redraw();
  }

  private void editAppend(char append) {
    this.editState.append(append);
    onEditChange(this.editState.buffer);
    redraw();
  }

  protected void editAppend(String append) {
    this.editState.append(append);
    onEditChange(this.editState.buffer);
    redraw();
  }

  protected String getInitialEditBufferValue() {
    return "";
  }

  @Override
  protected void onBlur() {
    super.onBlur();
    if (this.editing) {
      editFinished(true);
      redraw();
    }
  }

  private void editFinished(boolean save) {
    this.editing = false;
    if (save) {
      saveEditBuffer(this.editState.buffer);
    } else {
      onEditChange(getValueString());
    }
    onEditFinished();
  }

  protected double getFillWidthNormalized() {
    return 0;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    if (this.progressPixels > 0) {
      vg.beginPath();
      vg.line(2, this.height-2, 2 + this.progressPixels, this.height-2);
      vg.strokeColor(this.hasProgressColor ? this.progressColor : ui.theme.primaryColor);
      vg.stroke();
    }

    vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
    if (this.editing) {
      vg.beginPath();
      vg.fillColor(ui.theme.editTextBackgroundColor);
      vgRoundedRect(vg);
      vg.fill();
    } else {
      if (!this.enabled) {
        vg.beginPath();
        vg.fillColor(ui.theme.controlDisabledColor);
        vg.rect(1, 1, this.width-2, this.height-2);
        vg.fill();
      }
      if (this.hasFill) {
        if (this.fillStyle == FillStyle.UNDERLINE) {
          int fillWidth = (int) (getFillWidthNormalized() * (this.width-5));
          if (fillWidth > 0) {
            vg.beginPath();
            vg.strokeColor(this.fillColor);
            vg.line(2, this.height-2, 2 + fillWidth, this.height-2);
            vg.stroke();
          }
        } else if (this.fillStyle == FillStyle.FULL) {
          int fillWidth = (int) (getFillWidthNormalized() * (this.width-2));
          vg.beginPath();
          vg.fillColor(this.fillColor);
          vg.rect(1, 1, fillWidth, this.height - 2);
          vg.fill();
        }
      }
    }

    if (this.editing) {
      vg.fillColor(ui.theme.editTextColor);
    } else if (!this.enabled) {
      vg.fillColor(ui.theme.controlDisabledTextColor);
    } else {
      vg.fillColor(hasFontColor() ? getFontColor() : ui.theme.controlTextColor);
    }

    // Get the display string, clip it to width
    final float availableWidth = this.width - TEXT_MARGIN - 1;
    final String rawString = this.editing ? this.editState.buffer : getValueString();

    onDrawText(ui, vg, this.editState, rawString, this.editing, this.textAlignHorizontal, 0, 0, this.width, this.height, availableWidth);
  }

  /**
   * Horrendous helper method to render an editable text field
   *
   * @param ui UI context
   * @param vg VGraphics object
   * @param editState The editing state
   * @param rawString Raw string to render
   * @param cursor Whether to draw cursor
   * @param textAlignHorizontal
   * @param x X offset
   * @param y Y offset
   * @param width Available width from x
   * @param height Available height from y
   * @param availableWidth
   */
  public static void onDrawText(UI ui, VGraphics vg, EditState editState, String rawString, boolean cursor, VGraphics.Align textAlignHorizontal, float x, float y, float width, float height, float availableWidth) {
    final int rawLength = (rawString != null) ? rawString.length() : 0;
    String clippedString = (rawString != null) ? clipTextToWidth(vg, rawString, availableWidth) : null;

    // Clamp these values upfront, since they're updated by the LX engine thread
    // and we are here on the UI thread, they could be out of bounds.
    int editCursor = LXUtils.constrain(editState.cursor, 0, rawLength);
    int editRangeStart = LXUtils.constrain(editState.rangeStart, 0, rawLength);
    int editRangeEnd = LXUtils.constrain(editState.rangeEnd, 0, rawLength);

    if (cursor) {
      // The string is too big and we can't see where we're editing...
      final int editMax = LXUtils.max(editCursor, editRangeEnd);
      if (editMax > clippedString.length()) {
        // Okay, we're editing, but either the cursor or the end of the selection
        // is out of range. We're going to have to clip some stuff off the front
        // and shift the whole string left.
        int numClippedFromFront = 0;

        // First, chop the string down to the max edit length. We know this
        // string is too long, but we're gonna have to adjust it in one of two ways.
        final String startToRange = rawString.substring(0, editMax);
        if (editCursor >= editRangeEnd) {
          // The cursor is ahead of the edit range, then let's just clip the
          // string from the front and have the cursor at the very end
          clippedString = clipTextToWidth(vg, startToRange, availableWidth, false);
          numClippedFromFront = startToRange.length() - clippedString.length();
        } else {
          // The selected range is ahead of the cursor. We'd like to show *all* of the
          // selected range if we can, but if it doesn't fit, we'll stick with the
          // cursor at the very left.
          final String cursorRange = rawString.substring(editCursor, editRangeEnd);
          if (vg.textWidth(cursorRange) > availableWidth) {
            // Can't see it all, we clip from back-to-front starting from the
            // edit cursor, showing as much as possible
            clippedString = clipTextToWidth(vg, cursorRange, availableWidth);
            numClippedFromFront = editCursor;
          } else {
            // The whole range does fit! Get the end of the selection range on the
            // right and let the cursor land somewhere in the middle.
            clippedString = clipTextToWidth(vg, startToRange, availableWidth, false);
            numClippedFromFront = startToRange.length() - clippedString.length();
          }
        }

        // Move the goalposts based upon how much we clipped off the front...
        editCursor -= numClippedFromFront;
        editRangeStart -= numClippedFromFront;
        editRangeEnd -= numClippedFromFront;

      }
    }

    // Draw the display string
    if (clippedString != null) {
      if (textAlignHorizontal == VGraphics.Align.LEFT) {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
        vg.text(x + TEXT_MARGIN, y + height / 2 + 1, clippedString);
        vg.fill();
      } else {
        vg.beginPath();
        vg.textAlign(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE);
        vg.text(x + width / 2, y + height / 2 + 1, clippedString);
        vg.fill();
      }
    }

    if (cursor) {
      // Clamp the cursor values to available bounds, it's possible that we have a redraw
      // after editBuffer has been updated but the cursor is not yet updated
      final int length = clippedString.length();
      editCursor = LXUtils.constrain(editCursor, 0, length);
      editRangeStart = LXUtils.constrain(editRangeStart, 0, length);
      editRangeEnd = LXUtils.constrain(editRangeEnd, 0, length);

      float fullWidth = 0;
      if (textAlignHorizontal != VGraphics.Align.LEFT) {
        fullWidth = vg.textWidth(clippedString);
      }

      if (editRangeStart != editRangeEnd) {
        final float rangeStartWidth = (editRangeStart == 0) ? 0 : vg.textWidth(clippedString.substring(0, editRangeStart));
        final float rangeEndWidth = (editRangeEnd == 0) ? 0 : vg.textWidth(clippedString.substring(0, editRangeEnd));
        float rangeStartX = 0, rangeEndX = 0;
        if (textAlignHorizontal == VGraphics.Align.LEFT) {
          rangeStartX = TEXT_MARGIN + rangeStartWidth;
          rangeEndX = TEXT_MARGIN + rangeEndWidth;
        } else {
          rangeStartX = (width - fullWidth) / 2f + rangeStartWidth;
          rangeEndX = (width - fullWidth) / 2f + rangeEndWidth;
        }

        vg.beginPath();
        vg.fillColor(ui.theme.editTextColor.mask(0x55));
        vg.rect(x + rangeStartX, y + 1, rangeEndX - rangeStartX, height - 2);
        vg.fill();
      }

      float cursorWidth = 0, cursorX = 0;
      if ((clippedString != null) && (editCursor > 0)) {
        cursorWidth = vg.textWidth(clippedString.substring(0, editCursor));
      }
      if (textAlignHorizontal == VGraphics.Align.LEFT) {
        cursorX = TEXT_MARGIN + cursorWidth;
      } else {
        cursorX = (width - fullWidth) / 2f + cursorWidth;
      }

      vg.beginPath();
      vg.strokeColor(ui.theme.editTextColor.mask((int) LXUtils.lerp(255, 100, editState.cursorBasis)));
      vg.line(x + cursorX, y + 1, x + cursorX, y + height-1);
      vg.stroke();
    }
  }

  protected abstract boolean isValidCharacter(char keyChar);

  /**
   * Subclasses may optionally override to decrement value in response to arrows.
   * Decrement is invoked for the left or down arrow keys.
   *
   * @param keyEvent Key event
   */
  protected void decrementValue(KeyEvent keyEvent) {}

  /**
   * Subclasses may optionally override to decrement value in response to arrows.
   * Increment is invoked for the right or up keys.
   *
   * @param keyEvent Key event
   */
  protected void incrementValue(KeyEvent keyEvent) {}

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.editable) {
      if (this.editing) {
        // Editing!
        if (isValidCharacter(keyChar)) {
          if (!keyEvent.isCommand()) {
            keyEvent.consume();
            editAppend(keyChar);
          }
        } else if (keyEvent.isEnter()) {
          keyEvent.consume();
          editFinished(true);
          redraw();
        } else if (keyEvent.isCommand(KeyEvent.VK_X)) {
          final String cut = this.editState.cut();
          if (cut != null) {
            keyEvent.consume();
            getLX().clipboard.setItem(new LXTextValue(cut));
            onEditChange(this.editState.buffer);
            redraw();
          }
        } else if (keyEvent.isCommand(KeyEvent.VK_A)) {
          keyEvent.consume();
          this.editState.selectAll();
          redraw();
        } else if (keyEvent.isDelete()) {
          keyEvent.consume();
          if (this.editState.buffer.length() > 0) {
            if (keyEvent.isShiftDown() || keyEvent.isControlDown() || keyEvent.isMetaDown()) {
              this.editState.deleteAll();
              onEditChange(this.editState.buffer);
            } else {
              this.editState.delete();
              onEditChange(this.editState.buffer);
            }
            redraw();
          }
        } else if (keyCode == KeyEvent.VK_LEFT) {
          keyEvent.consume();
          this.editState.cursorLeft(keyEvent);
          redraw();
        } else if (keyCode == KeyEvent.VK_RIGHT) {
          keyEvent.consume();
          this.editState.cursorRight(keyEvent);
          redraw();
        } else if (keyCode == KeyEvent.VK_UP) {
          if (keyEvent.isShiftDown()) {
            keyEvent.consume();
            this.editState.setRange(0);
          } else if (keyEvent.isCommand()) {
            keyEvent.consume();
            this.editState.setCursor(0);
          }
        } else if (keyCode == KeyEvent.VK_DOWN) {
          if (keyEvent.isShiftDown()) {
            keyEvent.consume();
            this.editState.setRange(this.editState.buffer.length());
          } else if (keyEvent.isCommand()) {
            keyEvent.consume();
            this.editState.setCursor(this.editState.buffer.length());
          }
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
          keyEvent.consume();
          editFinished(false);
          redraw();
        }
      } else if (this.enabled) {
        // Not editing
        if (this.immediateEdit && isValidCharacter(keyChar) && !keyEvent.isCommand()) {
          keyEvent.consume();
          if (this.immediateAppend) {
            edit(getInitialEditBufferValue() + keyChar);
          } else {
            edit(Character.toString(keyChar));
          }
          onEditChange(this.editState.buffer);
        } else if (this.immediateEdit && keyEvent.isDelete()) {
          String editBuffer = getInitialEditBufferValue();
          if (!editBuffer.isEmpty()) {
            keyEvent.consume();
            if (keyEvent.isShiftDown() || keyEvent.isCommand()) {
              edit("");
            } else {
              edit(editBuffer.substring(0, editBuffer.length() - 1));
            }
            onEditChange(this.editState.buffer);
          }
        } else if (keyEvent.isEnter()) {
          if (this.returnKeyEdit) {
            keyEvent.consume();
            edit();
          }
        } else if ((keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_DOWN)) {
          decrementValue(keyEvent);
        } else if ((keyCode == KeyEvent.VK_RIGHT) || (keyCode == KeyEvent.VK_UP)) {
          incrementValue(keyEvent);
        }
      }
    }
  }

  /**
   * Subclasses may optionally implement to change value based upon mouse click+drag in the box.
   *
   * @param mouseEvent Mouse event
   * @param offset Units of mouse movement, positive or negative
   */
  protected void incrementMouseValue(MouseEvent mouseEvent, int offset) {}

  private float dAccum = 0;

  private LXCommand.Parameter.SetValue mouseDragSetValue = null;

  protected void setValueCommand(double value) {
    if (this.mouseDragSetValue != null) {
      getLX().command.perform(this.mouseDragSetValue.update(value));
    } else if (getParameter() != null){
      if (this.useCommandEngine) {
        getLX().command.perform(new LXCommand.Parameter.SetValue(getParameter(), value));
      } else {
        getParameter().setValue(value);
      }
    }
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    this.dAccum = 0;
    LXParameter parameter = getParameter();
    if (parameter != null) {
      if (this.useCommandEngine) {
        this.mouseDragSetValue = new LXCommand.Parameter.SetValue(parameter, 0);
      }
    }
  }

  @Override
  protected void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
    super.onMouseReleased(mouseEvent, mx, my);
    this.mouseDragSetValue = null;
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    if (this.enabled && this.editable && this.mouseEditable) {
      mouseEvent.consume();
      this.dAccum -= dy;
      int offset = (int) (this.dAccum / 5);
      this.dAccum = this.dAccum - (offset * 5);
      if (!this.editing) {
        incrementMouseValue(mouseEvent, offset);
      }
    }
  }

}
