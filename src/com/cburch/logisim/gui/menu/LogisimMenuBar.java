/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 

package com.cburch.logisim.gui.menu;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenu;

public class LogisimMenuBar extends JMenuBar {
    public static final LogisimMenuItem PRINT = new LogisimMenuItem("Print");
    public static final LogisimMenuItem EXPORT_GIF = new LogisimMenuItem("ExportGIF");
    public static final LogisimMenuItem CUT = new LogisimMenuItem("Cut");
    public static final LogisimMenuItem COPY = new LogisimMenuItem("Copy");
    public static final LogisimMenuItem PASTE = new LogisimMenuItem("Paste");
    public static final LogisimMenuItem DELETE = new LogisimMenuItem("Delete");
    public static final LogisimMenuItem SELECT_ALL = new LogisimMenuItem("SelectAll");
    public static final LogisimMenuItem ADD_CIRCUIT = new LogisimMenuItem("AddCircuit");
    public static final LogisimMenuItem RENAME_CIRCUIT = new LogisimMenuItem("RenameCircuit");
    public static final LogisimMenuItem SET_MAIN_CIRCUIT = new LogisimMenuItem("SetMainCircuit");
    public static final LogisimMenuItem REMOVE_CIRCUIT = new LogisimMenuItem("RemoveCircuit");
    public static final LogisimMenuItem ANALYZE_CIRCUIT = new LogisimMenuItem("AnalyzeCircuit");

    private class MyListener implements LocaleListener {
        public void localeChanged() {
            file.localeChanged();
            edit.localeChanged();
            project.localeChanged();
            simulate.localeChanged();
            help.localeChanged();
        }
    }
    
    private JFrame parent;
    private MyListener listener;
    private Project proj;
    private SimulateListener simulateListener = null;
    private HashMap menuItems = new HashMap(); // LogisimMenuItem -> JMenuItem
    
    private MenuFile file;
    private MenuEdit edit;
    private MenuProject project;
    private MenuSimulate simulate;
    private MenuHelp help;
    
    public LogisimMenuBar(JFrame parent, Project proj) {
        this.parent = parent;
        this.listener = new MyListener();
        this.proj = proj;
        
        add(file = new MenuFile(this));
        add(edit = new MenuEdit(this));
        add(project = new MenuProject(this));
        add(simulate = new MenuSimulate(this));
        //add(new WindowMenu(parent));
        add(help = new MenuHelp(this));
        
        LocaleManager.addLocaleListener(listener);
        listener.localeChanged();
    }
    
    public void setEnabled(LogisimMenuItem which, boolean value) {
        JMenuItem item = (JMenuItem) menuItems.get(which);
        if(item != null) item.setEnabled(value);
    }
    
    public void addActionListener(LogisimMenuItem which, ActionListener l) {
        MenuItem item = (MenuItem) menuItems.get(which);
        if(item != null) item.addActionListener(l);
    }
    
    public void removeActionListener(LogisimMenuItem which, ActionListener l) {
        MenuItem item = (MenuItem) menuItems.get(which);
        if(item != null) item.removeActionListener(l);
    }
    
    public void setSimulateListener(SimulateListener l) {
        simulateListener = l;
    }
    
    public void setCircuitState(Simulator sim, CircuitState state) {
        simulate.setCurrentState(sim, state);
    }

    Project getProject() {
        return proj;
    }
    
    JFrame getParentWindow() {
        return parent;
    }
    
    void registerItem(LogisimMenuItem which, MenuItem item) {
        menuItems.put(which, item);
    }
    
    void fireStateChanged(Simulator sim, CircuitState state) {
        if(simulateListener != null) {
            simulateListener.stateChangeRequested(sim, state);
        }
    }
}
