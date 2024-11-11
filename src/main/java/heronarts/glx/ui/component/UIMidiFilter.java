/**
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
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

import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.command.LXCommand;
import heronarts.lx.midi.LXMidiSource;
import heronarts.lx.midi.MidiFilterParameter;

public class UIMidiFilter extends UI2dComponent {

  public static final int WIDTH = 12;
  public static final int HEIGHT = 12;

  private class Overlay extends UI2dContainer {

    Overlay(UI ui) {
      super(0, 0, 68, (midiSource == null) ? 110 : 128);
      setBackgroundColor(ui.theme.deviceFocusedBackgroundColor);
      setBorderColor(ui.theme.contextBorderColor);
      setBorderRounding(4);

      final float yp = (midiSource == null) ? 20 : 38;

      addChildren(
        new UIButton(4, 4, 60, 16, midiFilter.enabled).setLabel("MIDI"),
        new UIEnumBox(4, yp+2, 60, 16, midiFilter.channel),
        new UIIntegerBox(4, yp+20, 29, 16, midiFilter.minNote),
        new UIIntegerBox(35, yp+20, 29, 16, midiFilter.noteRange),
        new UILabel.Control(ui, 4, yp+38, 60, 12, "Range").setTextAlignment(VGraphics.Align.CENTER),
        new UIIntegerBox(4, yp+54, 29, 16, midiFilter.minVelocity),
        new UIIntegerBox(35, yp+54, 29, 16, midiFilter.velocityRange),
        new UILabel.Control(ui, 4, yp+72, 60, 12, "Velocity").setTextAlignment(VGraphics.Align.CENTER)
      );

      if (midiSource != null) {
        new UIMidiSource(4, 22, 60, midiSource)
        .setDirection(UIDropMenu.Direction.UP)
        .setMenuWidth(190)
        .addToContainer(this, 1);
      }

    }
  }

  public enum OverlayPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT;
  }

  private final UI ui;
  private final Overlay overlay;
  private final MidiFilterParameter midiFilter;
  private final LXMidiSource.Selector midiSource;
  private OverlayPosition overlayPosition = OverlayPosition.BOTTOM_LEFT;

  public UIMidiFilter(UI ui, float x, float y, MidiFilterParameter midiFilter, LXMidiSource.Selector midiSource) {
    super(x, y, WIDTH, HEIGHT);
    this.ui = ui;
    this.midiFilter = midiFilter;
    this.midiSource = midiSource;
    this.overlay = new Overlay(ui);
    addListener(midiFilter.enabled, this.redraw);
  }

  public UIMidiFilter(UI ui, float x, float y, MidiFilterParameter midiFilter) {
    this(ui, x, y, midiFilter, null);
  }

  public UIMidiFilter setOverlayPosition(OverlayPosition overlayPosition) {
    this.overlayPosition = overlayPosition;
    return this;
  }

  @Override
  protected void onDraw(UI ui, VGraphics vg) {
    if (this.midiFilter.enabled.isOn()) {
      ui.theme.iconNote.setTint(ui.theme.primaryColor);
    } else {
      ui.theme.iconNote.setTint(ui.theme.controlFillColor);
    }
    vg.beginPath();
    vg.image(ui.theme.iconNote, -2, -2);
    vg.fill();
    ui.theme.iconNote.noTint();
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    mouseEvent.consume();
    if (mouseEvent.isCommand()) {
      this.ui.lx.command.perform(new LXCommand.Parameter.Toggle(this.midiFilter.enabled));
    } else {
      switch (this.overlayPosition) {
      case TOP_RIGHT:
        this.overlay.setPosition(this, this.width / 2, -this.overlay.getHeight() + this.height / 2);
        break;
      case TOP_LEFT:
        this.overlay.setPosition(this, -this.overlay.getWidth() + this.width / 2, -this.overlay.getHeight() + this.height / 2);
        break;
      case BOTTOM_RIGHT:
        this.overlay.setPosition(this, this.width / 2, this.height / 2);
        break;
      default:
      case BOTTOM_LEFT:
        this.overlay.setPosition(this, -this.overlay.getWidth() + this.width / 2, this.height / 2);
        break;
      }
      getUI().showContextOverlay(this.overlay);
    }
  }

  @Override
  public String getDescription() {
    return "MIDI filter configuration";
  }

  @Override
  public void dispose() {
    this.ui.clearContextOverlay(this.overlay);
    this.overlay.dispose();
    super.dispose();
  }
}
