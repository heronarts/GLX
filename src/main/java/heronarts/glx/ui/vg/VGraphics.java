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

package heronarts.glx.ui.vg;

import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGLUFramebufferBGFX;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.GLXUtils;
import heronarts.glx.View;
import heronarts.glx.ui.UI2dContext;
import heronarts.glx.ui.UIColor;
import heronarts.lx.color.LXColor;

import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGBGFX.*;

/**
 * Pretty much just a wrapper around the LWJGL NanoVGBGFX library, that makes it a bit more
 * idiomatic java style and doesn't require the API client to keep track of the NanoVG context
 * handle or prefix everything with nvg. Also makes for simpler method chaining calls.
 */
public class VGraphics {

  public static enum Winding {
    CCW(NVG_CCW),
    CW(NVG_CW);

    private final int raw;

    private Winding(int raw) {
      this.raw = raw;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public static enum Align {
    LEFT(NVG_ALIGN_LEFT, true),
    CENTER(NVG_ALIGN_CENTER, true),
    RIGHT(NVG_ALIGN_RIGHT, true),
    TOP(NVG_ALIGN_TOP, false),
    MIDDLE(NVG_ALIGN_MIDDLE, false),
    BOTTOM(NVG_ALIGN_BOTTOM, false),
    BASELINE(NVG_ALIGN_BASELINE, false);

    private final int raw;
    private boolean isHorizontal;

    private Align(int raw, boolean isHorizontal) {
      this.raw = raw;
      this.isHorizontal = isHorizontal;
    }

    public boolean isHorizontal() {
      return this.isHorizontal;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public static enum LineJoin {
    MITER(NVG_MITER),
    ROUND(NVG_ROUND),
    BEVEL(NVG_BEVEL);

    private final int raw;

    private LineJoin(int raw) {
      this.raw = raw;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public static enum LineCap {
    BUTT(NVG_BUTT),
    ROUND(NVG_ROUND),
    SQUARE(NVG_SQUARE);

    private final int raw;

    private LineCap(int raw) {
      this.raw = raw;
    }

    public int asInt() {
      return this.raw;
    }
  }

  public class Font {
    public final int id;
    public final String name;
    public float size = 10;
    private final ByteBuffer fontData;

    private Font(int id, String name, ByteBuffer fontData) {
      this.id = id;
      this.name = name;
      this.fontData = fontData;
    }

    public Font fontSize(float size) {
      this.size = size;
      return this;
    }

    public void dispose() {
      MemoryUtil.memFree(this.fontData);
    }
  }

  public class Image {
    public final int id;
    public final int width;
    public final int height;
    public final Paint paint;
    private final NVGColor tint = NVGColor.create();
    private final ByteBuffer imageData;
    private final boolean isRgbData;

    private Image(int id, ByteBuffer imageData, int w, int h, boolean is2x, boolean isRgbData) {
      this.id = id;
      this.imageData = imageData;
      if (is2x) {
        w /= 2;
        h /= 2;
      }
      this.width = w;
      this.height = h;
      this.paint = imagePattern(0, 0, w, h, id);
      this.isRgbData = isRgbData;
      noTint();
    }

    private static ByteBuffer bufferARGB(ByteBuffer rgbaData, int[] argb) {
      for (int color : argb) {
        rgbaData.putInt(LXColor.toABGR(color));
      }
      rgbaData.flip();
      return rgbaData;
    }

    /**
     * Updates the data in an image created from an ARGB data buffer. Can not be
     * called on Images that were created by loading a file.
     *
     * @param argb ARGB data array, must have length image.width x image.height
     */
    public void update(int[] argb) {
      if (!this.isRgbData) {
        throw new IllegalStateException("Cannot update image not created with raw RGB data");
      }
      if (argb.length != (this.width * this.height)) {
        throw new IllegalArgumentException("ARGB array length (" + argb.length + ") doesn't match width(" + this.width + ") x height(" + this.height +")");
      }
      nvgUpdateImage(vg, this.id, bufferARGB(this.imageData, argb));
    }

    public void noTint() {
      setTint(1f, 1f, 1f, 1f);
    }

    public void setTint(UIColor color) {
      setTint(color.get());
    }

    public void setTint(int argb) {
      setTint(
        (0xff & (argb >>> 16)) / 255f,
        (0xff & (argb >>> 8)) / 255f,
        (0xff & argb) / 255f,
        (argb >>> 24) / 255f
      );
    }

    public void setTint(float r, float g, float b, float a) {
      this.tint.r(r);
      this.tint.g(g);
      this.tint.b(b);
      this.tint.a(a);
    }

    public void dispose() {
      nvgDeleteImage(vg, this.id);
      MemoryUtil.memFree(this.imageData);
    }
  }

  public class Paint {

    private final NVGPaint paint = NVGPaint.create();

    public Paint imagePattern(float ox, float oy, float ex, float ey, int image) {
      return imagePattern(ox, oy, ex, ey, 1f, image);
    }

    public Paint imagePattern(float ox, float oy, float ex, float ey, float alpha, int image) {
      nvgImagePattern(vg, ox, oy, ex, ey, 0, image, alpha, this.paint);
      return this;
    }
  }

  public class Framebuffer {
    private final UI2dContext context;
    private NVGLUFramebufferBGFX buffer = null;
    private final Paint paint = new Paint();

    // NOTE: width and height are in UI pixel space
    private float width;
    private float height;

    private short viewId;
    private final int imageFlags;
    private boolean isStale = true;

    public Framebuffer(UI2dContext context, float w, float h, int imageFlags) {
      this.context = context;
      this.width = w;
      this.height = h;
      this.imageFlags = imageFlags;
      this.viewId = 0;
    }

    public Framebuffer markStale() {
      this.isStale = true;
      return this;
    }

    public boolean isStale() {
      return this.isStale;
    }

    public Paint getPaint() {
      if (this.buffer == null) {
        GLX.error(
          "Cannot use VGraphics.Framebuffer.getPaint() before initialize() - " +
          this.context.getDebugClassHierarchy()
        );
      }
      return this.paint;
    }

    public int getHandle() {
      if (this.buffer == null) {
        throw new IllegalStateException("Cannot use VGraphics.Framebuffer.getHandle() before initialize()");
      }
      return this.buffer.handle();
    }

    public Framebuffer setView(short viewId) {
      this.viewId = viewId;
      return this;
    }

    public Framebuffer initialize() {
      if (this.isStale) {
        GLX.error(new Exception(), "Framebuffer had to initialize itself before a bind() call ever occurred - " + this.context.getDebugClassHierarchy());
        rebuffer();
        this.isStale = false;
      }
      return this;
    }

    public Framebuffer bind() {
      if (this.isStale) {
        rebuffer();
      }
      view.bind(this.viewId);
      nvgluSetViewFramebuffer(this.viewId, this.buffer);
      nvgluBindFramebuffer(this.buffer);
      return this;
    }

    public void markForResize(float w, float h) {
      if (this.width != w || this.height != h) {
        this.width = w;
        this.height = h;
        markStale();
      }
    }

    private void rebuffer() {
      if (this.buffer != null) {
        nvgluDeleteFramebuffer(this.buffer);
      }

      // NOTE: the framebuffer needs to be in framebuffer pixel space!
      // So we multiply our floating-point ui pixel dimensions by the
      // scaling factor and we round up to the next integer to make sure
      // we've got enough framebuffer pixels to cover it! Note that this
      // extra sub-pixel is okay, see the nvgBeginFrame() call where
      // the actual frame size is passed as a float.
      this.buffer = nvgluCreateFramebuffer(vg,
        (int) Math.ceil(this.width * glx.getUIContentScaleX()),
        (int) Math.ceil(this.height * glx.getUIContentScaleY()),
        this.imageFlags
      );

      if (this.buffer == null) {
        throw new RuntimeException("nvgluCreateFramebuffer failed!! Not a good situation.");
      }

      // Note what happens here... the framebuffer is in framebuffer-pixel space. But
      // when we're going to paint it into another UI2dContext, those pixels will be in
      // UI-space. So the paint image pattern is in UI-space width/height

      this.paint.imagePattern(0, 0, this.width, glx.isOpenGL() ? -this.height : this.height, this.buffer.image());

      this.isStale = false;
    }

    public void dispose() {
      if (this.buffer != null) {
        nvgluDeleteFramebuffer(this.buffer);
      }
      this.buffer = null;
    }

  }

  private final GLX glx;
  private final View view;
  private final long vg;
  private final NVGPaint paintLinearGradient = NVGPaint.create();
  private final NVGColor fillColorLinearGradientStart = NVGColor.create();
  private final NVGColor fillColorLinearGradientEnd = NVGColor.create();
  private final NVGColor fillColor = NVGColor.create();
  private final NVGColor strokeColor = NVGColor.create();
  private final Set<Framebuffer> allocatedBuffers = new HashSet<Framebuffer>();

  public VGraphics(GLX glx) {
    this.glx = glx;
    this.vg = nvgCreate(true, 0, NULL);
    this.view = new View(glx);
    this.view.setClearFlags(BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL);
  }

  public long getHandle() {
    return this.vg;
  }

  public Framebuffer createFramebuffer(UI2dContext context, float w, float h, int imageFlags) {
    Framebuffer framebuffer = new Framebuffer(context, w, h, imageFlags);
    this.allocatedBuffers.add(framebuffer);
    return framebuffer;
  }

  public void bindFramebuffer(Framebuffer framebuffer) {
    framebuffer.bind();
  }

  public void deleteFrameBuffer(Framebuffer framebuffer) {
    nvgluDeleteFramebuffer(framebuffer.buffer);
    this.allocatedBuffers.remove(framebuffer);
  }

  public void notifyContentScaleChanged() {
    for (Framebuffer framebuffer : this.allocatedBuffers) {
      framebuffer.markStale();
    }
  }

  private void _setColor(NVGColor color, int argb) {
    _setColor(color,
      (0xff & (argb >>> 16)) / 255f,
      (0xff & (argb >>> 8)) / 255f,
      (0xff & argb) / 255f,
      (argb >>> 24) / 255f
    );
  }

  private void _setColor(NVGColor color, float r, float g, float b, float a) {
    color.a(a);
    color.r(r);
    color.g(g);
    color.b(b);
  }

  public VGraphics fillLinearGradient(float sx, float sy, float ex, float ey, int scolor, int ecolor) {
    _setColor(this.fillColorLinearGradientStart, scolor);
    _setColor(this.fillColorLinearGradientEnd, ecolor);
    nvgLinearGradient(this.vg, sx, sy, ex, ey, this.fillColorLinearGradientStart, this.fillColorLinearGradientEnd, this.paintLinearGradient);
    nvgFillPaint(this.vg, this.paintLinearGradient);
    return this;
  }

  public VGraphics fillColor(UIColor color) {
    return fillColor(color.get());
  }

  public VGraphics fillColor(int argb) {
    return fillColor(
      (0xff & (argb >>> 16)) / 255f,
      (0xff & (argb >>> 8)) / 255f,
      (0xff & argb) / 255f,
      (argb >>> 24) / 255f
    );
  }

  public VGraphics fillColor(float r, float g, float b) {
    return fillColor(r, g, b, 1f);
  }

  public VGraphics fillColor(float r, float g, float b, float a) {
    _setColor(this.fillColor, r, g, b, a);
    nvgFillColor(this.vg, this.fillColor);
    return this;
  }

  public VGraphics fillPaint(Paint paint) {
    nvgFillPaint(this.vg, paint.paint);
    return this;
  }

  public VGraphics strokeColor(UIColor color) {
    return strokeColor(color.get());
  }

  public VGraphics strokeColor(int argb) {
    return strokeColor(
      (0xff & (argb >>> 16)) / 255f,
      (0xff & (argb >>> 8)) / 255f,
      (0xff & argb) / 255f,
      (argb >>> 24) / 255f
    );
  }

  public VGraphics strokeWidth(float strokeWidth) {
    nvgStrokeWidth(this.vg, strokeWidth);
    return this;
  }

  public VGraphics strokeColor(float r, float g, float b) {
    return strokeColor(r, g, b, 1f);
  }

  public VGraphics strokeColor(float r, float g, float b, float a) {
    this.strokeColor.a(a);
    this.strokeColor.r(r);
    this.strokeColor.g(g);
    this.strokeColor.b(b);
    nvgStrokeColor(this.vg, this.strokeColor);
    return this;
  }

  public VGraphics lineJoin(LineJoin lineJoin) {
    nvgLineJoin(this.vg, lineJoin.asInt());
    return this;
  }

  public VGraphics lineCap(LineCap lineCap) {
    nvgLineCap(this.vg, lineCap.asInt());
    return this;
  }

  public VGraphics beginFrame(float width, float height) {
    // NOTE: The nvgBeginFrame call wants width and height in
    // post-scaled framebuffer space, and it also needs
    // to know what the content scaling factor is. It only
    // takes one scaling factor so let's just hope for the
    // best if X/Y scaling are unequal on some weird system...
    nvgBeginFrame(
      this.vg,
      width, // * this.glx.getUIContentScaleX(),
      height, // * this.glx.getUIContentScaleY(),
      this.glx.getUIContentScaleX()
    );
    return this;
  }

  public VGraphics endFrame() {
    nvgEndFrame(this.vg);
    return this;
  }

  public VGraphics beginPath() {
    nvgBeginPath(this.vg);
    return this;
  }

  public VGraphics closePath() {
    nvgClosePath(this.vg);
    return this;
  }

  public VGraphics pathWinding(Winding dir) {
    nvgPathWinding(this.vg, dir.asInt());
    return this;
  }

  public VGraphics moveTo(float x, float y) {
    nvgMoveTo(this.vg, x, y);
    return this;
  }

  public VGraphics line(float x1, float y1, float x2, float y2) {
    moveTo(x1, y1);
    return lineTo(x2, y2);
  }

  public VGraphics lineTo(float x, float y) {
    nvgLineTo(this.vg, x, y);
    return this;
  }

  public VGraphics bezierTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
    nvgBezierTo(this.vg, c1x, c1y, c2x, c2y, x, y);
    return this;
  }

  public VGraphics quadTo(float cx, float cy, float x, float y) {
    nvgQuadTo(this.vg, cx, cy, x, y);
    return this;
  }

  public VGraphics arcTo(float x1, float y1, float x2, float y2, float radius) {
    nvgArcTo(this.vg, x1, y1, x2, y2, radius);
    return this;
  }

  public VGraphics arc(float cx, float cy, float r, float a0, float a1) {
    return arc(cx, cy, r, a0, a1, Winding.CW);
  }

  public VGraphics arc(float cx, float cy, float r, float a0, float a1, Winding dir) {
    nvgArc(this.vg, cx, cy, r, a0, a1, dir.asInt());
    return this;
  }

  public VGraphics beginPathMoveToArcFill(float cx, float cy, float r, float a0, float a1) {
    if (a1 - a0 > Math.PI) {
      // NOTE(bugfix): there's a bug on windows when contentscale is applied.
      // if the arc spans over PI (180degrees) and becomes convex, then the content
      // scale becomes lost and the shape is drawn in the wrong place. will have
      // to figure this out some other time, for now, breaking into two arcs fixes it
      float aHack = a0 + (a1-a0)/2;
      beginPath();
      moveTo(cx, cy);
      arc(cx, cy, r, a0, aHack);
      fill();
      beginPath();
      moveTo(cx, cy);
      arc(cx, cy, r, aHack - .1f, a1);
      fill();
      return this;
    }
    beginPath();
    moveTo(cx, cy);
    arc(cx, cy, r, a0, a1);
    fill();
    return this;
  }

  public VGraphics rect(float x, float y, float w, float h) {
    nvgRect(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics rect(float x, float y, float w, float h, float r) {
    if (r > 0) {
      return roundedRect(x, y, w, h, r);
    }
    return rect(x, y, w, h);
  }

  public VGraphics roundedRect(float x, float y, float w, float h, float r) {
    nvgRoundedRect(this.vg, x, y, w, h, r);
    return this;
  }

  public VGraphics roundedRectVarying(float x, float y, float w, float h, float radTopLeft, float radTopRight, float radBottomRight, float radBottomLeft) {
    nvgRoundedRectVarying(this.vg, x, y, w, h, radTopLeft, radTopRight, radBottomRight, radBottomLeft);
    return this;
  }

  public VGraphics ellipse(float cx, float cy, float rx, float ry) {
    nvgEllipse(this.vg, cx, cy, rx, ry);
    return this;
  }

  public VGraphics circle(float cx, float cy, float r) {
    nvgCircle(this.vg, cx, cy, r);
    return this;
  }

  public VGraphics image(Image image, float x, float y) {
    return image(image, x, y, 1f);
  }

  public VGraphics image(Image image, float x, float y, float alpha) {
    image.paint.imagePattern(x, y, image.width, image.height, alpha, image.id);
    image.paint.paint.innerColor(image.tint);
    fillPaint(image.paint);
    return rect(x, y, image.width, image.height);
  }

  public Paint imagePattern(float ox, float oy, float ex, float ey, int image) {
    return imagePattern(ox, oy, ex, ey, 1f, image);
  }

  public Paint imagePattern(float ox, float oy, float ex, float ey, float alpha, int image) {
    return new Paint().imagePattern(ox, oy, ex, ey, alpha, image);
  }

  public VGraphics fill() {
    nvgFill(this.vg);
    return this;
  }

  public VGraphics stroke() {
    nvgStroke(this.vg);
    return this;
  }

  /**
   * Creates an image from an array of colors
   *
   * @param argb Array of ARGB values
   * @param width Width of image
   * @param height Height of image
   * @return Image object suitable for VGraphics painting
   */
  public Image createImageARGB(int[] argb, int width, int height) {
    if (argb.length != (width * height)) {
      throw new IllegalArgumentException("ARGB array length (" + argb.length + ") doesn't match width(" + width + ") x height(" + height +")");
    }
    ByteBuffer rgbaData = Image.bufferARGB(MemoryUtil.memAlloc(argb.length * 4), argb);
    int image = nvgCreateImageRGBA(this.vg, width, height, 0, rgbaData);
    return new Image(image, rgbaData, width, height, false, true);
  }

  public Image loadImage(String imagePath) throws IOException {
    return createImageMem(GLXUtils.loadResource("images/" + imagePath), imagePath.contains("@2x."));
  }

  public Image loadIcon(String iconPath) throws IOException {
    return createImageMem(GLXUtils.loadResource("icons/" + iconPath), iconPath.contains("@2x."));
  }

  public Font loadFont(String fontName, String fontPath) throws IOException {
    return createFontMem(fontName, GLXUtils.loadResource("fonts/" + fontPath));
  }

  private Font createFontMem(String name, ByteBuffer fontData) {
    int font = nvgCreateFontMem(this.vg, name, fontData, 0);
    return new Font(font, name, fontData);
  }

  private Image createImageMem(ByteBuffer imageData, boolean is2x) {
    int image = nvgCreateImageMem(this.vg, 0, imageData);
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      nvgImageSize(this.vg, image, width, height);
      return new Image(image, imageData, width.get(0), height.get(0), is2x, false);
    }
  }

  public VGraphics fontSize(float size) {
    nvgFontSize(this.vg, size);
    return this;
  }

  public VGraphics fontBlur(float blur) {
    nvgFontBlur(this.vg, blur);
    return this;
  }

  public VGraphics textLetterSpacing(float spacing) {
    nvgTextLetterSpacing(this.vg, spacing);
    return this;
  }

  public VGraphics textLineHeight(float lineHeight) {
    nvgTextLineHeight(this.vg, lineHeight);
    return this;
  }

  public VGraphics textAlign(Align horizontal) {
    return textAlign(horizontal, Align.BASELINE);
  }

  public VGraphics textAlign(Align horizontal, Align vertical) {
    if (!horizontal.isHorizontal()) {
      throw new IllegalArgumentException("Cannot set horizontal alignment to vertical value: " + horizontal);
    }
    if (vertical.isHorizontal()) {
      throw new IllegalArgumentException("Cannot set vertical alignment to horizontal value: " + vertical);
    }
    nvgTextAlign(this.vg, horizontal.asInt() | vertical.asInt());
    return this;
  }

  public VGraphics fontFace(Font font) {
    nvgFontFaceId(this.vg, font.id);
    nvgFontSize(this.vg, font.size);
    return this;
  }

  public float text(float x, float y, String str) {
    return nvgText(this.vg, x, y, str);
  }

  public VGraphics textBox(float x, float y, float breakRowWidth, String str) {
    nvgTextBox(this.vg, x, y, breakRowWidth, str);
    return this;
  }

  public float textBoxHeight(String str, float width) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      FloatBuffer bounds = stack.mallocFloat(4);
      nvgTextBoxBounds(this.vg, 0, 0, width, str, bounds);
      return bounds.get(3);
    }
  }

  public float textWidth(String str) {
    return nvgTextBounds(this.vg, 0, 0, str, (FloatBuffer) null);
  }

  public VGraphics translate(float tx, float ty) {
    nvgTranslate(this.vg, tx, ty);
    return this;
  }

  public VGraphics rotate(float angle) {
    nvgRotate(this.vg, angle);
    return this;
  }

  public VGraphics scissor(float x, float y, float w, float h) {
    nvgScissor(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics intersectScissor(float x, float y, float w, float h) {
    nvgIntersectScissor(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics reset() {
    nvgReset(this.vg);
    return this;
  }

  public VGraphics resetScissor() {
    nvgResetScissor(this.vg);
    return this;
  }

  public VGraphics scissorPush(float x, float y, float w, float h) {
    nvgSave(this.vg);
    nvgIntersectScissor(this.vg, x, y, w, h);
    return this;
  }

  public VGraphics scissorPop() {
    nvgRestore(this.vg);
    return this;
  }

  public VGraphics save() {
    nvgSave(this.vg);
    return this;
  }

  public VGraphics restore() {
    nvgRestore(this.vg);
    return this;
  }

}
