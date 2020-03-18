/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

public class SelectionActions {
    private SelectionActions() { }

    public static Action move(int dx, int dy, Collection connectPoints) {
        return new Move(dx, dy, connectPoints);
    }

    public static Action clear() {
        return new Clear();
    }

    public static Action cut() {
        return new Cut();
    }

    public static Action copy() {
        return new Copy();
    }

    public static Action paste() {
        return new Paste();
    }

    private static class Move extends Action {
        Action moveAction;
        Action wiresRemove;
        Action wiresAdd;
        int dx;
        int dy;
        Collection connectPoints;

        Move(int dx, int dy, Collection connectPoints) {
            this.dx = dx;
            this.dy = dy;
            if((dx == 0 && dy != 0) || (dx != 0 && dy == 0)) {
                this.connectPoints = connectPoints;
            }
        }

        public String getName() {
            return Strings.get("moveSelectionAction");
        }

        public void doIt(Project proj) {
            moveAction = proj.getSelection().translateAll(dx, dy);
            moveAction.doIt(proj);
            if(connectPoints != null && !connectPoints.isEmpty()) {
                Circuit circ = proj.getCurrentCircuit();
                HashSet removals = new HashSet();
                HashSet additions = new HashSet();
                for(Iterator it = connectPoints.iterator(); it.hasNext(); ) {
                    Location loc = (Location) it.next();
                    Wire removal = null;
                    for(Iterator it2 = circ.getComponents(loc).iterator(); it2.hasNext(); ) {
                        Object obj = it2.next();
                        if(obj instanceof Wire) {
                            Wire w = (Wire) obj;
                            if(w.isVertical() == (dx == 0)) {
                                if(w.isVertical()) {
                                    if((w.getOtherEnd(loc).getY() < loc.getY()) == (dy < 0)) removal = w;
                                } else {
                                    if((w.getOtherEnd(loc).getX() < loc.getX()) == (dx < 0)) removal = w;
                                }
                            }
                        }
                    }
                    if(removal == null) {
                        Wire w = Wire.create(loc, loc.translate(dx, dy));
                        additions.add(w);
                    } else {
                        removals.add(removal);
                        Wire w = Wire.create(removal.getOtherEnd(loc), loc.translate(dx, dy));
                        additions.add(w);
                    }
                }
                if(!removals.isEmpty()) {
                    wiresRemove = CircuitActions.removeComponents(circ, removals);
                    wiresRemove.doIt(proj);
                }
                wiresAdd = CircuitActions.addComponents(circ, additions);
                wiresAdd.doIt(proj);
            }
        }

        public void undo(Project proj) {
            if(wiresAdd != null) {
                wiresAdd.undo(proj);
                wiresAdd = null;
            }
            if(wiresRemove != null) {
                wiresRemove.undo(proj);
                wiresRemove = null;
            }
            moveAction.undo(proj);
        }
    }

    private static class Clear extends Action {
        Action clearAction;

        Clear() { }

        public String getName() {
            return Strings.get("clearSelectionAction");
        }

        public void doIt(Project proj) {
            clearAction = proj.getSelection().deleteAll();
            clearAction.doIt(proj);
        }

        public void undo(Project proj) {
            clearAction.undo(proj);
        }
    }

    private static class Cut extends Action {
        Action first = new Copy();
        Action second = new Clear();

        Cut() { }

        public String getName() {
            return Strings.get("cutSelectionAction");
        }

        public void doIt(Project proj) {
            first.doIt(proj);
            second.doIt(proj);
        }

        public void undo(Project proj) {
            second.undo(proj);
            first.undo(proj);
        }
    }

    private static class Copy extends Action {
        Clipboard oldClip;

        Copy() { }

        public boolean isModification() { return false; }

        public String getName() {
            return Strings.get("copySelectionAction");
        }

        public void doIt(Project proj) {
            oldClip = Clipboard.get();
            Clipboard.set(proj.getSelection(),
                    proj.getFrame().getAttributeTable().getAttributeSet());
        }

        public void undo(Project proj) {
            Clipboard.set(oldClip);
        }
    }

    private static class Paste extends Action {
        Action pasteAction;

        Paste() { }

        public String getName() {
            return Strings.get("pasteClipboardAction");
        }

        public void doIt(Project proj) {
            Clipboard clip = Clipboard.get();
            pasteAction = proj.getSelection().paste(clip);
            pasteAction.doIt(proj);
        }

        public void undo(Project proj) {
            pasteAction.undo(proj);
        }
    }

}
