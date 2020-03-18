/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.IteratorUtil;
import com.cburch.logisim.util.SmallSet;

class CircuitWires {
    static class SplitterData {
        WireBundle[] end_bundle; // PointData associated with each end

        SplitterData(int fan_out) {
            end_bundle = new WireBundle[fan_out + 1];
        }
    }

    private static class ThreadBundle {
        int loc;
        WireBundle b;
        ThreadBundle(int loc, WireBundle b) {
            this.loc = loc;
            this.b = b;
        }
    }

    static class State {
        BundleMap bundleMap;
        HashMap thr_values = new HashMap(); // WireThread -> Value

        State(BundleMap bundleMap) {
            this.bundleMap = bundleMap;
        }
        
        public Object clone() {
            State ret = new State(this.bundleMap);
            ret.thr_values.putAll(this.thr_values);
            return ret;
        }
    }

    static class BundleMap {
        boolean computed = false;
        HashMap pointBundles = new HashMap(); // Point -> WireBundle
        HashSet bundles = new HashSet();
        boolean isValid = true;
        // NOTE: It would make things more efficient if we also had
        // a set of just the first bundle in each tree.
        HashSet incompatibilityData = null;

        HashSet getWidthIncompatibilityData() {
            return incompatibilityData;
        }

        void addWidthIncompatibilityData(WidthIncompatibilityData e) {
            if(incompatibilityData == null) incompatibilityData = new HashSet();
            incompatibilityData.add(e);
        }

        WireBundle getBundleAt(Location p) {
            return (WireBundle) pointBundles.get(p);
        }

        WireBundle createBundleAt(Location p) {
            WireBundle ret = (WireBundle) pointBundles.get(p);
            if(ret == null) {
                ret = new WireBundle();
                pointBundles.put(p, ret);
                ret.points.add(p);
                bundles.add(ret);
            }
            return ret;
        }

        boolean isValid() {
            return isValid;
        }

        void invalidate() {
            isValid = false;
        }

        void setBundleAt(Location p, WireBundle b) {
            pointBundles.put(p, b);
        }

        Set getBundlePoints() {
            return pointBundles.keySet();
        }

        Set getBundles() {
            return bundles;
        }

        synchronized void markComputed() {
            computed = true;
            notifyAll();
        }

        synchronized void waitUntilComputed() {
            while(!computed) {
                try { wait(); } catch(InterruptedException e) { }
            }
        }
    }

    // user-given data
    private HashSet wires = new HashSet(); // of Wires
    private HashSet splitters = new HashSet(); // of Splitters
    final CircuitPoints points = new CircuitPoints();

    // derived data
    private Bounds bounds = Bounds.EMPTY_BOUNDS;
    private BundleMap bundleMap = null;

    CircuitWires() { }

    //
    // query methods
    //
    Set getWidthIncompatibilityData() {
        return getBundleMap().getWidthIncompatibilityData();
    }

    void ensureComputed() {
        getBundleMap();
    }

    BitWidth getWidth(Location q) {
        BitWidth det = points.getWidth(q);
        if(det != BitWidth.UNKNOWN) return det;

        BundleMap bmap = getBundleMap();
        if(!bmap.isValid()) return BitWidth.UNKNOWN;
        WireBundle qb = bmap.getBundleAt(q);
        if(qb != null && qb.isValid()) return qb.getWidth();

        return BitWidth.UNKNOWN;
    }

    Location getWidthDeterminant(Location q) {
        BitWidth det = points.getWidth(q);
        if(det != BitWidth.UNKNOWN) return q;

        WireBundle qb = getBundleMap().getBundleAt(q);
        if(qb != null && qb.isValid()) return qb.getWidthDeterminant();

        return q;
    }

    Iterator getComponents() {
        return IteratorUtil.createJoinedIterator(splitters.iterator(),
            wires.iterator());
    }

    Set getWires() {
        return wires;
    }

