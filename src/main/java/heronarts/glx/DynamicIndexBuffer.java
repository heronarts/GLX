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

/**
 * A bgfx index buffer with contents that can be updated dynamically between frames.
 */
public class DynamicIndexBuffer {

  private final ByteBuffer indexData;
  private final short indexBufferHandle;
  private final int numIndices;

  /**
   * Constructs a new dynamic index buffer
   *
   * @param glx GLX instance
   * @param numIndices How many indices are in the buffer (number of indices, not bytes)
   */
  public DynamicIndexBuffer(GLX glx, int numIndices) {
    this(glx, numIndices, false);
  }

  /**
   * Constructs a new dynamic index buffer
   *
   * @param glx GLX instance
   * @param numIndices How many indices are in the buffer (number of indices, not bytes)
   * @param int32 Whether to use int32 size rather than int16 for index references (for large buffers > 65K)
   */
  public DynamicIndexBuffer(GLX glx, int numIndices, boolean int32) {
    this.indexData = MemoryUtil.memAlloc((int32 ? Integer.BYTES : Short.BYTES) * numIndices);
    this.indexBufferHandle = bgfx_create_dynamic_index_buffer(numIndices, int32 ? BGFX_BUFFER_INDEX32 : BGFX_BUFFER_NONE);
    this.numIndices = numIndices;
  }

  /**
   * Returns the bgfx handle for the vertex buffer
   *
   * @return BGFX buffer handle
   */
  public short getHandle() {
    return this.indexBufferHandle;
  }

  /**
   * Returns the number of indices in the buffer
   *
   * @return Number of indices in the buffer (numeric count, not bytes)
   */
  public int getNumIndices() {
    return this.numIndices;
  }

  /**
   * Returns the raw data buffer used to populate the buffer
   *
   * @return Raw data buffer
   */
  public ByteBuffer getIndexData() {
    return this.indexData;
  }

  /**
   * Update the underlying BGFX index buffer with the index buffer data
   */
  public void update() {
    bgfx_update_dynamic_index_buffer(this.indexBufferHandle, 0, bgfx_make_ref(this.indexData));
  }

  /**
   * Clean up this component, free all memory resources
   */
  public void dispose() {
    bgfx_destroy_dynamic_index_buffer(this.indexBufferHandle);
    MemoryUtil.memFree(this.indexData);
  }
}
