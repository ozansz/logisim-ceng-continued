/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.ArraySet;
import com.cburch.logisim.util.SmallSet;

public class CircuitState {
    private class MyCircuitListener implements CircuitListener {
        public void circuitChanged(CircuitEvent event) {
            int action = event.getAction();
            if(action == CircuitEvent.ACTION_ADD) {
                Component comp = (Component) event.getData();
                if(comp instanceof Wire) {
                    Wire w = (Wire) comp;
                    markPointAsDirty(w.getEnd0());
                    markPointAsDirty(w.getEnd1());
                } else {
                    markComponentAsDirty(comp);
                    if(base != null) base.checkComponentEnds(CircuitState.this, comp);
                }
            } else if(action == CircuitEvent.ACTION_REMOVE) {
                Component comp = (Component) event.getData();
                if(comp instanceof Subcircuit) {
                    // disconnect from tree
                    CircuitState substate = (CircuitState) getData(comp);
                    if(substate != null && substate.parentComp == comp) {
                        substates.remove(substate);
                        substate.parentState = null;
                        substate.parentComp = null;
                    }
                }

                if(comp instanceof Wire) {
                    Wire w = (Wire) comp;
                    markPointAsDirty(w.getEnd0());
                    markPointAsDirty(w.getEnd1());
                } else {
                    if(base != null) base.checkComponentEnds(CircuitState.this, comp);
                    dirtyComponents.remove(comp);
                }
            } else if(action == CircuitEvent.ACTION_CLEAR) {
                substates.clear();
                wireData = null;
                componentData.clear();
                values.clear();
                dirtyComponents.clear();
                dirtyPoints.clear();
                causes.clear();
            } else if(action == CircuitEvent.ACTION_CHANGE) {
                Component comp = (Component) event.getData();
                markComponentAsDirty(comp);
                if(base != null) base.checkComponentEnds(CircuitState.this, comp);
            } else if(action == CircuitEvent.ACTION_INVALIDATE) {
                Component comp = (Component) event.getData();
                markComponentAsDirty(comp);
                if(base != null) base.checkComponentEnds(CircuitState.this, comp);
            }
        }
    }

    private MyCircuitListener myCircuitListener = new MyCircuitListener();
    private Propagator base = null; // base of tree of CircuitStates
    private Project proj; // project where circuit lies
    private Circuit circuit; // circuit being simulated

    private CircuitState parentState = null; // parent in tree of CircuitStates
    private Subcircuit parentComp = null; // subcircuit component containing this state
    private ArraySet substates = new ArraySet(); // children in tree of CircuitStates

    private CircuitWires.State wireData = null;
    private HashMap componentData = new HashMap(); // Components -> Objects
    private HashMap values = new HashMap(); // Points -> Values
    private SmallSet dirtyComponents = new SmallSet(); // of Components
    private SmallSet dirtyPoints = new SmallSet(); // of Points
    HashMap causes = new HashMap(); // of SetDatas, managed by Propagator

    private static int lastId = 0;
    private int id = lastId++;

    public CircuitState(Project proj, Circuit circuit) {
        this.proj = proj;
        this.circuit = circuit;
        circuit.addCircuitListener(myCircuitListener);
    }
    
    Project getProject() {
        return proj;
    }
    
    Subcircuit getSubcircuit() {
        return parentComp;
    }
    
    public CircuitState cloneState() {
        CircuitState ret = new CircuitState(proj, circuit);
        ret.copyFrom(this, new Propagator(ret));
        ret.parentComp = null;
        ret.parentState = null;
        return ret;
    }
    
    private void copyFrom(CircuitState src, Propagator base) {
        this.base = base;
        this.parentComp = src.parentComp;
        this.parentState = src.parentState;
        HashMap substateData = new HashMap();
        for(Iterator it = src.substates.iterator(); it.hasNext(); ) {
            CircuitState oldSub = (CircuitState) it.next();
            CircuitState newSub = new CircuitState(src.proj, oldSub.circuit);
            newSub.copyFrom(oldSub, base);
            this.substates.add(newSub);
            substateData.put(oldSub, newSub);
        }
        for(Iterator it = src.componentData.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Object oldValue = src.componentData.get(key);
            if(oldValue instanceof CircuitState) {
                Object newValue = substateData.get(oldValue);
                if(newValue != null) this.componentData.put(key, newValue);
                else this.componentData.remove(key);
            } else {
                Object newValue;
                if(oldValue instanceof ComponentState) {
                    newValue = ((ComponentState) oldValue).clone();
                } else {
                    newValue = oldValue;
                }
                this.componentData.put(key, newValue);
            }
        }
        for(Iterator it = src.causes.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Propagator.SetData oldValue = (Propagator.SetData) src.causes.get(key);
            Propagator.SetData newValue = oldValue.cloneFor(this);
            this.causes.put(key, newValue);
        }
        if(src.wireData != null) {
            this.wireData = (CircuitWires.State) src.wireData.clone();
        }
        this.values.putAll(src.values);
        this.dirtyComponents.addAll(src.dirtyComponents);
        this.dirtyPoints.addAll(src.dirtyPoints);
    }

    public String toString() {
        return "State" + id + "[" + circuit.getName() + "]";
    }

    //
    // public methods
    //
    public Circuit getCircuit() {
        return circuit;
    }
    
    public CircuitState getParentState() {
        return parentState;
    }
    
    public Set getSubstates() { // returns Set of CircuitStates
        return substates;
    }

