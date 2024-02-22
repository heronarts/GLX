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

import java.util.Arrays;
import java.util.List;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UIContextActions;
import heronarts.glx.ui.UICopy;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.clipboard.LXClipboardItem;
import heronarts.lx.clipboard.LXTextValue;

/**
 * A simple text label object. Draws a string aligned top-left to its x-y
 * position.
 */
public class UILabel extends UI2dComponent {

  public static final float DEFAULT_HEIGHT = 12;

  private int topPadding = 0;
  private int rightPadding = 0;
  private int leftPadding = 0;
  private int bottomPadding = 0;
  private boolean breakLines = false;
  private boolean autoHeight = false;

  public static class Copyable extends UILabel implements UIFocus, UICopy, UIContextActions {
    public Copyable(float w, float h, String label) {
      this(0, 0, w, h, label);
    }

    public Copyable(float x, float y, float w, float h, String label) {
      super(x, y, w, h);
      setLabel(label);
    }

    @Override
    public LXClipboardItem onCopy() {
      return new LXTextValue(getLabel());
    }

    private final List<Action> actions = Arrays.asList(new Action[] {
      new Action("Copy Value") {
        @Override
        public void onContextAction(UI ui) {
          ui.lx.clipboard.setItem(onCopy());
        }
      }
    });

    @Override
    public List<Action> getContextActions() {
      return this.actions;
    }
  }

  /**
   * Label text
   */
  private String label = "";

  public static class Control extends UILabel {

    public Control(UI ui, float w, float h, String label) {
      this(ui, 0, 0, w, h, label);
    }

    public Control(UI ui, float x, float y, float w, float h, String label) {
      super(x, y, w, h);
      setLabel(label);
      setFont(ui.theme.getControlFont());
      setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
    }
  }

  public UILabel() {
    this(0, 0, 0, 0);
  }

  public UILabel(float w, String label) {
    this(w, DEFAULT_HEIGHT, label);
  }

  public UILabel(float w, float h, String label) {
    this(0, 0, w, h, label);
  }

  public UILabel(float x, float y, float w, String label) {
    this(x, y, w, DEFAULT_HEIGHT, label);
  }

  public UILabel(float x, float y, float w, float h) {
    super(x, y, w, h);
  }

  public UILabel(float x, float y, float w, float h, String label) {
    this(x, y, w, h);
    setLabel(label);
  }

  /**
   * Sets the label to render text multi-line
   *
   * @param breakLines Whether to break lines
   * @return this
   */
  public UILabel setBreakLines(boolean breakLines) {
    return setBreakLines(breakLines, false);
  }

  /**
   * Sets the label to render text multi-line
   *
   * @param breakLines Whether to break lines
   * @param autoHeight Whether to automatically set height
   * @return this
   */
  public UILabel setBreakLines(boolean breakLines, boolean autoHeight) {
    if (this.breakLines != breakLines || this.autoHeight != autoHeight) {
      this.breakLines = breakLines;
      this.autoHeight = autoHeight;
      redraw();
    }
    return this;
  }

  /**
   * Sets padding on all 4 sides
   *
   * @param padding Padding
   * @return this
   */
  public UILabel setPadding(int padding) {
    return setPadding(padding, padding, padding, padding);
  }

  /**
   * Sets padding on top and sides, CSS style
   *
   * @param topBottom Top bottom padding
   * @param leftRight Left right padding
   * @return this
   */
  public UILabel setPadding(int topBottom, int leftRight) {
    return setPadding(topBottom, leftRight, topBottom, leftRight);
  }

  /**
   * Sets padding on all 4 sides
   *
   * @param topPadding Top padding
   * @param rightPadding Right padding
   * @param bottomPadding Bottom padding
   * @param leftPadding Left padding
   * @return this
   */
  public UILabel setPadding(int topPadding, int rightPadding, int bottomPadding, int leftPadding) {
    boolean redraw = false;
    if (this.topPadding != topPadding) {
      this.topPadding = topPadding;
      redraw = true;
    }
    if (this.rightPadding != rightPadding) {
      this.rightPadding = rightPadding;
      redraw = true;
    }
    if (this.bottomPadding != bottomPadding) {
      this.bottomPadding = bottomPadding;
      redraw = true;
    }
    if (this.leftPadding != leftPadding) {
      this.leftPadding = leftPadding;
      redraw = true;
    }
    if (redraw) {
      redraw();
    }
    return this;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    vg.fontFace(hasFont() ? getFont() : ui.theme.getLabelFont());
    vg.fillColor(hasFontColor() ? getFontColor() : ui.theme.labelColor);
    float tx = this.leftPadding, ty = this.topPadding;
    switch (this.textAlignHorizontal) {
    case CENTER:
      tx = this.width / 2;
      break;
    case RIGHT:
      tx = this.width - this.rightPadding;
      break;
    default:
      break;
    }
    switch (this.textAlignVertical) {
    case BASELINE:
      ty = this.height - this.bottomPadding;
      break;
    case BOTTOM:
      ty = this.height - this.bottomPadding;
      break;
    case MIDDLE:
      ty = this.height / 2;
      break;
    default:
      break;
    }
    if (this.breakLines) {
      vg.beginPath();
      vg.textAlign(this.textAlignHorizontal, this.textAlignVertical);
      vg.textBox(tx + this.textOffsetX, ty + this.textOffsetY, this.width - this.leftPadding - this.rightPadding, this.label);
      vg.fill();
      if (this.autoHeight) {
        setHeight(this.topPadding + this.bottomPadding + vg.textBoxHeight(this.label, this.width - this.leftPadding - this.rightPadding));
      }
    } else {
      String str = clipTextToWidth(vg, this.label, this.width - this.leftPadding - this.rightPadding);
      vg.beginPath();
      vg.textAlign(this.textAlignHorizontal, this.textAlignVertical);
      vg.text(tx + this.textOffsetX, ty + this.textOffsetY + 1, str);
      vg.fill();
    }
  }

  private static final int MAX_LABEL_LENGTH = 1024;

  public String getLabel() {
    return this.label;
  }

  public UILabel setLabel(String label) {
    if (this.label != label) {
      // Avoid crashing the UI with crazy long display strings, 1K is more than enough
      // spew and should be cleaned up into multiple components if there's really more
      // than that
      if (label.length() > MAX_LABEL_LENGTH) {
        label = label.substring(0, MAX_LABEL_LENGTH-3) + "...";
      }
      this.label = label;
      redraw();
    }
    return this;
  }

  @Override
  public String getDescription() {
    String description = super.getDescription();
    return (description != null) ? description : this.label;
  }
}
