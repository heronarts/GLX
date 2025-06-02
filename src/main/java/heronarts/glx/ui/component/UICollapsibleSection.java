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

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIMouseFocus;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;

/**
 * Section with a title which can collapse/expand
 */
public class UICollapsibleSection extends UI2dContainer implements UIMouseFocus {

  protected static final int PADDING = 4;
  protected static final int TITLE_X = 18;
  private static final int TITLE_LABEL_HEIGHT = 12;
  private static final int CHEVRON_PADDING = 20;
  public static final int BAR_HEIGHT = 20;
  private static final int CLOSED_HEIGHT = TITLE_LABEL_HEIGHT + 2*PADDING;
  private static final int CONTENT_Y = CLOSED_HEIGHT;

  private final UILabel title;
  private boolean expanded = true;
  private float expandedHeight;
  private BooleanParameter expandedParameter = null;
  private UI2dComponent footer = null;
  private boolean footerVisible = true;
  private boolean alwaysOpen = false;

  private final LXParameterListener expandedListener = p -> {
    _setExpanded(((BooleanParameter) p).isOn(), false);
  };

  private final UI2dContainer content;

  public UICollapsibleSection(UI ui, float w, float h) {
    this(ui, 0, 0, w, h);
  }

  public UICollapsibleSection(UI ui, float w, float h, BooleanParameter expandedParameter) {
    this(ui, w, h);
    setExpandedParameter(expandedParameter);
  }

  /**
   * Constructs a new collapsible section
   *
   * @param ui UI
   * @param x Xpos
   * @param y Ypos
   * @param w Width
   * @param h Height
   */
  public UICollapsibleSection(UI ui, float x, float y, float w, float h) {
    super(x, y, w, h);
    setBackgroundColor(ui.theme.deviceBackgroundColor);
    setFocusBackgroundColor(ui.theme.deviceFocusedBackgroundColor);
    setBorderRounding(4);

    this.title = new UILabel(TITLE_X, PADDING, this.width - TITLE_X - CHEVRON_PADDING, TITLE_LABEL_HEIGHT);
    this.title.setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
    addTopLevelComponent(this.title);

    this.expandedHeight = (int) Math.max(CLOSED_HEIGHT, h);
    updateHeight();
    this.content = new UI2dContainer(PADDING, CONTENT_Y, this.width - 2*PADDING, Math.max(0, this.expandedHeight - PADDING - CONTENT_Y)) {
      @Override
      public void onResize() {
        expandedHeight = (this.height <= 0 ? CLOSED_HEIGHT : CONTENT_Y + this.height + PADDING);
        if (expanded) {
          updateHeight();
        }
      }
    };
    setContentTarget(this.content);
  }

  public UICollapsibleSection setAlwaysOpen() {
    if (this.expandedParameter != null) {
      throw new IllegalStateException("Cannot setAlwaysOpen() on UICollapsibleSection with expansion parameter");
    }
    setExpanded(true);
    this.alwaysOpen = true;
    return this;
  }

  /**
   * Whether the section is presently expanded
   *
   * @return Whether section is expanded
   */
  public boolean isExpanded() {
    return this.expanded;
  }

  /**
   * Set the section to follow and update a parameter for its expansion state
   *
   * @param expandedParameter Parameter to follow and update for expansion changes
   * @return this
   */
  public UICollapsibleSection setExpandedParameter(BooleanParameter expandedParameter) {
    if (this.expandedParameter != null) {
      this.expandedParameter.removeListener(this.expandedListener);
    }
    this.expandedParameter = null;
    if (expandedParameter != null) {
      this.expandedParameter = expandedParameter;
      this.expandedParameter.addListener(this.expandedListener, true);
    }
    return this;
  }

  protected UICollapsibleSection setTitleX(float x) {
    this.title.setX(x);
    this.title.setWidth(this.width - CHEVRON_PADDING - x);
    return this;
  }

  /**
   * Sets the title of the section
   *
   * @param title Title
   * @return this
   */
  public UICollapsibleSection setTitle(String title) {
    this.title.setLabel(title);
    return this;
  }

  public static void drawHorizontalExpansionTriangle(UI ui, VGraphics vg, boolean expanded) {
    vg.fillColor(ui.theme.sectionExpanderBackgroundColor);
    vg.beginPath();
    float x = 5;
    float y = BAR_HEIGHT - 5;
    if (expanded) {
      vg.moveTo(x, y);
      vg.lineTo(x+10, y);
      vg.lineTo(x+10, y-10);
    } else {
      vg.moveTo(x, y);
      vg.lineTo(x, y-10);
      vg.lineTo(x+10, y-10);
    }
    vg.closePath();
    vg.fill();
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    super.onDraw(ui, vg);
    UICollapsibleSection.drawHorizontalExpansionTriangle(ui, vg, this.expanded);
  }

  /**
   * Toggles the expansion state of the section
   *
   * @return this
   */
  public UICollapsibleSection toggle() {
    return setExpanded(!this.expanded);
  }

  /**
   * Sets the expanded state of this section
   *
   * @param expanded Whether section is expanded
   * @return this
   */
  public UICollapsibleSection setExpanded(boolean expanded) {
    return _setExpanded(expanded, true);
  }

