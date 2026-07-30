/**
 * Copyright 2019- Justin K. Belcher, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package heronarts.glx;

import org.lwjgl.system.MemoryStack;

import heronarts.lx.LXPreferences;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetMonitorWorkarea;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a physical monitor
 */
public class Monitor extends LXPreferences.WindowSettings {

  final long handle;
  final boolean isPrimary;
  final String label;

  private boolean hasError = false;

  public Monitor(long handle, boolean isPrimary, String label) {
    this.handle = handle;
    this.isPrimary = isPrimary;
    this.label = label;

    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer xPos = stack.mallocInt(1);
      IntBuffer yPos = stack.mallocInt(1);
      IntBuffer xSize = stack.mallocInt(1);
      IntBuffer ySize = stack.mallocInt(1);
      glfwGetMonitorWorkarea(handle, xPos, yPos, xSize, ySize);
      int x = xPos.get();
      int y = yPos.get();
      int width = xSize.get();
      int height = ySize.get();

      // Note: 0 is a valid value, so we can't compare to NULL
      setPosition(x, y);

      if (width != NULL && height != NULL) {
        setSize(width, height);
      }
    } catch (Exception ex) {
      this.hasError = true;
      GLX.error(ex, "  Failed to get monitor workarea");
    }
  }

  public boolean hasError() {
    return this.hasError;
  }
}
