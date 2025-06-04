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

import static org.lwjgl.bgfx.BGFX.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXVertexLayout;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.BGFXEngine;
import heronarts.glx.GLX;
import heronarts.glx.GLXUtils;
import heronarts.glx.Texture;
import heronarts.glx.VertexBuffer;
import heronarts.glx.View;
import heronarts.glx.shader.ShaderProgram.Uniform;

public class Tex2d {

  private final GLX glx;
  private BGFXVertexLayout vertexLayout;
  private short program;

  private final Uniform.Sampler uniformTexture;

  private ByteBuffer vertexBuffer;
  private short vbh;

  private ByteBuffer vsCode;
  private ByteBuffer fsCode;

  protected final Matrix4f modelMatrix = new Matrix4f();
  protected final FloatBuffer modelMatrixBuf;

  private final static float[][] VERTEX_BUFFER_DATA = {
    { 0f, 0f, 0f, 0f, 0f },
    { 1f, 0f, 0f, 1f, 0f },
    { 0f, 1f, 0f, 0f, 1f },
    { 1f, 1f, 0f, 1f, 1f }
  };

  // Texture/framebuffer coordinates are +Y up on OpenGL
  private final static float[][] VERTEX_BUFFER_DATA_OPENGL = {
    { 0f, 0f, 0f, 0f, 1f },
    { 1f, 0f, 0f, 1f, 1f },
    { 0f, 1f, 0f, 0f, 0f },
    { 1f, 1f, 0f, 1f, 0f }
  };

  public Tex2d(BGFXEngine bgfx) {
    bgfx.glx.assertBgfxThreadAllocation(getClass());
    this.glx = bgfx.glx;

    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.modelMatrix.get(this.modelMatrixBuf);

    this.vertexLayout = BGFXVertexLayout.calloc();
    bgfx_vertex_layout_begin(this.vertexLayout, bgfx.getRenderer());
    bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_POSITION, 3,
      BGFX_ATTRIB_TYPE_FLOAT, false, false);
    bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_TEXCOORD0, 2,
      BGFX_ATTRIB_TYPE_FLOAT, false, false);
    bgfx_vertex_layout_end(this.vertexLayout);

    this.vertexBuffer = MemoryUtil
      .memAlloc(VERTEX_BUFFER_DATA.length * 5 * Float.BYTES);
    for (float[] fl : bgfx.isOpenGL() ? VERTEX_BUFFER_DATA_OPENGL : VERTEX_BUFFER_DATA) {
      for (float f : fl) {
        this.vertexBuffer.putFloat(f);
      }
    }
    this.vertexBuffer.flip();
    this.vbh = bgfx_create_vertex_buffer(
      bgfx_make_ref(this.vertexBuffer), this.vertexLayout, BGFX_BUFFER_NONE);

    try {
      this.vsCode = GLXUtils.loadShader(bgfx, "vs_view2d");
      this.fsCode = GLXUtils.loadShader(bgfx, "fs_view2d");
      this.program = bgfx_create_program(
        bgfx_create_shader(bgfx_make_ref(this.vsCode)),
        bgfx_create_shader(bgfx_make_ref(this.fsCode)),
        true
      );
      this.uniformTexture = new Uniform.Sampler(bgfx.glx, "s_texColor");
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  private static final long DEFAULT_BGFX_STATE =
    BGFX.BGFX_STATE_WRITE_RGB |
    BGFX.BGFX_STATE_WRITE_A |
    BGFX.BGFX_STATE_WRITE_Z |
    BGFX.BGFX_STATE_BLEND_ALPHA;

  public void submit(View view, Texture texture, VertexBuffer vertexBuffer) {
    submit(view, DEFAULT_BGFX_STATE, texture, vertexBuffer);
  }

  public void submit(View view, long bgfxState, Texture texture, VertexBuffer vertexBuffer) {
    this.modelMatrix.identity();
    this.modelMatrix.get(this.modelMatrixBuf);
    bgfx_set_transform(this.modelMatrixBuf);
    submitPostTransform(view, bgfxState, texture, vertexBuffer);
  }

  public void submitPostTransform(View view, long bgfxState, Texture texture, VertexBuffer vertexBuffer) {
    this.uniformTexture.setTexture(0, texture, 0xffffffff);
    bgfx_set_state(bgfxState, 0);
    bgfx_set_vertex_buffer(0, vertexBuffer.getHandle(), 0, vertexBuffer.getNumVertices());
    bgfx_submit(view.getId(), this.program, 0, BGFX_DISCARD_ALL);
  }

  public void submit(View view, short texHandle, float x, float y, float w, float h) {
    this.modelMatrix.identity().translate(x, y, 0).scale(w, h, 1);
    this.modelMatrix.get(this.modelMatrixBuf);
    bgfx_set_transform(this.modelMatrixBuf);
    this.uniformTexture.setTexture(0, texHandle, 0xffffffff);
    bgfx_set_state(
      BGFX_STATE_WRITE_RGB |
      BGFX_STATE_WRITE_A |
      BGFX_STATE_WRITE_Z |
      BGFX_STATE_BLEND_ALPHA |
      BGFX_STATE_PT_TRISTRIP,
      0);
    bgfx_set_vertex_buffer(0, this.vbh, 0, VERTEX_BUFFER_DATA.length);
    bgfx_submit(view.getId(), this.program, 0, BGFX_DISCARD_ALL);
  }

  public void dispose() {
    this.glx.bgfxThreadDispose(getClass(), () -> {
      MemoryUtil.memFree(this.vertexBuffer);
      MemoryUtil.memFree(this.vsCode);
      MemoryUtil.memFree(this.fsCode);
      this.vertexLayout.free();
      MemoryUtil.memFree(this.modelMatrixBuf);
      this.uniformTexture.dispose();
      bgfx_destroy_program(this.program);
    });
  }

}