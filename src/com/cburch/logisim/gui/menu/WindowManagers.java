/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JFrame;

import com.cburch.logisim.analyze.gui.AnalyzerManager;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.WindowMenuItemManager;

public class WindowManagers {
    private WindowManagers() { }
    
    public static void initialize() {
        if(!initialized) {
            initialized = true;
            AnalyzerManager.initialize();
            Projects.addPropertyChangeListener(Projects.projectListProperty, myListener);
            computeListeners();
        }
    }
    
    private static boolean initialized = false;
    private static MyListener myListener = new MyListener();
    private static HashMap projectMap = new LinkedHashMap(); // Project -> ProjectItemManager
    
    private static class MyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            computeListeners();
        }
    }

    private static class ProjectManager extends WindowMenuItemManager
            implements ProjectListener, LibraryListener {
        private Project proj;
        
        ProjectManager(Project proj) {
            super(proj.getLogisimFile().getName(), false);
            this.proj = proj;
            proj.addProjectListener(this);
            proj.addLibraryListener(this);
            frameOpened(proj.getFrame());
        }
        
        public JFrame getJFrame(boolean create) {
            return proj.getFrame();
        }
        
        public void projectChanged(ProjectEvent event) {
            if(event.getAction() == ProjectEvent.ACTION_SET_FILE) {
                setText((String) proj.getLogisimFile().getName());
            }
        }

        public void libraryChanged(LibraryEvent event) {
            if(event.getAction() == LibraryEvent.SET_NAME) {
                setText((String) event.getData());
            }           
        }
    }
    
    private static void computeListeners() {
        List nowOpen = Projects.getOpenProjects();
        
        HashSet closed = new HashSet(projectMap.keySet());
        closed.removeAll(nowOpen);
        for(Iterator it = closed.iterator(); it.hasNext(); ) {
            Project proj = (Project) it.next();
            ProjectManager manager = (ProjectManager) projectMap.get(proj);
            manager.frameClosed(manager.getJFrame(false));
            projectMap.remove(proj);
        }
        
        HashSet opened = new LinkedHashSet(nowOpen);
        opened.removeAll(projectMap.keySet());
        for(Iterator it = opened.iterator(); it.hasNext(); ) {
            Project proj = (Project) it.next();
            ProjectManager manager = new ProjectManager(proj);
            projectMap.put(proj, manager);
        }
    }
}
