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

package heronarts.glx.event;

import static org.lwjgl.glfw.GLFW.*;

public class KeyEvent extends Event {

  // Right now we're just wrapping GLFW... but application code should reference
  // our own codes in case this needs to change in the future.

  public static final int VK_UNKNOWN = GLFW_KEY_UNKNOWN;
  public static final int VK_SPACE = GLFW_KEY_SPACE;
  public static final int VK_APOSTROPHE = GLFW_KEY_APOSTROPHE;
  public static final int VK_COMMA = GLFW_KEY_COMMA;
  public static final int VK_MINUS = GLFW_KEY_MINUS;
  public static final int VK_PERIOD = GLFW_KEY_PERIOD;
  public static final int VK_SLASH = GLFW_KEY_SLASH;
  public static final int VK_0 = GLFW_KEY_0;
  public static final int VK_1 = GLFW_KEY_1;
  public static final int VK_2 = GLFW_KEY_2;
  public static final int VK_3 = GLFW_KEY_3;
  public static final int VK_4 = GLFW_KEY_4;
  public static final int VK_5 = GLFW_KEY_5;
  public static final int VK_6 = GLFW_KEY_6;
  public static final int VK_7 = GLFW_KEY_7;
  public static final int VK_8 = GLFW_KEY_8;
  public static final int VK_9 = GLFW_KEY_9;
  public static final int VK_SEMICOLON = GLFW_KEY_SEMICOLON;
  public static final int VK_EQUAL = GLFW_KEY_EQUAL;
  public static final int VK_A = GLFW_KEY_A;
  public static final int VK_B = GLFW_KEY_B;
  public static final int VK_C = GLFW_KEY_C;
  public static final int VK_D = GLFW_KEY_D;
  public static final int VK_E = GLFW_KEY_E;
  public static final int VK_F = GLFW_KEY_F;
  public static final int VK_G = GLFW_KEY_G;
  public static final int VK_H = GLFW_KEY_H;
  public static final int VK_I = GLFW_KEY_I;
  public static final int VK_J = GLFW_KEY_J;
  public static final int VK_K = GLFW_KEY_K;
  public static final int VK_L = GLFW_KEY_L;
  public static final int VK_M = GLFW_KEY_M;
  public static final int VK_N = GLFW_KEY_N;
  public static final int VK_O = GLFW_KEY_O;
  public static final int VK_P = GLFW_KEY_P;
  public static final int VK_Q = GLFW_KEY_Q;
  public static final int VK_R = GLFW_KEY_R;
  public static final int VK_S = GLFW_KEY_S;
  public static final int VK_T = GLFW_KEY_T;
  public static final int VK_U = GLFW_KEY_U;
  public static final int VK_V = GLFW_KEY_V;
  public static final int VK_W = GLFW_KEY_W;
  public static final int VK_X = GLFW_KEY_X;
  public static final int VK_Y = GLFW_KEY_Y;
  public static final int VK_Z = GLFW_KEY_Z;
  public static final int VK_LEFT_BRACKET = GLFW_KEY_LEFT_BRACKET;
  public static final int VK_BACKSLASH = GLFW_KEY_BACKSLASH;
  public static final int VK_RIGHT_BRACKET = GLFW_KEY_RIGHT_BRACKET;
  public static final int VK_GRAVE_ACCENT = GLFW_KEY_GRAVE_ACCENT;
  public static final int VK_WORLD_1 = GLFW_KEY_WORLD_1;
  public static final int VK_WORLD_2 = GLFW_KEY_WORLD_2;
  public static final int VK_ESCAPE = GLFW_KEY_ESCAPE;
  public static final int VK_ENTER = GLFW_KEY_ENTER;
  public static final int VK_TAB = GLFW_KEY_TAB;
  public static final int VK_BACKSPACE = GLFW_KEY_BACKSPACE;
  public static final int VK_INSERT = GLFW_KEY_INSERT;
  public static final int VK_DELETE = GLFW_KEY_DELETE;
  public static final int VK_RIGHT = GLFW_KEY_RIGHT;
  public static final int VK_LEFT = GLFW_KEY_LEFT;
  public static final int VK_DOWN = GLFW_KEY_DOWN;
  public static final int VK_UP = GLFW_KEY_UP;
  public static final int VK_PAGE_UP = GLFW_KEY_PAGE_UP;
  public static final int VK_PAGE_DOWN = GLFW_KEY_PAGE_DOWN;
  public static final int VK_HOME = GLFW_KEY_HOME;
  public static final int VK_END = GLFW_KEY_END;
  public static final int VK_CAPS_LOCK = GLFW_KEY_CAPS_LOCK;
  public static final int VK_SCROLL_LOCK = GLFW_KEY_SCROLL_LOCK;
  public static final int VK_NUM_LOCK = GLFW_KEY_NUM_LOCK;
  public static final int VK_PRINT_SCREEN = GLFW_KEY_PRINT_SCREEN;
  public static final int VK_PAUSE = GLFW_KEY_PAUSE;
  public static final int VK_F1 = GLFW_KEY_F1;
  public static final int VK_F2 = GLFW_KEY_F2;
  public static final int VK_F3 = GLFW_KEY_F3;
  public static final int VK_F4 = GLFW_KEY_F4;
  public static final int VK_F5 = GLFW_KEY_F5;
  public static final int VK_F6 = GLFW_KEY_F6;
  public static final int VK_F7 = GLFW_KEY_F7;
  public static final int VK_F8 = GLFW_KEY_F8;
  public static final int VK_F9 = GLFW_KEY_F9;
  public static final int VK_F10 = GLFW_KEY_F10;
  public static final int VK_F11 = GLFW_KEY_F11;
  public static final int VK_F12 = GLFW_KEY_F12;
  public static final int VK_F13 = GLFW_KEY_F13;
  public static final int VK_F14 = GLFW_KEY_F14;
  public static final int VK_F15 = GLFW_KEY_F15;
  public static final int VK_F16 = GLFW_KEY_F16;
  public static final int VK_F17 = GLFW_KEY_F17;
  public static final int VK_F18 = GLFW_KEY_F18;
  public static final int VK_F19 = GLFW_KEY_F19;
  public static final int VK_F20 = GLFW_KEY_F20;
  public static final int VK_F21 = GLFW_KEY_F21;
  public static final int VK_F22 = GLFW_KEY_F22;
  public static final int VK_F23 = GLFW_KEY_F23;
  public static final int VK_F24 = GLFW_KEY_F24;
  public static final int VK_F25 = GLFW_KEY_F25;
  public static final int VK_KP_0 = GLFW_KEY_KP_0;
  public static final int VK_KP_1 = GLFW_KEY_KP_1;
  public static final int VK_KP_2 = GLFW_KEY_KP_2;
  public static final int VK_KP_3 = GLFW_KEY_KP_3;
  public static final int VK_KP_4 = GLFW_KEY_KP_4;
  public static final int VK_KP_5 = GLFW_KEY_KP_5;
  public static final int VK_KP_6 = GLFW_KEY_KP_6;
  public static final int VK_KP_7 = GLFW_KEY_KP_7;
  public static final int VK_KP_8 = GLFW_KEY_KP_8;
  public static final int VK_KP_9 = GLFW_KEY_KP_9;
  public static final int VK_KP_DECIMAL = GLFW_KEY_KP_DECIMAL;
  public static final int VK_KP_DIVIDE = GLFW_KEY_KP_DIVIDE;
  public static final int VK_KP_MULTIPLY = GLFW_KEY_KP_MULTIPLY;
  public static final int VK_KP_SUBTRACT = GLFW_KEY_KP_SUBTRACT;
  public static final int VK_KP_ADD = GLFW_KEY_KP_ADD;
  public static final int VK_KP_ENTER = GLFW_KEY_KP_ENTER;
  public static final int VK_KP_EQUAL = GLFW_KEY_KP_EQUAL;
  public static final int VK_LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;
  public static final int VK_LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;
  public static final int VK_LEFT_ALT = GLFW_KEY_LEFT_ALT;
  public static final int VK_LEFT_SUPER = GLFW_KEY_LEFT_SUPER;
  public static final int VK_RIGHT_SHIFT = GLFW_KEY_RIGHT_SHIFT;
  public static final int VK_RIGHT_CONTROL = GLFW_KEY_RIGHT_CONTROL;
  public static final int VK_RIGHT_ALT = GLFW_KEY_RIGHT_ALT;
  public static final int VK_RIGHT_SUPER = GLFW_KEY_RIGHT_SUPER;
  public static final int VK_MENU = GLFW_KEY_MENU;

