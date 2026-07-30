package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.utils.LXUtils;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.nanovg.NVGGlyphPosition;
import org.lwjgl.nanovg.NVGTextRow;

import java.awt.geom.Point2D;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * A text box that supports multi-line editing and word wrapping.
 */
public class UIMultiLineTextBox extends UITextBox {

  private static final char NEWLINE = '\n';

  private float lineHeight = 0;
  private int textMargin = 4;
  private int lineSpacing = 2;

  private TextBoxMap textBoxMap;

  private final List<String> lines = new ArrayList<>();

  public UIMultiLineTextBox(float w, float h) {
    this(0, 0, w, h);
  }

  public UIMultiLineTextBox(float x, float y, float w, float h) {
    this(x, y, w, h, null);
  }

  public UIMultiLineTextBox(float w, float h, StringParameter parameter) {
    this(0, 0, w, h, parameter);
  }

  public UIMultiLineTextBox(float x, float y, float w, float h, StringParameter parameter) {
    super(x, y, w, h);
    setParameter(parameter);
  }

  @Override
  public boolean isValidCharacter(char keyChar) {
    return keyChar == NEWLINE || super.isValidCharacter(keyChar);
  }

  @Override
  protected void drawText(UI ui,
                          VGraphics vg,
                          EditState editState,
                          String rawString,
                          boolean cursor,
                          VGraphics.Align textAlignHorizontal,
                          float x,
                          float y,
                          float width,
                          float height,
                          float availableWidth) {
    if (this.textBoxMap == null) {
      this.textBoxMap = new TextBoxMap();
    }
    this.textBoxMap.layout(vg, x, y, availableWidth);
    this.textBoxMap.setText(rawString);

    int editCursor = editState.cursor;
    int editRangeStart = editState.rangeStart;
    int editRangeEnd = editState.rangeEnd;

    // Text is drawn from baseline so we will offset Y by the ascender
    float[] textMetrics = vg.textMetrics();
    float ascender = textMetrics[0];
    // float descender = textMetrics[1];
    float lineHeight = textMetrics[2];
    this.lineHeight = lineHeight;

    // Draw all the text
    vg.beginPath();
    vg.textBox(TEXT_MARGIN + x, TEXT_MARGIN + ascender + y, availableWidth, rawString);
    vg.fill();

    // Highlight selection
    if (editRangeStart != editRangeEnd) {
      Point2D.Float startXY = this.textBoxMap.getCaretXY(vg, x, y, availableWidth, editRangeStart);
      Point2D.Float endXY = this.textBoxMap.getCaretXY(vg, x, y, availableWidth, editRangeEnd);

      if (startXY.getY() == endXY.getY()) {
        // Same line
        vg.beginPath();
        vg.fillColor(ui.theme.selectionColor.mask(100));
        vg.rect(TEXT_MARGIN + startXY.x,
          TEXT_MARGIN + startXY.y,
          endXY.x - startXY.x,
          lineHeight);
        vg.fill();

      } else {
        // Ends on a different line than it started

        // Draw rest of starting line
        vg.beginPath();
        vg.fillColor(ui.theme.selectionColor.mask(100));
        vg.rect(TEXT_MARGIN + startXY.x,
          TEXT_MARGIN + startXY.y,
          availableWidth - startXY.x,
          lineHeight);

        // Full highlighted lines
        if (startXY.y + lineHeight < endXY.y) {
          vg.rect(TEXT_MARGIN + startXY.x,
            TEXT_MARGIN + startXY.y,
            availableWidth,
            lineHeight);
        }
        vg.fill();
      }
    }

    // Get cursor position
    Point2D.Float caretXY = this.textBoxMap.getCaretXY(vg, x, y, availableWidth, editCursor);
    float cursorX = TEXT_MARGIN + caretXY.x;
    float cursorY = TEXT_MARGIN + caretXY.y;

    // Draw cursor
    if (cursor) {
      float cursorBrightness = editState.cursorBasis < .5f ? 1f : 0f;
      vg.beginPath();
      vg.strokeColor(ui.theme.editTextColor.mask((int) LXUtils.lerp(255, 0, cursorBrightness)));
      vg.strokeWidth(1);
      vg.moveTo(cursorX, cursorY);
      vg.lineTo(cursorX, cursorY + lineHeight);
      vg.stroke();
    }
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.editing) {
      // Shift + Enter: newline
      if (keyCode == KeyEvent.VK_ENTER && keyEvent.isShiftDown()) {
        editAppend("\n");
        keyEvent.consume();
        redraw();
      }

      // Regular Enter saves the text
      else if (keyCode == KeyEvent.VK_ENTER && !keyEvent.isShiftDown()) {
        // Let parent handle save
        super.onKeyPressed(keyEvent, keyChar, keyCode);
      }

      // Shift + Up: move selection end up one line

      // Shift + Down: move selection end down one line

      // Cmd + Shift + Up: select to beginning of text

      // Cmd + Shift + Down: select to end of text

      // Cmd + Shift + Left: select to start of line

      // Cmd + Shift + Right: select to end of line

      // Cmd + Left: move cursor left one word

      // Cmd + Right: move cursor right one word

    }

