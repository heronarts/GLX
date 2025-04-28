package heronarts.glx.event;

import static org.lwjgl.glfw.GLFW.*;

public class GamepadEvent extends Event {

  public static final int BUTTON_NONE = -1;
  public static final int BUTTON_A = GLFW_GAMEPAD_BUTTON_A;
  public static final int BUTTON_B = GLFW_GAMEPAD_BUTTON_B;
  public static final int BUTTON_X = GLFW_GAMEPAD_BUTTON_X;
  public static final int BUTTON_Y = GLFW_GAMEPAD_BUTTON_Y;
  public static final int BUTTON_LEFT_BUMPER = GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
  public static final int BUTTON_RIGHT_BUMPER = GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
  public static final int BUTTON_BACK = GLFW_GAMEPAD_BUTTON_BACK;
  public static final int BUTTON_START = GLFW_GAMEPAD_BUTTON_START;
  public static final int BUTTON_GUIDE = GLFW_GAMEPAD_BUTTON_GUIDE;
  public static final int BUTTON_LEFT_THUMB = GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
  public static final int BUTTON_RIGHT_THUMB = GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
  public static final int BUTTON_DPAD_UP = GLFW_GAMEPAD_BUTTON_DPAD_UP;
  public static final int BUTTON_DPAD_RIGHT = GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
  public static final int BUTTON_DPAD_DOWN = GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
  public static final int BUTTON_DPAD_LEFT = GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
  public static final int BUTTON_LAST = GLFW_GAMEPAD_BUTTON_LAST;
  // Repeats:
  public static final int BUTTON_CROSS = GLFW_GAMEPAD_BUTTON_CROSS;
  public static final int BUTTON_CIRCLE = GLFW_GAMEPAD_BUTTON_CIRCLE;
  public static final int BUTTON_SQUARE = GLFW_GAMEPAD_BUTTON_SQUARE;
  public static final int BUTTON_TRIANGLE = GLFW_GAMEPAD_BUTTON_TRIANGLE;

  public static final int AXIS_NONE = -1;
  public static final int AXIS_LEFT_X = GLFW_GAMEPAD_AXIS_LEFT_X;
  public static final int AXIS_LEFT_Y = GLFW_GAMEPAD_AXIS_LEFT_Y;
  public static final int AXIS_RIGHT_X = GLFW_GAMEPAD_AXIS_RIGHT_X;
  public static final int AXIS_RIGHT_Y = GLFW_GAMEPAD_AXIS_RIGHT_Y;
  public static final int AXIS_LEFT_TRIGGER = GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
  public static final int AXIS_RIGHT_TRIGGER = GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
  public static final int AXIS_LAST = GLFW_GAMEPAD_AXIS_LAST;

  public static enum Action {
    BUTTON_PRESS,
    BUTTON_RELEASE,
    AXIS_CHANGE
  };

  private static Action getButtonAction(int glfwAction) {
    if (glfwAction == GLFW_PRESS) {
      return Action.BUTTON_PRESS;
    } else if (glfwAction == GLFW_RELEASE) {
      return Action.BUTTON_RELEASE;
    }
    throw new IllegalArgumentException("Unknown GLFW gamepad action code: " + glfwAction);
  }

  public final Action action;
  public final int gamepadId;
  public final int button;
  public final int axis;
  public final float axisValue;

  public GamepadEvent(int gamepadId, int button, int action, int modifiers) {
    super(modifiers);

    this.action = getButtonAction(action);
    this.gamepadId = gamepadId;
    this.button = button;
    this.axis = AXIS_NONE;
    this.axisValue = 0.f;
  }

  public GamepadEvent(int gamepadId, int axis, float axisValue, int modifiers) {
    super(modifiers);

    this.action = Action.AXIS_CHANGE;
    this.gamepadId = gamepadId;
    this.button = BUTTON_NONE;
    this.axis = axis;
    this.axisValue = axisValue;
  }

  public Action getAction() {
    return this.action;
  }

  @Override
  public String toString() {
    return "GamepadEvent action=" + this.action
      + " gamepadId=" + this.gamepadId
      + " button=" + this.button
      + " axis=" + this.axis
      + " axisValue=" + this.axisValue
      + " modifiers=" + this.modifiers;
  }
}
