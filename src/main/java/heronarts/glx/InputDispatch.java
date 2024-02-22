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

package heronarts.glx;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.system.Platform;

import heronarts.glx.event.Event;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.lx.LXEngine;

public class InputDispatch implements LXEngine.Dispatch {

  private final GLX lx;

  private int modifiers = 0;
  private boolean mouseDragging = false;
  private double cursorX = 0, cursorY = 0;
  private boolean macosControlClick = false;
  private KeyEvent keyEvent = null;
  private MouseEvent previousMousePress = null;

  private final AtomicBoolean hasInputEvents = new AtomicBoolean(false);
  private final List<Event> lxThreadEventQueue = new ArrayList<Event>();
  private final List<Event> glfwThreadEventQueue = Collections.synchronizedList(new ArrayList<Event>());

  InputDispatch(GLX lx) {
    this.lx = lx;
  }

  private void queueEvent(Event event) {
    this.glfwThreadEventQueue.add(event);
    this.hasInputEvents.set(true);
  }

  void onFocus(double cursorX, double cursorY) {
    this.cursorX = cursorX;
    this.cursorY = cursorY;
  }

  void glfwKeyCallback(long window, int key, int scancode, int action, int mods) {
    this.modifiers = mods;
    queueEvent(this.keyEvent = new KeyEvent(key, scancode, action, mods));

    // Hack-toggle to UI perf logging on the UI thread
    if ((key == KeyEvent.VK_U) && (action == GLFW_PRESS) && keyEvent.isCommand() && keyEvent.isShiftDown()) {
      lx.toggleUIPerformanceDebug();
    }
  }

  void glfwCharCallback(long window, int codepoint) {
    if (this.keyEvent != null) {
      this.keyEvent.setKeyChar((char) codepoint);
    }
  }

  void glfwCursorPosCallback(long window, double x, double y) {
    // Apply cursor position scaling, to go from window-space into ui-space
    x *= this.lx.cursorScaleX;
    y *= this.lx.cursorScaleY;
    double dx = x - this.cursorX;
    double dy = y - this.cursorY;
    MouseEvent.Action action = this.mouseDragging ? MouseEvent.Action.DRAG : MouseEvent.Action.MOVE;
    queueEvent(new MouseEvent(action, (float) x, (float) y, (float) dx, (float) dy, this.modifiers));
    this.cursorX = x;
    this.cursorY = y;
  };

  void glfwMouseButtonCallback(long window, int button, int action, int mods) {
    // On MacOS, ctrl-click with LEFT behaves as RIGHT
    if (Platform.get() == Platform.MACOSX && (button == GLFW_MOUSE_BUTTON_LEFT)) {
      if ((action == GLFW_PRESS) && ((mods & GLFW_MOD_CONTROL) != 0)) {
        button = GLFW_MOUSE_BUTTON_RIGHT;
        this.macosControlClick = true;
      }

      // Even if ctrl-is not down anymore, the next release
      if ((action == GLFW_RELEASE) && this.macosControlClick) {
        button = GLFW_MOUSE_BUTTON_RIGHT;
        this.macosControlClick = false;
      }
    }

    // Keep track of dragging state
    if (button == GLFW_MOUSE_BUTTON_LEFT) {
      if (action == GLFW_PRESS) {
        this.mouseDragging = true;
      } else if (action == GLFW_RELEASE) {
        this.mouseDragging = false;
      }
    }

    // Create the mouse event
    MouseEvent mouseEvent = new MouseEvent(action, button, (float) this.cursorX, (float) this.cursorY, mods);

    // Detect double-presses
    if (action == GLFW_PRESS) {
      if ((this.previousMousePress != null) && this.previousMousePress.isRepeat(mouseEvent)) {
        mouseEvent.setCount(this.previousMousePress.getCount() + 1);
      }
      this.previousMousePress = mouseEvent;
    }

    queueEvent(mouseEvent);
  }

  void glfwScrollCallback(long window, double dx, double dy) {
    switch (Platform.get()) {
      case MACOSX:
        dx *= this.lx.systemContentScaleX;
        dy *= this.lx.systemContentScaleY;
        break;
      case WINDOWS:
        dx *= 20;
        dy *= 20;
        break;
      default:
        break;
    }
    queueEvent(new MouseEvent(MouseEvent.Action.SCROLL, (float) this.cursorX, (float) this.cursorY, (float) dx, (float) dy, this.modifiers));
  }

  public static final double POLL_TIMEOUT = 1/60.;

  void poll() {
    // It doesn't seem like V-Sync always works, definitely not on a Mac...
    // or we're getting stupidly high 100+ FPS framerate if we leave this
    // as just poll.
    // glfwPollEvents();

    // So we're going to do wait instead with a timeout such
    // that we'll only draw at max rate when input is active, otherwise
    // throttle to a reasonable framerate
    glfwWaitEventsTimeout(POLL_TIMEOUT);
  }

  private Event coalesceEvents(Event thisEvent, Event prevEvent) {
    if ((thisEvent instanceof MouseEvent) && (prevEvent instanceof MouseEvent)) {
      MouseEvent mouseEvent = (MouseEvent) thisEvent;
      MouseEvent prevMouseEvent = (MouseEvent) prevEvent;
      if (mouseEvent.action == prevMouseEvent.action) {
        switch (mouseEvent.action) {
        case SCROLL:
        case MOVE:
        case DRAG:
          return new MouseEvent(
            mouseEvent,
            mouseEvent.action,
            mouseEvent.x,
            mouseEvent.y,
            mouseEvent.dx + prevMouseEvent.dx,
            mouseEvent.dy + prevMouseEvent.dy,
            mouseEvent.modifiers
          );
        default:
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Invoked by the LXEngine thread to handle input events.
   */
  @Override
  public void dispatch() {
    if (!this.hasInputEvents.compareAndSet(true, false)) {
      // Nothing happened!
      return;
    }

    this.lxThreadEventQueue.clear();

    // Lock on the glfw input event queue, in case it's in the middle of polling...
    synchronized (this.glfwThreadEventQueue) {
      // Shallow copy all the glfw events onto an LX-owned thread, then clear it
      this.lxThreadEventQueue.addAll(this.glfwThreadEventQueue);
      this.glfwThreadEventQueue.clear();
    }

    // Release the lock and process events on the LX thread! This is to avoid a lot of
    // horrible message queue passing... the event handlers in UI objects are allowed
    // to directly call the LX engine interfaces to change parameter values, etc. without
    // fucking up the engine thread state

    // Do a first pass over the array, coalescing any events that are of the same
    // motion types. This will particularly save us on doing unnecessary parameter
    // or scroll updates.
    Event lastEvent = null;
    for (int i = 0; i < this.lxThreadEventQueue.size(); ++i) {
      Event event = this.lxThreadEventQueue.get(i);
      Event coalesce = coalesceEvents(event, lastEvent);
      if (coalesce != null) {
        this.lxThreadEventQueue.set(i-1, coalesce);
        this.lxThreadEventQueue.remove(i);
        lastEvent = coalesce;
        --i;
      } else {
        lastEvent = event;
      }
    }

    // Now process all of them in the UI layer
    for (Event event : this.lxThreadEventQueue) {
      if (event instanceof MouseEvent) {
        this.lx.ui.mouseEvent((MouseEvent) event);
      } else if (event instanceof KeyEvent) {
        this.lx.ui.keyEvent((KeyEvent) event);
      } else {
        throw new IllegalStateException("Illegal event type in queue: " + event);
      }
    }
  }

}
