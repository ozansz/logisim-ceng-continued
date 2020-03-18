/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.StringGetter;

class ActionRemove {
    private ActionRemove() { }
    
    static ComponentAction create(Circuit circuit, Collection toRemove) {
        if(toRemove.isEmpty()) return new ComponentAction(circuit);

        // take a first crack at computing the removed split locations, and
        // determine what sorts of things we are removing
        int wireCount = 0;
        int nonWireCount = 0;
        for(Iterator it = toRemove.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(obj instanceof Wire) {
                wireCount++;
            } else {
                nonWireCount++;
            }
        }
        
        // create the action to return
        StringGetter descriptor;
        if(nonWireCount == 0) {
            if(wireCount == 1) {
                descriptor = Strings.getter("removeWireAction");
            } else {
                descriptor = Strings.getter("removeWiresAction");
            }
        } else {
            if(nonWireCount == 1 && wireCount == 0) {
                Component comp = (Component) toRemove.iterator().next();
                descriptor = Strings.getter("removeComponentAction", comp.getFactory().getDisplayName());
            } else {
                descriptor = Strings.getter("removeComponentsAction");
            }
        }
        ComponentAction ret = new ComponentAction(circuit, descriptor);

        // Some remaining wires may need to merge if any split locations are removed
        CircuitPoints removeSplitLocs = WireUtil.computeCircuitPoints(toRemove);
        if(!removeSplitLocs.getSplitLocations().isEmpty()) {
            // Go through possibly removed split locations and add adjacent wires into merge
            // set. (Note that some apparently removed split locations may remain if they are
            // split locations for another reason also.)
            HashSet toMerge = new HashSet();
            for(Iterator it = removeSplitLocs.getSplitLocations().iterator(); it.hasNext(); ) {
                Location loc = (Location) it.next();
                boolean doMerge = true;
                Wire x0 = null; Wire x1 = null; // wires with x's equal
                Wire y0 = null; Wire y1 = null; // wires with y's equal
                for(Iterator it2 = circuit.getSplitCauses(loc).iterator(); doMerge && it2.hasNext(); ) {
                    Object comp = it2.next();
                    if(!toRemove.contains(comp)) {
                        if(comp instanceof Wire) {
                            Wire w = (Wire) comp;
                            if(w.is_x_equal) {
                                if(y0 != null) doMerge = false;
                                if(x0 == null) x0 = w;
                                else x1 = w;
                            } else {
                                if(x0 != null) doMerge = false;
                                if(y0 == null) y0 = w;
                                else y1 = w;
                            }
                        } else {
                            doMerge = false;
                        }
                    }
                }
                if(doMerge) {
                    if(x1 != null) { toMerge.add(x0); toMerge.add(x1); }
                    if(y1 != null) { toMerge.add(y0); toMerge.add(y1); }
                }
            }

            // Then add them all into the returned action
            if(!toMerge.isEmpty()) {
                for(Iterator it = toMerge.iterator(); it.hasNext(); ) {
                    ret.addToIncidentalRemovals((Wire) it.next());
                }
                for(Iterator it = WireUtil.mergeExclusive(toMerge).iterator(); it.hasNext(); ) {
                    ret.addToIncidentalAdditions((Wire) it.next());
                }
            }
        }
        
        for(Iterator it = toRemove.iterator(); it.hasNext(); ) {
            ret.addToRemovals((Component) it.next());
        }

        return ret;
    }
}
