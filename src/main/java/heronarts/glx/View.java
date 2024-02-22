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

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.ui.UI2dContext;

public class View {

  protected final GLX glx;

  protected short viewId;

  protected int x = 0, y = 0, width = 0, height = 0;

  private int clearColor = 0x000000ff;
  private int clearFlags = BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL;
  private float clearDepth = 1f;

  protected final Matrix4f viewMatrix = new Matrix4f();
  protected final FloatBuffer viewMatrixBuf;
  protected final Matrix4f projectionMatrix = new Matrix4f();
  protected final FloatBuffer projectionMatrixBuf;

  /**
   * Constructs a default view of the entire framebuffer
   *
   * @param glx GLX instance
   */
  public View(GLX glx) {
    this(glx, 0, 0, glx.getFrameBufferWidth(), glx.getFrameBufferHeight());
  }

  /**
   * Constructs a view of the given bounds. Bounds are expressed in framebuffer
   * coordinate space, with no awareness of content-scaling
   *
   * @param glx LX instance
   * @param x Top-left x position in framebuffer coordinates
   * @param y Top-left y position in framebuffer coordinates
   * @param w Width in framebuffer coordinates
   * @param h Height in framebuffer coordinates
   */
  public View(GLX glx, int x, int y, int w, int h) {
    this.glx = glx;
    this.viewId = 0;

    this.x = x;
    this.y = 0;
    this.width = w;
    this.height = h;

    // Set up model, view and projection matrices
    this.viewMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.projectionMatrixBuf = MemoryUtil.memAllocFloat(16);
    this.viewMatrix.get(this.viewMatrixBuf);
    this.projectionMatrix.get(this.projectionMatrixBuf);
  }

  public short getId() {
    return this.viewId;
  }

  public View setId(short viewId) {
    this.viewId = viewId;
    return this;
  }

  public View bind(short viewId) {
    setId(viewId);
    return bind();
  }

  public View bind() {

    // NOTE(mcslee): HACK! bgfx_reset_view disappeared from the C API, working on getting it back
    // but in the meantime we need these calls - figured out what it was doing from here:
    // https://github.com/bkaradzic/bgfx/blob/master/src/bgfx_p.h#L1826
    // bgfx_reset_view(this.viewId);
    bgfx_set_view_scissor(this.viewId, 0, 0, 0, 0);
    bgfx_set_view_mode(this.viewId, BGFX_VIEW_MODE_DEFAULT);
    bgfx_set_view_frame_buffer(this.viewId, BGFX_INVALID_HANDLE);

    // This is the actual code we want, actually GLX specific
    bgfx_set_view_rect(this.viewId, this.x, this.y, this.width, this.height);
    bgfx_set_view_clear(this.viewId, this.clearFlags, this.clearColor, this.clearDepth, 0);
    bgfx_set_view_transform(this.viewId, this.viewMatrixBuf, this.projectionMatrixBuf);

    return this;
  }

  public View touch() {
    bgfx_touch(this.viewId);
    return this;
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  public float getAspectRatio() {
    return this.width / (float) this.height;
  }

  public Matrix4f getViewMatrix() {
    return this.viewMatrix;
  }

  public Matrix4f getProjectionMatrix() {
    return this.projectionMatrix;
  }

  public View setCamera(Vector3f eye, Vector3f center, Vector3f up) {
    this.viewMatrix.setLookAtLH(eye, center, up);
    this.viewMatrix.get(this.viewMatrixBuf);
    return this;
  }

  public View setOrthographic(float x1, float x2, float y1, float y2, float z1, float z2) {
    this.projectionMatrix.setOrthoLH(x1, x2, y1, y2, z1, z2, this.glx.zZeroToOne);
    this.projectionMatrix.get(this.projectionMatrixBuf);
    return this;
  }

  public View setPerspective(float radians, float aspectRatio, float zNear, float zFar) {
    this.projectionMatrix.setPerspectiveLH(radians, aspectRatio, zNear, zFar, this.glx.zZeroToOne);
    this.projectionMatrix.get(this.projectionMatrixBuf);
    return this;
  }

  public View setScreenOrtho() {
    return setScreenOrtho(this.width, this.height);
  }

  public View setScreenOrtho(float width, float height) {
    this.viewMatrix.identity();
    this.viewMatrix.get(this.viewMatrixBuf);
    if (this.glx.isOpenGL()) {
      this.projectionMatrix.setOrtho(0, width, 0, height, -1, 1);
    } else {
      this.projectionMatrix.setOrthoLH(0, width, height, 0, -1, 1, this.glx.zZeroToOne);
    }
    this.projectionMatrix.get(this.projectionMatrixBuf);
    return this;
  }

  /**
   * Sets the coordinates of this view in framebuffer coordinate space, independent of content
   * scaling.
   *
   * @param x Top-left x in framebuffer pixels
   * @param y Top-left y in framebuffer pixels
   * @param width Width in framebuffer pixels
   * @param height Height in framebuffer pixels
   * @return this
   */
  public View setRect(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    return this;
  }

  public View setClearColor(int rgba) {
    this.clearColor = rgba;
    return this;
  }

  public View setClearFlags(int clearFlags) {
    this.clearFlags = clearFlags;
    return this;
  }

  public View setClearDepth(float clearDepth) {
    this.clearDepth = clearDepth;
    return this;
  }

  /**
   * Renders the given 2d context into this view
   *
   * @param context rendered 2d view
   * @return this
   */
  public View image(UI2dContext context) {
    if (this.glx.isOpenGL()) {
      // NOTE: buncha hacks here. OpenGL/Mac seems to have already taken framebuffer scaling into
      // acccount, so we just correct for UI zooming and the fact that Y is up on OpenGL framebuffers
      this.glx.program.tex2d.submit(
        this,
        bgfx_get_texture(context.getTexture(), 0),
        context.getX() * this.glx.uiZoom,
        getHeight() / glx.getSystemContentScaleY() - context.getY() * this.glx.uiZoom,
        context.getWidth() * this.glx.uiZoom,
        -context.getHeight() * this.glx.uiZoom
      );
    } else {
      // NOTE: context coordinates are in UI coordinate space. But the tex2d program
      // is in framebuffer coordinate space. We need to scale the bounds by the content
      // scale factor here to move from context's UI-space to framebuffer-space
      this.glx.program.tex2d.submit(
        this,
        bgfx_get_texture(context.getTexture(), 0),
        context.getX() * this.glx.getUIContentScaleX(),
        context.getY() * this.glx.getUIContentScaleY(),
        context.getWidth() * this.glx.getUIContentScaleX(),
        context.getHeight() * this.glx.getUIContentScaleY()
      );
    }
    return this;
  }

  public void dispose() {
    MemoryUtil.memFree(this.viewMatrixBuf);
    MemoryUtil.memFree(this.projectionMatrixBuf);
  }
}
