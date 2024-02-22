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

import static org.lwjgl.bgfx.BGFX.BGFX_UNIFORM_TYPE_VEC4;
import static org.lwjgl.bgfx.BGFX.bgfx_create_uniform;
import static org.lwjgl.bgfx.BGFX.bgfx_destroy_uniform;
import static org.lwjgl.bgfx.BGFX.bgfx_set_uniform;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.VertexBuffer;
import heronarts.glx.View;

/**
 * A global program used for rendering basic polygons with
 * a uniform fill color
 */
public class UniformFill extends ShaderProgram {

  private short uniformFillColor;
  private final FloatBuffer fillColorBuffer;

  public UniformFill(GLX glx) {
    super(glx, "vs_shape", "fs_shape");
    this.uniformFillColor = bgfx_create_uniform("u_fillColor", BGFX_UNIFORM_TYPE_VEC4, 1);
    this.fillColorBuffer = MemoryUtil.memAllocFloat(4);
    setFillColor(0xffffffff);
  }

  /**
   * Sets the fill color of the shape
   *
   * @param fillColor Fill color in ARGB format
   * @return this
   */
  public UniformFill setFillColor(int fillColor) {
    this.fillColorBuffer.put(0, ((fillColor >>> 16) & 0xff) / 255f);
    this.fillColorBuffer.put(1, ((fillColor >>> 8) & 0xff) / 255f);
    this.fillColorBuffer.put(2, (fillColor & 0xff) / 255f);
    this.fillColorBuffer.put(3, ((fillColor >>> 24) & 0xff) / 255f);
    return this;
  }

  public UniformFill submit(View view, long state, int fillColor, VertexBuffer ... vertexBuffers) {
    setFillColor(fillColor);
    super.submit(view, state, vertexBuffers);
    return this;
  }

  @Override
  protected void setUniforms(View view) {
    bgfx_set_uniform(this.uniformFillColor, this.fillColorBuffer, 1);
  }

  @Override
  public void dispose() {
    bgfx_destroy_uniform(this.uniformFillColor);
    MemoryUtil.memFree(this.fillColorBuffer);
    super.dispose();
  }

}
