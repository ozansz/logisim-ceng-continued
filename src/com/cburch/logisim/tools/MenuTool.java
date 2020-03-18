/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;


import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.Project;

import java.util.Collection;

public class MenuTool extends Tool {
    private class MenuComponent extends JPopupMenu
            implements ActionListener {
        Project proj;
        Circuit circ;
        Component comp;
        JMenuItem del = new JMenuItem(Strings.get("compDeleteItem"));
        JMenuItem attrs = new JMenuItem(Strings.get("compShowAttrItem"));

        MenuComponent(Project proj, Circuit circ, Component comp) {
            this.proj = proj;
            this.circ = circ;
            this.comp = comp;
            boolean canChange = proj.getLogisimFile().contains(circ);

            add(del); del.addActionListener(this);
            del.setEnabled(canChange);
            add(attrs); attrs.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if(src == del) {
                Circuit circ = proj.getCurrentCircuit();
//              proj.getSelection().removeComponent(comp);
                proj.doAction(CircuitActions.removeComponent(circ, comp));
            } else if(src == attrs) {
                proj.getFrame().viewComponentAttributes(circ, comp);
            }
        }
    }

    private class MenuSelection extends JPopupMenu
            implements ActionListener {
        Project proj;
        JMenuItem del = new JMenuItem(Strings.get("selDeleteItem"));
        JMenuItem cut = new JMenuItem(Strings.get("selCutItem"));
        JMenuItem copy = new JMenuItem(Strings.get("selCopyItem"));

        MenuSelection(Project proj) {
            this.proj = proj;
            boolean canChange = proj.getLogisimFile().contains(proj.getCurrentCircuit());
            add(del); del.addActionListener(this);
            del.setEnabled(canChange);
            add(cut); cut.addActionListener(this);
            cut.setEnabled(canChange);
            add(copy); copy.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if(src == del) {
                proj.doAction(SelectionActions.clear());
            } else if(src == cut) {
                proj.doAction(SelectionActions.cut());
            } else if(src == copy) {
                proj.doAction(SelectionActions.copy());
            }
        }

        public void show(JComponent parent, int x, int y) {
            super.show(this, x, y);
        }
    }

    public MenuTool() { }
    
    public boolean equals(Object other) {
        return other instanceof MenuTool;
    }
    
    public int hashCode() {
        return MenuTool.class.hashCode();
    }

    public String getName() { return "Menu Tool"; }

    public String getDisplayName() { return Strings.get("menuTool"); }

    public String getDescription() { return Strings.get("menuToolDesc"); }

    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Location pt = Location.create(x, y);

        JPopupMenu menu;
        Project proj = canvas.getProject();
        Selection sel = proj.getSelection();
        Collection in_sel = sel.getComponentsContaining(pt, g);
        if(!in_sel.isEmpty()) {
            Component comp = (Component) in_sel.iterator().next();
            if(sel.getComponents().size() > 1) {
                menu = new MenuSelection(proj);
            } else {
                menu = new MenuComponent(proj,
                    canvas.getCircuit(), comp);
                MenuExtender extender = (MenuExtender) comp.getFeature(MenuExtender.class);
                if(extender != null) extender.configureMenu(menu, proj);
            }
        } else {
            Collection cl = canvas.getCircuit().getAllContaining(pt, g);
            if(!cl.isEmpty()) {
                Component comp = (Component) cl.iterator().next();
                menu = new MenuComponent(proj,
                    canvas.getCircuit(), comp);
                MenuExtender extender = (MenuExtender) comp.getFeature(MenuExtender.class);
                if(extender != null) extender.configureMenu(menu, proj);
            } else {
                menu = null;
            }
        }

        if(menu != null) {
            canvas.showPopupMenu(menu, x, y);
        }
    }

    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Graphics g = c.getGraphics();
        g.fillRect(x + 2, y + 1, 9, 2);
        g.drawRect(x + 2, y + 3, 15, 12);
        g.setColor(Color.lightGray);
        g.drawLine(x + 4, y + 2, x + 8, y + 2);
        for(int y_offs = y + 6; y_offs < y + 15; y_offs += 3) {
            g.drawLine(x + 4, y_offs, x + 14, y_offs);
        }
    }
}
