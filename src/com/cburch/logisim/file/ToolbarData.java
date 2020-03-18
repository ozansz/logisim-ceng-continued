/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class ToolbarData {
    public static class Separator {
        public Separator() { }
    }

    public static interface ToolbarListener {
        public void toolbarChanged();
    }

    private EventSourceWeakSupport listeners = new EventSourceWeakSupport();
    private EventSourceWeakSupport toolListeners = new EventSourceWeakSupport();
    private ArrayList contents = new ArrayList();

    public ToolbarData() { }

    //
    // listener methods
    //
    public void addToolbarListener(ToolbarListener l) {
        listeners.add(l);
    }

    public void removeToolbarListener(ToolbarListener l) {
        listeners.remove(l);
    }
    
    public void addToolAttributeListener(AttributeListener l) {
        for(Iterator it = contents.iterator(); it.hasNext(); ) {
            Object o = it.next();
            if(o instanceof Tool) {
                AttributeSet attrs = ((Tool) o).getAttributeSet();
                if(attrs != null) attrs.addAttributeListener(l);
            }
        }
        toolListeners.add(l);
    }
    
    public void removeToolAttributeListener(AttributeListener l) {
        for(Iterator it = contents.iterator(); it.hasNext(); ) {
            Object o = it.next();
            if(o instanceof Tool) {
                AttributeSet attrs = ((Tool) o).getAttributeSet();
                if(attrs != null) attrs.removeAttributeListener(l);
            }
        }
        toolListeners.remove(l);
    }

    private void addAttributeListeners(Tool tool) {
        for(Iterator it = toolListeners.iterator(); it.hasNext(); ) {
            AttributeListener l = (AttributeListener) it.next();
            AttributeSet attrs = tool.getAttributeSet();
            if(attrs != null) attrs.addAttributeListener(l);
        }
    }

    private void removeAttributeListeners(Tool tool) { 
        for(Iterator it = toolListeners.iterator(); it.hasNext(); ) {
            AttributeListener l = (AttributeListener) it.next();
            AttributeSet attrs = tool.getAttributeSet();
            if(attrs != null) attrs.removeAttributeListener(l);
        }
    }

    public void fireToolbarChanged() {
        Iterator it = listeners.iterator();
        while(it.hasNext()) {
            ((ToolbarListener) it.next()).toolbarChanged();
        }
    }

    //
    // query methods
    //
    public List getContents() {
        return contents;
    }

    public Tool getFirstTool() {
        Iterator it = contents.iterator();
        while(it.hasNext()) {
            Object o = it.next();
            if(o instanceof Tool) return (Tool) o;
        }
        return null;
    }
    
    public int size() {
        return contents.size();
    }
    
    public Object get(int index) {
        return contents.get(index);
    }

    //
    // modification methods
    //
    public void copyFrom(ToolbarData other, LogisimFile file) {
        if(this == other) return;
        for(Iterator it1 = contents.iterator(); it1.hasNext(); ) {
            Object o = it1.next();
            if(o instanceof Tool) removeAttributeListeners((Tool) o);
        }
        this.contents.clear();
        Iterator it = other.contents.iterator();
        while(it.hasNext()) {
            Object o = it.next();
            if(o instanceof Separator) {
                this.addSeparator();
            } else if(o instanceof Tool) {
                Tool srcTool = (Tool) o;
                Tool toolCopy = file.findTool(srcTool);
                if(toolCopy != null) {
                    Tool dstTool = toolCopy.cloneTool();
                    AttributeSets.copy(srcTool.getAttributeSet(),
                            dstTool.getAttributeSet());
                    this.addTool(dstTool);
                    addAttributeListeners(toolCopy);
                }
            }
        }
        fireToolbarChanged();
    }

    public void addSeparator() {
        contents.add(new Separator());
        fireToolbarChanged();
    }

    public void addTool(Tool tool) {
        contents.add(tool);
        addAttributeListeners(tool);
        fireToolbarChanged();
    }

    public void addTool(int pos, Tool tool) {
        contents.add(pos, tool);
        addAttributeListeners(tool);
        fireToolbarChanged();
    }

    public void addSeparator(int pos) {
        contents.add(pos, new Separator());
        fireToolbarChanged();
    }

    public Object move(int from, int to) {
        Object moved = contents.remove(from);
        // if(to > from) --to;
        contents.add(to, moved);
        fireToolbarChanged();
        return moved;
    }

    public Object remove(int pos) {
        Object ret = contents.remove(pos);
        if(ret instanceof Tool) removeAttributeListeners((Tool) ret);
        fireToolbarChanged();
        return ret;
    }

    boolean usesToolFromSource(Tool query) {
        for(Iterator it = contents.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(obj instanceof Tool) {
                Tool tool = (Tool) obj;
                if(tool.sharesSource(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    //
    // package-protected methods
    //
    void replaceAll(HashMap toolMap) {
        boolean changed = false;
        for(ListIterator it = contents.listIterator(); it.hasNext(); ) {
            Object old = it.next();
            if(old instanceof AddTool) {
                ComponentFactory factory = ((AddTool) old).getFactory();
                if(toolMap.containsKey(factory)) {
                    changed = true;
                    removeAttributeListeners((Tool) old);
                    Tool newTool = (Tool) toolMap.get(factory);
                    if(newTool == null) {
                        it.remove();
                    } else {
                        Tool addedTool = newTool.cloneTool();
                        addAttributeListeners(addedTool);
                        LoadedLibrary.copyAttributes(addedTool.getAttributeSet(),
                                ((Tool) old).getAttributeSet());
                        it.set(addedTool);
                    }
                }
            } else if(toolMap.containsKey(old)) {
                changed = true;
                removeAttributeListeners((Tool) old);
                Tool newTool = (Tool) toolMap.get(old);
                if(newTool == null) {
                    it.remove();
                } else {
                    Tool addedTool = newTool.cloneTool();
                    addAttributeListeners(addedTool);
                    LoadedLibrary.copyAttributes(addedTool.getAttributeSet(),
                            ((Tool) old).getAttributeSet());
                    it.set(addedTool);
                }
            }
        }
        if(changed) fireToolbarChanged();
    }
}
