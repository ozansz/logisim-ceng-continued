/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.cburch.logisim.proj.Project;

class MenuProject extends Menu {
    private class MyListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            Project proj = menubar.getProject();
            if(src == loadBuiltin) {
                ProjectLibraryActions.doLoadBuiltinLibrary(proj);
            } else if(src == loadLogisim) {
                ProjectLibraryActions.doLoadLogisimLibrary(proj);
            } else if(src == loadJar) {
                ProjectLibraryActions.doLoadJarLibrary(proj);
            } else if(src == unload) {
                ProjectLibraryActions.doUnloadLibraries(proj);
            } else if(src == options) {
                JFrame frame = proj.getOptionsFrame(true);
                frame.setVisible(true);
            }
        }
    }
    
    private LogisimMenuBar menubar;
    private MyListener myListener = new MyListener();
    
    private MenuItem addCircuit = new MenuItem(this, LogisimMenuBar.ADD_CIRCUIT);
    private JMenu loadLibrary = new JMenu();
    private JMenuItem loadBuiltin = new JMenuItem();
    private JMenuItem loadLogisim = new JMenuItem();
    private JMenuItem loadJar = new JMenuItem();
    private JMenuItem unload = new JMenuItem();
    private MenuItem analyze = new MenuItem(this, LogisimMenuBar.ANALYZE_CIRCUIT);
    private MenuItem rename = new MenuItem(this, LogisimMenuBar.RENAME_CIRCUIT);
    private MenuItem setAsMain = new MenuItem(this, LogisimMenuBar.SET_MAIN_CIRCUIT);
    private MenuItem remove = new MenuItem(this, LogisimMenuBar.REMOVE_CIRCUIT);
    private JMenuItem options = new JMenuItem();

    MenuProject(LogisimMenuBar menubar) {
        this.menubar = menubar;

        menubar.registerItem(LogisimMenuBar.ADD_CIRCUIT, addCircuit);
        loadBuiltin.addActionListener(myListener);
        loadLogisim.addActionListener(myListener);
        loadJar.addActionListener(myListener);
        unload.addActionListener(myListener);
        menubar.registerItem(LogisimMenuBar.ANALYZE_CIRCUIT, analyze);
        menubar.registerItem(LogisimMenuBar.RENAME_CIRCUIT, rename);
        menubar.registerItem(LogisimMenuBar.SET_MAIN_CIRCUIT, setAsMain);
        menubar.registerItem(LogisimMenuBar.REMOVE_CIRCUIT, remove);
        options.addActionListener(myListener);
        
        loadLibrary.add(loadBuiltin);
        loadLibrary.add(loadLogisim);
        loadLibrary.add(loadJar);
        
        add(addCircuit);
        add(loadLibrary);
        add(unload);
        addSeparator();
        //add(analyze);
        add(rename);
        add(setAsMain);
        add(remove);
        addSeparator();
        add(options);

        boolean known = menubar.getProject() != null;
        loadLibrary.setEnabled(known);
        loadBuiltin.setEnabled(known);
        loadLogisim.setEnabled(known);
        loadJar.setEnabled(known);
        unload.setEnabled(known);
        options.setEnabled(known);
        computeEnabled();
    }
    
    public void localeChanged() {
        setText(Strings.get("projectMenu"));
        addCircuit.setText(Strings.get("projectAddCircuitItem"));
        loadLibrary.setText(Strings.get("projectLoadLibraryItem"));
        loadBuiltin.setText(Strings.get("projectLoadBuiltinItem"));
        loadLogisim.setText(Strings.get("projectLoadLogisimItem"));
        loadJar.setText(Strings.get("projectLoadJarItem"));
        unload.setText(Strings.get("projectUnloadLibrariesItem"));
        analyze.setText(Strings.get("projectAnalyzeCircuitItem"));
        rename.setText(Strings.get("projectRenameCircuitItem"));
        setAsMain.setText(Strings.get("projectSetAsMainItem"));
        remove.setText(Strings.get("projectRemoveCircuitItem"));
        options.setText(Strings.get("projectOptionsItem"));
    }
    
    void computeEnabled() {
        setEnabled(menubar.getProject() != null
                || addCircuit.hasListeners()
                || analyze.hasListeners()
                || rename.hasListeners()
                || setAsMain.hasListeners()
                || remove.hasListeners());
    }
}
