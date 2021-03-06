/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.util.Iterator;

import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryEventSource;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.ProjectLibraryActions;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

class ExplorerManip implements Explorer.Listener {
    private class MyListener
            implements ProjectListener, LibraryListener, AttributeListener {
        private LogisimFile curFile = null;
        
        public void projectChanged(ProjectEvent event) {
            int action = event.getAction();
            if(action == ProjectEvent.ACTION_SET_FILE) {
                setFile((LogisimFile) event.getOldData(),
                        (LogisimFile) event.getData());
                explorer.repaint();
            }
        }
        
        private void setFile(LogisimFile oldFile, LogisimFile newFile) {
            if(oldFile != null) {
                removeLibrary(oldFile);
                for(Iterator it = oldFile.getLibraries().iterator(); it.hasNext(); ) {
                    removeLibrary((Library) it.next());
                }
            }
            curFile = newFile;
            if(newFile != null) {
                addLibrary(newFile);
                for(Iterator it = newFile.getLibraries().iterator(); it.hasNext(); ) {
                    addLibrary((Library) it.next());
                }
            }
        }

        public void libraryChanged(LibraryEvent event) {
            int action = event.getAction();
            if(action == LibraryEvent.ADD_LIBRARY) {
                if(event.getSource() == curFile) {
                    addLibrary((Library) event.getData());
                }
            } else if(action == LibraryEvent.REMOVE_LIBRARY) {
                if(event.getSource() == curFile) {
                    removeLibrary((Library) event.getData());
                }
            } else if(action == LibraryEvent.ADD_TOOL) {
                Tool tool = (Tool) event.getData();
                AttributeSet attrs = tool.getAttributeSet();
                if(attrs != null) attrs.addAttributeListener(this);
            } else if(action == LibraryEvent.REMOVE_TOOL) {
                Tool tool = (Tool) event.getData();
                AttributeSet attrs = tool.getAttributeSet();
                if(attrs != null) attrs.removeAttributeListener(this);
            }
            explorer.repaint();
        }
        
        private void addLibrary(Library lib) {
            if(lib instanceof LibraryEventSource) {
                ((LibraryEventSource) lib).addLibraryListener(this);
            }
            for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
                Tool tool = (Tool) it.next();
                AttributeSet attrs = tool.getAttributeSet();
                if(attrs != null) attrs.addAttributeListener(this);
            }
        }
        
        private void removeLibrary(Library lib) {
            if(lib instanceof LibraryEventSource) {
                ((LibraryEventSource) lib).removeLibraryListener(this);
            }
            for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
                Tool tool = (Tool) it.next();
                AttributeSet attrs = tool.getAttributeSet();
                if(attrs != null) attrs.removeAttributeListener(this);
            }
        }


        public void attributeListChanged(AttributeEvent e) { }

        public void attributeValueChanged(AttributeEvent e) {
            explorer.repaint();
        }
        
    }
    
    private Project proj;
    private Explorer explorer;
    private MyListener myListener = new MyListener();
    private Tool lastSelected = null;
    
    ExplorerManip(Project proj, Explorer explorer) {
        this.proj = proj;
        this.explorer = explorer;
        proj.addProjectListener(myListener);
        myListener.setFile(null, proj.getLogisimFile());
    }

    public void selectionChanged(Explorer.Event event) {
        Object selected = event.getTarget();
        if(selected instanceof Tool) {
            lastSelected = proj.getTool();
            Tool tool = (Tool) selected;
            proj.setTool(tool);
            proj.getFrame().viewAttributes(tool);
        }
    }

    public void doubleClicked(Explorer.Event event) {
        Object clicked = event.getTarget();
        if(clicked instanceof AddTool) {
            AddTool tool = (AddTool) clicked;
            ComponentFactory source = tool.getFactory();
            if(source instanceof Circuit) {
                Circuit circ = (Circuit) source;
                proj.setCurrentCircuit(circ);
                if(lastSelected != null) proj.setTool(lastSelected);
            }
        }
    }
    
    public void moveRequested(Explorer.Event event, AddTool dragged, AddTool target) {
        LogisimFile file = proj.getLogisimFile();
        int draggedIndex = file.getTools().indexOf(dragged);
        int targetIndex = file.getTools().indexOf(target);
        if(targetIndex > draggedIndex) targetIndex++;
        proj.doAction(LogisimFileActions.moveCircuit(dragged, targetIndex));
    }
    
    public void deleteRequested(Explorer.Event event) {
        Object request = event.getTarget();
        if(request instanceof Library) {
            ProjectLibraryActions.doUnloadLibrary(proj, (Library) request);
        } else if(request instanceof AddTool) {
            ComponentFactory factory = ((AddTool) request).getFactory();
            if(factory instanceof Circuit) {
                ProjectCircuitActions.doRemoveCircuit(proj, (Circuit) factory);
            }
        }
    }

    public JPopupMenu menuRequested(Explorer.Event event) {
        Object clicked = event.getTarget();
        if(clicked instanceof AddTool) {
            AddTool tool = (AddTool) clicked;
            ComponentFactory source = tool.getFactory();
            if(source instanceof Circuit) {
                Circuit circ = (Circuit) source;
                return Popups.forCircuit(proj, tool, circ);
            } else {
                return null;
            }
        } else if(clicked instanceof Tool) {
            return null;
        } else if(clicked == proj.getLogisimFile()) {
            return Popups.forProject(proj);
        } else if(clicked instanceof Library) {
            boolean is_top = event.getTreePath().getPathCount() <= 2;
            return Popups.forLibrary(proj, (Library) clicked, is_top);
        } else {
            return null;
        }
    }

}
