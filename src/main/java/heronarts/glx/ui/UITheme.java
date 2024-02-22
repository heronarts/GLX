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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;

public class UITheme {

  private final List<Color> colors = new ArrayList<Color>();

  public class Color extends UIColor {

    private final String label;

    public Color(String label) {
      super(Theme.DEFAULT.colors.get(label));
      this.label = label;
      colors.add(this);
    }

    private Color set(int argb) {
      this.argb = argb;
      return this;
    }
  }

  public void setTheme(Theme theme) {
    this.knobDetentSize = theme.knobDetentSize;
    for (Color color : this.colors) {
      color.set(theme.colors.get(color.label));
    }
  }

  public enum Theme {
    DEFAULT("Default", 4,
      "label", "cccccc",

      "deviceBackground", "404040",
      "deviceFocusedBackground", "4c4c4c",
      "deviceSelection", "586658",
      "deviceSelectionText", "e0e0e0",

      "paneBackground", "040404",
      "paneInset", "242424",
      "paneScrollBar", "4c4c4c",
      "paneSelectorActive", "404040",
      "paneSelectorInactive", "333333",

      "toolbarBackground", "242424",
      "toolbarSectionBackground", "404040",
      "helpBackground", "242424",
      "helpText", "c0c0c0",

      "sectionExpanderBackground", "303030",
      "sceneStripBackground", "393939",
      "clipButtonBackground", "494949",

      "controlBackground", "222222",
      "controlFill", "222222",
      "controlBorder", "292929",
      "controlDetent", "333333",
      "controlHandle", "5f5f5f",
      "controlText", "cccccc",
      "controlActiveText", "ffffff",
      "controlDisabled", "303030",
      "controlDisabledText", "707070",
      "controlDisabledFill", "696969",
      "controlDisabledValue", "909090",

      "editTextBackground", "000000",
      "editText", "ffffff",

      "primary", "669966",
      "secondary", "666699",
      "focus", "669966",
      "attention", "ee0000",
      "restrictions", "292929",
      "cue", "666699",
      "aux", "996666",
      "busA", "669966",
      "busB", "a66812",
      "surface", "666699",
      "recording", "a00044",
      "cursor", "555555",
      "selection", "333333",
      "error", "ff0000",

      "listBackground", "191919",
      "listBorder", "191919",
      "listItemBackground", "222222",
      "listItemSelectedBackground", "404040",
      "listItemFocusedBackground", "333333",
      "listItemFocusedText", "ffffff",
      "listItemSecondary", "666666",
      "listScrollBar", "333333",
      "listSectionArrow", "666666",
      "listSectionText", "aaaaaa",

      "meterBackground", "191919",
      "messageBoxBackground", "222222",

      "iconDisabled", "505050",
      "iconInactive", "999999",

      "contextBackground", "222222",
      "contextBorder", "000000",
      "contextHighlight", "333333",

      "dialogBackground", "404040",
      "dialogInset", "191919",

      "midiMapping", "ff0000",
      "modulationSourceMapping", "00ff00",
      "modulationTargetMapping", "00cccc",

      "projectFileIcon", "999999"
    ),

