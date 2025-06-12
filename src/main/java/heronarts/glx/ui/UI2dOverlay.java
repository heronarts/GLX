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

/**
 * Consistent look and feel for overlay UI boxes
 */
public class UI2dOverlay extends UI2dContainer {

  protected final UI ui;

  public UI2dOverlay(UI ui, float w, float h) {
    super(0, 0, w, h);
    setBackgroundColor(ui.theme.deviceFocusedBackgroundColor);
    setBorderColor(ui.theme.contextBorderColor);
    setBorderRounding(4);
    this.ui = ui;
  }
}
