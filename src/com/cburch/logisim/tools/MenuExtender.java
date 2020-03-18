/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import javax.swing.JPopupMenu;

import com.cburch.logisim.proj.Project;

public interface MenuExtender {
    public void configureMenu(JPopupMenu menu, Project proj);
}
