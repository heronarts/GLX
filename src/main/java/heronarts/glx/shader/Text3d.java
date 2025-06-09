/**
 * Copyright 2025- Mark C. Slee, Heron Arts LLC
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
 * @author Zranger1
 */

package heronarts.glx.shader;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.joml.Vector3f;
import static org.lwjgl.bgfx.BGFX.*;

import heronarts.glx.GLX;
import heronarts.glx.GLXUtils;
import heronarts.glx.Texture;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.lx.color.LXColor;
import heronarts.lx.utils.LXUtils;

public class Text3d extends ShaderProgram {

  public static final char MIN_CHAR = 32;
  public static final char MAX_CHAR = 255;
  public static final int NUM_CHARS = MAX_CHAR - MIN_CHAR + 1;

  public enum TextFont {
    NORMAL("Inter-SemiBold"),
    BOLD("Inter-Black");

    private String name;

    private TextFont(String name) {
      this.name = name;
    }
  }

  public enum TextOrientation {
    /**
     * Text is oriented as placed in world-space
     */
    WORLD,

    /**
     * Text is oriented always facing the camera
     */
    CAMERA;
  }

  public enum TextScale {
    /**
     * Text size is defined relative to the world objects
     */
    WORLD,

    /**
     * Text size is defined in scaled UI screen pixels
     */
    PIXELS;
  }

  public enum HorizontalAlignment {
    LEFT(0),
    CENTER(-.5f),
    RIGHT(-1f);

    private final float offset;

    private HorizontalAlignment(float offset) {
      this.offset = offset;
    }
  }

  public enum VerticalAlignment {
    BOTTOM,
    BASELINE,
    MIDDLE,
    TOP;

    private float getOffset(Text3d.FontTexture font) {
      return switch(this) {
        case BOTTOM -> 0;
        case BASELINE -> -font.descent;
        case MIDDLE -> -.5f;
        case TOP -> -1;
      };
    }
  }

  public class Label {

    private boolean visible = true;
    public final String label;
    private final Vector3f textPosition = new Vector3f();
    private TextOrientation textOrientation = TextOrientation.WORLD;
    private TextScale textScale = TextScale.WORLD;
    private float textSize = 10;
    private int textColorARGB = LXColor.WHITE;
    private int backgroundColorARGB = LXColor.CLEAR;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
    private VerticalAlignment verticalAlignment = VerticalAlignment.BOTTOM;
    private VertexBuffer vertexBuffer = null;
    private boolean depthTest = false;
    private final FontTexture font;

    private Label(String label, FontTexture font) {
      this.label = label;
      this.font = font;
    }

    public boolean isVisible() {
      return this.visible;
    }

    public Label setVisible(boolean visible) {
      this.visible = visible;
      return this;
    }

    /**
     * Position of the label in world-space
     *
     * @param position Position in world-space
     * @return this
     */
    public Label setPosition(Vector3f position) {
      this.textPosition.set(position);
      return this;
    }

    /**
     * Position of the label in world-space
     *
     * @param position Position in world-space
     * @return this
     */
    public Label setPosition(float x, float y, float z) {
      this.textPosition.set(x, y, z);
      return this;
    }

    public Label setOrientation(TextOrientation textOrientation) {
      this.textOrientation = textOrientation;
      return this;
    }

    /**
     * Sets the text size, which is either in world-units or scaled UI pixel units
     * according to the TextScale setting
     *
     * @param textSize Text size, interpreted according to TextScale
     * @return
     */
    public Label setTextSize(float textSize) {
      this.textSize = textSize;
      return this;
    }

    public Label setTextScale(TextScale textScale) {
      this.textScale = textScale;
      return this;
    }

    public Label setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
      this. horizontalAlignment =  horizontalAlignment;
      return this;
    }

    public Label setVerticalAlignment(VerticalAlignment verticalAlignment) {
      this.verticalAlignment = verticalAlignment;
      return this;
    }

    public Label setDepthTest(boolean depthTest) {
      this.depthTest = depthTest;
      return this;
    }

    public Label setTextColorARGB(int textColorARGB) {
      this.textColorARGB = textColorARGB;
      return this;
    }

    public Label setBackgroundColorARGB(int backgroundColorARGB ) {
      this.backgroundColorARGB = backgroundColorARGB;
      return this;
    }

    public abstract class VertexBuffer extends heronarts.glx.VertexBuffer {
      protected float width;

      private VertexBuffer(char[] chars) {
        super(glx, chars.length * 6, VertexDeclaration.Attribute.POSITION, VertexDeclaration.Attribute.TEXCOORD0);
      }

      public float getWidth() {
        return this.width;
      }
    }