    Bounds getWireBounds() {
        if(bounds == Bounds.EMPTY_BOUNDS) {
            recomputeBounds();
        }
        return bounds;
    }
    
    WireBundle getWireBundle(Location query) {
        BundleMap bmap = getBundleMap();
        return bmap.getBundleAt(query);
    }

    //
    // action methods
    //
    // NOTE: this could be made much more efficient in most cases to
    // avoid voiding the bundle map.
    void add(Component comp) {
        if(comp instanceof Wire) {
            addWire((Wire) comp);
        } else if(comp instanceof Splitter) {
            splitters.add(comp);
        }
        points.add(comp);
        voidBundleMap();
    }

    void remove(Component comp) {
        if(comp instanceof Wire) {
            removeWire((Wire) comp);
        } else if(comp instanceof Splitter) {
            splitters.remove(comp);
        }
        points.remove(comp);
        voidBundleMap();
    }
    
    void add(Component comp, EndData end) {
        points.add(comp, end);
        voidBundleMap();
    }
    
    void remove(Component comp, EndData end) {
        points.remove(comp, end);
        voidBundleMap();
    }

    private void addWire(Wire w) {
        wires.add(w);

        if(bounds != Bounds.EMPTY_BOUNDS) { // update bounds
            bounds = bounds.add(w.e0).add(w.e1);
        }
    }

    private void removeWire(Wire w) {
        boolean removed = wires.remove(w);
        if(!removed) return;

        if(bounds != Bounds.EMPTY_BOUNDS) {
            // bounds is valid - invalidate if endpoint on border
            Bounds smaller = bounds.expand(-2);
            if(!smaller.contains(w.e0) || !smaller.contains(w.e1)) {
                bounds = Bounds.EMPTY_BOUNDS;
            }
        }
    }

    //
    // utility methods
    //
    void propagate(CircuitState circState, Set points) {
        BundleMap map = getBundleMap();
        SmallSet dirtyThreads = new SmallSet(); // affected threads

        // get state, or create a new one if current state is outdated
        State s = circState.getWireData();
        if(s == null || s.bundleMap != map) {
            // if it is outdated, we need to compute for all threads
            s = new State(map);
            for(Iterator it = map.getBundles().iterator(); it.hasNext(); ) {
                WireBundle b = (WireBundle) it.next();
                WireThread[] th = b.threads;
                if(b.isValid() && th != null) {
                    for(int i = 0; i < th.length; i++) {
                        dirtyThreads.add(th[i]);
                    }
                }
            }
            circState.setWireData(s);
        }

        // determine affected threads, and set values for unwired points
        for(Iterator it = points.iterator(); it.hasNext(); ) {
            Location p = (Location) it.next();
            WireBundle pb = map.getBundleAt(p);
            if(pb == null) { // point is not wired
                circState.setValueByWire(p, circState.getComponentOutputAt(p));
            } else {
                WireThread[] th = pb.threads;
                if(!pb.isValid() || th == null) {
                    // immediately propagate NILs across invalid bundles
                    SmallSet pbPoints = pb.points;
                    if(pbPoints == null) {
                        circState.setValueByWire(p, Value.NIL);
                    } else {
                        for(Iterator it2 = pbPoints.iterator(); it2.hasNext(); ) {
                            circState.setValueByWire((Location) it2.next(), Value.NIL);
                        }
                    }
                } else {
                    for(int i = 0; i < th.length; i++) {
                        dirtyThreads.add(th[i]);
                    }
                }
            }
        }

        if(dirtyThreads.isEmpty()) return;

        // determine values of affected threads
        HashSet bundles = new HashSet();
        for(Iterator it = dirtyThreads.iterator(); it.hasNext(); ) {
            WireThread t = (WireThread) it.next();
            Value v = getThreadValue(circState, t);
            s.thr_values.put(t, v);
            bundles.addAll(t.getBundles());
        }

        // now propagate values through circuit
        for(Iterator it = bundles.iterator(); it.hasNext(); ) {
            ThreadBundle tb = (ThreadBundle) it.next();
            WireBundle b = tb.b;

            Value bv = null;
            if(!b.isValid() || b.threads == null) {
                ; // do nothing
            } else if(b.threads.length == 1) {
                bv = (Value) s.thr_values.get(b.threads[0]);
            } else {
                Value[] tvs = new Value[b.threads.length];
                boolean tvs_valid = true;
                for(int i = 0; i < tvs.length; i++) {
                    Value tv = (Value) s.thr_values.get(b.threads[i]);
                    if(tv == null) { tvs_valid = false; break; }
                    tvs[i] = tv;
                }
                if(tvs_valid) bv = Value.create(tvs);
            }

            if(bv != null) {
                for(Iterator it2 = b.points.iterator(); it2.hasNext(); ) {
                    Location p = (Location) it2.next();
                    circState.setValueByWire(p, bv);
                }
            }
        }
    }

