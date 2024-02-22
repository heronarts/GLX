/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui;

import heronarts.glx.View;
import heronarts.glx.ui.vg.VGraphics;

public class UI2dContext extends UI2dContainer implements UILayer {

  private final VGraphics.Framebuffer framebuffer;

  boolean isOffscreen = false;

  /**
   * Constructs a new UI2dContext
   *
   * @param ui the UI to place it in
   * @param x x-position
   * @param y y-position
   * @param w width
   * @param h height
   */
  public UI2dContext(UI ui, float x, float y, float w, float h) {
    super(x, y, w, h);
    setUI(ui);
    this.framebuffer = ui.vg.createFramebuffer(this, w, h, 0);
  }

  public UI2dContext setOffscreen(boolean isOffscreen) {
    this.isOffscreen = isOffscreen;
    return this;
  }

  public short getTexture() {
    return (short) this.framebuffer.getHandle();
  }

  public VGraphics.Paint getPaint() {
    return this.framebuffer.getPaint();
  }

  /**
   * Renders the content of the context to its own framebuffer. Note that this
   * method assumes that all nested VGraphics instances beneath this one in the
   * tree have themselves already been rendered. This is because NanoVG is not
   * re-entrant and we share one instance.
   *
   * @param vg VGraphics instance
   */
  protected final void render(VGraphics vg, short viewId) {
    this.framebuffer.setView(viewId);

    this.scissor.reset(this);

    // Bind the framebuffer, which rebuilds if necessary
    vg.bindFramebuffer(this.framebuffer);
    vg.beginFrame(this.width, this.height);
    vg.scissor(0, 0, this.width, this.height);
    super.draw(this.ui, vg);
    vg.endFrame();

    // Note: this super.draw() call will have cleared the
    // needsRedraw and childNeedsRedraw flags on this element
    // and everything below it. That's fine, but we'll mark
    // ourselves as needing a blitting operation so
    // that the draw() pass gets our pixels out.
    this.needsBlit = true;
  }

  /**
   * Draws the context into a parent view. This method assumes that this
   * context's framebuffer texture has already been generated and is ready
   * to be drawn.
   *
   * @param ui UI context
   * @param view Parent view to draw into
   */
  @Override
  public final void draw(UI ui, View view) {
    if (this.isOffscreen || !isVisible()) {
      return;
    }

    // Ensure that our buffer exists.
    // NOTE: this shouldn't be necessary, but catches an edge case where
    // there are so many buffers that need redrawing that this buffer
    // didn't get serviced
    this.framebuffer.initialize();

    // NOTE: no rendering happens inside this method. The previous render() pass
    // will have ensured that our texture was rendered properly if it
    // needed to be.
    view.image(this);
  }

  /**
   * Draws this context into the given graphics context. Note that in this case
   * we assume our framebuffer is already drawn and that we're just blitting
   * it into a parent 2d context.
   *
   * @param ui UI context
   * @param vg Graphics context
   */
  @Override
  public void draw(UI ui, VGraphics vg) {
    if (this.isOffscreen || !isVisible()) {
      return;
    }

    // NOTE: similar to the above, we don't need to actually draw ourselves
    // here, we can assume that the render() pass has already happened and
    // our framebuffer has been generated appropriately. So we just nest
    // ourselves into the vg context.

    vg.beginPath();
    vg.fillPaint(this.framebuffer.getPaint());
    vg.rect(0, 0, this.width, this.height);
    vg.fill();

    this.needsBlit = false;
  }

  @Override
  protected void onResize() {
    this.framebuffer.markForResize(this.width, this.height);

    // Redraw with the force flag set, to ensure that this WHOLE thing is redrawn
    // *after* the framebuffer resize
    redraw();
  }

  @Override
  public void dispose() {
    this.ui.disposeFramebuffer(this.framebuffer);
    super.dispose();
  }

}
