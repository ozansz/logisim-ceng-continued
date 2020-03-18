/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.proj.Project;

class MemMenu implements ActionListener {
    private Project proj;
    private Frame frame;
    private Mem mem;
    private CircuitState circState;
    private JMenuItem edit = new JMenuItem(Strings.get("ramEditMenuItem"));
    private JMenuItem clear = new JMenuItem(Strings.get("ramClearMenuItem"));
    private JMenuItem load = new JMenuItem(Strings.get("ramLoadMenuItem"));
    private JMenuItem save = new JMenuItem(Strings.get("ramSaveMenuItem"));

    MemMenu(Project proj, Mem ram) {
        this.proj = proj;
        this.mem = ram;
        this.frame = proj.getFrame();
        this.circState = proj.getCircuitState();

        if(circState == null) {
            edit.setEnabled(false);
            clear.setEnabled(false);
            load.setEnabled(false);
            save.setEnabled(false);
        }

        edit.addActionListener(this);
        clear.addActionListener(this);
        load.addActionListener(this);
        save.addActionListener(this);
    }

    void appendTo(JPopupMenu menu) {
        menu.add(edit);
        menu.add(clear);
        menu.add(load);
        menu.add(save);
    }

    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if(src == edit) doEdit();
        else if(src == clear) doClear();
        else if(src == load) doLoad();
        else if(src == save) doSave();
    }

    private void doEdit() {
        MemState s = mem.getState(circState);
        if(s == null) return;
        HexFrame frame = mem.getHexFrame(proj, circState);
        frame.setVisible(true);
        frame.toFront();
    }

    private void doClear() {
        MemState s = mem.getState(circState);
        boolean isAllZero = s.getContents().isClear();
        if(isAllZero) return;

        int choice = JOptionPane.showConfirmDialog(frame,
                Strings.get("ramConfirmClearMsg"),
                Strings.get("ramConfirmClearTitle"),
                JOptionPane.YES_NO_OPTION);
        if(choice == JOptionPane.YES_OPTION) {
            s.getContents().clear();
        }
    }

    private void doLoad() {
        MemState s = mem.getState(circState);

        JFileChooser chooser = proj.createChooser();
        if(mem.getCurrentImage() != null)
            chooser.setSelectedFile(mem.getCurrentImage());
        chooser.setDialogTitle(Strings.get("ramLoadDialogTitle"));
        int choice = chooser.showOpenDialog(frame);
        if(choice == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                HexFile.open(s.getContents(), f);
                mem.setCurrentImage(f);
            } catch(IOException e) {
                JOptionPane.showMessageDialog(frame, e.getMessage(),
                        Strings.get("ramLoadErrorTitle"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doSave() {
        MemState s = mem.getState(circState);

        JFileChooser chooser = proj.createChooser();
        if(mem.getCurrentImage() != null)
            chooser.setSelectedFile(mem.getCurrentImage());
        chooser.setDialogTitle(Strings.get("ramSaveDialogTitle"));
        int choice = chooser.showSaveDialog(frame);
        if(choice == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                HexFile.save(f, s.getContents());
                mem.setCurrentImage(f);
            } catch(IOException e) {
                JOptionPane.showMessageDialog(frame, e.getMessage(),
                    Strings.get("ramSaveErrorTitle"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
