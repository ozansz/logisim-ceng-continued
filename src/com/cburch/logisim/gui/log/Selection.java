/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.log;

import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;

class Selection {
    private CircuitState root;
    private Model model;
    private ArrayList components; // of SelectedItems
    
    public Selection(CircuitState root, Model model) {
        this.root = root;
        this.model = model;
        components = new ArrayList();
    }

    public void addModelListener(ModelListener l) { model.addModelListener(l); }
    public void removeModelListener(ModelListener l) { model.removeModelListener(l); }

    public CircuitState getCircuitState() {
        return root;
    }
    
    public int size() {
        return components.size();
    }
    
    public SelectionItem get(int index) {
        return (SelectionItem) components.get(index);
    }
    
    public int indexOf(SelectionItem value) {
        return components.indexOf(value);
    }
    
    public void add(SelectionItem item) {
        components.add(item);
        model.fireSelectionChanged(new ModelEvent());
    }
    
    public void remove(int index) {
        components.remove(index);
        model.fireSelectionChanged(new ModelEvent());
    }
    
    public void move(int fromIndex, int toIndex) {
        if(fromIndex == toIndex) return;
        Object o = components.remove(fromIndex);
        components.add(toIndex, o);
        model.fireSelectionChanged(new ModelEvent());
    }
}
