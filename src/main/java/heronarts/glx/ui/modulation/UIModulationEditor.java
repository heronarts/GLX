/**
 * Copyright 2025- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui.modulation;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dOverlay;
import heronarts.lx.modulation.LXCompoundModulation;

public class UIModulationEditor extends UI2dOverlay {

  public static final int MODULATION_SPACING = 2;
  public static final int PADDING = 4;
  public static final int WIDTH = 220;

  private final UI ui;

  public UIModulationEditor(UI ui, LXCompoundModulation.Target target) {
    super(ui, WIDTH, 0);
    this.ui = ui;
    setArrowKeyFocus(UI2dContainer.ArrowKeyFocus.VERTICAL);
    setLayout(UI2dContainer.Layout.VERTICAL);
    setPadding(PADDING);
    setChildSpacing(MODULATION_SPACING);

    target.getModulations().forEach(modulation -> {
      new UIModulation.Compound(this.ui, modulation, PADDING, 0, getContentWidth() - 2*PADDING) {
        @Override
        public void remove() {
          // Get rid of this UI element, dispose of listeners to modulation parameters that
          // are about to be destroyed
          removeFromContainer().dispose();

          // Actually remove the modulation
          super.remove();

          // Fix the overlay dimensions or be rid of it
          if (UIModulationEditor.this.children.isEmpty()) {
            this.ui.clearContextOverlay(UIModulationEditor.this);
          } else {
            this.ui.resizeContextOverlay(UIModulationEditor.this);
          }
        }
      }
      .addToContainer(this);
    });

    // Catch when the overlay is hidden, nuke all the contents
    addListener(this.visible, p -> {
      if (!isVisible()) {
        removeAllChildren();
      }
    });
  }

}
