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
 */

package heronarts.glx;

import java.io.IOException;
import org.lwjgl.system.Platform;

public abstract class GLXApp {

  static {
    // Ensure that AWT is only used in headless mode
    System.setProperty("java.awt.headless", "true");
  }

  protected GLXApp(GLX.Flags flags) {
    main(flags);
  }

  protected void dispose() {}

  protected abstract GLX buildLXInstance(GLXWindow window, GLX.Flags flags) throws IOException;

  private void main(GLX.Flags flags) {
    try {
      GLX.log("Starting " + getClass().getName() + " version " + GLX.VERSION);
      GLX.log("Running java " +
        System.getProperty("java.version") + " " +
        System.getProperty("java.vendor") + " " +
        System.getProperty("os.name") + " " +
        System.getProperty("os.version") + " " +
        System.getProperty("os.arch")
      );

      if (Platform.get() == Platform.LINUX) {
        GLX.log("Forcing use of OpenGL on Linux");
        flags.useOpenGL = true;
      }

      // Run the full windowed application
      final GLXWindow window = new GLXWindow(flags);

      // Start the GLX application on another thread
      new Thread(() -> {
        try {
          applicationThread(window, flags);
        } catch (Throwable x) {
          GLX.error(x, "Unhandled exception in GLXApp application thread: " + x.getLocalizedMessage());
        }
      }).start();

      // Run the main event loop
      window.main();

    } catch (Throwable x) {
      GLX.error(x, "Unhandled exception in GLXApp.main: " + x.getLocalizedMessage());
    } finally {
      dispose();
    }
  }

  private void applicationThread(GLXWindow window, GLX.Flags flags) throws IOException {
    buildLXInstance(window, flags).run();
  }

}
