/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.SimulateListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.Base;
import com.cburch.logisim.tools.Tool;

class MenuListener {
    private class FileListener implements ActionListener {
        private void register() {
            menubar.addActionListener(LogisimMenuBar.EXPORT_GIF, this);
            menubar.addActionListener(LogisimMenuBar.PRINT, this);
        }
        
        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            Project proj = frame.getProject();
            if(src == LogisimMenuBar.EXPORT_GIF) {
                ExportGif.doExport(proj);
            } else if(src == LogisimMenuBar.PRINT) {
                Print.doPrint(proj);
            }
        }
    }

    private class EditListener
            implements ProjectListener, LibraryListener, PropertyChangeListener,
                ActionListener {
        private void register() {
            Project proj = frame.getProject();
            Clipboard.addPropertyChangeListener(Clipboard.contentsProperty, this);
            proj.addProjectListener(this);
            proj.addLibraryListener(this);
            
            menubar.addActionListener(LogisimMenuBar.CUT, this);
            menubar.addActionListener(LogisimMenuBar.COPY, this);
            menubar.addActionListener(LogisimMenuBar.PASTE, this);
            menubar.addActionListener(LogisimMenuBar.DELETE, this);
            menubar.addActionListener(LogisimMenuBar.SELECT_ALL, this);
            enableItems();
        }

        public void projectChanged(ProjectEvent e) {
            int action = e.getAction();
            if(action == ProjectEvent.ACTION_SET_FILE) {
                enableItems();
            } else if(action == ProjectEvent.ACTION_SET_CURRENT) {
                enableItems();
            } else if(action == ProjectEvent.ACTION_SELECTION) {
                enableItems();
            }
        }

        public void libraryChanged(LibraryEvent e) {
            int action = e.getAction();
            if(action == LibraryEvent.ADD_LIBRARY) {
                enableItems();
            } else if(action == LibraryEvent.REMOVE_LIBRARY) {
                enableItems();
            }
        }

        public void propertyChange(PropertyChangeEvent event) {
            if(event.getPropertyName().equals(Clipboard.contentsProperty)) {
                enableItems();
            }
        }
        
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            Project proj = frame.getProject();
            if(src == LogisimMenuBar.CUT) {
                proj.doAction(SelectionActions.cut());
            } else if(src == LogisimMenuBar.COPY) {
                proj.doAction(SelectionActions.copy());
            } else if(src == LogisimMenuBar.PASTE) {
                selectSelectTool(proj);
                proj.doAction(SelectionActions.paste());
            } else if(src == LogisimMenuBar.DELETE) {
                proj.doAction(SelectionActions.clear());
            } else if(src == LogisimMenuBar.SELECT_ALL) {
                selectSelectTool(proj);
                Selection sel = proj.getSelection();
                Circuit circ = proj.getCurrentCircuit();
                sel.addAll(circ.getWires());
                sel.addAll(circ.getNonWires());
                proj.repaintCanvas();
            }
        }
        
        private void selectSelectTool(Project proj) {
            for(Iterator it = proj.getLogisimFile().getLibraries().iterator(); it.hasNext(); ) {
                Object lib = it.next();
                if(lib instanceof Base) {
                    Base base = (Base) lib;
                    Tool tool = base.getTool("Select Tool");
                    if(tool != null) proj.setTool(tool);
                }
            }
        }

        public void enableItems() {
            Project proj = frame.getProject();
            Selection sel = proj == null ? null : proj.getSelection();
            boolean selEmpty = (sel == null ? true : sel.isEmpty());
            boolean canChange = proj != null && proj.getLogisimFile().contains(proj.getCurrentCircuit());
            
            boolean selectAvailable = false;
            for(Iterator it = proj.getLogisimFile().getLibraries().iterator(); it.hasNext(); ) {
                Object lib = it.next();
                if(lib instanceof Base) selectAvailable = true;
            }

            menubar.setEnabled(LogisimMenuBar.CUT, !selEmpty && selectAvailable && canChange);
            menubar.setEnabled(LogisimMenuBar.COPY, !selEmpty && selectAvailable);
            menubar.setEnabled(LogisimMenuBar.PASTE, selectAvailable && canChange
                && !Clipboard.isEmpty());
            menubar.setEnabled(LogisimMenuBar.DELETE, !selEmpty && selectAvailable && canChange);
            menubar.setEnabled(LogisimMenuBar.SELECT_ALL, selectAvailable);
        }
    }

    class ProjectMenuListener implements ProjectListener, LibraryListener,
                ActionListener {
        void register() {
            Project proj = frame.getProject();
            if(proj == null) {
                return;
            }

            proj.addProjectListener(this);
            proj.addLibraryListener(this);
            
            menubar.addActionListener(LogisimMenuBar.ADD_CIRCUIT, this);
            menubar.addActionListener(LogisimMenuBar.RENAME_CIRCUIT, this);
            menubar.addActionListener(LogisimMenuBar.SET_MAIN_CIRCUIT, this);
            menubar.addActionListener(LogisimMenuBar.REMOVE_CIRCUIT, this);
            menubar.addActionListener(LogisimMenuBar.ANALYZE_CIRCUIT, this);
            
            computeEnabled();
        }

        public void projectChanged(ProjectEvent event) {
            int action = event.getAction();
            if(action == ProjectEvent.ACTION_SET_CURRENT) {
                computeEnabled();
            } else if(action == ProjectEvent.ACTION_SET_FILE) {
                computeEnabled();
            }
        }
        
        public void libraryChanged(LibraryEvent event) {
            computeEnabled();
        }
        
        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            Project proj = frame.getProject();
            if(src == LogisimMenuBar.ADD_CIRCUIT) {
                ProjectCircuitActions.doAddCircuit(proj);
            } else if(src == LogisimMenuBar.ANALYZE_CIRCUIT) {
                ProjectCircuitActions.doAnalyze(proj, proj.getCurrentCircuit());
            } else if(src == LogisimMenuBar.RENAME_CIRCUIT) {
                ProjectCircuitActions.doRenameCircuit(proj, proj.getCurrentCircuit());
            } else if(src == LogisimMenuBar.SET_MAIN_CIRCUIT) {
                ProjectCircuitActions.doSetAsMainCircuit(proj, proj.getCurrentCircuit());
            } else if(src == LogisimMenuBar.REMOVE_CIRCUIT) {
                ProjectCircuitActions.doRemoveCircuit(proj, proj.getCurrentCircuit());
            }
        }
        
        private void computeEnabled() {
            Project proj = frame.getProject();
            boolean isWritableCircuit = proj != null
                && proj.getLogisimFile().contains(proj.getCurrentCircuit());
            boolean isMainCircuit = proj != null
                && proj.getLogisimFile().getMainCircuit() == proj.getCurrentCircuit();
            
            menubar.setEnabled(LogisimMenuBar.ADD_CIRCUIT, proj != null);
            menubar.setEnabled(LogisimMenuBar.ANALYZE_CIRCUIT, true);
            menubar.setEnabled(LogisimMenuBar.RENAME_CIRCUIT, isWritableCircuit);
            menubar.setEnabled(LogisimMenuBar.SET_MAIN_CIRCUIT, isWritableCircuit && !isMainCircuit);
            menubar.setEnabled(LogisimMenuBar.REMOVE_CIRCUIT, isWritableCircuit && proj.getLogisimFile().getTools().size() > 1);
        }
    }

    class SimulateMenuListener implements ProjectListener, SimulateListener {
        void register() {
            Project proj = frame.getProject();
            proj.addProjectListener(this);
            menubar.setSimulateListener(this);
            menubar.setCircuitState(proj.getSimulator(), proj.getCircuitState());
        }
        
        public void projectChanged(ProjectEvent event) {
            if(event.getAction() == ProjectEvent.ACTION_SET_STATE) {
                menubar.setCircuitState(frame.getProject().getSimulator(),
                        frame.getProject().getCircuitState());
            }
        }

        public void stateChangeRequested(Simulator sim, CircuitState state) {
            if(state != null) frame.getProject().setCircuitState(state);
        }
    }
    
    private Frame frame;
    private LogisimMenuBar menubar;
    private FileListener fileListener = new FileListener();
    private EditListener editListener = new EditListener();
    private ProjectMenuListener projectListener = new ProjectMenuListener();
    private SimulateMenuListener simulateListener = new SimulateMenuListener();

    public MenuListener(Frame frame, LogisimMenuBar menubar) {
        this.frame = frame;
        this.menubar = menubar;
    }
    
    public void register() {
        fileListener.register();
        editListener.register();
        projectListener.register();
        simulateListener.register();
    }

}

