/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class LoadedLibrary extends Library implements LibraryEventSource {
    private class MyListener implements LibraryListener {
        public void libraryChanged(LibraryEvent event) {
            fireLibraryEvent(event);
        }
    }
    
    private Library base;
    private boolean dirty = false;
    private MyListener myListener = new MyListener();
    private EventSourceWeakSupport listeners = new EventSourceWeakSupport();
    
    LoadedLibrary(Library base) {
        while(base instanceof LoadedLibrary) base = ((LoadedLibrary) base).base;
        this.base = base;
        if(base instanceof LibraryEventSource) {
            ((LibraryEventSource) base).addLibraryListener(myListener);
        }
    }
    
    public void addLibraryListener(LibraryListener l) {
        listeners.add(l);
    }
    
    public void removeLibraryListener(LibraryListener l) {
        listeners.remove(l);
    }

    public String getName() {
        return base.getName();
    }
    
    public String getDisplayName() {
        return base.getDisplayName();
    }
    
    public boolean isDirty() {
        return dirty || base.isDirty();
    }

    public List getTools() {
        return base.getTools();
    }
    
    public List getLibraries() {
        return base.getLibraries();
    }
    
    void setDirty(boolean value) {
        if(dirty != value) {
            dirty = value;
            fireLibraryEvent(LibraryEvent.DIRTY_STATE, isDirty() ? Boolean.TRUE : Boolean.FALSE);
        }
    }
    
    Library getBase() {
        return base;
    }
    
    void setBase(Library value) {
        if(base instanceof LibraryEventSource) {
            ((LibraryEventSource) base).removeLibraryListener(myListener);
        }
        Library old = base;
        base = value;
        resolveChanges(old);
        if(base instanceof LibraryEventSource) {
            ((LibraryEventSource) base).addLibraryListener(myListener);
        }
    }
    
    private void fireLibraryEvent(int action, Object data) {
        fireLibraryEvent(new LibraryEvent(this, action, data));
    }
    private void fireLibraryEvent(LibraryEvent event) {
        if(event.getSource() != this) {
            event = new LibraryEvent(this, event.getAction(), event.getData());
        }
        for(Iterator it = listeners.iterator(); it.hasNext(); ) {
            LibraryListener l = (LibraryListener) it.next();
            l.libraryChanged(event);
        }
    }
    
    private void resolveChanges(Library old) {
        if(listeners.size() == 0) return;
        
        if(!base.getDisplayName().equals(old.getDisplayName())) {
            fireLibraryEvent(LibraryEvent.SET_NAME, base.getDisplayName());
        }
        
        HashSet changes = new HashSet(old.getLibraries());
        changes.removeAll(base.getLibraries());
        for(Iterator it = changes.iterator(); it.hasNext(); ) {
            fireLibraryEvent(LibraryEvent.REMOVE_LIBRARY, it.next());
        }
        
        changes.clear();
        changes.addAll(base.getLibraries());
        changes.removeAll(old.getLibraries());
        for(Iterator it = changes.iterator(); it.hasNext(); ) {
            fireLibraryEvent(LibraryEvent.ADD_LIBRARY, it.next());
        }
        
        HashMap componentMap = new HashMap();
        HashMap toolMap = new HashMap();
        for(Iterator it = old.getTools().iterator(); it.hasNext(); ) {
            Tool oldTool = (Tool) it.next();
            Tool newTool = base.getTool(oldTool.getName());
            toolMap.put(oldTool, newTool);
            if(oldTool instanceof AddTool) {
                ComponentFactory oldFactory = ((AddTool) oldTool).getFactory();
                toolMap.put(oldFactory, newTool);
                if(newTool != null && newTool instanceof AddTool) {
                    ComponentFactory newFactory = ((AddTool) newTool).getFactory();
                    componentMap.put(oldFactory, newFactory);
                } else {
                    componentMap.put(oldFactory, null);
                }
            }
        }
        replaceAll(componentMap, toolMap);
        
        changes.clear();
        changes.addAll(old.getTools());
        changes.removeAll(toolMap.keySet());
        for(Iterator it = changes.iterator(); it.hasNext(); ) {
            fireLibraryEvent(LibraryEvent.REMOVE_TOOL, it.next());
        }

        changes.clear();
        changes.addAll(base.getTools());
        changes.removeAll(toolMap.values());
        for(Iterator it = changes.iterator(); it.hasNext(); ) {
            fireLibraryEvent(LibraryEvent.ADD_TOOL, it.next());
        }
    }
    
    private static void replaceAll(HashMap componentMap, HashMap toolMap) {
        for(Iterator it = Projects.getOpenProjects().iterator(); it.hasNext(); ) {
            Project proj = (Project) it.next();
            Object oldTool = proj.getTool();
            Object oldCircuit = proj.getCurrentCircuit();
            if(toolMap.containsKey(oldTool)) {
                proj.setTool((Tool) toolMap.get(oldTool));
            }
            if(componentMap.containsKey(oldCircuit)) {
                proj.setCurrentCircuit((Circuit) componentMap.get(oldCircuit));
            }
            replaceAll(proj.getLogisimFile(), componentMap, toolMap);
        }
        for(Iterator it = LibraryManager.instance.getLogisimLibraries().iterator(); it.hasNext(); ) {
            LogisimFile file = (LogisimFile) it.next();
            replaceAll(file, componentMap, toolMap);
        }
    }
    
    private static void replaceAll(LogisimFile file, HashMap componentMap, HashMap toolMap) {
        file.getOptions().getToolbarData().replaceAll(toolMap);
        file.getOptions().getMouseMappings().replaceAll(toolMap);
        for(Iterator it = file.getTools().iterator(); it.hasNext(); ) {
            Object tool = it.next();
            if(tool instanceof AddTool) {
                Object circuit = ((AddTool) tool).getFactory();
                if(circuit instanceof Circuit) {
                    replaceAll((Circuit) circuit, componentMap);
                }
            }
        }
    }

    private static void replaceAll(Circuit circuit, HashMap componentMap) {
        ArrayList toReplace = null;
        for(Iterator it = circuit.getNonWires().iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            if(componentMap.containsKey(comp.getFactory())) {
                if(toReplace == null) toReplace = new ArrayList();
                toReplace.add(comp);
            }
        }
        if(toReplace == null) return;
        for(int i = 0, n = toReplace.size(); i < n; i++) {
            Component comp = (Component) toReplace.get(i);
            circuit.remove(comp);
            ComponentFactory factory = (ComponentFactory) componentMap.get(comp.getFactory());
            if(factory != null) {
                AttributeSet newAttrs = createAttributes(factory, comp.getAttributeSet());
                circuit.add(factory.createComponent(comp.getLocation(), newAttrs));
            }
        }
    }
    
    private static AttributeSet createAttributes(ComponentFactory factory, AttributeSet src) {
        AttributeSet dest = factory.createAttributeSet();
        copyAttributes(dest, src);
        return dest;
    }
    
    static void copyAttributes(AttributeSet dest, AttributeSet src) {
        for(Iterator it = dest.getAttributes().iterator(); it.hasNext(); ) {
            Attribute destAttr = (Attribute) it.next();
            Attribute srcAttr = src.getAttribute(destAttr.getName());
            if(srcAttr != null) {
                dest.setValue(destAttr, src.getValue(srcAttr));
            }
        }
    }
}
