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

import org.lwjgl.bgfx.BGFXVertexLayout;

public class VertexDeclaration {

  public static int ATTRIB_POSITION = 1 << 0;
  public static int ATTRIB_COLOR0 = 1 << 1;
  public static int ATTRIB_TEXCOORD0 = 1 << 2;
  public static int ATTRIB_NORMAL = 1 << 3;

  public enum Attribute {
    POSITION,
    COLOR0,
    TEXCOORD0,
    NORMAL;
  }

  private final BGFXVertexLayout handle;
  private int stride = 0;

  public VertexDeclaration(GLX glx, int attributes) {
    this.handle = BGFXVertexLayout.calloc();
    bgfx_vertex_layout_begin(this.handle, glx.getRenderer());
    if ((attributes & ATTRIB_POSITION) != 0) {
      addLayout(Attribute.POSITION);
    }
    if ((attributes & ATTRIB_COLOR0) != 0) {
      addLayout(Attribute.COLOR0);
    }
    if ((attributes & ATTRIB_TEXCOORD0) != 0) {
      addLayout(Attribute.TEXCOORD0);
    }
    if ((attributes & ATTRIB_NORMAL) != 0) {
      addLayout(Attribute.NORMAL);
    }
    bgfx_vertex_layout_end(this.handle);
  }

  public VertexDeclaration(GLX glx, Attribute ... attributes) {
    this.handle = BGFXVertexLayout.calloc();
    bgfx_vertex_layout_begin(this.handle, glx.getRenderer());
    for (Attribute attribute : attributes) {
      addLayout(attribute);
    }
    bgfx_vertex_layout_end(this.handle);
  }

  private void addLayout(Attribute attribute) {
    switch (attribute) {
      case POSITION -> {
        bgfx_vertex_layout_add(this.handle, BGFX_ATTRIB_POSITION, 3, BGFX_ATTRIB_TYPE_FLOAT, false, false);
        this.stride += 3 * Float.BYTES;
      }
      case COLOR0 -> {
        bgfx_vertex_layout_add(this.handle, BGFX_ATTRIB_COLOR0, 4, BGFX_ATTRIB_TYPE_UINT8, true, false);
        this.stride += 4;
      }
      case NORMAL -> {
        bgfx_vertex_layout_add(this.handle, BGFX_ATTRIB_NORMAL, 3, BGFX_ATTRIB_TYPE_FLOAT, false, false);
        this.stride += 3 * Float.BYTES;
      }
      case TEXCOORD0 -> {
        bgfx_vertex_layout_add(this.handle, BGFX_ATTRIB_TEXCOORD0, 2, BGFX_ATTRIB_TYPE_FLOAT, false, false);
        this.stride += 2 * Float.BYTES;
      }
      default -> {
        throw new IllegalArgumentException("Unknown VertexDeclaration.Attribute type: " + attribute);
      }
    }
  }

  public BGFXVertexLayout getHandle() {
    return this.handle;
  }

  public void dispose() {
    this.handle.free();
  }

  public int getStride() {
    return this.stride;
  }
}
