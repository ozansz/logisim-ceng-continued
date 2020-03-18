/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Iterator;

import com.cburch.logisim.data.Location;

class ActionShorten {
    private ActionShorten() { }

    static ComponentAction create(Circuit circuit, Wire wn) {
        // An empty wire won't shorten anything
        if(wn.e0.equals(wn.e1)) return null;
        
        // Determine which wire, if any, is being shortened, and
        // what the resulting wire would be.
        Wire toShorten = null;
        Wire afterShorten = null;
        Location shortenLoc = null; // location of shortening
        for(Iterator it = circuit.getWires().iterator(); it.hasNext(); ) {
            Wire w = (Wire) it.next();

            if(w.overlaps(wn)) {
                boolean match0 = w.e0.equals(wn.e0);
                boolean match1 = w.e1.equals(wn.e1);

                if(match0 && match1) {
                    toShorten = w;
                    afterShorten = null;
                } else if(match0 && w.contains(wn.e1)) {
                    // shorten left/top end of wire
                    toShorten = w;
                    afterShorten = Wire.create(wn.e1, w.e1);
                    shortenLoc = wn.e1;
                } else if(match1 && w.contains(wn.e0)) {
                    // shorten right/bottom end
                    toShorten = w;
                    afterShorten = Wire.create(w.e0, wn.e0);
                    shortenLoc = wn.e0;
                }
            }
        }

        // Return null if shortening not requested
        if(toShorten == null) return null;

        // Create the shortening action. 
        ComponentAction ret = new ComponentAction(circuit, Strings.getter("shortenWireAction"));
        ret.addToRemovals(toShorten);
        if(afterShorten != null) {
            ret.addToAdditions(afterShorten);
        }

        // It may be that this should lead to merging wires that run
        // perpendicular to the site where the wire was shortened from.
        // (This could actually be both sites, if the user is removing
        // the entire wire.)
        for(int endIndex = 0; endIndex < 2; endIndex++) {
            Location end = endIndex == 0 ? wn.e0 : wn.e1;
            Location shortenEnd = endIndex == 0 ? toShorten.e0 : toShorten.e1;
            
            if(!end.equals(shortenEnd)) continue;
            
            Wire perp0 = null, perp1 = null; // wires perpendicular to toShorten
            boolean doMerge = true;
            for(Iterator it = circuit.getSplitCauses(end).iterator(); it.hasNext(); ) {
                Object o = it.next();
                if(o instanceof Wire) {
                    Wire w = (Wire) o;
                    if(w.is_x_equal == toShorten.is_x_equal) {
                        if(w != toShorten) doMerge = false;
                    } else {
                        if(perp0 == null) perp0 = w;
                        else perp1 = w;
                    }
                } else {
                    doMerge = false;
                }
            }
            if(doMerge && perp1 != null) {
                Location newEnd0 = perp0.e0.compareTo(perp1.e0) < 0 ? perp0.e0 : perp1.e0;
                Location newEnd1 = perp0.e1.compareTo(perp1.e1) > 0 ? perp0.e1 : perp1.e1;
                ret.addToIncidentalRemovals(perp0);
                ret.addToIncidentalRemovals(perp1);
                ret.addToIncidentalAdditions(Wire.create(newEnd0, newEnd1));
            }
        }
        
        // It may also be that the user has shortened a wire that was
        // crossing another wire so that it now ends at the other wire,
        // in which case the other wire should be split.
        if(shortenLoc != null) {
            for(Iterator it = circuit.getWires().iterator(); it.hasNext(); ) {
                Wire w = (Wire) it.next();
                if(w.contains(shortenLoc) && !w.endsAt(shortenLoc)
                        && w != toShorten) {
                    ret.addToIncidentalRemovals(w);
                    ret.addToIncidentalAdditions(Wire.create(w.e0, shortenLoc));
                    ret.addToIncidentalAdditions(Wire.create(shortenLoc, w.e1));
                }
            }
        }

        return ret;
    }
}
