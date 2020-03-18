/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.GraphicsUtil;

public class Circuit extends AbstractComponentFactory {
    private class MyComponentListener implements ComponentListener {
        public void endChanged(ComponentEvent e) {
            Component comp = e.getSource();
            EndData oldEnd = (EndData) e.getOldData();
            EndData newEnd = (EndData) e.getData();
            wires.remove(comp, oldEnd);
            wires.add(comp, newEnd);
            fireEvent(CircuitEvent.ACTION_INVALIDATE, comp);
        }
        public void componentInvalidated(ComponentEvent e) {
            fireEvent(CircuitEvent.ACTION_INVALIDATE, e.getSource());
        }
    }

    private MyComponentListener myComponentListener = new MyComponentListener();
    private String name;
    private EventSourceWeakSupport listeners = new EventSourceWeakSupport();
    private HashSet comps = new HashSet(); // doesn't include wires
    CircuitPins pins = new CircuitPins();
    CircuitWires wires = new CircuitWires();
        // wires is package-protected for CircuitState and Analyze only.
    private ArrayList clocks = new ArrayList();

    public Circuit(String name) {
        this.name = name;
    }
    
    public void clear() {
        Set oldComps = comps;
        comps = new HashSet();
        pins = new CircuitPins();
        wires = new CircuitWires();
        clocks.clear();
        fireEvent(CircuitEvent.ACTION_CLEAR, oldComps);
    }

    public String toString() {
        return name;
    }

    //
    // Listener methods
    //
    public void addCircuitListener(CircuitListener what) {
        listeners.add(what);
    }

    public void removeCircuitListener(CircuitListener what) {
        listeners.remove(what);
    }

    private void fireEvent(int action, Object data) {
        fireEvent(new CircuitEvent(action, this, data));
    }

