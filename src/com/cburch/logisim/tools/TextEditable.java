/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import com.cburch.logisim.comp.ComponentUserEvent;

public interface TextEditable {
    public Caret getTextCaret(ComponentUserEvent event);
}
