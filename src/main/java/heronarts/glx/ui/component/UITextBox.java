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

package heronarts.glx.ui.component;

import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.UICopy;
import heronarts.glx.ui.UIPaste;
import heronarts.lx.clipboard.LXClipboardItem;
import heronarts.lx.clipboard.LXTextValue;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.StringParameter;

public class UITextBox extends UIInputBox implements UICopy, UIPaste {

  private final static String NO_VALUE = "-";
  private static final String VALID_CHARACTERS =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ,.<>?;':\"[]{}-=_+`~!@#$%^&*()|1234567890/\\";

  private String value = NO_VALUE;
  private StringParameter parameter = null;
  private boolean isEmptyValueAllowed = false;
  private String validCharacters = VALID_CHARACTERS;

  private final LXParameterListener parameterListener = (p) -> {
    setValue(this.parameter.getString(), false);
  };

  public UITextBox() {
    this(0, 0, 0, 0);
  }

  public UITextBox(float w, float h) {
    this(0, 0, w, h);
  }

  public UITextBox(float x, float y, float w, float h) {
    super(x, y, w, h);
    enableImmediateAppend();
    setMouseEditable(false);
  }

  public UITextBox(float w, float h, StringParameter parameter) {
    this(0, 0, w, h, parameter);
  }

  public UITextBox(float x, float y, float w, float h, StringParameter parameter) {
    this(x, y, w, h);
    setParameter(parameter);
  }

  @Override
  public LXParameter getParameter() {
    return this.parameter;
  }

  public UITextBox setParameter(StringParameter parameter) {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    this.parameter = parameter;
    if (parameter != null) {
      this.parameter.addListener(this.parameterListener);
      setValue(parameter.getString(), false);
    } else {
      setValue(NO_VALUE);
    }
    return this;
  }

  @Override
  public String getDescription() {
    return UIParameterControl.getDescription(this.parameter);
  }

  public String getValue() {
    return this.value;
  }

  @Override
  protected String getValueString() {
    return this.value;
  }

  @Override
  protected String getInitialEditBufferValue() {
    return this.value;
  }

  public UITextBox setEmptyValueAllowed(boolean isEmptyValueAllowed) {
    this.isEmptyValueAllowed = isEmptyValueAllowed;
    return this;
  }

  public UITextBox setValue(String value) {
    return setValue(value, true);
  }

  public UITextBox setValue(String value, boolean pushToParameter) {
    if (!this.value.equals(value)) {
      this.value = value;
      if (pushToParameter && (this.parameter != null)) {
        if (this.useCommandEngine) {
          getUI().lx.command.perform(new LXCommand.Parameter.SetString(this.parameter, value));
        } else {
          this.parameter.setValue(value);
        }
      }
      onValueChange(this.value);
      redraw();
    }
    return this;
  }

  /**
   * Subclasses may override to handle value changes
   *
   * @param value New value being set
   */
  protected /* abstract */ void onValueChange(String value) {}

  @Override
  protected void saveEditBuffer(String editBuffer) {
    String value = editBuffer.trim();
    if (this.isEmptyValueAllowed || (value.length() > 0)) {
      setValue(value);
    }
  }

  /**
   * Set a custom list of valid characters for this text box
   *
   * @param validCharacters Valid characters
   * @return this
   */
  public UITextBox setValidCharacters(String validCharacters) {
    this.validCharacters = validCharacters;
    return this;
  }

  public static boolean isValidTextCharacter(char keyChar) {
    return VALID_CHARACTERS.indexOf(keyChar) >= 0;
  }

  @Override
  public boolean isValidCharacter(char keyChar) {
    return this.validCharacters.indexOf(keyChar) >= 0;
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    if (this.enabled && !this.editing && this.editable) {
      if (mouseEvent.getButton() == MouseEvent.BUTTON_LEFT && mouseEvent.isDoubleClick()) {
        mouseEvent.consume();
        edit();
        redraw();
      }
    }
  }

  /**
   * Gives the text box focus and processes the key event which just occurred which would
   * give it focus
   *
   * @param keyEvent Key Event
   * @param keyChar Key character
   * @param keyCode Key code
   */
  public void focusKeyPress(KeyEvent keyEvent, char keyChar, int keyCode) {
    focus(keyEvent);
    onKeyPressed(keyEvent, keyChar, keyCode);
  }

  @Override
  public LXClipboardItem onCopy() {
    if (this.editing) {
      String editRange = getEditRange();
      if (!editRange.isEmpty()) {
        return new LXTextValue(editRange);
      }
    }
    String str = (this.parameter != null) ? this.parameter.getString() : getValue();
    if (!str.isEmpty()) {
      return new LXTextValue(str);
    }
    return null;
  }

  @Override
  public void onPaste(LXClipboardItem item) {
    if (item instanceof LXTextValue) {
      if (isEnabled() && isEditable()) {
        if (this.editing) {
          editAppend(((LXTextValue) item).getValue());
        } else {
          setValue(((LXTextValue) item).getValue());
          onEditFinished();
        }
      }
    }
  }

  @Override
  public void dispose() {
    if (this.parameter != null) {
      this.parameter.removeListener(this.parameterListener);
    }
    super.dispose();
  }
}
