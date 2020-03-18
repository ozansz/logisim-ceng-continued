/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;

public class WireUtil {
    private WireUtil() { }

    static CircuitPoints computeCircuitPoints(Collection components) {
        CircuitPoints points = new CircuitPoints();
        for(Iterator it = components.iterator(); it.hasNext(); ) {
            points.add((Component) it.next());
        }
        return points;
    }

    // Merge all parallel endpoint-to-endpoint wires within the given set.
    public static Collection mergeExclusive(Collection toMerge) {
        if(toMerge.size() <= 1) return toMerge;
        
        HashSet ret = new HashSet(toMerge);
        CircuitPoints points = computeCircuitPoints(toMerge);
        
        HashSet wires = new HashSet();
        for(Iterator it = points.getSplitLocations().iterator(); it.hasNext(); ) {
            Location loc = (Location) it.next();
            Collection at = points.getComponents(loc);
            if(at.size() == 2) {
                Iterator atIt = at.iterator();
                Object o0 = atIt.next();
                Object o1 = atIt.next();
                if(o0 instanceof Wire && o1 instanceof Wire) {
                    Wire w0 = (Wire) o0;
                    Wire w1 = (Wire) o1;
                    if(w0.is_x_equal == w1.is_x_equal) {
                        wires.add(w0);
                        wires.add(w1);
                    }
                }
            }
        }
        points = null;
        
        ret.removeAll(wires);
        while(!wires.isEmpty()) {
            Iterator it = wires.iterator();
            Wire w = (Wire) it.next();
            Location e0 = w.e0;
            Location e1 = w.e1;
            it.remove();
            boolean found;
            do {
                found = false;
                for(it = wires.iterator(); it.hasNext(); ) {
                    Wire cand = (Wire) it.next();
                    if(cand.e0.equals(e1)) {
                        e1 = cand.e1;
                        found = true;
                        it.remove();
                    } else if(cand.e1.equals(e0)) {
                        e0 = cand.e0;
                        found = true;
                        it.remove();
                    }
                }
            } while(found);
            ret.add(Wire.create(e0, e1));
        }
        
        return ret;
    }
}
