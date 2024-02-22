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
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.stb.STBImage.*;

public class GLXUtils {

  private GLXUtils() {}

  public static class Image {

    private final int[] pixels;

    public final int width;
    public final int height;
    public final int components;

    private Image(int[] argb, int width, int height) {
      this.pixels = argb;
      this.width = width;
      this.height = height;
      this.components = -1;
    }

    private Image(ByteBuffer imageBuffer) throws IOException {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer width = stack.mallocInt(1);
        IntBuffer height = stack.mallocInt(1);
        IntBuffer components = stack.mallocInt(1);
        ByteBuffer bytes = stbi_load_from_memory(imageBuffer, width, height, components, STBI_rgb_alpha);
        MemoryUtil.memFree(imageBuffer);

        if (bytes == null) {
          throw new IOException("STBI failed to load image data");
        }

        this.width = width.get(0);
        this.height = height.get(0);
        this.components = components.get(0);
        this.pixels = new int[this.width * this.height];

        // Swizzle the bytes into order
        for (int i = 0; i < this.width * this.height; ++i) {
          int ii = i << 2;
          byte r = bytes.get(ii);
          byte g = bytes.get(ii + 1);
          byte b = bytes.get(ii + 2);
          byte a = bytes.get(ii + 3);
          this.pixels[i] = ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
        }

        stbi_image_free(bytes);
      }

    }

    public int get(int x, int y) {
      return this.pixels[y*this.width + x];
    }

    public int getNormalized(float x, float y) {
      return get(
        (int) (x * (this.width - .01f)),
        (int) (y * (this.height - .01f))
      );
    }

    public float getAspectRatio() {
      return (float) this.width / (float) this.height;
    }
  }

  public static Image loadRaster(int[] argb, int width, int height) {
    return new Image(argb, width, height);
  }

  public static Image loadImage(String path) throws IOException {
    ByteBuffer buffer = loadFile(path);
    return (buffer != null) ? new Image(buffer) : null;
  }

  public static ByteBuffer loadShader(GLX glx, String name) throws IOException {
    String path = "shaders/";
    switch (glx.getRenderer()) {
    case BGFX_RENDERER_TYPE_DIRECT3D11:
    case BGFX_RENDERER_TYPE_DIRECT3D12:
      path += "dx11/";
      break;
    case BGFX_RENDERER_TYPE_DIRECT3D9:
      path += "dx9/";
      break;
    case BGFX_RENDERER_TYPE_OPENGL:
      path += "glsl/";
      break;
    case BGFX_RENDERER_TYPE_METAL:
      path += "metal/";
      break;
    default:
      throw new IOException("No shaders supported for " + bgfx_get_renderer_name(glx.getRenderer()) + " renderer");
    }
    return loadResource(path + name + ".bin");
  }

  /**
   * Gets an input stream for the resource at the given path
   *
   * @param resourcePath Path to the resource
   * @return Resource input stream
   * @throws IOException If loading resource stream fails
   */
  public static InputStream loadResourceStream(String resourcePath) throws IOException {
    Path path = Paths.get("src/main/resources/" + resourcePath);
    if (Files.isReadable(path)) {
      return Files.newInputStream(path);
    }

    URL url = GLXUtils.class.getResource("/" + resourcePath);
    if (url == null) {
      throw new IOException("Resource not found: " + resourcePath);
    }
    return url.openStream();
  }

  /**
   * Loads the resource at the given path into a newly allocated buffer. The buffer is owned by
   * the caller and must be freed explicitly.
   *
   * @param resourcePath Path to the resource
   * @return Buffer allocated by MemoryUtil
   * @throws IOException If there is an error loading the resource
   */
  public static ByteBuffer loadResource(String resourcePath) throws IOException {
    ByteBuffer resource = null;
    Path path = Paths.get("src/main/resources/" + resourcePath);
    if (Files.isReadable(path)) {
      try (
        SeekableByteChannel fc = Files.newByteChannel(path);
      ) {
        int totalBytes = (int) fc.size();
        int readBytes = 0;
        int read = 0;
        resource = MemoryUtil.memAlloc(totalBytes);
        do {
          if ((read = fc.read(resource)) == -1) break;
          readBytes += read;
        } while (readBytes < totalBytes);
        resource.flip();
        return resource;
      } catch (IOException iox) {
        MemoryUtil.memFree(resource);
        throw iox;
      }
    }

    URL url = GLXUtils.class.getResource("/" + resourcePath);
    if (url == null) {
      throw new IOException("Resource not found: " + resourcePath);
    }
    int resourceSize = url.openConnection().getContentLength();
    resource = MemoryUtil.memAlloc(resourceSize);
    try (
      InputStream stream = url.openStream();
      ReadableByteChannel rbc = Channels.newChannel(stream);
    ) {
      int read = 0;
      int readBytes = 0;
      do {
        if ((read = rbc.read(resource)) == -1) break;
        readBytes += read;
      } while (readBytes < resourceSize);
      resource.flip();
      return resource;
    } catch(IOException iox) {
      MemoryUtil.memFree(resource);
      throw iox;
    }
  }

  public static ByteBuffer loadFile(String file) throws IOException {
    ByteBuffer resource = null;
    Path path = Paths.get(file);
    if (Files.isReadable(path)) {
      try (
        SeekableByteChannel fc = Files.newByteChannel(path);
      ) {
        int totalBytes = (int) fc.size();
        int readBytes = 0;
        int read = 0;
        resource = MemoryUtil.memAlloc(totalBytes);
        do {
          if ((read = fc.read(resource)) == -1) break;
          readBytes += read;
        } while (readBytes < totalBytes);
        resource.flip();
        return resource;
      } catch (IOException iox) {
        MemoryUtil.memFree(resource);
        throw iox;
      }
    }
    return null;
  }
}
