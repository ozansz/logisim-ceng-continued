/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.StringGetter;

class ActionAdd {
    private ActionAdd() { }

    static ComponentAction create(Circuit circuit, Collection toAddBase) {
        if(toAddBase.isEmpty()) {
            return new ComponentAction(circuit);
        }
        
        Set curSplitLocs = circuit.getSplitLocations();
        
        // first split apart any wires in toAdd that hit split points,
        // and construct addSplitLocs
        HashSet toAdd = new HashSet();
        int wireCount = 0;
        int nonWireCount = 0;
        for(Iterator it = toAddBase.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(comp instanceof Wire) {
                wireCount++;
                Wire w = (Wire) comp;
                
                TreeSet endPoints = null;
                for(Iterator it2 = curSplitLocs.iterator(); it2.hasNext(); ) {
                    Location loc = (Location) it2.next();
                    if(w.contains(loc)) {
                        if(endPoints == null) {
                            endPoints = new TreeSet();
                            endPoints.add(w.e0);
                            endPoints.add(w.e1);
                        }
                        endPoints.add(loc);
                    }
                }
                
                if(endPoints == null || endPoints.size() == 2) {
                    toAdd.add(w);
                } else {
                    Iterator it2 = endPoints.iterator();
                    Location last = (Location) it2.next();
                    while(it2.hasNext()) {
                        Location here = (Location) it2.next();
                        toAdd.add(Wire.create(last, here));
                        last = here;
                    }
                }
            } else {
                nonWireCount++;
                toAdd.add(comp);
            }
        }
        
        StringGetter descriptor;
        if(nonWireCount == 0) {
            if(wireCount == 1) {
                descriptor = Strings.getter("addWireAction");
            } else {
                descriptor = Strings.getter("addWiresAction");
            }
        } else {
            if(nonWireCount == 1 && wireCount == 0) {
                Component comp = (Component) toAdd.iterator().next();
                descriptor = Strings.getter("addComponentAction", comp.getFactory().getDisplayName());
            } else {
                descriptor = Strings.getter("addComponentsAction");
            }
        }
        ComponentAction ret = new ComponentAction(circuit, descriptor);
        
        // Now go through each existing wire, and split it apart
        // as necessary and merge it with any overlapping wires
        CircuitPoints addSplitLocs = WireUtil.computeCircuitPoints(toAdd);
        for(Iterator it = circuit.getWires().iterator(); it.hasNext(); ) {
            Wire w = (Wire) it.next();
            
            // Determine the points where the wire should be split.
            TreeSet endPoints = null;
            for(Iterator it2 = addSplitLocs.getSplitLocations().iterator(); it2.hasNext(); ) {
                Location split = (Location) it2.next();
                if(w.contains(split)) {
                    boolean doSplit = false;
                    for(Iterator it3 = addSplitLocs.getSplitCauses(split).iterator(); it3.hasNext(); ) {
                        Object o = it3.next();
                        if(o instanceof Wire) {
                            Wire splitCause = (Wire) o;
                            if(splitCause.is_x_equal != w.is_x_equal) doSplit = true;
                        } else {
                            doSplit = true;
                        }
                    }
                    if(doSplit) {
                        if(endPoints == null) endPoints = new TreeSet();
                        endPoints.add(split);
                    }
                }
            }

            // Determine the segments of this wire after splitting, and whether
            // the added components allow the wire's first and last segments can
            // be extended
            int segCount;
            Location[] segEnd;
            boolean canExtend0 = true; // we can extend wire up/leftwards
            boolean canExtend1 = true; // we can extend wire down/rightwards
            if(endPoints == null) {
                segCount = 1;
                segEnd = new Location[] { w.e0, w.e1 };
            } else {
                canExtend0 = endPoints.add(w.e0); // we can extend wire up/leftwards
                canExtend1 = endPoints.add(w.e1); // we can extend wire down/rightwards
                
                // Now create an array representing the various segments.
                segCount = endPoints.size() - 1;
                segEnd = new Location[segCount + 1];
                Iterator pIt = endPoints.iterator();
                for(int i = 0; pIt.hasNext(); i++) {
                    Location p = (Location) pIt.next();
                    segEnd[i] = p;
                }
            }
            
            // Check whether any existing items prevent extension of the first or last segments
            if(canExtend0) {
                for(Iterator it2 = circuit.getComponents(w.e0).iterator(); it2.hasNext(); ) {
                    Object w2 = it2.next();
                    if(w2 != w) canExtend0 = false;
                }
            }
            if(canExtend1) {
                for(Iterator it2 = circuit.getComponents(w.e1).iterator(); it2.hasNext(); ) {
                    Object w2 = it2.next();
                    if(w2 != w) canExtend1 = false;
                }
            }
            
            // Check for overlapping wires, which may extend w, and in
            // any case will make some of the overlapping wires one that
            // should be "selected" (rather than incidentally added).
            boolean[] segSelected = new boolean[segCount];
            if(wireCount > 0) {
                for(Iterator it2 = toAdd.iterator(); it2.hasNext(); ) {
                    Object comp = it2.next();
                    if(comp instanceof Wire && w.overlaps((Wire) comp)) {
                        Wire wNew = (Wire) comp;
                        // Determine the ends of the wire, extending w
                        // if appropriate and otherwise truncatig the
                        // ends in queston to fit the old values.
                        Location e0 = wNew.e0;
                        if(e0.compareTo(segEnd[0]) < 0) {
                            if(canExtend0) {
                                segEnd[0] = e0;
                                canExtend0 = false;
                            } else {
                                e0 = segEnd[0];
                            }
                        }
                        Location e1 = wNew.e1;
                        if(e1.compareTo(segEnd[segCount]) > 0) {
                            if(canExtend1) {
                                segEnd[segCount] = e1;
                                canExtend1 = false;
                            } else {
                                e1 = segEnd[segCount];
                            }
                        }
                        
                        if(e0.compareTo(e1) < 0) {
                            it2.remove();
                            // select the segments that wNew overlaps 
                            for(int i = 0; i < segCount; i++) {
                                Location seg0 = segEnd[i];
                                Location seg1 = segEnd[i + 1];
                                if(seg1.compareTo(e0) >= 0 && seg0.compareTo(e1) <= 0) {
                                    segSelected[i] = true;
                                }
                            }
                        }
                    }
                }
            }
            
            if(segCount == 1 && segEnd[0].equals(w.e0) && segEnd[1].equals(w.e1)
                    && !segSelected[0]) {
                ; // do nothing - we just leave w untouched
            } else {
                ret.addToIncidentalRemovals(w);
                for(int i = 0; i < segCount; i++) {
                    Wire wNew = Wire.create(segEnd[i], segEnd[i + 1]);
                    if(segSelected[i]) toAdd.add(wNew);
                    else ret.addToIncidentalAdditions(wNew);
                }
            }
        }
        
        for(Iterator it = toAdd.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            ret.addToAdditions(comp);
        }

        return ret;
    }

}
