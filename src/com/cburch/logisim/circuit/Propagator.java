/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.util.PQueue;

class Propagator {
    static class SetData implements Comparable {
        int time;
        int serialNumber;
        CircuitState state; // state of circuit containing component
        Component cause;    // component emitting the value
        Location loc;       // the location at which value is emitted
        Value val;          // value being emitted
        SetData next = null;

        private SetData(int time, int serialNumber, CircuitState state,
                Location loc, Component cause, Value val) {
            this.time = time;
            this.serialNumber = serialNumber;
            this.state = state;
            this.cause = cause;
            this.loc = loc;
            this.val = val;
        }
        
        public int compareTo(Object other) {
            SetData o = (SetData) other;
            // Yes, these subtractions may overflow. This is intentional, as it
            // avoids potential wraparound problems as the counters increment.
            int ret = o.time - this.time;
            if(ret != 0) return ret;
            return o.serialNumber - this.serialNumber;
        }
        
        public SetData cloneFor(CircuitState newState) {
            Propagator newProp = newState.getPropagator();
            int dtime = newProp.clock - state.getPropagator().clock;
            SetData ret = new SetData(time + dtime,
                    newProp.setDataSerialNumber, newState, loc, cause, val);
            newProp.setDataSerialNumber++;
            if(this.next != null) ret.next = this.next.cloneFor(newState);
            return ret;
        }

        public String toString() {
            return loc + ":" + val + "(" + cause + ")";
        }
    }

    private static class ComponentPoint {
        Component cause;
        Location loc;

        public ComponentPoint(Component cause, Location loc) {
            this.cause = cause;
            this.loc = loc;
        }

        public int hashCode() {
            return 31 * cause.hashCode() + loc.hashCode();
        }

        public boolean equals(Object other) {
            if(!(other instanceof ComponentPoint)) return false;
            ComponentPoint o = (ComponentPoint) other;
            return this.cause.equals(o.cause) && this.loc.equals(o.loc);
        }
    }
    
    private static class Listener implements AttributeListener {
        WeakReference prop;
        
        public Listener(Propagator propagator) {
            prop = new WeakReference(propagator);
        }
        
        public void attributeListChanged(AttributeEvent e) { }

        public void attributeValueChanged(AttributeEvent e) {
            Propagator p = (Propagator) prop.get();
            if(p == null) {
                e.getSource().removeAttributeListener(this);
            } else if(e.getAttribute().equals(Options.sim_rand_attr)) {
                p.updateRandomness();
            }
        }
    }

    private CircuitState    root; // root of state tree
    
    /** The number of clock cycles to let pass before deciding that the
     * circuit is oscillating.
     */
    private int simLimit = 1000;

    /** On average, one out of every 2**simRandomShift propagations
     * through a component is delayed one step more than the component
     * requests. This noise is intended to address some circuits that would
     * otherwise oscillate within Logisim (though they wouldn't oscillate in
     * practice). */
    private volatile int simRandomShift;

    private PQueue  toProcess = new PQueue();
    private int clock = 0;
    private boolean isOscillating = false;
    private boolean oscAdding = false;
    private PropagationPoints oscPoints = new PropagationPoints(); 
    private int  ticks = 0;
    private Random noiseSource = new Random();
    private int noiseCount = 0;
    private int setDataSerialNumber = 0;
    
    static int lastId = 0;
    int id = lastId++;

    public Propagator(CircuitState root) {
        this.root = root;
        Listener l = new Listener(this);
        root.getProject().getOptions().getAttributeSet().addAttributeListener(l);
        updateRandomness();
    }
    
    private void updateRandomness() {
        Options opts = root.getProject().getOptions();
        Object rand = opts.getAttributeSet().getValue(Options.sim_rand_attr);
        int val = ((Integer) rand).intValue();
        int logVal = 0;
        while((1 << logVal) < val) logVal++;
        simRandomShift = logVal;
    }

    boolean isOscillating() {
        return isOscillating;
    }