  private UICollapsibleSection _setExpanded(boolean expanded, boolean pushToParam) {
    if (this.alwaysOpen) {
      LX.warning("Should not call setExpanded() on UICollapsibleSection that has setAlwaysOpen()");
      return this;
    }
    if (this.expanded != expanded) {
      this.expanded = expanded;
      this.content.setVisible(this.expanded);
      if (this.footer != null) {
        this.footer.setVisible(this.expanded && this.footerVisible);
      }
      updateHeight();
      redraw();

      // Push change to parameter
      if (pushToParam && (this.expandedParameter != null)) {
        this.expandedParameter.setValue(expanded);
      }
    }
    return this;
  }

  protected void updateHeight() {
    float contentHeight = this.expanded ? this.expandedHeight : CLOSED_HEIGHT;
    if (this.footer != null) {
      this.footer.setY(contentHeight);
      float footerHeight = this.footer.isVisible() ? this.footer.getHeight() : 0;
      if (footerHeight > 0) {
        footerHeight += PADDING;
      }
      contentHeight += footerHeight;
    }
    setHeight(contentHeight);
  }

  protected void setFooter(UI2dComponent footer) {
    if (this.footer != null) {
      throw new IllegalArgumentException("Cannot set footer twice on UICollapsibleSection: " + footer);
    }
    if (footer == null) {
      throw new IllegalArgumentException("Cannot set null footer on UICollapsibleSection");
    }
    this.footer = footer;
    this.footer.setVisible(this.expanded && this.footerVisible);
    addTopLevelComponent(footer);
  }

  protected void setFooterVisible(boolean footerVisible) {
    this.footerVisible = footerVisible;
    if (this.footer != null) {
      this.footer.setVisible(this.expanded && this.footerVisible);
      updateHeight();
    }
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (this.alwaysOpen) {
      return;
    }
    if (my < CONTENT_Y) {
      if ((mx < this.title.getX()) || mouseEvent.isDoubleClick()) {
        mouseEvent.consume();
        toggle();
      }
    }
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    super.onKeyPressed(keyEvent, keyChar, keyCode);
    if (this.alwaysOpen) {
      return;
    }
    if (keyCode == KeyEvent.VK_SPACE) {
      keyEvent.consume();
      toggle();
    }
  }

  @Override
  public UI2dContainer getContentTarget() {
    return this.content;
  }

  public static final int CONTROL_WIDTH = 60;
  public static final int CONTROL_HEIGHT = 16;

  protected UI2dContainer controlRow(UI ui, String label, LXParameter parameter) {
    return controlRow(ui, label, switch(parameter) {
      case BooleanParameter bool -> controlButton(bool);
      case DiscreteParameter discrete -> controlIntegerBox(discrete);
      case BoundedParameter bounded -> controlDoubleBox(bounded);
      default -> throw new IllegalArgumentException("Cannot make control row for parameter: " + parameter);
    });
  }

  protected UIButton controlButton(BooleanParameter bool) {
    return new UIButton(CONTROL_WIDTH, CONTROL_HEIGHT, bool);
  }

  protected UIIntegerBox controlIntegerBox(DiscreteParameter discrete) {
    return new UIIntegerBox(CONTROL_WIDTH, CONTROL_HEIGHT, discrete);
  }

  protected UIDoubleBox controlDoubleBox(BoundedParameter bounded) {
    return new UIDoubleBox(CONTROL_WIDTH, CONTROL_HEIGHT, bounded);
  }

  protected UI2dContainer controlRow(UI ui, String label, UI2dComponent control) {
    return UI2dContainer.newHorizontalContainer(CONTROL_HEIGHT, 0,
      controlLabel(ui, label),
      control.setWidth(CONTROL_WIDTH)
    );
  }

  protected UILabel controlLabel(UI ui, String label) {
    return new UILabel.Control(ui, getContentWidth() - CONTROL_WIDTH, CONTROL_HEIGHT, label);
  }

  @Override
  public void dispose() {
    setExpandedParameter(null);
    super.dispose();
  }

  public interface Utils {

    public default UI2dContainer controlRow(UI ui, float contentWidth, String label, UI2dComponent control) {
      return UI2dContainer.newHorizontalContainer(CONTROL_HEIGHT, 0,
        new UILabel.Control(ui, contentWidth - CONTROL_WIDTH, CONTROL_HEIGHT, label),
        control.setWidth(CONTROL_WIDTH)
      );
    }

    public default UILabel geometryLabel(UI ui, String label) {
      return (UILabel) new UILabel.Control(ui, 10, CONTROL_HEIGHT, label).setTextAlignment(VGraphics.Align.CENTER);
    }

    public default UIDoubleBox geometryBox(BoundedParameter p) {
      return (UIDoubleBox) new UIDoubleBox(42, CONTROL_HEIGHT, p)
        .setNormalizedMouseEditing(false)
        .setShiftMultiplier(10f);
    }

    public default UI2dComponent geometryContainer(UI ui, float contentWidth, UI2dComponent ... components) {
      return UI2dContainer.newVerticalContainer(contentWidth, 2, components)
        .setPadding(4)
        .setBackgroundColor(ui.theme.listBackgroundColor)
        .setBorderColor(ui.theme.listBorderColor)
        .setBorderRounding(4);
    }
  }
}
