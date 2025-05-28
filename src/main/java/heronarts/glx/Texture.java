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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.stb.STBImage.STBI_rgb_alpha;
import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public class Texture {

  private final short th;
  private final ByteBuffer textureData;
  private final ByteBuffer stbiData;

  public static Texture from2dImage(String path) throws IOException {
    return new Texture(path, true);
  }

  public Texture(String path) {
    this.stbiData = null;
    try {
      this.textureData = GLXUtils.loadResource("textures/" + path);
    } catch (IOException x) {
      throw new RuntimeException(x);
    }
    this.th = bgfx_create_texture(bgfx_make_ref(this.textureData), BGFX_TEXTURE_NONE, 0, null);
  }

  private Texture(String path, boolean is2d) throws IOException {
    this.textureData = null;
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      IntBuffer components = stack.mallocInt(1);
      this.stbiData = stbi_load(path, width, height, components, STBI_rgb_alpha);
      if (this.stbiData == null) {
        throw new IOException("STBI failed to load STBI image: " + path);
      }
      this.th = bgfx_create_texture_2d(width.get(), height.get(), false, 1, BGFX_TEXTURE_FORMAT_RGBA8, BGFX_TEXTURE_NONE, bgfx_make_ref(this.stbiData));
    }
  }

  public short getHandle() {
    return this.th;
  }

  public void dispose() {
    bgfx_destroy_texture(this.th);
    if (this.stbiData != null) {
      stbi_image_free(this.stbiData);
    }
    if (this.textureData != null) {
      MemoryUtil.memFree(this.textureData);
    }
  }
}
