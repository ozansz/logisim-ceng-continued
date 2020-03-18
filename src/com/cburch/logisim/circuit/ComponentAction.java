/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.SmallSet;
import com.cburch.logisim.util.StringGetter;

public class ComponentAction extends Action implements Cloneable {
    private static final StringGetter UNKNOWN = Strings.getter("unknownComponentAction");
    
    private Circuit circuit;
    private StringGetter descriptor;
    private SmallSet toAdd = new SmallSet();
    private SmallSet toRemove = new SmallSet();
    private SmallSet toAddIncidental = new SmallSet();
    private SmallSet toRemoveIncidental = new SmallSet();
    private Set toAddView = null;
    private Set toRemoveView = null;
    private Set toAddIncidentalView = null;
    private Set toRemoveIncidentalView = null;

    // creation & initalization methods - this is to be done
    // within this package only
    public ComponentAction(Circuit circuit) {
        this(circuit, UNKNOWN);
    }
    ComponentAction(Circuit circuit, StringGetter descriptor) {
        this.circuit = circuit;
        this.descriptor = descriptor;
    }
    void addToAdditions(Component comp) { toAdd.add(comp); }
    void addToRemovals(Component comp) { toRemove.add(comp); }
    void addToIncidentalAdditions(Component comp) { toAddIncidental.add(comp); }
    void addToIncidentalRemovals(Component comp) { toRemoveIncidental.add(comp); }

    //
    // public methods
    //
    public Object clone() {
        ComponentAction ret = (ComponentAction) this.clone();
        ret.toAdd = (SmallSet) this.toAdd.clone();
        ret.toRemove = (SmallSet) this.toRemove.clone();
        ret.toAddIncidental = (SmallSet) this.toAddIncidental.clone();
        ret.toRemoveIncidental = (SmallSet) this.toRemoveIncidental.clone();
        ret.toAddView = null;
        ret.toRemoveView = null;
        ret.toAddIncidentalView = null;
        ret.toRemoveIncidentalView = null;
        return ret;
    }
    
    public Action append(Action otherAction) {
        if(!(otherAction instanceof ComponentAction)) return super.append(otherAction);
        
        ComponentAction other = (ComponentAction) otherAction;
        if(descriptor.equals(UNKNOWN)) descriptor = other.descriptor;
        toAdd.removeAll(other.toRemove);
        toAdd.removeAll(other.toRemoveIncidental);
        toRemove.removeAll(other.toAdd);
        toRemove.removeAll(other.toAddIncidental);
        toAddIncidental.removeAll(other.toRemove);
        toAddIncidental.removeAll(other.toRemoveIncidental);
        toRemoveIncidental.removeAll(other.toAdd);
        toRemoveIncidental.removeAll(other.toAddIncidental);
        
        toAdd.addAll(other.toAdd);
        toAddIncidental.addAll(other.toAddIncidental);
        toRemove.addAll(other.toRemove);
        toRemoveIncidental.addAll(other.toRemoveIncidental);
        return this;
    }

    public void setCircuit(Circuit circ) {
        this.circuit = circ;
    }

    public Collection getAdditions() {
        if(toAddView == null) {
            toAddView = Collections.unmodifiableSet(toAdd);
        }
        return toAddView;
    }

    public Collection getRemovals() {
        if(toRemoveView == null) {
            toRemoveView = Collections.unmodifiableSet(toRemove);
        }
        return toRemoveView;
    }
    
    public Collection getIncidentalAdditions() {
        if(toAddIncidentalView == null) {
            toAddIncidentalView = Collections.unmodifiableSet(toAddIncidental);
        }
        return toAddIncidentalView;
    }
    
    public Collection getIncidentalRemovals() {
        if(toRemoveIncidentalView == null) {
            toRemoveIncidentalView = Collections.unmodifiableSet(toRemoveIncidental);
        }
        return toRemoveIncidentalView;
    }

    //
    // Action methods
    //
    public String getName() { return descriptor.get(); }

    public void doIt(Project proj) {
        for(Iterator it = toRemove.iterator(); it.hasNext(); ) {
            Component c = (Component) it.next();
            proj.getSelection().remove(c);
            circuit.remove(c);
        }
        for(Iterator it = toRemoveIncidental.iterator(); it.hasNext(); ) {
            Component c = (Component) it.next();
            proj.getSelection().remove(c);
            circuit.remove(c);
        }
        for(Iterator it = toAdd.iterator(); it.hasNext(); ) {
            circuit.add((Component) it.next());
        }
        for(Iterator it = toAddIncidental.iterator(); it.hasNext(); ) {
            circuit.add((Component) it.next());
        }
    }

    public void undo(Project proj) {
        for(Iterator it = toAdd.iterator(); it.hasNext(); ) {
            Component c = (Component) it.next();
            proj.getSelection().remove(c);
            circuit.remove(c);
        }
        for(Iterator it = toAddIncidental.iterator(); it.hasNext(); ) {
            Component c = (Component) it.next();
            proj.getSelection().remove(c);
            circuit.remove(c);
        }
        for(Iterator it = toRemove.iterator(); it.hasNext(); ) {
            circuit.add((Component) it.next());
        }
        for(Iterator it = toRemoveIncidental.iterator(); it.hasNext(); ) {
            circuit.add((Component) it.next());
        }
    }
}
