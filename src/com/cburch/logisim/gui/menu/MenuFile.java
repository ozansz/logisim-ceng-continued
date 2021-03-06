/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.opts.OptionsFrame;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.util.MacCompatibility;

class MenuFile extends Menu implements ActionListener {
    private LogisimMenuBar menubar;
    private JMenuItem newi = new JMenuItem();
    private JMenuItem open = new JMenuItem();
    private JMenuItem close = new JMenuItem();
    private JMenuItem save = new JMenuItem();
    private JMenuItem saveAs = new JMenuItem();
    private MenuItem print = new MenuItem(this, LogisimMenuBar.PRINT);
    private MenuItem exportGif = new MenuItem(this, LogisimMenuBar.EXPORT_GIF);
    private JMenuItem prefs = new JMenuItem();
    private JMenuItem quit = new JMenuItem();

    public MenuFile(LogisimMenuBar menubar) {
        this.menubar = menubar;
        
        int menuMask = getToolkit().getMenuShortcutKeyMask();

        newi.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_N, menuMask));
        open.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_O, menuMask));
        close.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_W, menuMask));
        save.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, menuMask));
        saveAs.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, menuMask | InputEvent.SHIFT_MASK));
        print.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_P, menuMask));
        quit.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, menuMask));

        add(newi);
        add(open);
        addSeparator();
        add(close);
        add(save);
        add(saveAs);
        addSeparator();
        add(exportGif);
        add(print);
        if(!MacCompatibility.isPreferencesAutomaticallyPresent()) {
            addSeparator();
            add(prefs);
        }
        if(!MacCompatibility.isQuitAutomaticallyPresent()) {
            addSeparator();
            add(quit);
        }

        Project proj = menubar.getProject();
        newi.addActionListener(this);
        open.addActionListener(this);
        if(proj == null) {
            close.setEnabled(false);
            save.setEnabled(false);
            saveAs.setEnabled(false);
        } else {
            close.addActionListener(this);
            save.addActionListener(this);
            saveAs.addActionListener(this);
        }
        menubar.registerItem(LogisimMenuBar.EXPORT_GIF, exportGif);
        menubar.registerItem(LogisimMenuBar.PRINT, print);
        prefs.addActionListener(this);
        quit.addActionListener(this);
    }

    public void localeChanged() {
        this.setText(Strings.get("fileMenu"));
        newi.setText(Strings.get("fileNewItem"));
        open.setText(Strings.get("fileOpenItem"));
        close.setText(Strings.get("fileCloseItem"));
        save.setText(Strings.get("fileSaveItem"));
        saveAs.setText(Strings.get("fileSaveAsItem"));
        exportGif.setText(Strings.get("fileExportGifItem"));
        print.setText(Strings.get("filePrintItem"));
        prefs.setText(Strings.get("filePreferencesItem"));
        quit.setText(Strings.get("fileQuitItem"));
    }

    void computeEnabled() {
        setEnabled(true);
    }
    
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        Project proj = menubar.getProject();
        if(src == newi) {
            ProjectActions.doNew(proj);
        } else if(src == open) {
            ProjectActions.doOpen(proj == null ? null : proj.getFrame().getCanvas(), proj);
        } else if(src == close) {
            Frame frame = proj.getFrame();
            if(frame.confirmClose()) {
                frame.dispose();
                OptionsFrame f = proj.getOptionsFrame(false);
                if(f != null) f.dispose();
            }
        } else if(src == save) {
            ProjectActions.doSave(proj);
        } else if(src == saveAs) {
            ProjectActions.doSaveAs(proj);
        } else if(src == prefs) {
            PreferencesFrame.showPreferences();
        } else if(src == quit) {
            ProjectActions.doQuit();
        }
    }
}