  public static enum Action {
    PRESS,
    RELEASE,
    REPEAT
  };

  private static Action glfwAction(int glfwAction) {
    if (glfwAction == GLFW_PRESS) {
      return Action.PRESS;
    } else if (glfwAction == GLFW_RELEASE) {
      return Action.RELEASE;
    } else if (glfwAction == GLFW_REPEAT) {
      return Action.REPEAT;
    }
    throw new IllegalArgumentException("Unknown GLFW key action code: " + glfwAction);
  }

  public final Action action;
  public final int keyCode;
  public final int scanCode;
  private char keyChar;
  private boolean blurConsumed = false;

  public KeyEvent(int keyCode, int scanCode, int action, int modifiers) {
    super(modifiers);
    this.keyCode = keyCode;
    this.scanCode = scanCode;
    this.action = glfwAction(action);
  }

  public Action getAction() {
    return this.action;
  }

  public int getKeyCode() {
    return this.keyCode;
  }

  public void setKeyChar(char keyChar) {
    this.keyChar = keyChar;
  }

  public void consumeBlur() {
    this.blurConsumed = true;
  }

  public boolean isBlurConsumed() {
    return this.blurConsumed;
  }

  public char getKeyChar() {
    return this.keyChar;
  }

  public int getScanCode() {
    return this.scanCode;
  }

  public boolean isDigit() {
    return (this.keyChar >= '0' && this.keyChar <= '9');
  }

  public boolean isEnter() {
    return (this.keyCode == VK_ENTER) || (this.keyCode == VK_KP_ENTER);
  }

  public boolean isCommand(int keyCode) {
    return isCommand() && (this.keyCode == keyCode);
  }

  public boolean isShiftDown(int keyCode) {
    return isShiftDown() && (this.keyCode == keyCode);
  }

  public boolean isRepeat() {
    return this.action == Action.REPEAT;
  }

  public boolean isDelete() {
    return (this.keyCode == VK_DELETE) || (this.keyCode == VK_BACKSPACE);
  }

  @Override
  public String toString() {
    return "KeyEvent action=" + this.action + " keyCode=" + this.keyCode
      + " keyChar=" + this.keyChar + " scanCode=" + this.scanCode
      + " modifiers=" + this.modifiers;
  }

}