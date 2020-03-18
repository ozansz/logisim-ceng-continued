/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;

public class EventSourceWeakSupport {
    private LinkedList listeners = new LinkedList();

    public EventSourceWeakSupport() { }
    
    public void add(Object listener) {
        listeners.add(new WeakReference(listener));
    }
    
    public void remove(Object listener) {
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            Object l = ((WeakReference) it.next()).get();
            if(l == null || l == listener) it.remove();
        }
    }
    
    public int size() {
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            Object l = ((WeakReference) it.next()).get();
            if(l == null) it.remove();
        }
        return listeners.size();
    }
    
    public Iterator iterator() {
        // copy elements into another list in case any event handlers
        // want to add a listener
        ArrayList ret = new ArrayList(listeners.size());
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            Object l = ((WeakReference) it.next()).get();
            if(l == null) it.remove(); else ret.add(l);
        }
        return ret.iterator();
    }
}