    void draw(ComponentDrawContext context, Collection hidden) {
        boolean showState = context.getShowState();
        CircuitState state = context.getCircuitState();
        Graphics g = context.getGraphics();
        g.setColor(Color.BLACK);
        GraphicsUtil.switchToWidth(g, Wire.WIDTH);

        BundleMap bmap = getBundleMap();
        boolean isValid = bmap.isValid();
        if(hidden == null || hidden.size() == 0) {
            for(Iterator it = wires.iterator(); it.hasNext(); ) {
                Wire w = (Wire) it.next();
                Location s = w.e0;
                Location t = w.e1;
                WireBundle wb = bmap.getBundleAt(s);
                if(!wb.isValid()) {
                    g.setColor(Value.WIDTH_ERROR_COLOR);
                } else if(showState) {
                    if(!isValid) g.setColor(Value.NIL_COLOR);
                    else         g.setColor(state.getValue(s).getColor());
                } else {
                    g.setColor(Color.BLACK);
                }
                g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
            }

            for(Iterator it = points.getSplitLocations().iterator(); it.hasNext(); ) {
                Location loc = (Location) it.next();
                if(points.getComponentCount(loc) > 2) {
                    WireBundle wb = bmap.getBundleAt(loc);
                    if(wb != null) {
                        if(!wb.isValid()) {
                            g.setColor(Value.WIDTH_ERROR_COLOR);
                        } else if(showState) {
                            if(!isValid) g.setColor(Value.NIL_COLOR);
                            else         g.setColor(state.getValue(loc).getColor());
                        } else {
                            g.setColor(Color.BLACK);
                        }
                        g.fillOval(loc.getX() - 4, loc.getY() - 4, 8, 8);
                    }
                }
            }
        } else {
            for(Iterator it = wires.iterator(); it.hasNext(); ) {
                Wire w = (Wire) it.next();
                if(!hidden.contains(w)) {
                    Location s = w.e0;
                    Location t = w.e1;
                    WireBundle wb = bmap.getBundleAt(s);
                    if(!wb.isValid()) {
                        g.setColor(Value.WIDTH_ERROR_COLOR);
                    } else if(showState) {
                        if(!isValid) g.setColor(Value.NIL_COLOR);
                        else         g.setColor(state.getValue(s).getColor());
                    } else {
                        g.setColor(Color.BLACK);
                    }
                    g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
                }
            }

            // this is just an approximation, but it's good enough since
            // the problem is minor, and hidden only exists for a short
            // while at a time anway.
            for(Iterator it = points.getSplitLocations().iterator(); it.hasNext(); ) {
                Location loc = (Location) it.next();
                if(points.getComponentCount(loc) > 2) {
                    int icount = 0;
                    for(Iterator it2 = points.getComponents(loc).iterator(); it2.hasNext(); ) {
                        if(!hidden.contains(it2.next())) ++icount;
                    }
                    if(icount > 2) {
                        WireBundle wb = bmap.getBundleAt(loc);
                        if(wb != null) {
                            if(!wb.isValid()) {
                                g.setColor(Value.WIDTH_ERROR_COLOR);
                            } else if(showState) {
                                if(!isValid) g.setColor(Value.NIL_COLOR);
                                else         g.setColor(state.getValue(loc).getColor());
                            } else {
                                g.setColor(Color.BLACK);
                            }
                            g.fillOval(loc.getX() - 4, loc.getY() - 4, 8, 8);
                        }
                    }
                }
            }
        }
    }

