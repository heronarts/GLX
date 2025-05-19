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
import heronarts.glx.View;
import heronarts.lx.model.LXModel;

public class Phong extends ShaderProgram {

  private short uniformLightColor;
  private short uniformLightDirection;
  private short uniformLighting;

  private final FloatBuffer lightColorBuffer;
  private final FloatBuffer lightDirectionBuffer;
  private final FloatBuffer lightingBuffer;

  public Phong(GLX glx) {
    super(glx, "vs_phong", "fs_phong");

    this.uniformLightColor = bgfx_create_uniform("u_lightColor", BGFX_UNIFORM_TYPE_VEC4, 1);
    this.lightColorBuffer = MemoryUtil.memAllocFloat(4);
    setLightColor(0xffffffff);

    this.uniformLightDirection = bgfx_create_uniform("u_lightDirection", BGFX_UNIFORM_TYPE_VEC4, 1);
    this.lightDirectionBuffer = MemoryUtil.memAllocFloat(4);
    setLightDirection(0, 0, 1);

    this.uniformLighting = bgfx_create_uniform("u_lighting", BGFX_UNIFORM_TYPE_VEC4, 1);
    this.lightingBuffer = MemoryUtil.memAllocFloat(4);
    setLighting(LXModel.Mesh.Lighting.DEFAULT);
  }

  public void setLightColor(int argb) {
    this.lightColorBuffer.put(0, ((argb >>> 16) & 0xff) / 255f);
    this.lightColorBuffer.put(1, ((argb >>> 8) & 0xff) / 255f);
    this.lightColorBuffer.put(2, (argb & 0xff) / 255f);
    this.lightColorBuffer.put(3, ((argb >>> 24) & 0xff) / 255f);
  }

  public void setLightDirection(LXModel.Mesh.Vertex v) {
    setLightDirection(v.x, v.y, v.z);
  }

  public void setLightDirection(float x, float y, float z) {
    final float mag = (float) Math.sqrt(x*x + y*y + z*z);
    final float invMag = (mag == 0) ? 1 : mag;

    this.lightDirectionBuffer.put(0, x / invMag);
    this.lightDirectionBuffer.put(1, y / invMag);
    this.lightDirectionBuffer.put(2, z / invMag);
    this.lightDirectionBuffer.put(3, 0);
  }

  public void setLighting(LXModel.Mesh.Lighting lighting) {
    setLighting(lighting.ambient, lighting.diffuse, lighting.specular, lighting.shininess);
  }

  public void setLighting(float ambient, float diffuse, float specular, float shininess) {
    this.lightingBuffer.put(0, ambient);
    this.lightingBuffer.put(1, diffuse);
    this.lightingBuffer.put(2, specular);
    this.lightingBuffer.put(3, shininess);
  }

  @Override
  protected void setUniforms(View view) {
    bgfx_set_uniform(this.uniformLightColor, this.lightColorBuffer, 1);
    bgfx_set_uniform(this.uniformLightDirection, this.lightDirectionBuffer, 1);
    bgfx_set_uniform(this.uniformLighting, this.lightingBuffer, 1);
  }

  @Override
  public void dispose() {
    bgfx_destroy_uniform(this.uniformLighting);
    MemoryUtil.memFree(this.lightingBuffer);

    bgfx_destroy_uniform(this.uniformLightDirection);
    MemoryUtil.memFree(this.lightDirectionBuffer);

    bgfx_destroy_uniform(this.uniformLightColor);
    MemoryUtil.memFree(this.lightColorBuffer);
    super.dispose();
  }
}
