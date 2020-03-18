/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;

class ProjectsDirty {
    private ProjectsDirty() { }
    
    private static class DirtyListener implements LibraryListener {
        Project proj;
        
        DirtyListener(Project proj) {
            this.proj = proj;
        }
        
        public void libraryChanged(LibraryEvent event) {
            if(event.getAction() == LibraryEvent.DIRTY_STATE) {
                LogisimFile lib = proj.getLogisimFile();
                File file = lib.getLoader().getMainFile();
                LibraryManager.instance.setDirty(file, lib.isDirty());
            }
        }
    }
    
    private static class ProjectListListener implements PropertyChangeListener {
        public synchronized void propertyChange(PropertyChangeEvent event) {
            for(int i = 0, n = listeners.size(); i < n; i++) {
                DirtyListener l = (DirtyListener) listeners.get(i);
                l.proj.removeLibraryListener(l);
            }
            listeners.clear();
            for(Iterator it = Projects.getOpenProjects().iterator(); it.hasNext(); ) {
                Project proj = (Project) it.next();
                DirtyListener l = new DirtyListener(proj);
                proj.addLibraryListener(l);
                listeners.add(l);
                
                LogisimFile lib = proj.getLogisimFile();
                LibraryManager.instance.setDirty(lib.getLoader().getMainFile(), lib.isDirty());
            }
        }
    }
    
    private static ProjectListListener projectListListener = new ProjectListListener();
    private static ArrayList listeners = new ArrayList();

    public static void initialize() {
        Projects.addPropertyChangeListener(Projects.projectListProperty, projectListListener);
    }
}
