/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ComponentAction;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.WireUtil;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.CollectionUtil;

class SelectionBase {
    Project proj;
    private ArrayList listeners = new ArrayList();

    final HashSet selected = new HashSet(); // of selected Components in circuit
    final HashSet lifted = new HashSet(); // of selected Components removed
    final Set unionSet = CollectionUtil.createUnmodifiableSetUnion(selected, lifted);
    
    /** We may have components inserted as lifted because of a Paste; we need to track
     * the circuit actions related to dropping those components into the circuit
     * so that the Paste can be properly undone. */
    private Action dropped = null;

    private Bounds bounds = Bounds.EMPTY_BOUNDS;
    private boolean shouldSnap = false;

    public SelectionBase(Project proj) {
        this.proj = proj;
    }
    
    //
    // listener methods
    //
    public void addListener(Selection.Listener l) {
        listeners.add(l);
    }

    public void removeListener(Selection.Listener l) {
        listeners.remove(l);
    }

    public void fireSelectionChanged() {
        Selection.Event e = new Selection.Event(this);
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            ((Selection.Listener) it.next()).selectionChanged(e);
        }
    }

    //
    // query methods
    //
    public Bounds getBounds() {
        if(bounds == null) {
            bounds = computeBounds(unionSet);
        }
        return bounds;
    }

    public Bounds getBounds(Graphics g) {
        if(unionSet.isEmpty()) {
            bounds = Bounds.EMPTY_BOUNDS;
        } else {
            Iterator it = unionSet.iterator();
            bounds = ((Component) it.next()).getBounds(g);
            while(it.hasNext()) {
                Component comp = (Component) it.next();
                Bounds bds = comp.getBounds(g);
                bounds = bounds.add(bds);
            }
        }
        return bounds;
    }

    public boolean shouldSnap() {
        return shouldSnap;
    }
    
    public boolean hasConflictWhenMoved(int dx, int dy) {
        return hasConflictTranslated(unionSet, dx, dy, false);
    }

    //
    // action methods
    //
    public void add(Component comp) {
        if(selected.add(comp)) {
            if(shouldSnapComponent(comp)) shouldSnap = true;
            bounds = null;
            fireSelectionChanged();
        }
    }
    
    public void addAll(Collection comps) {
        if(selected.addAll(comps)) {
            computeShouldSnap();
            bounds = null;
            fireSelectionChanged();
        }
    }
    
    // removes from selection - NOT from circuit
    public void remove(Component comp) {
        boolean removed = selected.remove(comp);
        if(lifted.remove(comp)) {
            removed = true;
            Action addAction = CircuitActions.addComponent(proj.getCurrentCircuit(), comp, false);
            addAction.doIt(proj);
            if(dropped != null) dropped = dropped.append(addAction);
        }

        if(removed) {
            if(shouldSnapComponent(comp)) computeShouldSnap();
            bounds = null;
            fireSelectionChanged();
        }
    }
    
    public void dropAll() {
        if(!lifted.isEmpty()) {
            ComponentAction action = CircuitActions.addComponents(proj.getCurrentCircuit(), lifted);
            action.doIt(proj);
            if(dropped != null) dropped = dropped.append(action);
            selected.addAll(action.getAdditions());
            lifted.clear();
        }
    }
    
    public void clear() {
        clear(true);
    }
    
    // removes all from selection - NOT from circuit
    public void clear(boolean dropLifted) {
        if(selected.isEmpty() && lifted.isEmpty()) return;
        
        if(dropLifted && !lifted.isEmpty()) {
            Action action = CircuitActions.addComponents(proj.getCurrentCircuit(), lifted);
            action.doIt(proj);
            if(dropped != null) dropped = dropped.append(action);
        }
        
        selected.clear();
        lifted.clear();
        shouldSnap = false;
        bounds = Bounds.EMPTY_BOUNDS;
        
        fireSelectionChanged();
    }
    
    public Action paste(final Clipboard clipboard) {
        return new Action() {
            private HashSet oldSelected;
            private Action oldDropped;
            private HashMap componentCopies;
            private AttributeSet oldOldAttributeSet;
            private AttributeSet oldAttributeSet;
            private Circuit oldAttributeCircuit;
            private Component oldAttributeComponent;
            
            public String getName() {
                return Strings.get("pasteClipboardAction");
            }

            public void doIt(Project proj) {
                AttributeTable attrTable = proj.getFrame().getAttributeTable();
                AttributeSet attrsCur = attrTable.getAttributeSet();
                oldOldAttributeSet = null;
                oldAttributeSet = attrTable.getAttributeSet();
                oldAttributeCircuit = proj.getCurrentCircuit();
                oldAttributeComponent = proj.getFrame().getCanvas().getHaloedComponent();

                AttributeSet attrsInter = null;
                Component compInter = null;
                if(attrsCur == clipboard.getOldAttributeSet() || attrsCur == null) {
                    Collection clipSel = clipboard.getComponents();
                    attrsInter = clipboard.getNewAttributeSet();
                    for(Iterator it = clipSel.iterator(); it.hasNext(); ) {
                        Component c = (Component) it.next();
                        if(c.getAttributeSet() == attrsInter) compInter = c;
                    }
                }

                oldSelected = new HashSet(selected);
                oldSelected.addAll(lifted);
                clear();
                oldDropped = dropped;
            
                componentCopies = copyComponents(clipboard.getComponents());
                restore(Collections.EMPTY_SET, componentCopies.values(),
                        new ComponentAction(proj.getCurrentCircuit()));
                fireSelectionChanged();

                Component attrComp = null;
                if(compInter != null) {
                    attrComp = (Component) componentCopies.get(compInter);
                }
                if(attrComp == null && proj != null) {
                    AttributeSet attrs = attrTable.getAttributeSet();
                    if(attrs == null) {
                        if(!selected.isEmpty()) {
                            attrComp = (Component) selected.iterator().next();
                        } else if(!lifted.isEmpty()) {
                            attrComp = (Component) lifted.iterator().next();
                        }
                    }
                }
                if(attrComp != null) {
                    AttributeSet attrs = attrComp.getAttributeSet();
                    oldOldAttributeSet = clipboard.getOldAttributeSet();
                    clipboard.setOldAttributeSet(attrs);
                    attrTable.setAttributeSet(attrs);
                    proj.getFrame().getCanvas().setHaloedComponent(proj.getCurrentCircuit(), attrComp);
                }
            }

            public void undo(Project proj) {
                if(proj != null) {
                    proj.getFrame().getAttributeTable().setAttributeSet(oldAttributeSet);
                    proj.getFrame().getCanvas().setHaloedComponent(oldAttributeCircuit, oldAttributeComponent);
                }
                if(oldOldAttributeSet != null) {
                    clipboard.setOldAttributeSet(oldOldAttributeSet);
                }
                dropped.undo(proj);
                restore(oldSelected, Collections.EMPTY_SET, oldDropped);
                fireSelectionChanged();
            }
        };
    }
    
    public Action deleteAll() {
        return new Action() {
            private HashSet oldSelected = new HashSet(selected);
            private HashSet oldLifted = new HashSet(lifted);
            private Action oldDropped = dropped;
            private Action deleteAction;
            
            public String getName() {
                return Strings.get("clearSelectionAction");
            }
            
            public void doIt(Project proj) {
                restore(Collections.EMPTY_SET, Collections.EMPTY_SET, null);
                
                deleteAction = CircuitActions.removeComponents(proj.getCurrentCircuit(), oldSelected);
                deleteAction.doIt(proj);
                
                fireSelectionChanged();
            }
            
            public void undo(Project proj) {
                deleteAction.undo(proj);
                restore(oldSelected, oldLifted, oldDropped);
                fireSelectionChanged();
                fireSelectionChanged();
            }
        };
    }
    
    public Action translateAll(final int dx, final int dy) {
        return new Action() {
            private HashSet oldSelected = new HashSet(selected);
            private HashSet oldLifted = new HashSet(lifted);
            private Action oldDropped = dropped;
            private HashMap oldState = new HashMap();
            private Component oldAttrsComp = null;
            private Component newAttrsComp = null;
            
            private Action deleteAction;
            private ComponentAction addSelectedAction;
            private ComponentAction addLiftedAction;
            
            public String getName() {
                return Strings.get("moveSelectionAction");
            }
            
            public void doIt(Project proj) {
                HashMap selectedAfter = copyComponents(selected, dx, dy);
                HashMap liftedAfter = copyComponents(lifted, dx, dy);

                Circuit circuit = proj.getCurrentCircuit();
                CircuitState circState = proj.getCircuitState();
                AttributeTable attrTable = proj.getFrame().getAttributeTable();
                AttributeSet oldAttrs = attrTable.getAttributeSet();
                Component oldAttrsComp = null;
                Component newAttrsComp = null;
                if(oldAttrs != null) {
                    for(Iterator it = selectedAfter.keySet().iterator(); it.hasNext(); ) {
                        Component comp = (Component) it.next();
                        if(comp.getAttributeSet() == oldAttrs) {
                            oldAttrsComp = (Component) comp;
                            newAttrsComp = (Component) selectedAfter.get(comp);
                            break;
                        }
                    }
                    for(Iterator it = liftedAfter.keySet().iterator(); it.hasNext(); ) {
                        Component comp = (Component) it.next();
                        if(comp.getAttributeSet() == oldAttrs) {
                            oldAttrsComp = (Component) comp;
                            newAttrsComp = (Component) liftedAfter.get(comp);
                            break;
                        }
                    }
                }
                
                if(circState != null) {
                    for(Iterator it = selected.iterator(); it.hasNext(); ) {
                        Component comp = (Component) it.next();
                        Object compState = circState.getData(comp);
                        if(compState != null) oldState.put(comp, compState);
                    }
                }

                restore(Collections.EMPTY_SET, Collections.EMPTY_SET, null);
                
                deleteAction = CircuitActions.removeComponents(circuit, oldSelected);
                deleteAction.doIt(proj);
                
                addSelectedAction = CircuitActions.addComponents(circuit,
                        WireUtil.mergeExclusive(selectedAfter.values()));
                addSelectedAction.doIt(proj);
                for(Iterator it = oldState.keySet().iterator(); it.hasNext(); ) {
                    Component oldComp = (Component) it.next();
                    Component newComp = (Component) selectedAfter.get(oldComp);
                    Object state = oldState.get(oldComp);
                    circState.setData(newComp, state);
                }
                restore(addSelectedAction.getAdditions(), Collections.EMPTY_SET, null);
                
                addLiftedAction = CircuitActions.addComponents(circuit,
                        WireUtil.mergeExclusive(liftedAfter.values()));
                addLiftedAction.doIt(proj);
                selected.addAll(addLiftedAction.getAdditions());
                
                fireSelectionChanged();

                if(newAttrsComp != null) {
                    this.oldAttrsComp = oldAttrsComp;
                    this.newAttrsComp = newAttrsComp;
                    proj.getFrame().getCanvas().setHaloedComponent(proj.getCurrentCircuit(), newAttrsComp);
                    attrTable.setAttributeSet(newAttrsComp.getAttributeSet());
                    
                    Clipboard clip = Clipboard.get();
                    if(clip != null && clip.getOldAttributeSet() == oldAttrsComp.getAttributeSet()) {
                        clip.setOldAttributeSet(newAttrsComp.getAttributeSet());
                    }
                }
                
                computeShouldSnap();
            }
            
            public void undo(Project proj) {
                addLiftedAction.undo(proj);
                addSelectedAction.undo(proj);
                deleteAction.undo(proj);
                for(Iterator it = oldState.keySet().iterator(); it.hasNext(); ) {
                    Component oldComp = (Component) it.next();
                    Object state = oldState.get(oldComp);
                    proj.getCircuitState().setData(oldComp, state);
                }
                restore(oldSelected, oldLifted, oldDropped);
                fireSelectionChanged();

                if(oldAttrsComp != null) {
                    proj.getFrame().getCanvas().setHaloedComponent(proj.getCurrentCircuit(), oldAttrsComp);
                    proj.getFrame().getAttributeTable().setAttributeSet(
                            oldAttrsComp.getAttributeSet());
                    
                    Clipboard clip = Clipboard.get();
                    if(clip != null && clip.getOldAttributeSet() == newAttrsComp.getAttributeSet()) {
                        clip.setOldAttributeSet(oldAttrsComp.getAttributeSet());
                    }
                }
                
                computeShouldSnap();
            }
            
        };
    }

    //
    // private methods
    //
    private void restore(Collection oldSelected, Collection oldLifted, Action oldDropped) {
        selected.clear();
        selected.addAll(oldSelected);
        lifted.clear();
        lifted.addAll(oldLifted);
        dropped = oldDropped;
        bounds = null;
        computeShouldSnap();
    }
    
    private void computeShouldSnap() {
        shouldSnap = false;
        for(Iterator it = unionSet.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(shouldSnapComponent(comp)) {
                shouldSnap = true;
                return;
            }
        }
    }

    private static boolean shouldSnapComponent(Component comp) {
        Boolean shouldSnapValue = (Boolean) comp.getFactory().getFeature(ComponentFactory.SHOULD_SNAP, comp.getAttributeSet());
        return shouldSnapValue == null ? true : shouldSnapValue.booleanValue();
    }
    
    private boolean hasConflictTranslated(Collection components, int dx, int dy,
            boolean selfConflicts) {
        Circuit circuit = proj.getCurrentCircuit();
        if(circuit == null) return false;
        for(Iterator it = components.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(!(obj instanceof Wire)) {
                Component comp = (Component) obj;
                for(Iterator it2 = comp.getEnds().iterator(); it2.hasNext(); ) {
                    EndData endData = (EndData) it2.next();
                    if(endData != null && endData.isExclusive()) {
                        Location endLoc = endData.getLocation().translate(dx, dy);
                        Component conflict = circuit.getExclusive(endLoc);
                        if(conflict != null) {
                            if(selfConflicts || !components.contains(conflict)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private static Bounds computeBounds(Collection components) {
        if(components.isEmpty()) {
            return Bounds.EMPTY_BOUNDS;
        } else {
            Iterator it = components.iterator();
            Bounds ret = ((Component) it.next()).getBounds();
            while(it.hasNext()) {
                Component comp = (Component) it.next();
                Bounds bds = comp.getBounds();
                ret = ret.add(bds);
            }
            return ret;
        }
    }
    
    private HashMap copyComponents(Collection components) {
        // determine translation offset where we can legally place the clipboard
        int dx;
        int dy;
        Bounds bds = computeBounds(components);
        for(int index = 0; ; index++) {
            // compute offset to try: We try points along successively larger
            // squares radiating outward from 0,0
            if(index == 0) {
                dx = 0;
                dy = 0;
            } else {
                int side = 1;
                while(side * side <= index) side += 2;
                int offs = index - (side - 2) * (side - 2);
                dx = side / 2;
                dy = side / 2;
                if(offs < side - 1) { // top edge of square
                    dx -= offs;
                } else if(offs < 2 * (side - 1)) { // left edge
                    offs -= side - 1;
                    dx = -dx;
                    dy -= offs;
                } else if(offs < 3 * (side - 1)) { // right edge
                    offs -= 2 * (side - 1);
                    dx = -dx + offs;
                    dy = -dy;
                } else {
                    offs -= 3 * (side - 1);
                    dy = -dy + offs;
                }
                dx *= 10;
                dy *= 10;
            }
            
            if(bds.getX() + dx >= 0 && bds.getY() + dy >= 0
                    && !hasConflictTranslated(components, dx, dy, true)) {
                return copyComponents(components, dx, dy);
            }
        }
    }
    
    private HashMap copyComponents(Collection components, int dx, int dy) {
        HashMap ret = new HashMap();
        for(Iterator it = components.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            Component copy = comp.getFactory().createComponent(
                    comp.getLocation().translate(dx, dy),
                    (AttributeSet) comp.getAttributeSet().clone());
            ret.put(comp, copy);
        }
        return ret;
    }

    // debugging methods
    public void print() {
        System.err.println(" shouldSnap: " + shouldSnap()); //OK

        boolean hasPrinted = false;
        for(Iterator it = selected.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            System.err.println((hasPrinted ? "         " : " select: ") //OK
                    + comp + "  [" + comp.hashCode() + "]");
            hasPrinted = true;
        }

        hasPrinted = false;
        for(Iterator it = lifted.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            System.err.println((hasPrinted ? "         " : " lifted: ") //OK
                    + comp + "  [" + comp.hashCode() + "]");
            hasPrinted = true;
        }
    }

}
