/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.ListUtil;

public abstract class Library {
    public abstract String getName();

    public abstract List getTools();

    public String toString() { return getName(); }

    public String getDisplayName() { return getName(); }
    
    public boolean isDirty() { return false; }

    public List getLibraries() {
        return Collections.EMPTY_LIST;
    }

    public List getElements() {
        return ListUtil.joinImmutableLists(getTools(), getLibraries());
    }

    public Tool getTool(String name) {
        Iterator it = getTools().iterator();
        while(it.hasNext()) {
            Object o = it.next();
            if(o instanceof Tool && ((Tool) o).getName().equals(name)) {
                return (Tool) o;
            }
        }
        return null;
    }

    public boolean containsFromSource(Tool query) {
        for(Iterator it = getTools().iterator(); it.hasNext(); ) {
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
    
    public int indexOf(ComponentFactory query) {
        int index = 0;
        for(Iterator it = getTools().iterator(); it.hasNext(); index++) {
            Object obj = it.next();
            if(obj instanceof AddTool) {
                AddTool tool = (AddTool) obj;
                if(tool.getFactory() == query) return index;
            }
        }
        return -1;
    }
    
    public boolean contains(ComponentFactory query) {
        return indexOf(query) >= 0;
    }

    public Library getLibrary(String name) {
        Iterator it = getLibraries().iterator();
        while(it.hasNext()) {
            Object o = it.next();
            if(o instanceof Library && ((Library) o).getName().equals(name)) {
                return (Library) o;
            }
        }
        return null;
    }

}
