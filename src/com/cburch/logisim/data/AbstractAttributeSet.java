/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractAttributeSet implements Cloneable, AttributeSet {
    private ArrayList listeners = null;
    
    public AbstractAttributeSet() { }
    
    public Object clone() {
        AbstractAttributeSet ret;
        try {
            ret = (AbstractAttributeSet) super.clone();
        } catch(CloneNotSupportedException ex) {
            throw new UnsupportedOperationException();
        }
        ret.listeners = new ArrayList();
        this.copyInto(ret);
        return ret;
    }
    
    public void addAttributeListener(AttributeListener l) {
        if(listeners == null) listeners = new ArrayList();
        listeners.add(l);
    }
    public void removeAttributeListener(AttributeListener l) {
        listeners.remove(l);
        if(listeners.isEmpty()) listeners = null;
    }
    protected void fireAttributeValueChanged(Attribute attr, Object value) {
        if(listeners != null) {
            AttributeEvent event = new AttributeEvent(this, attr, value);
            for(int i = 0, n = listeners.size(); i < n; i++) {
                AttributeListener l = (AttributeListener) listeners.get(i);
                l.attributeValueChanged(event);
            }
        }
    }
    protected void fireAttributeListChanged() {
        if(listeners != null) {
            AttributeEvent event = new AttributeEvent(this);
            for(int i = 0, n = listeners.size(); i < n; i++) {
                AttributeListener l = (AttributeListener) listeners.get(i);
                l.attributeListChanged(event);
            }
        }
    }

    public boolean containsAttribute(Attribute attr) {
        return getAttributes().contains(attr);
    }
    public Attribute getAttribute(String name) {
        for(Iterator it = getAttributes().iterator(); it.hasNext(); ) {
            Attribute attr = (Attribute) it.next();
            if(attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }

    public boolean isReadOnly(Attribute attr) {
        return false;
    }
    public void setReadOnly(Attribute attr, boolean value) {
        throw new UnsupportedOperationException();
    }

    protected abstract void copyInto(AbstractAttributeSet dest);
    public abstract List getAttributes();
    public abstract Object getValue(Attribute attr);
    public abstract void setValue(Attribute attr, Object value);

}
