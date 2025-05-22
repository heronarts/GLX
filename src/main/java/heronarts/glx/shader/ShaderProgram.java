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

import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.GLXUtils;
import heronarts.glx.Texture;
import heronarts.glx.VertexBuffer;
import heronarts.glx.View;

public class ShaderProgram {

  public abstract static class Uniform {

    public enum Type {
      SAMPLER(BGFX_UNIFORM_TYPE_SAMPLER), VEC4(BGFX_UNIFORM_TYPE_VEC4);

      private final int bgfxType;

      private Type(int bgfxType) {
        this.bgfxType = bgfxType;
      }
    }

    protected final short handle;

    protected Uniform(Type type, String name) {
      this.handle = bgfx_create_uniform(name, type.bgfxType, 1);
    }

    public void dispose() {
      bgfx_destroy_uniform(this.handle);
    }

    public static class Sampler extends Uniform {
      public Sampler(String name) {
        super(Type.SAMPLER, name);
      }

      public void setTexture(int stage, Texture texture, int textureFlags) {
        setTexture(stage, texture.getHandle(), textureFlags);
      }

      public void setTexture(int stage, short textureHandle, int textureFlags) {
        bgfx_set_texture(stage, this.handle, textureHandle, textureFlags);
      }
    }

    public static class Vec4f extends Uniform {

      private final FloatBuffer buffer;

      public Vec4f(String name) {
        super(Type.VEC4, name);
        this.buffer = MemoryUtil.memAllocFloat(4);
      }

      public void setARGB(int argb) {
        set(((argb >>> 16) & 0xff) / 255f, ((argb >>> 8) & 0xff) / 255f,
          (argb & 0xff) / 255f, ((argb >>> 24) & 0xff) / 255f);
      }

      public void set(float... values) {
        if (values.length > 4) {
          throw new IllegalArgumentException(
            "Cannot pass more than 4 values to Uniform.Vec4f.set()");
        }
        int i = 0;
        for (float f : values) {
          this.buffer.put(i++, f);
        }
        bgfx_set_uniform(this.handle, this.buffer, 1);
      }

      @Override
      public void dispose() {
        super.dispose();
        MemoryUtil.memFree(this.buffer);
      }

    }
  }

  public static final long DEFAULT_BGFX_STATE = BGFX_STATE_WRITE_RGB
    | BGFX_STATE_WRITE_A | BGFX_STATE_WRITE_Z | BGFX_STATE_BLEND_ALPHA;

  private short handle;
  private ByteBuffer vertexShaderCode;
  private ByteBuffer fragmentShaderCode;
  protected long bgfxState = DEFAULT_BGFX_STATE;

  public ShaderProgram(GLX glx, String vsName, String fsName) {
    try {
      this.vertexShaderCode = GLXUtils.loadShader(glx, vsName);
      this.fragmentShaderCode = GLXUtils.loadShader(glx, fsName);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
    this.handle = bgfx_create_program(
      bgfx_create_shader(bgfx_make_ref(this.vertexShaderCode)),
      bgfx_create_shader(bgfx_make_ref(this.fragmentShaderCode)), true);
  }

  public void submit(View view) {
    submit(view, this.bgfxState);
  }

  public void submit(View view, VertexBuffer vertexBuffer) {
    submit(view, this.bgfxState, vertexBuffer);
  }

  public void submit(View view, long bgfxState) {
    submit(view, bgfxState, (VertexBuffer[]) null);
  }

  public void submit(View view, long bgfxState, VertexBuffer... vertexBuffers) {
    bgfx_set_state(bgfxState, 0);
    setUniforms(view);
    if (vertexBuffers != null) {
      int vertexStream = 0;
      for (VertexBuffer vertexBuffer : vertexBuffers) {
        if (vertexBuffer == null) {
          GLX.error(new Exception(
            "A null vertexBuffer was passed to ShaderProgram.submit"));
        } else {
          bgfx_set_vertex_buffer(vertexStream++, vertexBuffer.getHandle(), 0,
            vertexBuffer.getNumVertices());
        }
      }
    }
    setVertexBuffers(view);
    bgfx_submit(view.getId(), this.handle, 0, BGFX_DISCARD_ALL);
  }

  protected void setVertexBuffers(View view) {
    // Subclasses override to set additional vertex buffers
  }

  protected void setUniforms(View view) {
    // Subclasses override to set textures and uniforms
  }

  public void dispose() {
    bgfx_destroy_program(this.handle);
    MemoryUtil.memFree(this.vertexShaderCode);
    MemoryUtil.memFree(this.fragmentShaderCode);
  }
}