    //
    // helper methods
    //
    private void voidBundleMap() {
        bundleMap = null;
    }

    private BundleMap getBundleMap() {
        // Maybe we already have a valid bundle map (or maybe
        // one is in progress).
        BundleMap ret = bundleMap;
        if(ret != null) {
            ret.waitUntilComputed();
            return ret;
        }
        try {
            // Ok, we have to create our own.
            ret = new BundleMap();
            bundleMap = ret;
            computeBundleMap(ret);
        } catch(RuntimeException ex) {
            ret.invalidate();
            ret.markComputed();
            throw ex;
        } finally {
            // Mark the BundleMap as computed in case anybody is waiting for the result.
            ret.markComputed();
        }
        return ret;
    }

    // To be called by getBundleMap only
    private void computeBundleMap(BundleMap ret) {
        // make a WireBundle object for each tree of connected wires
        for(Iterator it = wires.iterator(); it.hasNext(); ) {
            Wire w = (Wire) it.next();
            WireBundle b0 = ret.getBundleAt(w.e0);
            if(b0 == null) {
                WireBundle b1 = ret.createBundleAt(w.e1);
                b1.points.add(w.e0); ret.setBundleAt(w.e0, b1);
            } else {
                WireBundle b1 = ret.getBundleAt(w.e1);
                if(b1 == null) { // t1 doesn't exist
                    b0.points.add(w.e1); ret.setBundleAt(w.e1, b0);
                } else {
                    b1.unite(b0); // unite b0 and b1
                }
            }
        }

        // merge any WireBundle objects united by previous step
        for(Iterator it = ret.getBundles().iterator(); it.hasNext(); ) {
            WireBundle b = (WireBundle) it.next();
            WireBundle bpar = b.find();
            if(bpar != b) { // b isn't group's representative
                for(Iterator it2 = b.points.iterator(); it2.hasNext(); ) {
                    Location pt = (Location) it2.next();
                    ret.setBundleAt(pt, bpar);
                    bpar.points.add(pt);
                }
                it.remove();
            }
        }

        // make a WireBundle object for each end of a splitter
        for(Iterator it = splitters.iterator(); it.hasNext(); ) {
            Splitter spl = (Splitter) it.next();
            List ends = spl.getEnds();
            int num_ends = ends.size();
            for(int index = 0; index < num_ends; index++) {
                EndData end = (EndData) ends.get(index);
                Location p = end.getLocation();
                WireBundle pb = ret.createBundleAt(p);
                pb.setWidth(end.getWidth(), p);
            }
        }

        // set the width for each bundle whose size is known
        // based on components
        for(Iterator it = ret.getBundlePoints().iterator(); it.hasNext(); ) {
            Location p = (Location) it.next();
            WireBundle pb = ret.getBundleAt(p);
            BitWidth width = points.getWidth(p);
            if(width != BitWidth.UNKNOWN) {
                pb.setWidth(width, p);
            }
        }

        // determine the bundles at the end of each splitter
        for(Iterator it = splitters.iterator(); it.hasNext(); ) {
            Splitter spl = (Splitter) it.next();
            List ends = spl.getEnds();
            int num_ends = ends.size();
            for(int index = 0; index < num_ends; index++) {
                EndData end = (EndData) ends.get(index);
                Location p = end.getLocation();
                WireBundle pb = ret.getBundleAt(p);
                pb.setWidth(end.getWidth(), p);
                spl.wire_data.end_bundle[index] = pb;
            }
        }

        // unite threads going through splitters
        for(Iterator it = splitters.iterator(); it.hasNext(); ) {
            Splitter spl = (Splitter) it.next();
            synchronized(spl) {
                SplitterAttributes spl_attrs = (SplitterAttributes) spl.getAttributeSet();
                byte[] bit_end = spl_attrs.bit_end;
                SplitterData spl_data = spl.wire_data;
                WireBundle from_bundle = spl_data.end_bundle[0];
                if(from_bundle == null || !from_bundle.isValid()) continue;
    
                for(int i = 0; i < bit_end.length; i++) {
                    int j = bit_end[i];
                    if(j > 0) {
                        int thr = spl.bit_thread[i];
                        WireBundle to_bundle = spl_data.end_bundle[j];
                        if(to_bundle.isValid()) {
                            if(i >= from_bundle.threads.length) {
                                throw new ArrayIndexOutOfBoundsException("from " + i + " of " + from_bundle.threads.length);
                            }
                            if(thr >= to_bundle.threads.length) {
                                throw new ArrayIndexOutOfBoundsException("to " + thr + " of " + to_bundle.threads.length);
                            }
                            from_bundle.threads[i].unite(to_bundle.threads[thr]);
                        }
                    }
                }
            }
        }

        // merge any threads united by previous step
        for(Iterator it = ret.getBundles().iterator(); it.hasNext(); ) {
            WireBundle b = (WireBundle) it.next();
            if(b.isValid() && b.threads != null) {
                for(int i = 0; i < b.threads.length; i++) {
                    WireThread thr = b.threads[i].find();
                    b.threads[i] = thr;
                    thr.getBundles().add(new ThreadBundle(i, b));
                }
            }
        }

        // All threads are sewn together! Compute the exception set before leaving
        Collection exceptions = points.getWidthIncompatibilityData();
        if(exceptions != null && exceptions.size() > 0) {
            for(Iterator it = exceptions.iterator(); it.hasNext(); ) {
                ret.addWidthIncompatibilityData((WidthIncompatibilityData) it.next());
            }
        }
        for(Iterator it = ret.getBundles().iterator(); it.hasNext(); ) {
            WireBundle b = (WireBundle) it.next();
            WidthIncompatibilityData e = b.getWidthIncompatibilityData();
            if(e != null) ret.addWidthIncompatibilityData(e);
        }
    }