    public String toString() {
        return "Prop" + id;
    }
    
    public void drawOscillatingPoints(ComponentDrawContext context) {
        if(isOscillating) oscPoints.draw(context);
    }

    //
    // public methods
    //
    CircuitState getRootState() {
        return root;
    }
    
    void reset() {
        toProcess.clear();
        root.reset();
        isOscillating = false;
    }
    
    void propagate() {
        oscPoints.clear();
        clearDirtyPoints();
        clearDirtyComponents();

        int oscThreshold = simLimit;
        int logThreshold = 3 * oscThreshold / 4;
        int iters = 0;
        while(!toProcess.isEmpty()) {
            iters++;
            
            if(iters < logThreshold) {
                stepInternal(null);
            } else if(iters < oscThreshold) {
                oscAdding = true;
                stepInternal(oscPoints);
            } else {
                isOscillating = true;
                oscAdding = false;
                return;
            }
        }
        isOscillating = false;
        oscAdding = false;
        oscPoints.clear();
    }
    
    void step(PropagationPoints changedPoints) {
        oscPoints.clear();
        clearDirtyPoints();
        clearDirtyComponents();
        
        PropagationPoints oldOsc = oscPoints;
        oscAdding = changedPoints != null;
        oscPoints = changedPoints;
        stepInternal(changedPoints);
        oscAdding = false;
        oscPoints = oldOsc;
    }
    
    private void stepInternal(PropagationPoints changedPoints) {
        if(toProcess.isEmpty()) return;
        
        // update clock
        clock = ((SetData) toProcess.peek()).time;

        // propagate all values for this clock tick
        HashMap visited = new HashMap(); // State -> set of ComponentPoints handled
        while(true) {
            SetData data = (SetData) toProcess.peek();
            if(data == null || data.time != clock) break;
            toProcess.remove();
            CircuitState state = data.state;

            // if it's already handled for this clock tick, continue
            HashSet handled = (HashSet) visited.get(state);
            if(handled != null) {
                if(!handled.add(new ComponentPoint(data.cause, data.loc))) continue;
            } else {
                handled = new HashSet();
                visited.put(state, handled);
                handled.add(new ComponentPoint(data.cause, data.loc));
            }
            
            /*DEBUGGING - comment out
            Simulator.log(data.time + ": proc " + data.loc + " in "
                    + data.state + " to " + data.val
                    + " by " + data.cause); // */
            
            if(changedPoints != null) changedPoints.add(state, data.loc);

            // change the information about value
            SetData oldHead = (SetData) state.causes.get(data.loc);
            Value   oldVal  = computeValue(oldHead);
            SetData newHead = addCause(state, oldHead, data);
            Value   newVal  = computeValue(newHead);

            // if the value at point has changed, propagate it
            if(!newVal.equals(oldVal)) {
                state.markPointAsDirty(data.loc);
            }
        }

        clearDirtyPoints();
        clearDirtyComponents();
    }
    
    boolean isPending() {
        return !toProcess.isEmpty();
    }

    /*TODO for the SimulatorPrototype class
    void step() {
        clock++;
        
        // propagate all values for this clock tick
        HashMap visited = new HashMap(); // State -> set of ComponentPoints handled
        while(!toProcess.isEmpty()) {
            SetData data;
            data = (SetData) toProcess.peek();
            if(data.time != clock) break;
            toProcess.remove();
            CircuitState state = data.state;

            // if it's already handled for this clock tick, continue
            HashSet handled = (HashSet) visited.get(state);
            if(handled != null) {
                if(!handled.add(new ComponentPoint(data.cause, data.loc))) continue;
            } else {
                handled = new HashSet();
                visited.put(state, handled);
                handled.add(new ComponentPoint(data.cause, data.loc));
            }
            
            if(oscAdding) oscPoints.add(state, data.loc);

            // change the information about value
            SetData oldHead = (SetData) state.causes.get(data.loc);
            Value   oldVal  = computeValue(oldHead);
            SetData newHead = addCause(state, oldHead, data);
            Value   newVal  = computeValue(newHead);

            // if the value at point has changed, propagate it
            if(!newVal.equals(oldVal)) {
                state.markPointAsDirty(data.loc);
            }
        }

        clearDirtyPoints();
        clearDirtyComponents();
    } */
    
