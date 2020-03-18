/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.data;


import java.awt.Window;

import javax.swing.JTextField;

import com.cburch.logisim.util.StringGetter;

public abstract class Attribute {
    private String name;
    private StringGetter disp;

    public Attribute(String name, StringGetter disp) {
        this.name = name;
        this.disp = disp;
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return disp.get();
    }

    public java.awt.Component getCellEditor(Window source, Object value) {
        return getCellEditor(value);
    }

    protected java.awt.Component getCellEditor(Object value) {
        return new JTextField(toDisplayString(value));
    }

    public String toDisplayString(Object value) {
        return value.toString();
    }

    public String toStandardString(Object value) {
        return value.toString();
    }

    public abstract Object parse(String value);
}
