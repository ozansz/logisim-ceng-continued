/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.CustomHandles;

public class Selection extends SelectionBase {
    public static class Event {
        Object source;
        Event(Object source) { this.source = source; }
        public Object getSource() { return source; }
    }

    public static interface Listener {
        public void selectionChanged(Selection.Event event);
    }

    private boolean isVisible = true;

    public Selection(Project proj) {
        super(proj);
    }

    //
    // query methods
    //
    public boolean isEmpty() {
        return selected.isEmpty() && lifted.isEmpty();
    }

    public boolean equals(Object other) {
        if(!(other instanceof Selection)) return false;
        Selection otherSelection = (Selection) other;
        return this.selected.equals(otherSelection.selected)
            && this.lifted.equals(otherSelection.lifted);
    }

    public Collection getComponents() {
        return unionSet;
    }
    
    public Collection getAnchoredComponents() {
        return selected;
    }

    public Collection getHiddenComponents() {
        return isVisible ? Collections.EMPTY_SET : unionSet;
    }

    public Collection getComponentsContaining(Location query) {
        HashSet ret = new HashSet();
        for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(comp.contains(query)) ret.add(comp);
        }
        return ret;
    }

    public Collection getComponentsContaining(Location query, Graphics g) {
        HashSet ret = new HashSet();
        for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(comp.contains(query, g)) ret.add(comp);
        }
        return ret;
    }

    public Collection getComponentsWithin(Bounds bds) {
        HashSet ret = new HashSet();
        for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(bds.contains(comp.getBounds())) ret.add(comp);
        }
        return ret;
    }

    public Collection getComponentsWithin(Bounds bds, Graphics g) {
        HashSet ret = new HashSet();
        for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(bds.contains(comp.getBounds(g))) ret.add(comp);
        }
        return ret;
    }

    public boolean contains(Component comp) {
        return unionSet.contains(comp);
    }

    //
    // graphics methods
    //
    public void setVisible(boolean value) {
        isVisible = value;
    }

    public void draw(ComponentDrawContext context) {
        if(isVisible) {
            Graphics g = context.getGraphics();

            for(Iterator it = lifted.iterator(); it.hasNext(); ) {
                Component c = (Component) it.next();
                Location loc = c.getLocation();

                Graphics g_new = g.create();
                context.setGraphics(g_new);
                c.getFactory().drawGhost(context, Color.GRAY,
                        loc.getX(), loc.getY(), c.getAttributeSet());
                g_new.dispose();
            }

            for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
                Component comp = (Component) it.next();

                Graphics g_new = g.create();
                context.setGraphics(g_new);
                CustomHandles handler = (CustomHandles) comp.getFeature(CustomHandles.class);
                if(handler == null) {
                    context.drawHandles(comp);
                } else {
                    handler.drawHandles(context);
                }
                g_new.dispose();
            }

            context.setGraphics(g);
        }
    }

    public void drawGhostsShifted(ComponentDrawContext context,
            int dx, int dy) {
        if(shouldSnap()) {
            dx = Canvas.snapXToGrid(dx);
            dy = Canvas.snapYToGrid(dy);
        }
        Graphics g = context.getGraphics();
        for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            AttributeSet attrs = comp.getAttributeSet();
            Location loc = comp.getLocation();
            int x = loc.getX() + dx;
            int y = loc.getY() + dy;
            context.setGraphics(g.create());
            comp.getFactory().drawGhost(context, Color.gray, x, y, attrs);
            context.getGraphics().dispose();
        }
        context.setGraphics(g);
    }
    
    public void print() {
        System.err.println(" isVisible: " + isVisible); //OK
        super.print();
    }

}
