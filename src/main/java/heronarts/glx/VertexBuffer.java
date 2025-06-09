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

package heronarts.glx;

import static org.lwjgl.bgfx.BGFX.*;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

public abstract class VertexBuffer implements BGFXEngine.Resource {

  private final GLX glx;
  private final VertexDeclaration vertexDeclaration;
  private final ByteBuffer vertexData;
  private final short vbh;
  private final int numVertices;

  public static class UnitCube extends VertexBuffer {
    UnitCube(GLX glx) {
      super(glx, 14, VertexDeclaration.Attribute.POSITION);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      putVertex(+0.5f, +0.5f, +0.5f); // Back-top-right
      putVertex(-0.5f, +0.5f, +0.5f); // Back-top-left
      putVertex(+0.5f, -0.5f, +0.5f); // Back-bottom-right
      putVertex(-0.5f, -0.5f, +0.5f); // Back-bottom-left
      putVertex(-0.5f, -0.5f, -0.5f); // Front-bottom-left
      putVertex(-0.5f, +0.5f, +0.5f); // Back-top-left
      putVertex(-0.5f, +0.5f, -0.5f); // Front-top-left
      putVertex(+0.5f, +0.5f, +0.5f); // Back-top-right
      putVertex(+0.5f, +0.5f, -0.5f); // Front-top-right
      putVertex(+0.5f, -0.5f, +0.5f); // Back-bottom-right
      putVertex(+0.5f, -0.5f, -0.5f); // Front-bottom-right
      putVertex(-0.5f, -0.5f, -0.5f); // Front-bottom-left
      putVertex(+0.5f, +0.5f, -0.5f); // Front-top-right
      putVertex(-0.5f, +0.5f, -0.5f); // Front-top-left
    }
  }

  public static class UnitCubeWithNormals extends VertexBuffer {
    UnitCubeWithNormals(GLX glx) {
      super(glx, 36, VertexDeclaration.Attribute.POSITION, VertexDeclaration.Attribute.NORMAL);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      // Front
      putVertex(-0.5f, -0.5f, -0.5f, 0, 0, -1f);
      putVertex(+0.5f, -0.5f, -0.5f, 0, 0, -1f);
      putVertex(-0.5f, +0.5f, -0.5f, 0, 0, -1f);
      putVertex(-0.5f, +0.5f, -0.5f, 0, 0, -1f);
      putVertex(+0.5f, -0.5f, -0.5f, 0, 0, -1f);
      putVertex(+0.5f, +0.5f, -0.5f, 0, 0, -1f);

      // Right
      putVertex(+0.5f, -0.5f, -0.5f, 1f, 0, 0);
      putVertex(+0.5f, -0.5f, +0.5f, 1f, 0, 0);
      putVertex(+0.5f, +0.5f, -0.5f, 1f, 0, 0);
      putVertex(+0.5f, +0.5f, -0.5f, 1f, 0, 0);
      putVertex(+0.5f, -0.5f, +0.5f, 1f, 0, 0);
      putVertex(+0.5f, +0.5f, +0.5f, 1f, 0, 0);

      // Back
      putVertex(+0.5f, -0.5f, +0.5f, 0, 0, 1f);
      putVertex(-0.5f, -0.5f, +0.5f, 0, 0, 1f);
      putVertex(+0.5f, +0.5f, +0.5f, 0, 0, 1f);
      putVertex(+0.5f, +0.5f, +0.5f, 0, 0, 1f);
      putVertex(-0.5f, -0.5f, +0.5f, 0, 0, 1f);
      putVertex(-0.5f, +0.5f, +0.5f, 0, 0, 1f);

      // Left
      putVertex(-0.5f, -0.5f, +0.5f, -1f, 0, 0);
      putVertex(-0.5f, -0.5f, -0.5f, -1f, 0, 0);
      putVertex(-0.5f, +0.5f, +0.5f, -1f, 0, 0);
      putVertex(-0.5f, +0.5f, +0.5f, -1f, 0, 0);
      putVertex(-0.5f, -0.5f, -0.5f, -1f, 0, 0);
      putVertex(-0.5f, +0.5f, -0.5f, -1f, 0, 0);

      // Top
      putVertex(-0.5f, +0.5f, -0.5f, 0, 1f, 0);
      putVertex(+0.5f, +0.5f, -0.5f, 0, 1f, 0);
      putVertex(-0.5f, +0.5f, +0.5f, 0, 1f, 0);
      putVertex(-0.5f, +0.5f, +0.5f, 0, 1f, 0);
      putVertex(+0.5f, +0.5f, -0.5f, 0, 1f, 0);
      putVertex(+0.5f, +0.5f, +0.5f, 0, 1f, 0);

      // Bottom
      putVertex(+0.5f, -0.5f, -0.5f, 0, 1f, 0);
      putVertex(-0.5f, -0.5f, -0.5f, 0, 1f, 0);
      putVertex(+0.5f, -0.5f, +0.5f, 0, 1f, 0);
      putVertex(+0.5f, -0.5f, +0.5f, 0, 1f, 0);
      putVertex(-0.5f, -0.5f, -0.5f, 0, 1f, 0);
      putVertex(-0.5f, -0.5f, +0.5f, 0, 1f, 0);

    }
  }