    Propagator getPropagator() {
        if(base == null) {
            base = new Propagator(this);
            markAllComponentsDirty();
        }
        return base;
    }
    
    public void drawOscillatingPoints(ComponentDrawContext context) {
        if(base != null) base.drawOscillatingPoints(context);
    }

    public Object getData(Component comp) {
        return componentData.get(comp);
    }

    public void setData(Component comp, Object data) {
        if(comp instanceof Subcircuit) {
            CircuitState oldState = (CircuitState) componentData.get(comp);
            CircuitState newState = (CircuitState) data;
            if(oldState != newState) {
                // There's something new going on with this subcircuit.
                // Maybe the subcircuit is new, or perhaps it's being
                // removed.
                if(oldState != null && oldState.parentComp == comp) {
                    // it looks like it's being removed
                    substates.remove(oldState);
                    oldState.parentState = null;
                    oldState.parentComp = null;
                }
                if(newState != null && newState.parentState != this) {
                    // this is the first time I've heard about this CircuitState
                    substates.add(newState);
                    newState.base = this.base;
                    newState.parentState = this;
                    newState.parentComp = (Subcircuit) comp;
                    newState.markAllComponentsDirty();
                }
            }
        }
        componentData.put(comp, data);
    }

    public Value getValue(Location pt) {
        Value ret = (Value) values.get(pt);
        if(ret != null) return ret;

        BitWidth wid = circuit.getWidth(pt);
        return Value.createUnknown(wid);
    }

    public void setValue(Location pt, Value val, Component cause, int delay) {
        if(base != null) base.setValue(this, pt, val, cause, delay);
    }

    public void markComponentAsDirty(Component comp) {
        dirtyComponents.add(comp);
    }

    public void markPointAsDirty(Location pt) {
        dirtyPoints.add(pt);
    }

    //
    // methods for other classes within package
    //
    boolean isSubstate() {
        return parentState != null;
    }

    void processDirtyComponents() {
        if(!dirtyComponents.isEmpty()) {
            // This seeming wasted copy is to avoid ConcurrentModifications
            // if we used an iterator instead.
            Object[] toProcess = dirtyComponents.toArray();
            dirtyComponents.clear();
            for(int i = 0; i < toProcess.length; i++) {
                Component comp = (Component) toProcess[i];
                comp.propagate(this);
                if(comp instanceof Pin && parentState != null) {
                    // should be propagated in superstate
                    parentComp.propagate(parentState);
                }
            }
        }

        Object[] subs = substates.toArray();
        for(int i = 0, n = subs.length; i < n; i++) {
            CircuitState substate = (CircuitState) subs[i];
            substate.processDirtyComponents();
        }
    }

    void processDirtyPoints() {
        if(!dirtyPoints.isEmpty()) {
            circuit.wires.propagate(this, dirtyPoints);
            dirtyPoints.clear();
        }

        Object[] subs = substates.toArray();
        for(int i = 0, n = subs.length; i < n; i++) {
            CircuitState substate = (CircuitState) subs[i];
            substate.processDirtyPoints();
        }
    }
    
    void reset() {
        wireData = null;
        for(Iterator it = componentData.keySet().iterator(); it.hasNext(); ) {
            Object comp = (Object) it.next();
            if(!(comp instanceof Subcircuit)) it.remove();
        }
        values.clear();
        dirtyComponents.clear();
        dirtyPoints.clear();
        causes.clear();
        markAllComponentsDirty();
        
        for(Iterator it = substates.iterator(); it.hasNext(); ) {
            CircuitState sub = (CircuitState) it.next();
            sub.reset();
        }
    }

    boolean tick(int ticks) {
        boolean ret = false;
        ArrayList clocks = circuit.getClocks();
        for(int i = 0; i < clocks.size(); i++) {
            Clock clock = (Clock) clocks.get(i);
            ret |= clock.tick(this, ticks);
        }

        Object[] subs = substates.toArray();
        for(int i = 0, n = subs.length; i < n; i++) {
            CircuitState substate = (CircuitState) subs[i];
            ret |= substate.tick(ticks);
        }
        return ret;
    }

    CircuitWires.State getWireData() {
        return wireData;
    }

    void setWireData(CircuitWires.State data) {
        wireData = data;
    }

    Value getComponentOutputAt(Location p) {
        // for CircuitWires - to get values, ignoring wires' contributions
        Propagator.SetData cause_list
            = (Propagator.SetData) causes.get(p);
        return Propagator.computeValue(cause_list);
    }

    Value getValueByWire(Location p) {
        return (Value) values.get(p);
    }

    void setValueByWire(Location p, Value v) {
        // for CircuitWires - to set value at point
        boolean changed;
        if(v == Value.NIL) {
            Object old = values.remove(p);
            changed = (old != null && old != Value.NIL);
        } else {
            Object old = values.put(p, v);
            changed = !v.equals(old);
        }
        if(changed) {
            boolean found = false;
            for(Iterator it = circuit.getComponents(p).iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if(!(obj instanceof Wire) && !(obj instanceof Splitter)) {
                    found = true;
                    Component c = (Component) obj;
                    markComponentAsDirty(c);
                }
            }
            // NOTE: this will cause a double-propagation on components
            // whose outputs have just changed.
            
            if(found && base != null) base.locationTouched(this, p);
        }
    }

    //
    // private methods
    // 
    private void markAllComponentsDirty() {
        dirtyComponents.addAll(circuit.getNonWires());
    }
}
