/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.comp;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.EventSourceWeakSupport;

public abstract class ManagedComponent extends AbstractComponent {
    private EventSourceWeakSupport listeners = new EventSourceWeakSupport();
    private Location loc;
    private AttributeSet attrs;
    private ArrayList ends;
    private List ends_view;
    private Bounds bounds = null;

    public ManagedComponent(Location loc, AttributeSet attrs, int num_ends) {
        this.loc = loc;
        this.attrs = attrs;
        this.ends = new ArrayList(num_ends);
        this.ends_view = Collections.unmodifiableList(ends);
    }

    //
    // abstract AbstractComponent methods
    //
    public abstract ComponentFactory getFactory();

    public void addComponentListener(ComponentListener l) {
        listeners.add(l);
    }

    public void removeComponentListener(ComponentListener l) {
        listeners.remove(l);
    }

    protected void fireEndChanged(ComponentEvent e) {
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ComponentListener l = (ComponentListener) it.next();
            l.endChanged(e);
        }
    }

    protected void fireComponentInvalidated(ComponentEvent e) {
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ComponentListener l = (ComponentListener) it.next();
            l.componentInvalidated(e);
        }
    }

    public Location getLocation() {
        return loc;
    }

    public AttributeSet getAttributeSet() {
        return attrs;
    }

    public Bounds getBounds() {
        if(bounds == null) {
            Location loc = getLocation();
            bounds = getFactory().getOffsetBounds(getAttributeSet())
                .translate(loc.getX(), loc.getY());
        }
        return bounds;
    }

    public java.util.List getEnds() {
        return ends_view;
    }

    public abstract void propagate(CircuitState state);

    //
    // methods for altering data
    //
    public void clearManager() {
        for(Iterator it = ends.iterator(); it.hasNext(); ) {
            fireEndChanged(new ComponentEvent(this, it.next(), null));
        }
        ends.clear();
        bounds = null;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }
    
    public void setAttributeSet(AttributeSet value) {
        attrs = value;
    }

    public void setEnd(int i, EndData data) {
        if(i == ends.size()) {
            ends.add(data);
            fireEndChanged(new ComponentEvent(this, null, data));
        } else {
            EndData old = (EndData) ends.get(i);
            if(old == null || !old.equals(data)) {
                ends.set(i, data);
                fireEndChanged(new ComponentEvent(this, old, data));
            }
        }
    }

    public void setEnd(int i, Location end, BitWidth width, int type) {
        setEnd(i, new EndData(end, width, type));
    }

    public void setEnd(int i, Location end, BitWidth width, int type, boolean exclusive) {
        setEnd(i, new EndData(end, width, type, exclusive));
    }

    public Location getEndLocation(int i) {
        return getEnd(i).getLocation();
    }

    //
    // user interface methods
    //
    public void expose(ComponentDrawContext context) {
        Bounds bounds = getBounds();
        java.awt.Component dest = context.getDestination();
        if(bounds != null) {
            dest.repaint(bounds.getX() - 5, bounds.getY() - 5,
                bounds.getWidth() + 10, bounds.getHeight() + 10);
        }
    }
    
    public Object getFeature(Object key) {
        return null;
    }
}
