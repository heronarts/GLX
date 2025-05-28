/**
 * Copyright 2019- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.shader;

import heronarts.glx.BGFXEngine;
import heronarts.glx.VertexBuffer;
import heronarts.glx.View;

/**
 * A global program used for rendering basic polygons with
 * a uniform fill color
 */
public class UniformFill extends ShaderProgram {

  private final Uniform.Vec4f uniformFillColor;

  private int fillColorARGB;

  public UniformFill(BGFXEngine bgfx) {
    super(bgfx, "vs_shape", "fs_shape");
    this.uniformFillColor = new Uniform.Vec4f("u_fillColor");
    setFillColor(0xffffffff);
  }

  /**
   * Sets the fill color of the shape
   *
   * @param fillColorARGB Fill color in ARGB format
   * @return this
   */
  public UniformFill setFillColor(int fillColorARGB) {
    this.fillColorARGB = fillColorARGB;
    return this;
  }

  public UniformFill submit(View view, long state, int fillColor, VertexBuffer ... vertexBuffers) {
    setFillColor(fillColor);
    super.submit(view, state, vertexBuffers);
    return this;
  }

  @Override
  protected void setUniforms(View view) {
    this.uniformFillColor.setARGB(this.fillColorARGB);
  }

  @Override
  public void dispose() {
    this.uniformFillColor.dispose();
    super.dispose();
  }

}
