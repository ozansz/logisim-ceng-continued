/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.proj;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.StringUtil;

public class ProjectActions {
    private ProjectActions() { }

    public static Project doNew(SplashScreen monitor) {
        if(monitor != null) monitor.setProgress(SplashScreen.FILE_CREATE);
        Loader loader = new Loader(monitor);
        LogisimFile file = LogisimPreferences.getTemplate(loader).cloneLogisimFile(loader);
        
        return completeProject(monitor, loader, file);
    }
    
    private static Project completeProject(SplashScreen monitor, Loader loader,
            LogisimFile file) {
        if(monitor != null) monitor.setProgress(SplashScreen.PROJECT_CREATE);
        Project ret = new Project(file);
        
        if(monitor != null) monitor.setProgress(SplashScreen.FRAME_CREATE);
        Frame frame = new Frame(ret);
        ret.setFrame(frame);
        frame.setVisible(true);
        frame.toFront();
        frame.getCanvas().grabFocus();
        loader.setParent(frame);
        return ret;
    }

    public static Project doNew(Project baseProject) {
        Loader loader = new Loader(baseProject == null ? null : baseProject.getFrame());
        LogisimFile template = LogisimPreferences.getTemplate(loader);
        Project newProj = new Project(template.cloneLogisimFile(loader));
        
        Frame frame = new Frame(newProj);
        newProj.setFrame(frame);
        frame.setVisible(true);
        frame.getCanvas().grabFocus();
        newProj.getLogisimFile().getLoader().setParent(frame);
        return newProj;
    }
    
    public static Project doOpen(SplashScreen monitor, File source)
            throws LoadFailedException {
        if(monitor != null) monitor.setProgress(SplashScreen.FILE_LOAD);
        Loader loader = new Loader(monitor);
        LogisimFile file = loader.openLogisimFile(source);
        
        return completeProject(monitor, loader, file);
    }

    public static void doOpen(Component parent, Project baseProject) {
        JFileChooser chooser;
        if(baseProject != null) {
            Loader oldLoader = baseProject.getLogisimFile().getLoader();
            chooser = oldLoader.createChooser();
            if(oldLoader.getMainFile() != null) {
                chooser.setSelectedFile(oldLoader.getMainFile());
            }
        } else {
            chooser = new JFileChooser();
        }
        chooser.setFileFilter(Loader.LOGISIM_FILTER);
                    
        int returnVal = chooser.showOpenDialog(parent);
        if(returnVal != JFileChooser.APPROVE_OPTION) return;
        doOpen(parent, baseProject, chooser.getSelectedFile());
    }

    public static Project doOpen(Component parent,
            Project baseProject, File f) {
        Project proj = Projects.findProjectFor(f);
        Loader loader = null;
        if(proj != null) {
            proj.getFrame().toFront();
            loader = proj.getLogisimFile().getLoader();
            if(proj.isFileDirty()) {
                String message = StringUtil.format(Strings.get("openAlreadyMessage"),
                        proj.getLogisimFile().getName());
                String[] options = {
                        Strings.get("openAlreadyLoseChangesOption"),
                        Strings.get("openAlreadyNewWindowOption"),
                        Strings.get("openAlreadyCancelOption"),
                    };
                int result = JOptionPane.showOptionDialog(proj.getFrame(),
                        message, Strings.get("openAlreadyTitle"), 0,
                        JOptionPane.QUESTION_MESSAGE, null,
                        options, options[2]);
                if(result == 0) {
                    ; // keep proj as is, so that load happens into the window
                } else if(result == 1) {
                    proj = null; // we'll create a new project
                } else {
                    return proj;
                }
            }
        }

        if(proj == null && baseProject != null && baseProject.isStartupScreen()) {
            proj = baseProject;
            proj.setStartupScreen(false);
            loader = baseProject.getLogisimFile().getLoader();
        } else {
            loader = new Loader(baseProject == null ? parent : baseProject.getFrame());
        }

        try {
            LogisimFile lib = loader.openLogisimFile(f);
            if(lib == null) return null;
            if(proj == null) {
                proj = new Project(lib);
            } else {
                proj.setLogisimFile(lib);
            }
        } catch(LoadFailedException ex) {
            JOptionPane.showMessageDialog(parent,
                StringUtil.format(Strings.get("fileOpenError"),
                    ex.toString()),
                Strings.get("fileOpenErrorTitle"),
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        Frame frame = proj.getFrame();
        if(frame == null) {
            frame = new Frame(proj);
            proj.setFrame(frame);
        }
        frame.setVisible(true);
        frame.toFront();
        frame.getCanvas().grabFocus();
        proj.getLogisimFile().getLoader().setParent(frame);
        return proj;
    }
    

    // returns true if save is completed
    public static boolean doSaveAs(Project proj) {
        Loader loader = proj.getLogisimFile().getLoader();
        JFileChooser chooser = loader.createChooser();
        chooser.setFileFilter(Loader.LOGISIM_FILTER);
        if(loader.getMainFile() != null) {
            chooser.setSelectedFile(loader.getMainFile());
        }
        int returnVal = chooser.showSaveDialog(proj.getFrame());
        if(returnVal != JFileChooser.APPROVE_OPTION) return false;

        File f = chooser.getSelectedFile();
        if(!f.getName().endsWith(Loader.LOGISIM_EXTENSION)) {
            String oldName = f.getName();
            int extStart = oldName.indexOf('.');
            if(extStart < 0) {
                f = new File(f.getParentFile(), oldName + Loader.LOGISIM_EXTENSION);
            } else {
                String extension = oldName.substring(extStart);
                int action = JOptionPane.showConfirmDialog(proj.getFrame(),
                        StringUtil.format(Strings.get("replaceExtensionMessage"), extension),
                        Strings.get("replaceExtensionTitle"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if(action == JOptionPane.YES_OPTION) {
                    f = new File(f.getParentFile(), oldName.substring(0, extStart) + Loader.LOGISIM_EXTENSION);
                }
            }
        }
        
        if(f.exists()) {
            int confirm = JOptionPane.showConfirmDialog(proj.getFrame(),
                Strings.get("confirmOverwriteMessage"),
                Strings.get("confirmOverwriteTitle"),
                JOptionPane.YES_NO_OPTION);
            if(confirm != JOptionPane.YES_OPTION) return false;
        }
        return doSave(proj, f);
    }

    public static boolean doSave(Project proj) {
        Loader loader = proj.getLogisimFile().getLoader();
        File f = loader.getMainFile();
        if(f == null) return doSaveAs(proj);
        else return doSave(proj, f);
    }
    
    private static boolean doSave(Project proj, File f) {
        Loader loader = proj.getLogisimFile().getLoader();
        Tool oldTool = proj.getTool();
        proj.setTool(null);
        boolean ret = loader.save(proj.getLogisimFile(), f);
        if(ret) proj.setFileAsClean();
        proj.setTool(oldTool);
        return ret;
    }

    public static void doQuit() {
        ArrayList toClose = new ArrayList(Projects.getOpenProjects());
        for(Iterator it = toClose.iterator(); it.hasNext(); ) {
            Project proj = (Project) it.next();
            if(!proj.confirmClose(Strings.get("confirmQuitTitle"))) return;
        }
        System.exit(0);
    }
}
