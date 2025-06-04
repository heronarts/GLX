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

import org.joml.Vector3f;

import heronarts.glx.GLX;
import heronarts.glx.View;
import heronarts.lx.model.LXModel;

public class Phong extends ShaderProgram {

  private final Uniform.Vec4f uniformLightColor;
  private final Uniform.Vec4f uniformLightDirection;
  private final Uniform.Vec4f uniformLighting;
  private final Uniform.Vec4f uniformEyePosition;

  private int lightColorARGB = 0;
  private float[] lightDirection = new float[4];
  private float[] lighting = new float[4];
  private float[] eyePosition = new float[4];

  public Phong(GLX glx) {
    super(glx, "vs_phong", "fs_phong");

    this.uniformLightColor = new Uniform.Vec4f(glx, "u_lightColor");
    setLightColor(0xffffffff);

    this.uniformLightDirection = new Uniform.Vec4f(glx, "u_lightDirection");
    setLightDirection(0, 0, 1);

    this.uniformLighting = new Uniform.Vec4f(glx,"u_lighting");
    setLighting(LXModel.Mesh.Lighting.DEFAULT);

    this.uniformEyePosition = new Uniform.Vec4f(glx,"u_eyePosition");
  }

  /**
   * Set light color in ARGB format
   *
   * @param lightColorARGB Light color, ARGB
   */
  public void setLightColor(int lightColorARGB) {
    this.lightColorARGB = lightColorARGB;
  }

  public void setLightDirection(LXModel.Mesh.Vertex v) {
    setLightDirection(v.x, v.y, v.z);
  }

  public void setLightDirection(float x, float y, float z) {
    final float mag = (float) Math.sqrt(x*x + y*y + z*z);
    final float invMag = (mag == 0) ? 1 : mag;

    this.lightDirection[0] = x / invMag;
    this.lightDirection[1] = y / invMag;
    this.lightDirection[2] = z / invMag;
  }

  public void setLighting(LXModel.Mesh.Lighting lighting) {
    setLighting(lighting.ambient, lighting.diffuse, lighting.specular, lighting.shininess);
  }

  public void setLighting(float ambient, float diffuse, float specular, float shininess) {
    this.lighting[0] = ambient;
    this.lighting[1] = diffuse;
    this.lighting[2] = specular;
    this.lighting[3] = shininess;
  }

  public void setEyePosition(Vector3f eye) {
    setEyePosition(eye.x, eye.y, eye.z);
  }

  public void setEyePosition(float x, float y, float z) {
    this.eyePosition[0] = x;
    this.eyePosition[1] = y;
    this.eyePosition[2] = z;
  }

  @Override
  protected void setUniforms(View view) {
    this.uniformLightColor.setARGB(this.lightColorARGB);
    this.uniformLightDirection.set(this.lightDirection);
    this.uniformLighting.set(this.lighting);
    this.uniformEyePosition.set(this.eyePosition);
  }

  @Override
  public void dispose() {
    this.uniformLighting.dispose();
    this.uniformLightDirection.dispose();
    this.uniformLightColor.dispose();
    this.uniformEyePosition.dispose();
    super.dispose();
  }
}