    private void _initVertexBuffer(UI ui) {
      if (this.vertexBuffer != null) {
        return;
      }
      final char[] chars = this.label.toCharArray();
      this.vertexBuffer = new VertexBuffer(chars) {
        @Override
        protected void bufferData(ByteBuffer buffer) {
          float x = 0;

          for (char ch : chars) {
            if (!LXUtils.inRange(ch, MIN_CHAR, MAX_CHAR)) {
              GLX.error("Invalid character codepoint in UI3dLabels.Label: " + (int) ch);
              ch = ' ';
            }
            final FontTexture.GlyphMetrics metrics = font.glyphMetrics[ch - MIN_CHAR];
            final float tx0 = metrics.x / font.atlasWidth;
            final float tx1 = tx0 + metrics.width / font.atlasWidth;
            final float ty0 = 1 - metrics.y / font.atlasHeight;
            final float ty1 = ty0 - metrics.height / font.atlasHeight;

            final float charWidth = metrics.width / font.atlasHeight;
            final float charHeight = metrics.height / font.atlasHeight;

            putVertex(x, 0, 0);
            putTex2d(tx0, ty0);

            putVertex(x + charWidth, 0, 0);
            putTex2d(tx1, ty0);

            putVertex(x, charHeight, 0);
            putTex2d(tx0, ty1);

            putVertex(x, charHeight, 0);
            putTex2d(tx0, ty1);

            putVertex(x + charWidth, 0, 0);
            putTex2d(tx1, ty0);

            putVertex(x + charWidth, 0 + charHeight, 0);
            putTex2d(tx1, ty1);

            x += charWidth;
          }
          this.width = x;
        }
      };
    }

    private final static long TEXT_BGFX_STATE = 0
      | BGFX_STATE_WRITE_RGB
      | BGFX_STATE_WRITE_Z
      | BGFX_STATE_WRITE_A
      | BGFX_STATE_BLEND_ALPHA
      ;

    public void draw(UI ui, View view) {
      if (this.visible) {
        _initVertexBuffer(ui);
        long state = TEXT_BGFX_STATE;
        if (this.depthTest) {
          state |= BGFX_STATE_DEPTH_TEST_LESS;
        }
        setLabel(this).submit(view, state, this.vertexBuffer);
      }
    }

