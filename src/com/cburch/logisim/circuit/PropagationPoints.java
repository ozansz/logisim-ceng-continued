/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;

class PropagationPoints {
    private static class Entry {
        private CircuitState state;
        private Location loc;
        
        private Entry(CircuitState state, Location loc) {
            this.state = state;
            this.loc = loc;
        }
        
        public boolean equals(Object other) {
            if(!(other instanceof Entry)) return false;
            Entry o = (Entry) other;
            return state.equals(o.state) && loc.equals(o.loc);
        }
        
        public int hashCode() {
            return state.hashCode() * 31 + loc.hashCode();
        }
    }
    
    private HashSet data;
    
    PropagationPoints() {
        this.data = new HashSet();
    }
    
    void add(CircuitState state, Location loc) {
        data.add(new Entry(state, loc));
    }
    
    void clear() {
        data.clear();
    }
    
    boolean isEmpty() {
        return data.isEmpty();
    }
    
    void draw(ComponentDrawContext context) {
        if(data.isEmpty()) return;
        
        CircuitState state = context.getCircuitState();
        HashMap stateMap = new HashMap();
        for(Iterator it = state.getSubstates().iterator(); it.hasNext(); ) {
            CircuitState s = (CircuitState) it.next();
            addSubstates(stateMap, s, s);
        }
        
        Graphics g = context.getGraphics();
        GraphicsUtil.switchToWidth(g, 2);
        for(Iterator it = data.iterator(); it.hasNext(); ) {
            Entry e = (Entry) it.next();
            if(e.state == state) {
                Location p = e.loc;
                g.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
            } else if(stateMap.containsKey(e.state)) {
                CircuitState substate = (CircuitState) stateMap.get(e.state);
                Subcircuit subcirc = substate.getSubcircuit();
                Bounds b = subcirc.getBounds();
                g.drawRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        }
        GraphicsUtil.switchToWidth(g, 1);
    }
    
    private void addSubstates(HashMap map, CircuitState source, CircuitState value) {
        map.put(source, value);
        for(Iterator it = source.getSubstates().iterator(); it.hasNext(); ) {
            CircuitState s = (CircuitState) it.next();
            addSubstates(map, s, value);
        }
    }
}
