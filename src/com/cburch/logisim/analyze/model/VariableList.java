/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import com.cburch.logisim.util.IntegerFactory;

public class VariableList {
    private ArrayList listeners = new ArrayList();
    private int maxSize;
    private ArrayList data;
    private List dataView;
    
    public VariableList(int maxSize) {
        this.maxSize = maxSize;
        data = maxSize > 16 ? new ArrayList() : new ArrayList(maxSize);
        dataView = Collections.unmodifiableList(data);
    }
    
    //
    // listener methods
    //
    public void addVariableListListener(VariableListListener l) {
        listeners.add(l);
    }
    
    public void removeVariableListListener(VariableListListener l) {
        listeners.remove(l);
    }
    
    private void fireEvent(int type) {
        fireEvent(type, null, null);
    }
    
    private void fireEvent(int type, String variable) {
        fireEvent(type, variable, null);
    }
    
    private void fireEvent(int type, String variable, Object data) {
        int len = listeners.size();
        if(len == 0) return;
        VariableListEvent event = new VariableListEvent(this, type, variable, data);
        for(int i = 0; i < len; i++) {
            VariableListListener l = (VariableListListener) listeners.get(i);
            l.listChanged(event);
        }
    }
    
    //
    // data methods
    //
    public int getMaximumSize() {
        return maxSize;
    }
    
    public List getAll() {
        return dataView;
    }
    
    public int indexOf(String name) {
        return data.indexOf(name);
    }

    public int size() {
        return data.size();
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    public boolean isFull() {
        return data.size() >= maxSize;
    }
    
    public String get(int index) {
        return (String) data.get(index);
    }
    
    public void setAll(List values) {
        if(values.size() > maxSize) {
            throw new IllegalArgumentException("maximum size is " + maxSize);
        }
        data.clear();
        data.addAll(values);
        fireEvent(VariableListEvent.ALL_REPLACED);
    }
    
    public void add(String name) {
        if(data.size() >= maxSize) {
            throw new IllegalArgumentException("maximum size is " + maxSize);
        }
        data.add(name);
        fireEvent(VariableListEvent.ADD, name);
    }
    
    public void remove(String name) {
        int index = data.indexOf(name);
        if(index < 0) throw new NoSuchElementException("input " + name);
        data.remove(index);
        fireEvent(VariableListEvent.REMOVE, name, IntegerFactory.create(index));
    }
    
    public void move(String name, int delta) {
        int index = data.indexOf(name);
        if(index < 0) throw new NoSuchElementException(name);
        int newIndex = index + delta;
        if(newIndex < 0) {
            throw new IllegalArgumentException("cannot move index " + index
                    + " by " + delta);
        }
        if(newIndex > data.size() - 1) {
            throw new IllegalArgumentException("cannot move index " + index
                    + " by " + delta + ": size " + data.size());
        }
        if(index == newIndex) return;
        data.remove(index);
        data.add(newIndex, name);
        fireEvent(VariableListEvent.MOVE, name, IntegerFactory.create(newIndex - index));
    }
    
    public void replace(String oldName, String newName) {
        int index = data.indexOf(oldName);
        if(index < 0) throw new NoSuchElementException(oldName);
        if(oldName.equals(newName)) return;
        data.set(index, newName);
        fireEvent(VariableListEvent.REPLACE, oldName,
                IntegerFactory.create(index));
    }

}