    LIGHT("Light", 8,
      "label", "191919",

      "deviceBackground", "888888",
      "deviceFocusedBackground", "8f8f8f",
      "deviceSelection", "8cd867",
      "deviceSelectionText", "111111",

      "paneBackground", "707070",
      "paneInset", "595959",
      "paneScrollBar", "494949",
      "paneSelectorActive", "8f8f8f",
      "paneSelectorInactive", "7f7f7f",

      "toolbarBackground", "7f7f7f",
      "toolbarSectionBackground", "7f7f7f",
      "helpBackground", "808080",
      "helpText", "111111",

      "sectionExpanderBackground", "444444",
      "sceneStripBackground", "707070",
      "clipButtonBackground", "505050",

      "controlBackground", "b7b7b7",
      "controlFill", "393939",
      "controlBorder", "444444",
      "controlDetent", "808080",
      "controlHandle", "9f9f9f",
      "controlText", "191919",
      "controlActiveText", "000000",
      "controlDisabled", "909090",
      "controlDisabledText", "303030",
      "controlDisabledFill", "a0a0a0",
      "controlDisabledValue", "c9c9c9",

      "editTextBackground", "d9d9d9",
      "editText", "000000",

      "primary", "8cd867",
      "secondary", "7ddbf3",
      "focus", "c9c9c9",
      "attention", "ff3333",
      "restrictions", "292929",
      "cue", "7ddbf3",
      "aux", "f67247",
      "busA", "8cd867",
      "busB", "d88c67",
      "surface", "8080bf",
      "recording", "ec624a",
      "cursor", "555555",
      "selection", "cccccc",
      "error", "cc0000",

      "listBackground", "888888",
      "listBorder", "444444",
      "listItemBackground", "888888",
      "listItemSelectedBackground", "a0a0a0",
      "listItemFocusedBackground", "999999",
      "listItemSecondary", "b7b7b7",
      "listItemFocusedText", "000000",
      "listScrollBar", "555555",
      "listSectionArrow", "666666",
      "listSectionText", "333333",

      "meterBackground", "292929",
      "messageBoxBackground", "7f7f7f",

      "iconDisabled", "505050",
      "iconInactive", "404040",

      "contextBackground", "b7b7b7",
      "contextBorder", "333333",
      "contextHighlight", "c9c9c9",

      "dialogBackground", "707070",
      "dialogInset", "888888",

      "midiMapping", "ff0000",
      "modulationSourceMapping", "00ff00",
      "modulationTargetMapping", "00cccc",

      "projectFileIcon", "444444"
    );

    public final String name;
    public final Map<String, Integer> colors = new HashMap<String, Integer>();
    public final int knobDetentSize;

