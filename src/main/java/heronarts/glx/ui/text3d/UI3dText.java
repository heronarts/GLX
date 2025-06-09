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

package heronarts.glx.ui.text3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import heronarts.glx.GLX;
import heronarts.glx.View;
import heronarts.glx.shader.Text3d;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;

public class UI3dText extends UI3dComponent {

  private final List<Text3d.Label> labels = new CopyOnWriteArrayList<>();

  private final GLX glx;

  public UI3dText(GLX glx) {
    this.glx = glx;
  }

  public Text3d.Label addLabel(String text) {
    return addLabel(text, Text3d.TextFont.NORMAL);
  }

  public Text3d.Label addLabel(String text, Text3d.TextFont textFont) {
    final Text3d.Label label = this.glx.program.text3d.createLabel(text, textFont);
    this.labels.add(label);
    return label;
  }

  public void removeLabel(Text3d.Label label) {
    this.labels.remove(label);
    label.dispose();
  }

  @Override
  protected void onDraw(UI ui, View view) {
    this.labels.forEach(label -> label.draw(ui, view));
  }

  @Override
  public void dispose() {
    this.labels.forEach(label -> label.dispose());
    this.labels.clear();
    super.dispose();
  }

}
