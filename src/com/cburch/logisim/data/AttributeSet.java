/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.data;

import java.util.List;

public interface AttributeSet {
    public Object clone();
    public void addAttributeListener(AttributeListener l);
    public void removeAttributeListener(AttributeListener l);

    public List getAttributes();
    public boolean containsAttribute(Attribute attr);
    public Attribute getAttribute(String name);

    public boolean isReadOnly(Attribute attr);
    public void setReadOnly(Attribute attr, boolean value); // optional

    public Object getValue(Attribute attr);
    public void setValue(Attribute attr, Object value);
}
