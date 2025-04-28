/**
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
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

import heronarts.glx.ui.UI;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.midi.MidiSelector;

public class UIMidiSelector extends UIDropMenu {

  private final MidiSelector<?> selector;

  public UIMidiSelector(float x, float y, float w, MidiSelector<?> selector) {
    super(x, y, w, selector.terminal);
    this.selector = selector;
    addListener(selector, this.redraw);
  }

  public UIMidiSelector(float w, MidiSelector<?> selector) {
    this(0, 0, w, selector);
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    if (this.selector.missingDevice) {
      drawDisabledBackground(ui, vg);

      // If we're holding the setting for a MIDI device that isn't present,
      // we should draw it grayed out here.
      vg.fontFace(hasFont() ? getFont() : ui.theme.getControlFont());
      vg.fillColor(ui.theme.controlDisabledTextColor);
      vg.beginPath();
      vg.textAlign(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);

      String name = this.selector.name.getString();
      if (name == null) {
        name = "<None>";
      }
      vg.text(4 + this.textOffsetX, this.height / 2 + 1 + this.textOffsetY, clipTextToWidth(vg, name, this.width - 12));

      drawTriangle(ui, this, vg, this.textOffsetY);

    } else {
      super.onDraw(ui, vg);
    }
  }
}