    public void dispose() {
      if (this.vertexBuffer != null) {
        this.vertexBuffer.dispose();
      }
    }
  }

  private class FontTexture {

    private class GlyphMetrics {

      public final float width; // width of glyph in pixels
      public final float height; // height of glyph in pixels
      public final float x; // x offset of glyph in texture atlas
      public final float y; // y offset of glyph in texture atlas

      private GlyphMetrics(float width, float height, float x, float y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
      }
    }

    private final TextFont textFont;

    private final float descent;

    private final float atlasWidth;
    private final float atlasHeight;

    private final GlyphMetrics[] glyphMetrics = new GlyphMetrics[NUM_CHARS];

    private final Texture texture;

    private FontTexture(TextFont textFont) {
      this.textFont = textFont;

      try (InputStream resource = GLXUtils.loadResourceStream("fonts/" + textFont.name + ".font3d");
           DataInputStream is = new DataInputStream(resource)) {

        final int ascent = is.readInt();
        final int descent = is.readInt();
        final int leading = is.readInt();
        final int total = ascent + descent + leading;
        // this.ascent = ascent / (float) total;
        this.descent = descent / (float) total;
        // this.leading = leading / (float) total;

        final int glyphCount = is.readInt();
        final int texWidth = is.readInt();
        final int texHeight = is.readInt();
        this.atlasWidth = texWidth;
        this.atlasHeight = texHeight;

        // read glyph info and build glyph map
        for (int i = 0; i < glyphCount; i++) {
          int charWidth = is.readInt();
          int charHeight = is.readInt();
          int charX = is.readInt();
          int charY = is.readInt();
          this.glyphMetrics[i] = new GlyphMetrics(charWidth, charHeight, charX, charY);
        }
        // read image alpha texture data
        final byte[] imageData = new byte[texWidth * texHeight];
        is.read(imageData, 0, imageData.length);
        this.texture = new Texture(glx, texWidth, texHeight, BGFX_TEXTURE_FORMAT_A8, imageData);
      } catch (IOException iox) {
        throw new RuntimeException(iox);
      }
    }

    private void dispose() {
      this.texture.dispose();
    }
  }

  private final Uniform.Sampler uniformFontSampler;
  private final Uniform.Vec4f uniformTextPosition;
  private final Uniform.Vec4f uniformTextOffset;
  private final Uniform.Vec4f uniformTextMetrics;
  private final Uniform.Vec4f uniformTextColor;
  private final Uniform.Vec4f uniformBackgroundColor;

  private final FontTexture[] fontTextures = new FontTexture[TextFont.values().length];

  private int textColorARGB = LXColor.WHITE;
  private int backgroundColorARGB = LXColor.CLEAR;
  private final Vector3f textPosition = new Vector3f();
  private float textOffsetX = 0;
  private float textOffsetY = 0;
  private float textSize = 10;
  private TextOrientation textOrientation = TextOrientation.WORLD;
  private TextScale textScale = TextScale.WORLD;
  private TextFont textFont = TextFont.NORMAL;

  public Text3d(GLX glx) {
    super(glx, "vs_text3d", "fs_text3d");

    int fi = 0;
    for (TextFont textFont : TextFont.values()) {
      this.fontTextures[fi++] = new FontTexture(textFont);
    }

    this.uniformFontSampler = new Uniform.Sampler(glx, "s_font");
    this.uniformTextColor = new Uniform.Vec4f(glx, "u_textColor");
    this.uniformTextPosition = new Uniform.Vec4f(glx, "u_textPosition");
    this.uniformTextOffset = new Uniform.Vec4f(glx, "u_textOffset");
    this.uniformTextMetrics = new Uniform.Vec4f(glx, "u_textMetrics");
    this.uniformBackgroundColor = new Uniform.Vec4f(glx, "u_backgroundColor");

  }

  public Label createLabel(String text) {
    return createLabel(text, TextFont.NORMAL);
  }

  public Label createLabel(String text, TextFont textFont) {
    return new Label(text, getFontTexture(textFont));
  }

  private FontTexture getFontTexture(TextFont textFont) {
    return this.fontTextures[textFont.ordinal()];
  }

  public Text3d reset() {
    setTextColorARGB(LXColor.WHITE);
    setBackgroundColorARGB(LXColor.CLEAR);
    setTextPosition(0, 0, 0);
    setTextOffset(0, 0);
    setTextSize(10);
    setTextOrientation(TextOrientation.WORLD);
    setTextScale(TextScale.WORLD);
    setTextFont(TextFont.NORMAL);
    return this;
  }

  public Text3d setTextColorARGB(int textColorARGB) {
    this.textColorARGB = textColorARGB;
    return this;
  }

  public Text3d setBackgroundColorARGB(int backgroundColorARGB ) {
    this.backgroundColorARGB = backgroundColorARGB;
    return this;
  }

  public Text3d setTextPosition(Vector3f textPosition) {
    this.textPosition.set(textPosition);
    return this;
  }

  public Text3d setTextPosition(float x, float y, float z) {
    this.textPosition.set(x, y, z);
    return this;
  }

  public Text3d setTextSize(float textSize) {
    this.textSize = textSize;
    return this;
  }

  public Text3d setTextOffset(float textOffsetX, float textOffsetY) {
    this.textOffsetX = textOffsetX;
    this.textOffsetY = textOffsetY;
    return this;
  }

  public Text3d setTextOrientation(TextOrientation textOrientation) {
    this.textOrientation = textOrientation;
    return this;
  }

  public Text3d setTextScale(TextScale textScale) {
    this.textScale = textScale;
    return this;
  }

  public Text3d setLabel(Label label) {
    setTextColorARGB(label.textColorARGB);
    setBackgroundColorARGB(label.backgroundColorARGB);
    setTextPosition(label.textPosition);
    setTextFont(label.font.textFont);
    setTextOffset(
      label.horizontalAlignment.offset * label.vertexBuffer.getWidth(),
      label.verticalAlignment.getOffset(label.font)
    );
    setTextSize(label.textSize);
    setTextOrientation(label.textOrientation);
    setTextScale(label.textScale);
    return this;
  }

  private Text3d setTextFont(TextFont textFont) {
    this.textFont = textFont;
    return this;
  }

  @Override
  protected void setUniforms(View view) {
    this.uniformFontSampler.setTexture(0, getFontTexture(this.textFont).texture, BGFX_SAMPLER_NONE);
    this.uniformTextPosition.set(this.textPosition.x, this.textPosition.y, this.textPosition.z, this.textOrientation.ordinal());
    this.uniformTextOffset.set(this.textOffsetX, this.textOffsetY);
    this.uniformTextMetrics.set(this.textSize, this.textScale.ordinal(), view.getAspectRatio(), 2f / view.getHeight());
    this.uniformTextColor.setARGB(this.textColorARGB);
    this.uniformBackgroundColor.setARGB(this.backgroundColorARGB);
  }

  @Override
  public void dispose() {
    this.uniformFontSampler.dispose();
    this.uniformTextColor.dispose();
    this.uniformBackgroundColor.dispose();
    this.uniformTextOffset.dispose();
    this.uniformTextMetrics.dispose();
    for (FontTexture fontTexture : this.fontTextures) {
      fontTexture.dispose();
    }
    super.dispose();
  }

}
