package heronarts.glx;

import heronarts.lx.DisplaySettings;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetMonitorWorkarea;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a physical monitor
 */
public class Monitor extends DisplaySettings {

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

  public Monitor(boolean isPrimary, int x, int y, int width, int height) {
    this.handle = NULL;
    this.label = "";
    this.isPrimary = isPrimary;
    setPosition(x, y);
    setSize(width, height);
  }

  public boolean hasError() {
    return this.hasError;
  }

  public boolean equals(Monitor that) {
    return super.equals(that) &&
      this.isPrimary == that.isPrimary;
  }

}
