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
 * @author ZRanger1
 */

package heronarts.glx.ui.text3d;


import heronarts.glx.shader.Text3d;
import heronarts.lx.color.LXColor;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * FontMaker - Texture Atlas Creation Tool
 *
 * <p>This is the simplest possible alpha-only texture atlas implementation. Very fast and
 * lightweight, not a lot of features.
 *
 * <p>Converts Truetype fonts to bitmap fonts with glyph information for each character, for use by
 * the heronarts.glx.shader.Text3d shader program
 *
 * <p>To use, just set the input and output filenames in main and run FontMaker from the IDE.
 */
public class FontMaker {

  /**
   * Create font atlas from TTF and write it to a file
   *
   * @param inPath input stream to truetype font file
   * @param outPath output stream to font atlas data file
   * @param size Font size
   * @throws FontFormatException if the font file doesn't contain the required font tables
   * @throws IOException if the file can't be read/written
   */
  public FontMaker(String inPath, String outPath, int size) throws FontFormatException, IOException {
    try (FileInputStream is = new FileInputStream(inPath);
          DataOutputStream os = new DataOutputStream(new FileOutputStream(outPath))) {
      final Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, size);
      buildFontAtlas(font, os);
    }
  }

  // create the actual font texture atlas
  private void buildFontAtlas(Font font, DataOutputStream os) throws IOException {
    int imageWidth = 0;
    int imageHeight = 0;

    // Create a small temporary image for font metrics for this character
    Graphics2D gMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    gMetrics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gMetrics.setFont(font);
    FontMetrics metrics = gMetrics.getFontMetrics();

    final BufferedImage[] characters = new BufferedImage[256];

    // make a pass through the font to determine overall width and height
    for (int i = Text3d.MIN_CHAR; i <= Text3d.MAX_CHAR; i++) {
      BufferedImage ch = createCharImage(font, metrics, (char) i);
      characters[i] = ch;
      imageWidth += ch.getWidth();
      imageHeight = Math.max(imageHeight, ch.getHeight());
    }

    // create an image for our output glyph set
    BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();

    // write header information to the file
    int glyphCount = Text3d.MAX_CHAR - Text3d.MIN_CHAR + 1;
    os.writeInt(metrics.getAscent());
    os.writeInt(metrics.getDescent());
    os.writeInt(metrics.getLeading());
    os.writeInt(glyphCount);
    os.writeInt(imageWidth);
    os.writeInt(imageHeight);

    float x = 0;

    // Generate glyphs for standard printable characters, starting with SPACE
    // (0-31 are control codes, so we skip them) and write them to the output file
    for (int i = Text3d.MIN_CHAR; i <= Text3d.MAX_CHAR; i++) {
      final BufferedImage charImage = characters[i];
      final float charWidth = charImage.getWidth();
      final float charHeight = charImage.getHeight();

      // draw character to atlas image
      g.drawImage(charImage, (int) x, 0, null);

      // Save character glyph info -- character size and position in texture atlas
      os.writeInt((int) charWidth);
      os.writeInt((int) charHeight);
      os.writeInt((int) x);
      os.writeInt((int) (image.getHeight() - charHeight));

      x += charWidth;
    }

    // get a copy of the full RGBA image data and use it to create
    // a compact alpha channel-only texture for use by the renderer.
    int width = image.getWidth();
    int height = image.getHeight();

    int[] pixels = new int[width * height];
    image.getRGB(0, 0, width, height, pixels, 0, width);

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        os.write((byte) (0xFF & LXColor.alpha(pixels[i * width + j])));
      }
    }

    gMetrics.dispose();
  }

  private BufferedImage createCharImage(Font font, FontMetrics metrics, char c) {

    int charWidth = metrics.charWidth(c);
    int charHeight = metrics.getHeight();

    // This should actually never happen...
    if (charWidth == 0) {
      throw new IllegalStateException("Character codepoint in font " + font.getName() + " has 0 width: " + (int) c);
    }

    // Create actual glyph image for this character
    final BufferedImage image = new BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setFont(font);
    g.setPaint(java.awt.Color.WHITE);
    g.drawString(String.valueOf(c), 0, metrics.getAscent());
    g.dispose();
    return image;
  }

  public static void main(String[] args) {
    final String[] fonts = {
      "Inter-SemiBold",
      "Inter-Black"
    };
    for (String font : fonts) {
      final String inFile = "src/main/resources/fonts/" + font + ".otf";
      final String outFile = "src/main/resources/fonts/" + font + ".font3d";
      final int sizeInPoints = 96;
      try {
        new FontMaker(inFile, outFile, sizeInPoints);
        System.out.println("Created font: " + outFile);
      } catch (Exception x) {
        System.out.println("FontMaker: Error creating font " + font + ": " + x.getMessage());
        x.printStackTrace();
      }
    }
    System.out.println("FontMaker: done!");
  }
}
