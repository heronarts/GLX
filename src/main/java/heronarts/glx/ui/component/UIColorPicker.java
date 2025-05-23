/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui.component;

import heronarts.glx.GLX;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.UITimerTask;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;

public class UIColorPicker extends UI2dComponent {

  public enum Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
  };

  private Corner corner = Corner.BOTTOM_RIGHT;

  private ColorParameter color;
  private final LXNormalizedParameter subparameter;

  private UIColorOverlay uiColorOverlay = null;

  private boolean enabled = true;

  private int drawColor = LXColor.BLACK;

  private boolean deviceMode = false;

  public UIColorPicker(ColorParameter color) {
    this(UIKnob.WIDTH, UIKnob.WIDTH, color);
  }

  public UIColorPicker(float w, float h, ColorParameter color) {
    this(0, 0, w, h, color);
  }

  public UIColorPicker(float x, float y, float w, float h, ColorParameter color) {
    this(x, y, w, h, color, null, false);
  }

  public UIColorPicker(float x, float y, float w, float h, ColorParameter color, LXNormalizedParameter subparameter) {
    this(x, y, w, h, color, subparameter, false);
  }

  protected UIColorPicker(float x, float y, float w, float h, ColorParameter color, boolean isDynamic) {
    this(x, y, w, h, color, null, isDynamic);
  }

  protected UIColorPicker(float x, float y, float w, float h, ColorParameter color, LXNormalizedParameter subparameter, boolean isDynamic) {
    super(x, y, w, h);
    setColor(color);
    this.subparameter = subparameter;

    // Redraw with color in real-time, if modulated
    if (!isDynamic) {
      setDescription(UIParameterControl.getDescription(subparameter != null ? subparameter : color));
      addLoopTask(new UITimerTask(30, UITimerTask.Mode.FPS) {
        @Override
        protected void run() {
          setDrawColor(color.calcColor());
        }
      });
    }
  }

  protected void setDrawColor(int drawColor) {
    if (this.drawColor != drawColor) {
      this.drawColor = drawColor;
      redraw();
    }
  }

  public UIColorPicker setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  protected UIColorPicker setDeviceMode(boolean deviceMode) {
    this.deviceMode = deviceMode;
    return this;
  }

  private final LXParameterListener redrawSwatch = (p) -> {
    if (this.uiColorOverlay != null) {
      this.uiColorOverlay.swatch.redraw();
    }
  };

  protected UIColorOverlay buildColorOverlay(UI ui) {
    return new UIColorOverlay(ui);
  }

  public UIColorPicker setCorner(Corner corner) {
    this.corner = corner;
    return this;
  }

  void setColor(ColorParameter color) {
    if (this.color != null) {
      this.color.removeListener(this.redrawSwatch);
    }
    this.color = color;
    if (color != null) {
      color.addListener(this.redrawSwatch, true);
    } else {
      setDrawColor(LXColor.BLACK);
    }
    if (this.uiColorOverlay != null) {
      this.uiColorOverlay.updateColor();
    }
  }

  @Override
  public void drawBorder(UI ui, VGraphics vg) {
    if (this.deviceMode) {
      vg.beginPath();
      vg.strokeColor(ui.theme.controlBorderColor);
      vg.rect(UIKnob.KNOB_MARGIN + .5f, .5f, UIKnob.KNOB_SIZE - 1, UIKnob.KNOB_SIZE - 1);
      vg.stroke();
    } else {
      super.drawBorder(ui, vg);
    }
  }

  @Override
  public void onDraw(UI ui, VGraphics vg) {
    vg.beginPath();
    vg.fillColor(this.drawColor);
    if (this.deviceMode) {
      vg.rect(UIKnob.KNOB_MARGIN, 0, UIKnob.KNOB_SIZE, UIKnob.KNOB_SIZE);
    } else {
      vgRoundedRect(vg, .5f, .5f, this.width-1, this.height-1);
    }
    vg.fill();

    if (this.deviceMode) {
      UIParameterControl.drawParameterLabel(ui, vg, this, this.subparameter != null ? this.subparameter.getLabel() : (this.color != null) ? this.color.getLabel() : "-");
    }
  }

  protected void hideOverlay() {
    getUI().clearContextOverlay(this.uiColorOverlay);
  }

  private void showOverlay() {
    final float overlap = 6;

    if (this.uiColorOverlay == null) {
      this.uiColorOverlay = buildColorOverlay(getUI());
    }

    switch (this.corner) {
    case BOTTOM_LEFT:
      this.uiColorOverlay.setPosition(this, overlap - this.uiColorOverlay.getWidth(), this.height - overlap);
      break;
    case BOTTOM_RIGHT:
      this.uiColorOverlay.setPosition(this, this.width - overlap, this.height - overlap);
      break;
    case TOP_LEFT:
      this.uiColorOverlay.setPosition(this, overlap - this.uiColorOverlay.getWidth(), overlap - this.uiColorOverlay.getHeight());
      break;
    case TOP_RIGHT:
      this.uiColorOverlay.setPosition(this, this.width - overlap, overlap - this.uiColorOverlay.getHeight());
      break;
    }

    getUI().showContextOverlay(this.uiColorOverlay);
  }

  @Override
  public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    if (this.enabled) {
      mouseEvent.consume();
      showOverlay();
    }
    super.onMousePressed(mouseEvent, mx, my);
  }

  @Override
  public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    if (this.enabled) {
      if (keyEvent.isEnter() || (keyCode == KeyEvent.VK_SPACE)) {
        keyEvent.consume();
        showOverlay();
      } else if (keyCode == KeyEvent.VK_ESCAPE) {
        if ((this.uiColorOverlay != null) && (this.uiColorOverlay.isVisible())) {
          keyEvent.consume();
          hideOverlay();
        }
      }
    }
    super.onKeyPressed(keyEvent, keyChar, keyCode);
  }

  @Override
  public void dispose() {
    if (this.color != null) {
      this.color.removeListener(this.redrawSwatch);
    }
    if (this.uiColorOverlay != null) {
      getUI().clearContextOverlay(this.uiColorOverlay);
      this.uiColorOverlay.dispose();
    }
    super.dispose();
  }

  protected class UIColorOverlay extends UI2dContainer {

    private final UISwatch swatch;
    private final UIDoubleBox hue, saturation, brightness;
    private final UIIntegerBox red, green, blue;
    private final UITextBox hex;
    private final LXParameterListener rgbListener = p -> { setRGBBoxes(); };
    private ColorParameter rgbColor;
    private boolean inRGBUpdate = false;

    UIColorOverlay(UI ui) {
      this(ui, color instanceof LinkedColorParameter ? 38 : 8);
    }

    UIColorOverlay(UI ui, float extraHeight) {
      super(0, 0, 240, UISwatch.HEIGHT + extraHeight);

      setBackgroundColor(UI.get().theme.deviceBackgroundColor);
      setBorderColor(UI.get().theme.controlBorderColor);
      setBorderRounding(6);

      this.swatch = new UISwatch();
      this.swatch.addToContainer(this);

      final float xs = this.swatch.getX() + this.swatch.getWidth();
      final float ys = 11;
      final float bw = 44;

      addChildren(
        this.hue = new UIDoubleBox(xs+12, ys, bw, 14, color.hue),
        new UILabel.Control(ui, xs, ys, 12, 14, "H"),

        this.saturation = new UIDoubleBox(xs+12, ys + 18, bw, 14, color.saturation),
        new UILabel.Control(ui, xs, ys + 18, 12, 14, "S"),

        this.brightness = new UIDoubleBox(xs+12, ys + 36, bw, 14, color.brightness),
        new UILabel.Control(ui, xs, ys + 36, 12, 14, "V"),

        this.red = new UIIntegerBox(xs+12, ys + 54, bw, 14) {
          @Override
          public String getDescription() {
            return "Displays the red value of the color";
          }

          @Override
          public void onValueChange(int value) {
            if (!inRGBUpdate) {
              color.setColor((color.getColor() & ~LXColor.R_MASK) | (value << LXColor.R_SHIFT));
            }
          }
        }
        .setWrappable(false)
        .setRange(0, 255),
        new UILabel.Control(ui, xs, ys + 54, 12, 14, "R"),

        this.green = new UIIntegerBox(xs+12, ys + 72, bw, 14) {
          @Override
          public String getDescription() {
            return "Displays the green value of the color";
          }

          @Override
          public void onValueChange(int value) {
            if (!inRGBUpdate) {
              color.setColor((color.getColor() & ~LXColor.G_MASK) | (value << LXColor.G_SHIFT));
            }
          }
        }
        .setWrappable(false)
        .setRange(0, 255),
        new UILabel.Control(ui, xs, ys + 72, 12, 14, "G"),

        this.blue = new UIIntegerBox(xs+12, ys + 90, bw, 14) {
          @Override
          public String getDescription() {
            return "Displays the blue value of the color";
          }

          @Override
          public void onValueChange(int value) {
            if (!inRGBUpdate) {
              color.setColor((color.getColor() & ~LXColor.B_MASK) | value);
            }
          }
        }
        .setWrappable(false)
        .setRange(0, 255),
        new UILabel.Control(ui, xs, ys + 90, 12, 14, "B"),

        this.hex = (UITextBox) new UITextBox(xs+12, ys + 108, bw, 14) {

          @Override
          public String getDescription() {
            return "Displays the color value as a RGB hex string";
          }

          @Override
          public void onEditFinished() {
            String val = getValue().trim().replace("#", "");
            boolean reset = true;
            if (val.length() == 6) {
              try {
                color.setColor(LXColor.ALPHA_MASK | Integer.parseInt(val, 16));
                reset = false;
              } catch (NumberFormatException nfx) {
                GLX.error(nfx, "Invalid hex string in color RGB box: " + val);
              }
            }
            if (reset) {
              setRGBBoxes();
            }
          }
        }
        .setValidCharacters("#ABCDEFabcdef0123456789")
        .disableImmediateAppend()
        .setTextAlignment(VGraphics.Align.CENTER)
        .setTopMargin(1),
        new UILabel.Control(ui, xs, ys + 108, 12, 14, "#")
      );

      if (color instanceof LinkedColorParameter) {
        LinkedColorParameter linkedColor = (LinkedColorParameter) color;

        // Horizontal break
        new UI2dComponent(12, 140, 220, 1) {}
        .setBorderColor(ui.theme.controlBorderColor)
        .addToContainer(this);

        final UIIntegerBox indexBox = new UIIntegerBox(20, 16, linkedColor.index);

        UI2dContainer.newHorizontalContainer(16, 4,
          new UIButton(64, 16, linkedColor.mode),
          new UILabel(28, 11, "Index").setFont(ui.theme.getControlFont()),
          indexBox
        )
        .setPosition(12, 148)
        .addToContainer(this);

        addListener(linkedColor.mode, p -> {
          boolean isLinked = linkedColor.mode.getEnum() == LinkedColorParameter.Mode.PALETTE;
          swatch.setEnabled(!isLinked);
          this.hue.setEnabled(!isLinked);
          this.saturation.setEnabled(!isLinked);
          this.brightness.setEnabled(!isLinked);
          this.red.setEnabled(!isLinked);
          this.green.setEnabled(!isLinked);
          this.blue.setEnabled(!isLinked);
          this.hex.setEnabled(!isLinked);
          indexBox.setEnabled(isLinked);
        }, true);
      }

      updateColor();
    }

    private void setRGBBoxes() {
      if (color != null) {
        int c = color.getColor();
        this.inRGBUpdate = true;
        this.red.setValue(0xff & LXColor.red(c));
        this.green.setValue(0xff & LXColor.green(c));
        this.blue.setValue(0xff & LXColor.blue(c));
        this.hex.setValue(String.format("%06x", c & LXColor.RGB_MASK));
        this.inRGBUpdate = false;
      }
    }

    private void updateColor() {
      if (this.rgbColor != null) {
        this.rgbColor.removeListener(this.rgbListener);
        this.rgbColor = null;
      }
      if (color == null) {
        this.hue.setParameter(null);
        this.saturation.setParameter(null);
        this.brightness.setParameter(null);
      } else {
        this.hue.setParameter(color.hue);
        this.saturation.setParameter(color.saturation);
        this.brightness.setParameter(color.brightness);
        this.rgbColor = color;
        color.addListener(this.rgbListener, true);
      }
    }

    @Override
    public void dispose() {
      if (this.rgbColor != null) {
        this.rgbColor.removeListener(this.rgbListener);
        this.rgbColor = null;
      }
      super.dispose();
    }

    private class UISwatch extends UI2dComponent implements UIFocus {

      private static final float PADDING = 8;

      private static final float GRID_X = PADDING;
      private static final float GRID_Y = PADDING;

      private static final float GRID_WIDTH = 120;
      private static final float GRID_HEIGHT = 120;

      private static final float BRIGHT_SLIDER_X = 140;
      private static final float BRIGHT_SLIDER_Y = PADDING;
      private static final float BRIGHT_SLIDER_WIDTH = 16;
      private static final float BRIGHT_SLIDER_HEIGHT = GRID_HEIGHT;

      private static final float WIDTH = BRIGHT_SLIDER_X + BRIGHT_SLIDER_WIDTH + 2*PADDING;
      private static final float HEIGHT = GRID_HEIGHT + 2*PADDING;

      private boolean enabled = true;

      public UISwatch() {
        super(4, 4, WIDTH, HEIGHT);
        setFocusCorners(false);
      }

      private void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      @Override
      public void onDraw(UI ui, VGraphics vg) {
        final int xStops = 6;
        final int yStops = 40;
        final float xStep = GRID_WIDTH / xStops;
        final float yStep = GRID_HEIGHT / yStops;

        float hue = color.hue.getBaseValuef();
        float saturation = color.saturation.getBaseValuef();
        float brightness = color.brightness.getBaseValuef();

        // Main color grid
        for (int y = 0; y < yStops; ++y) {
          for (int x = 0; x < xStops; ++x) {
            vg.fillLinearGradient(GRID_X + x * xStep, 0, GRID_X + (x+1) * xStep, 0,
              LXColor.hsb(x * 360 / xStops, 100f - y * 100f / yStops, brightness),
              LXColor.hsb((x+1) * 360 / xStops, 100f - y * 100f / yStops, brightness));
            vg.beginPath();
            vg.rect(GRID_X + x * xStep - .5f, GRID_Y + y * yStep - .5f, xStep + 1, yStep + 1);
            vg.fill();
          }
        }

        // Brightness slider
        vg.fillLinearGradient(BRIGHT_SLIDER_X, BRIGHT_SLIDER_Y, BRIGHT_SLIDER_X, BRIGHT_SLIDER_HEIGHT,
          LXColor.hsb(hue, saturation, 100),
          LXColor.hsb(hue, saturation, 0)
        );
        vg.beginPath();
        vg.rect(BRIGHT_SLIDER_X, BRIGHT_SLIDER_Y - .5f, BRIGHT_SLIDER_WIDTH, BRIGHT_SLIDER_HEIGHT + 1);
        vg.fill();

        // Color square
        vg.beginPath();
        vg.strokeColor(brightness < 50 ? UIColor.WHITE : UIColor.BLACK);
        vg.ellipse(
          GRID_X + hue / 360 * GRID_WIDTH,
          GRID_Y + (1 - saturation / 100) * GRID_HEIGHT,
          4,
          4
        );
        vg.stroke();

        // Brightness triangle
        vg.beginPath();
        vg.fillColor(ui.theme.controlTextColor);
        float xp = BRIGHT_SLIDER_X;
        float yp = BRIGHT_SLIDER_Y + (1 - brightness / 100) * BRIGHT_SLIDER_HEIGHT;
        vg.moveTo(xp, yp);
        vg.lineTo(xp - 6, yp - 4);
        vg.lineTo(xp - 6, yp + 4);
        vg.closePath();
        vg.moveTo(xp + BRIGHT_SLIDER_WIDTH, yp);
        vg.lineTo(xp + BRIGHT_SLIDER_WIDTH + 6, yp + 4);
        vg.lineTo(xp + BRIGHT_SLIDER_WIDTH + 6, yp - 4);
        vg.closePath();
        vg.fill();
      }

      private boolean draggingBrightness = false;
      private LXCommand.Parameter.SetValue setBrightness = null;
      private LXCommand.Parameter.SetColor setColor = null;

      @Override
      public void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
        if (!this.enabled) {
          return;
        }
        mouseEvent.consume();
        this.setBrightness = null;
        this.setColor = null;
        if (this.draggingBrightness = (mx > GRID_X + GRID_WIDTH)) {
          this.setBrightness = new LXCommand.Parameter.SetValue(color.brightness, color.brightness.getBaseValue());
        } else {
          this.setColor = new LXCommand.Parameter.SetColor(color);
          setHueSaturation(mx, my);
        }
      }

      @Override
      public void onMouseReleased(MouseEvent mouseEvent, float mx, float my) {
        this.setBrightness = null;
        this.setColor = null;
      }

      private void setHueSaturation(float mx, float my) {
        mx = LXUtils.clampf(mx - GRID_X, 0, GRID_WIDTH);
        my = LXUtils.clampf(my - GRID_Y, 0, GRID_WIDTH);

        double hue = mx / GRID_WIDTH * 360;
        double saturation = 100 - my / GRID_HEIGHT * 100;
        getLX().command.perform(this.setColor.update(hue, saturation));
      }

      @Override
      public void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
        if (!this.enabled) {
          return;
        }
        mouseEvent.consume();
        if (this.draggingBrightness) {
          if (dy != 0) {
            float brightness = color.brightness.getBaseValuef();
            brightness = LXUtils.clampf(brightness - 100 * dy / BRIGHT_SLIDER_HEIGHT, 0, 100);
            getLX().command.perform(this.setBrightness.update(brightness));
          }
        } else {
          setHueSaturation(mx, my);
        }
      }

      @Override
      public void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
        if (!this.enabled) {
          return;
        }
        float inc = keyEvent.isShiftDown() ? 10 : 2;
        if (keyCode == KeyEvent.VK_UP) {
          keyEvent.consume();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.saturation,
            LXUtils.clampf(color.saturation.getBaseValuef() + inc, 0, 100)
          ));
        } else if (keyCode == KeyEvent.VK_DOWN) {
          keyEvent.consume();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.saturation,
            LXUtils.clampf(color.saturation.getBaseValuef() - inc, 0, 100)
          ));
        } else if (keyCode == KeyEvent.VK_LEFT) {
          keyEvent.consume();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.hue,
            LXUtils.clampf(color.hue.getBaseValuef() - 3*inc, 0, 360)
          ));
        } else if (keyCode == KeyEvent.VK_RIGHT) {
          keyEvent.consume();
          getLX().command.perform(new LXCommand.Parameter.SetValue(color.hue,
            LXUtils.clampf(color.hue.getBaseValuef() + 3*inc, 0, 360)
          ));
        }
      }

    }
  }


}
