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

public class DynamicVertexBuffer implements BGFXEngine.Resource {

  private final GLX glx;
  private final VertexDeclaration vertexDeclaration;
  private final ByteBuffer vertexData;

  private final short vertexBufferHandle;
  private final int numVertices;

  public DynamicVertexBuffer(GLX glx, int numVertices) {
    this(glx, numVertices, VertexDeclaration.Attribute.POSITION, VertexDeclaration.Attribute.COLOR0);
  }

  public DynamicVertexBuffer(GLX glx, int numVertices, VertexDeclaration.Attribute ... attributes) {
    glx.assertBgfxThreadAllocation(this);
    this.glx = glx;
    this.vertexDeclaration = new VertexDeclaration(glx, attributes);
    this.vertexData = MemoryUtil.memAlloc(this.vertexDeclaration.getStride() * numVertices);
    this.vertexBufferHandle = bgfx_create_dynamic_vertex_buffer(numVertices, this.vertexDeclaration.getHandle(), BGFX_BUFFER_NONE);
    this.numVertices = numVertices;
  }

  public short getHandle() {
    return this.vertexBufferHandle;
  }

  public int getNumVertices() {
    return this.numVertices;
  }

  public ByteBuffer getVertexData() {
    return this.vertexData;
  }

  public void update() {
    this.glx.assertBgfxThreadUpdate(this);
    bgfx_update_dynamic_vertex_buffer(this.vertexBufferHandle, 0, bgfx_make_ref(this.vertexData));
  }

  public void dispose() {
    if (this.glx.bgfxThreadDispose(this)) {
      bgfx_destroy_dynamic_vertex_buffer(this.vertexBufferHandle);
      MemoryUtil.memFree(this.vertexData);
      this.vertexDeclaration.dispose();
    }
  }
}