    private Theme(String name, int knobDetentSize, String ... colors) {
      this.name = name;
      this.knobDetentSize = knobDetentSize;
      for (int i = 0; i < colors.length; i +=2) {
        String field = colors[i];
        String hex = colors[i+1];
        if (hex.length() == 6) {
          this.colors.put(field, 0xff000000 | Integer.parseInt(hex, 16));
        } else {
          throw new IllegalArgumentException("UITheme color must be 6 hex digits - " + field);
        }
      }
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private final VGraphics.Font deviceFont;
  private final VGraphics.Font labelFont;
  private final VGraphics.Font controlFont;

  private int knobDetentSize;

  public final Color labelColor = new Color("label");

  public final Color deviceBackgroundColor = new Color("deviceBackground");
  public final Color deviceFocusedBackgroundColor = new Color("deviceFocusedBackground");
  public final Color deviceSelectionColor = new Color("deviceSelection");
  public final Color deviceSelectionTextColor = new Color("deviceSelectionText");

  public final Color paneBackgroundColor = new Color("paneBackground");
  public final Color paneInsetColor = new Color("paneInset");
  public final Color paneScrollBarColor = new Color("paneScrollBar");
  public final Color paneSelectorActiveColor = new Color("paneSelectorActive");
  public final Color paneSelectorInactiveColor = new Color("paneSelectorInactive");

  public final Color toolbarBackgroundColor = new Color("toolbarBackground");
  public final Color toolbarSectionBackgroundColor = new Color("toolbarSectionBackground");
  public final Color helpBackgroundColor = new Color("helpBackground");
  public final Color helpTextColor = new Color("helpText");

  public final Color sectionExpanderBackgroundColor = new Color("sectionExpanderBackground");
  public final Color sceneStripBackgroundColor = new Color("sceneStripBackground");
  public final Color clipButtonBackgroundColor = new Color("clipButtonBackground");

  public final Color controlBackgroundColor = new Color("controlBackground");
  public final Color controlFillColor = new Color("controlFill");
  public final Color controlBorderColor = new Color("controlBorder");
  public final Color controlHandleColor = new Color("controlHandle");
  public final Color controlDetentColor = new Color("controlDetent");
  public final Color controlTextColor = new Color("controlText");
  public final Color controlActiveTextColor = new Color("controlActiveText");
  public final Color controlDisabledColor = new Color("controlDisabled");
  public final Color controlDisabledTextColor = new Color("controlDisabledText");
  public final Color controlDisabledFillColor = new Color("controlDisabledFill");
  public final Color controlDisabledValueColor = new Color("controlDisabledValue");

  public final Color editTextBackgroundColor = new Color("editTextBackground");
  public final Color editTextColor = new Color("editText");

  public final Color primaryColor = new Color("primary");
  public final Color secondaryColor = new Color("secondary");
  public final Color focusColor = new Color("focus");
  public final Color attentionColor = new Color("attention");
  public final Color restrictionsColor = new Color("restrictions");
  public final Color cueColor = new Color("cue");
  public final Color auxColor = new Color("aux");
  public final Color busAColor = new Color("busA");
  public final Color busBColor = new Color("busB");
  public final Color surfaceColor = new Color("surface");
  public final Color recordingColor = new Color("recording");
  public final Color cursorColor = new Color("cursor");
  public final Color selectionColor = new Color("selection");
  public final Color errorColor = new Color("error");

  public final Color listBackgroundColor = new Color("listBackground");
  public final Color listBorderColor = new Color("listBorder");
  public final Color listItemBackgroundColor = new Color("listItemBackground");
  public final Color listItemSelectedBackgroundColor = new Color("listItemSelectedBackground");
  public final Color listItemFocusedBackgroundColor = new Color("listItemFocusedBackground");
  public final Color listItemFocusedTextColor = new Color("listItemFocusedText");
  public final Color listItemSecondaryColor = new Color("listItemSecondary");
  public final Color listScrollBarColor = new Color("listScrollBar");
  public final Color listSectionArrowColor = new Color("listSectionArrow");
  public final Color listSectionTextColor = new Color("listSectionText");

  public final Color meterBackgroundColor = new Color("meterBackground");

  public final Color messageBoxBackgroundColor = new Color("messageBoxBackground");

  public final Color iconDisabledColor = new Color("iconDisabled");
  public final Color iconInactiveColor = new Color("iconInactive");

  public final Color contextBackgroundColor = new Color("contextBackground");
  public final Color contextBorderColor = new Color("contextBorder");
  public final Color contextHighlightColor = new Color("contextHighlight");

  public final Color dialogBackgroundColor = new Color("dialogBackground");
  public final Color dialogInsetColor = new Color("dialogInset");

  public final Color midiMappingColor = new Color("midiMapping");
  public final Color modulationSourceMappingColor = new Color("modulationSourceMapping");
  public final Color modulationTargetMappingColor = new Color("modulationTargetMapping");

  public final Color projectFileIconColor = new Color("projectFileIcon");

  public final VGraphics.Image iconNote;
  public final VGraphics.Image iconTempo;
  public final VGraphics.Image iconControl;
  public final VGraphics.Image iconTrigger;
  public final VGraphics.Image iconTriggerSource;
  public final VGraphics.Image iconLoop;
  public final VGraphics.Image iconMap;
  public final VGraphics.Image iconArm;
  public final VGraphics.Image iconLfo;
  public final VGraphics.Image iconLoad;
  public final VGraphics.Image iconSave;
  public final VGraphics.Image iconSaveAs;
  public final VGraphics.Image iconNew;
  public final VGraphics.Image iconOpen;
  public final VGraphics.Image iconKeyboard;
  public final VGraphics.Image iconPreferences;
  public final VGraphics.Image iconUndo;
  public final VGraphics.Image iconRedo;
  public final VGraphics.Image iconTempoDown;
  public final VGraphics.Image iconTempoUp;
  public final VGraphics.Image iconOscInput;
  public final VGraphics.Image iconOscOutput;
  public final VGraphics.Image iconPatternTransition;
  public final VGraphics.Image iconPatternRotate;
  public final VGraphics.Image iconPlay;
  public final VGraphics.Image iconView;
  public final VGraphics.Image iconEdit;
  public final VGraphics.Image iconPlaylist;
  public final VGraphics.Image iconBlend;
  public final VGraphics.Image iconAdd;
  public final VGraphics.Image iconSearch;

  UITheme(VGraphics vg) throws IOException {
    this.controlFont = loadFont(vg, "Inter-SemiBold", "Inter-SemiBold.otf");
    this.controlFont.fontSize(10);
    LX.initProfiler.log("GLX: UI: Theme: controlFont");

    this.labelFont = this.deviceFont = loadFont(vg, "Inter-Black", "Inter-Black.otf");
    this.labelFont.fontSize(10);
    LX.initProfiler.log("GLX: UI: Theme: labelFont");

    this.knobDetentSize = 4;

    this.iconNote = loadIcon(vg, "icon-note@2x.png");
    this.iconTempo = loadIcon(vg, "icon-tempo@2x.png");
    this.iconControl = loadIcon(vg, "icon-control@2x.png");
    this.iconTrigger = loadIcon(vg, "icon-trigger@2x.png");
    this.iconTriggerSource = loadIcon(vg, "icon-trigger-source@2x.png");
    this.iconLoop = loadIcon(vg, "icon-loop@2x.png");
    this.iconMap = loadIcon(vg, "icon-map@2x.png");
    this.iconArm = loadIcon(vg, "icon-arm@2x.png");
    this.iconLfo = loadIcon(vg, "icon-lfo@2x.png");
    this.iconLoad = loadIcon(vg, "icon-load@2x.png");
    this.iconSave = loadIcon(vg, "icon-save@2x.png");
    this.iconSaveAs = loadIcon(vg, "icon-save-as@2x.png");
    this.iconNew = loadIcon(vg, "icon-new@2x.png");
    this.iconOpen = loadIcon(vg, "icon-open@2x.png");
    this.iconKeyboard = loadIcon(vg, "icon-keyboard@2x.png");
    this.iconPreferences = loadIcon(vg, "icon-preferences@2x.png");
    this.iconUndo = loadIcon(vg, "icon-undo@2x.png");
    this.iconRedo = loadIcon(vg, "icon-redo@2x.png");
    this.iconTempoDown = loadIcon(vg, "icon-tempo-down@2x.png");
    this.iconTempoUp = loadIcon(vg, "icon-tempo-up@2x.png");
    this.iconOscInput = loadIcon(vg, "icon-osc-input@2x.png");
    this.iconOscOutput = loadIcon(vg, "icon-osc-output@2x.png");
    this.iconPatternTransition = loadIcon(vg, "icon-pattern-transition@2x.png");
    this.iconPatternRotate = loadIcon(vg, "icon-pattern-rotate@2x.png");
    this.iconPlay = loadIcon(vg, "icon-play@2x.png");
    this.iconView = loadIcon(vg, "icon-view@2x.png");
    this.iconEdit = loadIcon(vg, "icon-edit@2x.png");
    this.iconPlaylist = loadIcon(vg, "icon-playlist@2x.png");
    this.iconBlend = loadIcon(vg, "icon-blend@2x.png");
    this.iconAdd = loadIcon(vg, "icon-add@2x.png");
    this.iconSearch = loadIcon(vg, "icon-search@2x.png");
    LX.initProfiler.log("GLX: UI: Theme: Icons");
  }

  private final List<VGraphics.Font> fonts = new ArrayList<VGraphics.Font>();

  private VGraphics.Font loadFont(VGraphics vg, String name, String filename) throws IOException {
    VGraphics.Font font = vg.loadFont(name, filename);
    this.fonts.add(font);
    return font;
  }

  private final List<VGraphics.Image> icons = new ArrayList<VGraphics.Image>();

  private VGraphics.Image loadIcon(VGraphics vg, String filename) throws IOException {
    VGraphics.Image icon = vg.loadIcon(filename);
    this.icons.add(icon);
    return icon;
  }

  public int getKnobDetentSize() {
    return this.knobDetentSize;
  }

  /**
   * Gets the default item font
   *
   * @return The default item font
   */
  public VGraphics.Font getControlFont() {
    return this.controlFont;
  }

  /**
   * Label font
   *
   * @return font
   */
  public VGraphics.Font getLabelFont() {
    return this.labelFont;
  }

  public VGraphics.Font getDeviceFont() {
    return this.deviceFont;
  }

  public void dispose() {
    for (VGraphics.Font font: this.fonts) {
      font.dispose();
    }
    this.fonts.clear();

    for (VGraphics.Image icon : this.icons) {
      icon.dispose();
    }
    this.icons.clear();
  }

}