    private Value getThreadValue(CircuitState state, WireThread t) {
        Value ret = Value.UNKNOWN;
        Iterator it = t.getBundles().iterator();
        while(it.hasNext()) {
            ThreadBundle tb = (ThreadBundle) it.next();
            Iterator it2 = tb.b.points.iterator();
            while(it2.hasNext()) {
                Location p = (Location) it2.next();
                Value val = state.getComponentOutputAt(p);
                if(val != null && val != Value.NIL) {
                    ret = ret.combine(val.get(tb.loc));
                }
            }
        }
        return ret;
    }

    private void recomputeBounds() {
        Iterator it = wires.iterator();
        if(!it.hasNext()) {
            bounds = Bounds.EMPTY_BOUNDS;
            return;
        }

        Wire w = (Wire) it.next();
        int xmin = w.e0.getX();
        int ymin = w.e0.getY();
        int xmax = w.e1.getX();
        int ymax = w.e1.getY();
        while(it.hasNext()) {
            w = (Wire) it.next();
            int x0 = w.e0.getX(); if(x0 < xmin) xmin = x0;
            int x1 = w.e1.getX(); if(x1 > xmax) xmax = x1;
            int y0 = w.e0.getY(); if(y0 < ymin) ymin = y0;
            int y1 = w.e1.getY(); if(y1 > ymax) ymax = y1;
        }
        bounds = Bounds.create(xmin, ymin,
            xmax - xmin + 1, ymax - ymin + 1);
    }
}
