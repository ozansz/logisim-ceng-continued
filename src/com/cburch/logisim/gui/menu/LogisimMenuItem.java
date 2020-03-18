/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

class LogisimMenuItem {
    private String name;
    
    LogisimMenuItem(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
}