    private void fireEvent(CircuitEvent event) {
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            CircuitListener what = (CircuitListener) it.next();
            what.circuitChanged(event);
        }
    }

    void addPinListener(CircuitPinListener l) { pins.addPinListener(l); }
    void removePinListener(CircuitPinListener l) { pins.removePinListener(l); }

    //
    // access methods
    //
    // getName given in ComponentFactory methods
    public Set getWidthIncompatibilityData() {
        return wires.getWidthIncompatibilityData();
    }

    public BitWidth getWidth(Location p) {
        return wires.getWidth(p);
    }

    public Location getWidthDeterminant(Location p) {
        return wires.getWidthDeterminant(p);
    }
    
    public boolean hasConflict(Component comp) {
        return wires.points.hasConflict(comp);
    }
    
    public Component getExclusive(Location loc) {
        return wires.points.getExclusive(loc);
    }

    private Set getComponents() {
        return CollectionUtil.createUnmodifiableSetUnion(comps, wires.getWires());
    }

    public Set getWires() {
        return wires.getWires();
    }

    public Set getNonWires() {
        return comps;
    }

    public Collection getComponents(Location loc) {
        return wires.points.getComponents(loc);
    }
    
    public Collection getSplitCauses(Location loc) {
        return wires.points.getSplitCauses(loc);
    }
    
    public Collection getWires(Location loc) {
        return wires.points.getWires(loc);
    }
    
    public Collection getNonWires(Location loc) {
        return wires.points.getNonWires(loc);
    }
    
    public Set getSplitLocations() {
        return wires.points.getSplitLocations();
    }

    public Collection getAllContaining(Location pt) {
        HashSet ret = new HashSet();
        for(Iterator it = getComponents().iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(comp.contains(pt)) ret.add(comp);
        }
        return ret;
    }

    public Collection getAllContaining(Location pt, Graphics g) {
        HashSet ret = new HashSet();
        for(Iterator it = getComponents().iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(comp.contains(pt, g)) ret.add(comp);
        }
        return ret;
    }

    public Collection getAllWithin(Bounds bds) {
        HashSet ret = new HashSet();
        for(Iterator it = getComponents().iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(bds.contains(comp.getBounds())) ret.add(comp);
        }
        return ret;
    }

    public Collection getAllWithin(Bounds bds, Graphics g) {
        HashSet ret = new HashSet();
        for(Iterator it = getComponents().iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(bds.contains(comp.getBounds(g))) ret.add(comp);
        }
        return ret;
    }

    public Bounds getBounds() {
        Iterator it = comps.iterator();
        if(!it.hasNext()) return wires.getWireBounds();
        Component first = (Component) it.next();
        Bounds firstBounds = first.getBounds();
        int xMin = firstBounds.getX();
        int yMin = firstBounds.getY();
        int xMax = xMin + firstBounds.getWidth();
        int yMax = yMin + firstBounds.getHeight();
        while(it.hasNext()) {
            Component c = (Component) it.next();
            Bounds bds = c.getBounds();
            int x0 = bds.getX(); int x1 = x0 + bds.getWidth();
            int y0 = bds.getY(); int y1 = y0 + bds.getHeight();
            if(x0 < xMin) xMin = x0;
            if(x1 > xMax) xMax = x1;
            if(y0 < yMin) yMin = y0;
            if(y1 > yMax) yMax = y1;
        }
        return Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin)
            .add(wires.getWireBounds());
    }

    public Bounds getBounds(Graphics g) {
        Bounds ret = wires.getWireBounds();
        int xMin = ret.getX();
        int yMin = ret.getY();
        int xMax = xMin + ret.getWidth();
        int yMax = yMin + ret.getHeight();
        for(Iterator it = comps.iterator(); it.hasNext(); ) {
            Component c = (Component) it.next();
            Bounds bds = c.getBounds(g);
            int x0 = bds.getX(); int x1 = x0 + bds.getWidth();
            int y0 = bds.getY(); int y1 = y0 + bds.getHeight();
            if(x0 < xMin) xMin = x0;
            if(x1 > xMax) xMax = x1;
            if(y0 < yMin) yMin = y0;
            if(y1 > yMax) yMax = y1;
        }
        return Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    ArrayList getClocks() {
        return clocks;
    }

    //
    // action methods
    //
    public void setName(String name) {
        this.name = name;
        fireEvent(CircuitEvent.ACTION_SET_NAME, name);
    }

    public void add(Component c) {
        if(c instanceof Wire) {
            Wire w = (Wire) c;
            if(w.getEnd0().equals(w.getEnd1())) return;
            wires.add(w);
        } else {
            // add it into the circuit
            wires.add(c);
            comps.add(c);
            if(c instanceof Pin) pins.addPin((Pin) c);
            else if(c instanceof Clock) clocks.add(c);
            c.addComponentListener(myComponentListener);
        }
        fireEvent(CircuitEvent.ACTION_ADD, c);
    }

    public void remove(Component c) {
        if(c instanceof Wire) {
            wires.remove(c);
        } else {
            wires.remove(c);
            comps.remove(c);
            if(c instanceof Pin) pins.removePin((Pin) c);
            else if(c instanceof Clock) clocks.remove(c);
            c.removeComponentListener(myComponentListener);
        }
        fireEvent(CircuitEvent.ACTION_REMOVE, c);
    }

    public void componentChanged(Component c) {
        fireEvent(CircuitEvent.ACTION_CHANGE, c);
    }

    //
    // Graphics methods
    //
    public void draw(ComponentDrawContext context, Collection hidden) {
        Graphics g = context.getGraphics();
        Graphics g_copy = g.create();
        context.setGraphics(g_copy);
        wires.draw(context, hidden);

        if(hidden == null || hidden.size() == 0) {
            for(Iterator it = comps.iterator(); it.hasNext(); ) {
                Component c = (Component) it.next();

                Graphics g_new = g.create();
                context.setGraphics(g_new);
                g_copy.dispose();
                g_copy = g_new;

                c.draw(context);
            }
        } else {
            for(Iterator it = comps.iterator(); it.hasNext(); ) {
                Component c = (Component) it.next();
                if(!hidden.contains(c)) {
                    Graphics g_new = g.create();
                    context.setGraphics(g_new);
                    g_copy.dispose();
                    g_copy = g_new;

                    c.draw(context);
                }
            }
        }
        context.setGraphics(g);
        g_copy.dispose();
    }

    //
    // ComponentFactory methods
    //
    public String getName() { return name; }

    public String getDisplayName() { return name; }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new Subcircuit(loc, this, attrs);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return pins.getOffsetBounds((CircuitAttributes) attrs);
    }

    public AttributeSet createAttributeSet() {
        return new CircuitAttributes(this);
    }
    
    public Object getFeature(Object key, AttributeSet attrs) {
        if(key == FACING_ATTRIBUTE_KEY) return CircuitAttributes.FACING_ATTR;
        return super.getFeature(key, attrs);
    }
    
    public void drawGhost(ComponentDrawContext context, Color color, int x,
            int y, AttributeSet attrs) {
        super.drawGhost(context, color, x, y, attrs);
        
        Graphics g = context.getGraphics();
        Bounds bds = getOffsetBounds(attrs).translate(x, y);
        GraphicsUtil.switchToWidth(g, 2);
        Direction facing = ((CircuitAttributes) attrs).getFacing();
        int ax;
        int ay;
        int an;
        if(facing == Direction.SOUTH) {
            ax = bds.getX() + bds.getWidth() - 1;
            ay = bds.getY() + bds.getHeight() / 2;
            an = 90;
        } else if(facing == Direction.NORTH) {
            ax = bds.getX() + 1;
            ay = bds.getY() + bds.getHeight() / 2;
            an = -90;
        } else if(facing == Direction.WEST) {
            ax = bds.getX() + bds.getWidth() / 2;
            ay = bds.getY() + bds.getHeight() - 1;
            an = 0;
        } else {
            ax = bds.getX() + bds.getWidth() / 2;
            ay = bds.getY() + 1;
            an = 180;
        }
        g.drawArc(ax - 4, ay - 4, 8, 8, an, 180);
        g.setColor(Color.BLACK);
    }

    //
    // helper methods for other classes in package
    //
    void configureComponent(Subcircuit comp) {
        // for Subcircuit to get the pins on the subcircuit configured
        pins.configureComponent(comp);
    }

}
