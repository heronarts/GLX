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

public abstract class VertexBuffer {

  private final VertexDeclaration vertexDeclaration;
  private final ByteBuffer vertexData;
  private final short vbh;
  private final int numVertices;

  public static class UnitCube extends VertexBuffer {
    public UnitCube(GLX glx) {
      super(glx, 14, VertexDeclaration.ATTRIB_POSITION);
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

  public static class UnitCubeEdges extends VertexBuffer {

    public static final int NUM_VERTICES = 24;

    public UnitCubeEdges(GLX glx) {
      this(glx, NUM_VERTICES);
    }

    protected UnitCubeEdges(GLX glx, int numVertices) {
      super(glx, numVertices, VertexDeclaration.ATTRIB_POSITION);
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

  public VertexBuffer(GLX glx, int numVertices) {
    this(glx, numVertices, VertexDeclaration.ATTRIB_POSITION | VertexDeclaration.ATTRIB_TEXCOORD0);
  }

  public VertexBuffer(GLX glx, int numVertices, int attributes) {
    this.vertexDeclaration = new VertexDeclaration(glx, attributes);
    this.vertexData = MemoryUtil.memAlloc(this.vertexDeclaration.getStride() * numVertices);
    bufferData(this.vertexData);
    this.vertexData.flip();
    this.vbh = bgfx_create_vertex_buffer(bgfx_make_ref(this.vertexData), this.vertexDeclaration.getHandle(), BGFX_BUFFER_NONE);
    this.numVertices = numVertices;
  }

  protected abstract void bufferData(ByteBuffer buffer);

  protected void putVertex(float x, float y, float z) {
    putVertex(this.vertexData, x, y, z);
  }

  public static void putVertex(ByteBuffer buffer, float x, float y, float z) {
    buffer.putFloat(x);
    buffer.putFloat(y);
    buffer.putFloat(z);
  }

  protected void putTex2d(float u, float v) {
    putTex2d(this.vertexData, u, v);
  }

  public static void putTex2d(ByteBuffer buffer, float u, float v) {
    buffer.putFloat(u);
    buffer.putFloat(v);
  }

  public short getHandle() {
    return this.vbh;
  }

  public int getNumVertices() {
    return this.numVertices;
  }

  public void dispose() {
    bgfx_destroy_vertex_buffer(this.vbh);
    MemoryUtil.memFree(this.vertexData);
    this.vertexDeclaration.dispose();
  }
}