    if (!keyEvent.isConsumed()) {
      super.onKeyPressed(keyEvent, keyChar, keyCode);
    }
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    // Single click to start editing (override parent class' double-click behavior)
    if (this.enabled && !this.editing && this.editable) {
      if (mouseEvent.getButton() == MouseEvent.BUTTON_LEFT) {
        mouseEvent.consume();
        edit();
        redraw();
        return;
      }
    }
    super.onMousePressed(mouseEvent, mx, my);
  }

  @Override
  public void dispose() {
    // TODO: this doesn't work, this stuff got allocated on the UI thread by
    // nanoVG, disposing it from the LX thread can cause a crash when the
    // memory goes away before the UI thread is done with it.

    if (this.textBoxMap != null) {
      this.textBoxMap.dispose();
    }
    super.dispose();
  }

  /**
   * Returns the total height needed to display all wrapped lines
   *
   * @return Content height in pixels
   */
  public float getContentHeight() {
    int numLines = Math.max(1, this.lines.size());
    return 2 * textMargin + numLines * this.lineHeight + (numLines - 1) * lineSpacing;
  }

  private class TextBoxMap {

    private String text = "";
    private ByteBuffer ascii;     // direct backing store for NanoVG
    private long baseAddr;
    private int lenBytes;

    // cached layout inputs
    private float lastX, lastY, lastW;
    private boolean dirty = true;

    // growable direct buffers
    private NVGTextRow.Buffer rows;
    private NVGGlyphPosition.Buffer glyphs;
    private int rowsCap = 0;
    private int glyphsCap = 0;

    private final List<Row> layout = new ArrayList<>();

    public TextBoxMap() {
      ensureRows(16);
      ensureGlyphs(64);
      setText("");
    }

    public void setText(String t) {
      if (t == null) {
        t = "";
      }
      if (t.equals(this.text)) {
        return;
      }

      this.text = t;
      if (ascii != null) {
        memFree(ascii);
      }

      // ASCII-only assumption: 1 char == 1 byte.
      ascii = memASCII(t, false);
      baseAddr = memAddress(ascii);
      lenBytes = ascii.remaining();
      dirty = true;
    }

    /** Call if font size/face/spacing/lineHeight/etc changes on vg (affects wrapping/metrics). */
    public void markStyleDirty() {
      dirty = true;
    }

    public Point2D.Float getCaretXY(VGraphics vg, float x, float y, float breakWidth, int cursorIndex) {
      layout(vg, x, y, breakWidth);

      if (layout.isEmpty()) {
        return new Point2D.Float(x, y);
      }

      int caretByte = LXUtils.clamp(cursorIndex, 0, lenBytes);

      // find row containing caret
      Row row = layout.getLast();
      for (Row r : layout) {
        if (caretByte <= r.endByte) {
          row = r;
          break;
        }
      }

      // no glyphs => start of line
      if (row.glyphCount == 0) {
        return new Point2D.Float(x, row.y);
      }

      // at/after row end => measure actual text width (includes trailing spaces)
      if (caretByte >= row.endByte) {
        String sub = text.substring(row.startByte, Math.min(caretByte, lenBytes));
        return new Point2D.Float(x + vg.textWidth(sub), row.y);
      }

      // caret at start of first glyph whose byteOffset >= caretByte
      int gi = lowerBound(row.byteOffset, caretByte);
      if (gi >= row.glyphCount) {
        gi = row.glyphCount - 1;
      }
      return new Point2D.Float(row.x[gi], row.y);
    }

    private void layout(VGraphics vg, float x, float y, float breakWidth) {
      if (!dirty && x == lastX && y == lastY && breakWidth == lastW) {
        return;
      }
      lastX = x; lastY = y; lastW = breakWidth;
      dirty = false;

      layout.clear();
      if (lenBytes == 0) {
        return;
      }

      int nrows = breakLinesGrow(vg, breakWidth);

      float penY = y;
      for (int i = 0; i < nrows; i++) {
        NVGTextRow tr = rows.get(i);

        int startByte = LXUtils.clamp((int)(tr.start() - baseAddr), 0, lenBytes);
        int endByte   = LXUtils.clamp((int)(tr.end()   - baseAddr), startByte, lenBytes);

        ByteBuffer slice = ascii.duplicate();
        slice.position(startByte).limit(endByte);

        int ng = glyphPositionsGrow(vg, x, penY, slice, startByte, endByte);

        Row r = new Row();
        r.startByte = startByte;
        r.endByte = endByte;
        r.y = penY;
        r.glyphCount = ng;
        r.byteOffset = new int[ng];
        r.x = new float[ng];
        r.maxX = new float[ng];

        for (int g = 0; g < ng; g++) {
          NVGGlyphPosition gp = glyphs.get(g);
          int b = LXUtils.clamp((int)(gp.str() - baseAddr), startByte, endByte);
          r.byteOffset[g] = b;
          r.x[g] = gp.x();
          r.maxX[g] = gp.maxx();
        }

        layout.add(r);
        penY += lineHeight;
      }

      // Add an empty row if last character is a newline, so caret can move to the next line
      if (lenBytes > 0 && ascii.get(lenBytes - 1) == '\n') {
        Row r = new Row();
        r.startByte = lenBytes;
        r.endByte = lenBytes;
        r.y = penY;
        r.glyphCount = 0;
        r.byteOffset = new int[0];
        r.x = new float[0];
        r.maxX = new float[0];
        layout.add(r);
      }
    }

    private int breakLinesGrow(VGraphics vg, float breakWidth) {
      while (true) {
        int n = vg.textBreakLines(ascii, breakWidth, rows);
        if (n == 0) {
          return 0;
        }
        if (n < rowsCap) {
          return n;
        }

        // if last.next != 0, there are more rows -> truncated
        if (rows.get(n - 1).next() == 0L) {
          return n;
        }

        ensureRows(rowsCap * 2);
      }
    }

    private int glyphPositionsGrow(VGraphics vg, float x, float y, ByteBuffer slice, int startByte, int endByte) {
      while (true) {
        int n = vg.textGlyphPositions(x, y, slice, glyphs);
        if (n < glyphsCap) {
          return n;
        }
        ensureGlyphs(glyphsCap * 2);
      }
    }

    private void ensureRows(int min) {
      if (rowsCap >= min) {
        return;
      }
      int cap = Math.max(min, rowsCap == 0 ? 16 : rowsCap * 2);
      if (rows != null) {
        rows.free();
      }
      rows = NVGTextRow.create(cap);
      rowsCap = cap;
    }

    private void ensureGlyphs(int min) {
      if (glyphsCap >= min) {
        return;
      }
      int cap = Math.max(min, glyphsCap == 0 ? 64 : glyphsCap * 2);
      if (glyphs != null) {
        glyphs.free();
      }
      glyphs = NVGGlyphPosition.create(cap);
      glyphsCap = cap;
    }

    private static int lowerBound(int[] a, int key) {
      int lo = 0, hi = a.length;
      while (lo < hi) {
        int mid = (lo + hi) >>> 1;
        if (a[mid] < key) lo = mid + 1;
        else hi = mid;
      }
      return lo;
    }

    private void dispose() {
      if (ascii != null) {
        memFree(ascii);
        ascii = null;
      }
      if (rows != null) {
        rows.free();
        rows = null;
      }
      if (glyphs != null) {
        glyphs.free();
        glyphs = null;
      }
    }

    private static final class Row {
      int startByte, endByte;
      float y;
      int glyphCount;
      int[] byteOffset;
      float[] x, maxX;
    }
  }

}