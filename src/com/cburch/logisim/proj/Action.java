/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.proj;

public abstract class Action {
    private static class UnionAction extends Action {
        Action first;
        Action second;

        UnionAction(Action first, Action second) {
            this.first = first;
            this.second = second;
        }

        public boolean isModification() {
            return first.isModification() || second.isModification();
        }

        public String getName() { return first.getName(); }

        public void doIt(Project proj) {
            first.doIt(proj);
            second.doIt(proj);
        }

        public void undo(Project proj) {
            second.undo(proj);
            first.undo(proj);
        }
    }

    public boolean isModification() { return true; }

    public abstract String getName();

    public abstract void doIt(Project proj);

    public abstract void undo(Project proj);

    public boolean shouldAppendTo(Action other) { return false; }

    public Action append(Action other) {
        return new UnionAction(this, other);
    }

}
