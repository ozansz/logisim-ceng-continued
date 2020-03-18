/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JList;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;

class CircuitJList extends JList {
    public CircuitJList(Project proj, boolean includeEmpty) {
        LogisimFile file = proj.getLogisimFile();
        Circuit current = proj.getCurrentCircuit();
        Vector options = new Vector();
        boolean currentFound = false;
        for(Iterator it = file.getTools().iterator(); it.hasNext(); ) {
            Tool t = (Tool) it.next();
            if(t instanceof AddTool) {
                ComponentFactory c = ((AddTool) t).getFactory();
                if(c instanceof Circuit) {
                    Circuit circ = (Circuit) c;
                    if(!includeEmpty || circ.getBounds() != Bounds.EMPTY_BOUNDS) {
                        if(circ == current) currentFound = true;
                        options.add(circ);
                    }
                }
            }
        }
        
        setListData(options);
        if(currentFound) setSelectedValue(current, true);
        setVisibleRowCount(Math.min(6, options.size()));
    }
    
    public List getSelectedCircuits() {
        Object[] selected = getSelectedValues();
        if(selected != null && selected.length > 0) {
            ArrayList ret = new ArrayList(selected.length);
            for(int i = 0; i < selected.length; i++) {
                ret.add(selected[i]);
            }
            return ret;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

}
