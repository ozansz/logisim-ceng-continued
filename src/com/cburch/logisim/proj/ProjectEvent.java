/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.Tool;


public class ProjectEvent {
    public final static int ACTION_SET_FILE     = 0; // change file
    public final static int ACTION_SET_CURRENT  = 1; // change current
    public final static int ACTION_SET_TOOL     = 2; // change tool
    public final static int ACTION_COMPLETED    = 3; // action executed
    public final static int ACTION_SELECTION    = 4; // selection alterd
    public final static int ACTION_SET_STATE    = 5; // circuit state changed

    private int action;
    private Project proj;
    private Object old_data;
    private Object data;

    ProjectEvent(int action, Project proj, Object old, Object data) {
        this.action = action;
        this.proj = proj;
        this.old_data = old;
        this.data = data;
    }

    ProjectEvent(int action, Project proj, Object data) {
        this.action = action;
        this.proj = proj;
        this.data = data;
    }

    ProjectEvent(int action, Project proj) {
        this.action = action;
        this.proj = proj;
        this.data = null;
    }

    // access methods
    public int getAction() {
        return action;
    }

    public Project getProject() {
        return proj;
    }

    public Object getOldData() {
        return old_data;
    }

    public Object getData() {
        return data;
    }

    // convenience methods
    public LogisimFile getLogisimFile() {
        return proj.getLogisimFile();
    }

    public Circuit getCircuit() {
        return proj.getCurrentCircuit();
    }

    public Tool getTool() {
        return proj.getTool();
    }

}
