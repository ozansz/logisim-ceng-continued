/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

public class PropertyChangeWeakSupport {
    private static final String ALL_PROPERTIES = "ALL PROPERTIES";
    
    private static class ListenerData {
        String property;
        WeakReference listener;
        ListenerData(String property, PropertyChangeListener listener) {
            this.property = property;
            this.listener = new WeakReference(listener);
        }
    }
    
    private Object source;
    private LinkedList listeners = new LinkedList();

    public PropertyChangeWeakSupport(Object source) {
        this.source = source;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ALL_PROPERTIES, listener);
    }
    
    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        listeners.add(new ListenerData(property, listener));
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        removePropertyChangeListener(ALL_PROPERTIES, listener);
    }
    
    public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ListenerData data = (ListenerData) it.next();
            PropertyChangeListener l = (PropertyChangeListener) data.listener.get();
            if(l == null) {
                it.remove();
            } else if(data.property.equals(property) && l == listener) {
                it.remove();
            }
        }
    }
    
    public void firePropertyChange(String property, Object oldValue, Object newValue) {
        PropertyChangeEvent e = null;
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ListenerData data = (ListenerData) it.next();
            PropertyChangeListener l = (PropertyChangeListener) data.listener.get();
            if(l == null) {
                it.remove();
            } else if(data.property == ALL_PROPERTIES
                    || data.property.equals(property)) {
                if(e == null) {
                    e = new PropertyChangeEvent(source, property, oldValue, newValue);
                }
                l.propertyChange(e);
            }
        }
    }
    
    public void firePropertyChange(String property, int oldValue, int newValue) {
        PropertyChangeEvent e = null;
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ListenerData data = (ListenerData) it.next();
            PropertyChangeListener l = (PropertyChangeListener) data.listener.get();
            if(l == null) {
                it.remove();
            } else if(data.property == ALL_PROPERTIES
                    || data.property.equals(property)) {
                if(e == null) {
                    e = new PropertyChangeEvent(source, property,
                        IntegerFactory.create(oldValue), IntegerFactory.create(newValue));
                }
                l.propertyChange(e);
            }
        }
    }
    
    public void firePropertyChange(String property, boolean oldValue, boolean newValue) {
        PropertyChangeEvent e = null;
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ListenerData data = (ListenerData) it.next();
            PropertyChangeListener l = (PropertyChangeListener) data.listener.get();
            if(l == null) {
                it.remove();
            } else if(data.property == ALL_PROPERTIES
                    || data.property.equals(property)) {
                if(e == null) {
                    e = new PropertyChangeEvent(source, property,
                        Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
                }
                l.propertyChange(e);
            }
        }
    }
    
}