    void locationTouched(CircuitState state, Location loc) {
        if(oscAdding) oscPoints.add(state, loc);
    }

    //
    // package-protected helper methods
    //
    void setValue(CircuitState state, Location pt, Value val,
            Component cause, int delay) {
        if(cause instanceof Wire || cause instanceof Splitter) return;
        if(delay <= 0) {
            delay = 1;
        }
        int randomShift = simRandomShift;
        if(randomShift > 0) { // random noise is turned on
            // multiply the delay by 32 so that the random noise
            // only changes the delay by 3%.
            delay <<= randomShift;
            if(!(cause instanceof Subcircuit)) {
                if(noiseCount > 0) {
                    noiseCount--;
                } else {
                    delay++;
                    noiseCount = noiseSource.nextInt(1 << randomShift);
                }
            }
        }
        toProcess.add(new SetData(clock + delay, setDataSerialNumber,
                state, pt, cause, val));
        /*DEBUGGING - comment out
        Simulator.log(clock + ": set " + pt + " in "
                + state + " to " + val
                + " by " + 
                cause + " after " + delay); //*/

        setDataSerialNumber++;
    }

    boolean tick() {
        ticks++;
        return root.tick(ticks);
    }

    //
    // private methods
    //
    void checkComponentEnds(CircuitState state, Component comp) {
        for(Iterator it = comp.getEnds().iterator(); it.hasNext(); ) {
            EndData end = (EndData) it.next();
            Location   loc      = end.getLocation();
            SetData oldHead = (SetData) state.causes.get(loc);
            Value   oldVal  = computeValue(oldHead);
            SetData newHead = removeCause(state, oldHead, loc, comp);
            Value   newVal  = computeValue(newHead);
            Value   wireVal = state.getValueByWire(loc);

            if(!newVal.equals(oldVal) || wireVal != null) {
                state.markPointAsDirty(loc);
            }
            if(wireVal != null) state.setValueByWire(loc, Value.NIL);
        }
    }

    private void clearDirtyPoints() {
        root.processDirtyPoints();
    }

    private void clearDirtyComponents() {
        root.processDirtyComponents();
    }

    private SetData addCause(CircuitState state, SetData head,
            SetData data) {
        if(data.val == null || data.val.isUnknown()) { // actually, it should be removed
            return removeCause(state, head, data.loc, data.cause);
        }

        HashMap causes = state.causes;

        // first check whether this is change of previous info.
        boolean replaced = false;
        for(SetData n = head; n != null; n = n.next) {
            if(n.cause == data.cause) {
                n.val = data.val;
                replaced = true;
                break;
            }
        }

        // otherwise, insert to list of causes
        if(!replaced) {
            if(head == null) {
                causes.put(data.loc, data);
                head = data;
            } else {
                data.next = head.next;
                head.next = data;
            }
        }

        return head;
    }

    private SetData removeCause(CircuitState state, SetData head,
            Location loc, Component cause) {
        HashMap causes = state.causes;
        if(head == null) {
            ;
        } else if(head.cause == cause) {
            head = head.next;
            if(head == null) causes.remove(loc);
            else causes.put(loc, head);
        } else {
            SetData prev = head;
            SetData cur = head.next;
            while(cur != null) {
                if(cur.cause == cause) {
                    prev.next = cur.next;
                    break;
                }
                prev = cur;
                cur = cur.next;
            }
        }
        return head;
    }

    //
    // static methods
    //
    static Value computeValue(SetData causes) {
        if(causes == null) return Value.NIL;
        Value ret = causes.val;
        for(SetData n = causes.next; n != null; n = n.next) {
            ret = ret.combine(n.val);
        }
        return ret;
    }

}