  public static class UnitCubeEdges extends VertexBuffer {

    public static final int NUM_VERTICES = 24;

    UnitCubeEdges(GLX glx) {
      this(glx, NUM_VERTICES);
    }

    protected UnitCubeEdges(GLX glx, int numVertices) {
      super(glx, numVertices, VertexDeclaration.Attribute.POSITION);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      putVertex(+0.5f, +0.5f, +0.5f); // Back-top-right
      putVertex(-0.5f, +0.5f, +0.5f); // Back-top-left

      putVertex(-0.5f, +0.5f, +0.5f); // Back-top-left
      putVertex(-0.5f, -0.5f, +0.5f); // Back-bottom-left

      putVertex(-0.5f, -0.5f, +0.5f); // Back-bottom-left
      putVertex(+0.5f, -0.5f, +0.5f); // Back-bottom-right

      putVertex(+0.5f, -0.5f, +0.5f); // Back-bottom-right
      putVertex(+0.5f, +0.5f, +0.5f); // Back-top-right

      putVertex(+0.5f, +0.5f, -0.5f); // Front-top-right
      putVertex(-0.5f, +0.5f, -0.5f); // Front-top-left

      putVertex(-0.5f, +0.5f, -0.5f); // Front-top-left
      putVertex(-0.5f, -0.5f, -0.5f); // Front-bottom-left

      putVertex(-0.5f, -0.5f, -0.5f); // Front-bottom-left
      putVertex(+0.5f, -0.5f, -0.5f); // Front-bottom-right

      putVertex(+0.5f, -0.5f, -0.5f); // Front-bottom-right
      putVertex(+0.5f, +0.5f, -0.5f); // Front-top-right

      putVertex(+0.5f, +0.5f, -0.5f); // Front-top-right
      putVertex(+0.5f, +0.5f, +0.5f); // Back-top-right

      putVertex(-0.5f, +0.5f, -0.5f); // Front-top-left
      putVertex(-0.5f, +0.5f, +0.5f); // Back-top-left

      putVertex(-0.5f, -0.5f, -0.5f); // Front-bottom-left
      putVertex(-0.5f, -0.5f, +0.5f); // Back-bottom-left

      putVertex(+0.5f, -0.5f, -0.5f); // Front-bottom-right
      putVertex(+0.5f, -0.5f, +0.5f); // Back-bottom-right
    }
  }

  @Deprecated
  public VertexBuffer(GLX glx, int numVertices) {
    this(glx, numVertices, VertexDeclaration.Attribute.POSITION, VertexDeclaration.Attribute.TEXCOORD0);
  }

  public VertexBuffer(GLX glx, int numVertices, VertexDeclaration.Attribute ... attributes) {
    this(glx, numVertices, new VertexDeclaration(glx, attributes));
  }

  private VertexBuffer(GLX glx, int numVertices, VertexDeclaration vertexDeclaration) {
    glx.assertBgfxThreadAllocation(this);
    this.glx = glx;
    this.vertexDeclaration = vertexDeclaration;
    this.vertexData = MemoryUtil.memAlloc(this.vertexDeclaration.getStride() * numVertices);
    bufferData(this.vertexData);
    this.vertexData.flip();
    this.vbh = bgfx_create_vertex_buffer(bgfx_make_ref(this.vertexData), this.vertexDeclaration.getHandle(), BGFX_BUFFER_NONE);
    this.numVertices = numVertices;
  }

  protected abstract void bufferData(ByteBuffer buffer);

  protected void putNormal(float nx, float ny, float nz) {
    putVertex(nx, ny, nz);
  }

  protected void putVertex(float x, float y, float z, float nx, float ny, float nz) {
    putVertex(this.vertexData, x, y, z, nx, ny, nz);
  }

  protected void putVertex(float x, float y, float z) {
    putVertex(this.vertexData, x, y, z);
  }

  public static void putVertex(ByteBuffer buffer, float x, float y, float z) {
    buffer.putFloat(x);
    buffer.putFloat(y);
    buffer.putFloat(z);
  }

  public static void putVertex(ByteBuffer buffer, float x, float y, float z, float nx, float ny, float nz) {
    buffer.putFloat(x);
    buffer.putFloat(y);
    buffer.putFloat(z);
    buffer.putFloat(nx);
    buffer.putFloat(ny);
    buffer.putFloat(nz);
  }

  protected void putTex2d(float u, float v) {
    putTex2d(this.vertexData, u, v);
  }

  public static void putTex2d(ByteBuffer buffer, float u, float v) {
    buffer.putFloat(u);
    buffer.putFloat(v);
  }

  protected void putTex3d(float u, float v, float w) {
    putTex3d(this.vertexData, u, v, w);
  }

  public static void putTex3d(ByteBuffer buffer, float u, float v, float w) {
    buffer.putFloat(u);
    buffer.putFloat(v);
    buffer.putFloat(w);
  }

  public short getHandle() {
    return this.vbh;
  }

  public int getNumVertices() {
    return this.numVertices;
  }

  public void dispose() {
    if (this.glx.bgfxThreadDispose(this)) {
      bgfx_destroy_vertex_buffer(this.vbh);
      MemoryUtil.memFree(this.vertexData);
      this.vertexDeclaration.dispose();
    }
  }
}
