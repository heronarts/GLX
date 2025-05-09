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

package heronarts.glx.ui;

import heronarts.glx.event.KeyEvent;
import heronarts.lx.clipboard.LXClipboardItem;

public interface UIDuplicate extends UICopy, UIPaste {
  public default void onDuplicate(KeyEvent keyEvent) {
    LXClipboardItem item = onCopy();
    if (item != null) {
      onPaste(item);
      keyEvent.consume();
    }
  }
}
