/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;


import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.IntegerFactory;

public class MouseMappings {
    public static interface MouseMappingsListener {
        public void mouseMappingsChanged();
    }

    private ArrayList listeners = new ArrayList();
    private HashMap map = new HashMap();
    private int cache_mods;
    private Tool cache_tool;

    public MouseMappings() { }

    //
    // listener methods
    //
    public void addMouseMappingsListener(MouseMappingsListener l) {
        listeners.add(l);
    }

    public void removeMouseMappingsListener(MouseMappingsListener l) {
        listeners.add(l);
    }

    private void fireMouseMappingsChanged() {
        Iterator it = listeners.iterator();
        while(it.hasNext()) {
            ((MouseMappingsListener) it.next()).mouseMappingsChanged();
        }
    }

    //
    // query methods
    //
    public Map getMappings() { // returns Integer -> Tool
        return map;
    }

    public Set getMappedModifiers() {
        return map.keySet();
    }

    public Tool getToolFor(MouseEvent e) {
        return getToolFor(e.getModifiers());
    }

    public Tool getToolFor(int mods) {
        if(mods == cache_mods) {
            return cache_tool;
        } else {
            Tool ret = (Tool) map.get(IntegerFactory.create(mods));
            cache_mods = mods;
            cache_tool = ret;
            return ret;
        }
    }

    public Tool getToolFor(Integer mods) {
        if(mods.intValue() == cache_mods) {
            return cache_tool;
        } else {
            Tool ret = (Tool) map.get(mods);
            cache_mods = mods.intValue();
            cache_tool = ret;
            return ret;
        }
    }

    public boolean usesToolFromSource(Tool query) {
        for(Iterator it = map.values().iterator(); it.hasNext(); ) {
            Tool tool = (Tool) it.next();
            if(tool.sharesSource(query)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsSelectTool() {
        for(Iterator it = map.values().iterator(); it.hasNext(); ) {
            Object tool = it.next();
            if(tool instanceof SelectTool) return true;
        }
        return false;
    }

    //
    // modification methods
    //
    public void copyFrom(MouseMappings other, LogisimFile file) {
        if(this == other) return;
        cache_mods = -1;
        this.map.clear();
        Iterator it = other.map.keySet().iterator();
        while(it.hasNext()) {
            Integer mods = (Integer) it.next();
            Tool srcTool = (Tool) other.map.get(mods);
            Tool dstTool = file.findTool(srcTool);
            if(dstTool != null) {
                dstTool = dstTool.cloneTool();
                AttributeSets.copy(srcTool.getAttributeSet(),
                        dstTool.getAttributeSet());
                this.map.put(mods, dstTool);
            }
        }
        fireMouseMappingsChanged();
    }

    public void setToolFor(MouseEvent e, Tool tool) {
        setToolFor(e.getModifiers(), tool);
    }

    public void setToolFor(int mods, Tool tool) {
        if(mods == cache_mods) cache_mods = -1;

        if(tool == null) {
            Object old = map.remove(IntegerFactory.create(mods));
            if(old != null) fireMouseMappingsChanged();
        } else {
            Object old = map.put(IntegerFactory.create(mods), tool);
            if(old != tool) fireMouseMappingsChanged();
        }
    }

    public void setToolFor(Integer mods, Tool tool) {
        if(mods.intValue() == cache_mods) cache_mods = -1;

        if(tool == null) {
            Object old = map.remove(mods);
            if(old != null) fireMouseMappingsChanged();
        } else {
            Object old = map.put(mods, tool);
            if(old != tool) fireMouseMappingsChanged();
        }
    }

    //
    // package-protected methods
    //
    void replaceAll(HashMap toolMap) {
        boolean changed = false;
        for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Object tool = map.get(key);
            if(tool instanceof AddTool) {
                ComponentFactory factory = ((AddTool) tool).getFactory();
                if(toolMap.containsKey(factory)) {
                    changed = true;
                    Tool newTool = (Tool) toolMap.get(factory);
                    if(newTool == null) {
                        map.remove(key);
                    } else {
                        Tool clone = newTool.cloneTool();
                        LoadedLibrary.copyAttributes(clone.getAttributeSet(),
                                ((Tool) tool).getAttributeSet());
                        map.put(key, clone);
                    }
                }
            } else {
                if(toolMap.containsKey(tool)) {
                    changed = true;
                    Tool newTool = (Tool) toolMap.get(tool);
                    if(newTool == null) {
                        map.remove(key);
                    } else {
                        Tool clone = newTool.cloneTool();
                        LoadedLibrary.copyAttributes(clone.getAttributeSet(),
                                ((Tool) tool).getAttributeSet());
                        map.put(key, clone);
                    }
                }
            }
        }
        if(changed) fireMouseMappingsChanged();
    }
}
