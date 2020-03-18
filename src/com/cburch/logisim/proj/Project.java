/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.proj;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JFileChooser;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.opts.OptionsFrame;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class Project {
    private static final int MAX_UNDO_SIZE = 64;

    private static class ActionData {
        CircuitState circuitState;
        Action action;

        public ActionData(CircuitState circuitState, Action action) {
            this.circuitState = circuitState;
            this.action = action;
        }
    }

    private class MyListener
            implements Selection.Listener, LibraryListener, CircuitListener {
        public void selectionChanged(Selection.Event e) {
            fireEvent(ProjectEvent.ACTION_SELECTION, selection);
        }
        
        public void libraryChanged(LibraryEvent event) {
            int action = event.getAction();
            if(action == LibraryEvent.REMOVE_LIBRARY) {
                Library unloaded = (Library) event.getData();
                if(tool != null && unloaded.containsFromSource(tool)) {
                    setTool(null);
                }
            } else if(action == LibraryEvent.REMOVE_TOOL) {
                if(event.getData() == getCurrentCircuit()) {
                    setCurrentCircuit(file.getMainCircuit());
                }
            }
        }

        public void circuitChanged(CircuitEvent event) {
            if(event.getAction() == CircuitEvent.ACTION_REMOVE) {
                Component comp = (Component) event.getData();
                if(selection != null) {
                    selection.remove(comp);
                }
            } else if(event.getAction() == CircuitEvent.ACTION_CLEAR) {
                selection.clear(false);
            }
        }
    }

    private Simulator simulator = new Simulator();
    private LogisimFile file;
    private CircuitState circuitState;
    private HashMap stateMap = new HashMap();
    private Frame frame = null;
    private OptionsFrame optionsFrame = null;
    private LogFrame logFrame = null;
    private Tool tool = null;
    private LinkedList undoLog = new LinkedList();
    private int undoMods = 0;
    private EventSourceWeakSupport projectListeners = new EventSourceWeakSupport();
    private EventSourceWeakSupport fileListeners = new EventSourceWeakSupport();
    private EventSourceWeakSupport circuitListeners = new EventSourceWeakSupport();
    private ProjectEvent repaint;
    private Dependencies depends;
    private Selection selection = new Selection(this);
    private MyListener myListener = new MyListener();
    private boolean startupScreen = false;

    private tr.edu.metu.ceng.ceng232.grader.Grader grader = null;

    public Project(LogisimFile file) {
        selection.addListener(myListener);
        addLibraryListener(myListener);
        addCircuitListener(myListener);
        this.repaint = new ProjectEvent(ProjectEvent.ACTION_COMPLETED,
            this, null);
        setLogisimFile(file);

        grader = new tr.edu.metu.ceng.ceng232.grader.Grader(this);
    }

    public void setFrame(Frame value) {
        if(frame == value) return;
        Frame oldValue = frame;
        frame = value;
        Projects.windowCreated(this, oldValue, value);
    }

    //
    // access methods
    //
    public LogisimFile getLogisimFile() {
        return file;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public Options getOptions() {
        return file.getOptions();
    }

    public Dependencies getDependencies() {
        return depends;
    }

    public Frame getFrame() {
        return frame;
    }
    
    public OptionsFrame getOptionsFrame(boolean create) {
        if(optionsFrame == null || optionsFrame.getLogisimFile() != file) {
            if(create) optionsFrame = new OptionsFrame(this);
            else optionsFrame = null;
        }
        return optionsFrame;
    }
    
    public LogFrame getLogFrame(boolean create) {
        if(logFrame == null) {
            if(create) logFrame = new LogFrame(this);
        }
        return logFrame;
    }

    public Circuit getCurrentCircuit() {
        return circuitState == null ? null : circuitState.getCircuit();
    }

    public CircuitState getCircuitState() {
        return circuitState;
    }
    
    public CircuitState getCircuitState(Circuit circuit) {
        if(circuitState != null && circuitState.getCircuit() == circuit) {
            return circuitState;
        } else {
            CircuitState ret = (CircuitState) stateMap.get(circuit);
            if(ret == null) {
                ret = new CircuitState(this, circuit);
                stateMap.put(circuit, ret);
            }
            return ret;
        }
    }

    public Action getLastAction() {
        if(undoLog.size() == 0) {
            return null;
        } else {
            return ((ActionData) undoLog.getLast()).action;
        }
    }

    public Tool getTool() {
        return tool;
    }

    public Selection getSelection() {
        return selection;
    }

    public boolean isFileDirty() {
        return undoMods != 0;
    }

    public JFileChooser createChooser() {
        if(file == null) return new JFileChooser();
        Loader loader = file.getLoader();
        return loader == null ? new JFileChooser() : loader.createChooser();
    }

    //
    // Listener methods
    //
    public void addProjectListener(ProjectListener what) {
        projectListeners.add(what);
    }

    public void removeProjectListener(ProjectListener what) {
        projectListeners.remove(what);
    }
    
    public void addLibraryListener(LibraryListener value) {
        fileListeners.add(value);
        if(file != null) file.addLibraryListener(value);
    }
    
    public void removeLibraryListener(LibraryListener value) {
        fileListeners.remove(value);
        if(file != null) file.removeLibraryListener(value);
    }
    
    public void addCircuitListener(CircuitListener value) {
        circuitListeners.add(value);
        Circuit current = getCurrentCircuit();
        if(current != null) current.addCircuitListener(value);
    }
    
    public void removeCircuitListener(CircuitListener value) {
        circuitListeners.remove(value);
        Circuit current = getCurrentCircuit();
        if(current != null) current.removeCircuitListener(value);
    }

    private void fireEvent(int action, Object old, Object data) {
        fireEvent(new ProjectEvent(action, this, old, data));
    }

    private void fireEvent(int action, Object data) {
        fireEvent(new ProjectEvent(action, this, data));
    }

    private void fireEvent(ProjectEvent event) {
        for(Iterator it = projectListeners.iterator(); it.hasNext(); ) {
            ProjectListener what = (ProjectListener) it.next();
            what.projectChanged(event);
        }
    }
    
    // We track whether this project is the empty project opened
    // at startup by default, because we want to close it
    // immediately as another project is opened, if there
    // haven't been any changes to it.
    public boolean isStartupScreen() {
        return startupScreen;
    }
    
    public boolean confirmClose(String title) {
        return frame.confirmClose(title);
    }

    //
    // actions
    //
    public void setStartupScreen(boolean value) {
        startupScreen = value;
    }

    public void setLogisimFile(LogisimFile value) {
        LogisimFile old = this.file;
        if(old != null) {
            for(Iterator it = fileListeners.iterator(); it.hasNext(); ) {
                old.removeLibraryListener((LibraryListener) it.next());
            }
        }
        file = value;
        stateMap.clear();
        depends = new Dependencies(file);
        undoLog.clear();
        undoMods = 0;
        fireEvent(ProjectEvent.ACTION_SET_FILE, old, file);
        setCurrentCircuit(file.getMainCircuit());
        if(file != null) {
            for(Iterator it = fileListeners.iterator(); it.hasNext(); ) {
                LibraryListener l = (LibraryListener) it.next();
                file.addLibraryListener(l);
            }
        }
        file.setDirty(true); // toggle it so that everybody hears the file is fresh
        file.setDirty(false);
    }

    public void setCircuitState(CircuitState value) {
        if(value == null || circuitState == value) return;

        CircuitState old = circuitState;
        Circuit oldCircuit = old == null ? null : old.getCircuit();
        Circuit newCircuit = value.getCircuit();
        boolean circuitChanged = old == null || oldCircuit != newCircuit;
        if(circuitChanged) {
            if(tool != null) tool.deselect(frame.getCanvas());
            selection.clear();
            if(tool != null) tool.select(frame.getCanvas());
            if(oldCircuit != null) {
                for(Iterator it = circuitListeners.iterator(); it.hasNext(); ) {
                    oldCircuit.removeCircuitListener((CircuitListener) it.next());
                }
            }
        }
        circuitState = value;
        stateMap.put(circuitState.getCircuit(), circuitState);
        simulator.setCircuitState(circuitState);
        if(circuitChanged) {
            fireEvent(ProjectEvent.ACTION_SET_CURRENT, oldCircuit, newCircuit);
            if(newCircuit != null) {
                for(Iterator it = circuitListeners.iterator(); it.hasNext(); ) {
                    newCircuit.addCircuitListener((CircuitListener) it.next());
                }
            }
        }
        fireEvent(ProjectEvent.ACTION_SET_STATE, old, circuitState);
    }

    public void setCurrentCircuit(Circuit circuit) {
        CircuitState circState = (CircuitState) stateMap.get(circuit);
        if(circState == null) circState = new CircuitState(this, circuit);
        setCircuitState(circState);
    }

    public void setTool(Tool value) {
        if(tool == value) return;
        Tool old = tool;
        if(old != null) old.deselect(frame.getCanvas());
        if(!selection.isEmpty()) {
            if(value == null) {
                selection.dropAll();
            } else if(!getOptions().getMouseMappings().containsSelectTool()) {
                selection.clear();
            }
        }
        startupScreen = false;
        tool = value;
        if(tool != null) tool.select(frame.getCanvas());
        fireEvent(ProjectEvent.ACTION_SET_TOOL, old, tool);
    }

    public void doAction(Action act) {
        if(act == null) return;
        startupScreen = false;
        act.doIt(this);
        if(!undoLog.isEmpty() && act.shouldAppendTo(getLastAction())) {
            ActionData firstData = (ActionData) undoLog.removeLast();
            Action first = firstData.action;
            if(first.isModification()) --undoMods;
            act = first.append(act);
        }
        while(undoLog.size() > MAX_UNDO_SIZE) {
            undoLog.removeFirst();
        }
        undoLog.add(new ActionData(circuitState, act));
        if(act.isModification()) ++undoMods;
        file.setDirty(isFileDirty());
        fireEvent(repaint);
    }

    public void undoAction() {
        if(undoLog != null && undoLog.size() > 0) {
            ActionData data = (ActionData) undoLog.removeLast();
            setCircuitState(data.circuitState);
            Action action = data.action;
            if(action.isModification()) --undoMods;
            action.undo(this);
            file.setDirty(isFileDirty());
            fireEvent(repaint);
        }
    }

    public void setFileAsClean() {
        undoMods = 0;
        file.setDirty(isFileDirty());
    }

    public void repaintCanvas() {
        // for actions that ought not be logged (i.e., those that
        // change nothing, except perhaps the current values within
        // the circuit)
        fireEvent(repaint);
    }
}
