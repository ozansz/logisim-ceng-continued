/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

import java.util.Iterator;

public class WiringTool extends Tool {
    private static Cursor cursor
        = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private static final Icon toolIcon = Icons.getIcon("wiring.gif");

    private boolean exists = false;
    private boolean inCanvas = false;
    private Location start = Location.create(0, 0);
    private Location cur = Location.create(0, 0);
    private boolean startShortening = false;
    private boolean shortening = false;
    private Action lastAction = null;

    public WiringTool() { }
    
    public boolean equals(Object other) {
        return other instanceof WiringTool;
    }
    
    public int hashCode() {
        return WiringTool.class.hashCode();
    }

    public String getName() {
        return "Wiring Tool";
    }

    public String getDisplayName() {
        return Strings.get("wiringTool");
    }

    public String getDescription() {
        return Strings.get("wiringToolDesc");
    }

    private int findX(int new_x, int new_y) {
        int dist_x = Math.abs(new_x - start.getX());
        int dist_y = Math.abs(new_y - start.getY());
        if(dist_y > dist_x) {
            return start.getX();
        } else {
            return new_x;
        }
    }

    private int findY(int new_x, int new_y) {
        int dist_x = Math.abs(new_x - start.getX());
        int dist_y = Math.abs(new_y - start.getY());
        if(dist_y > dist_x) {
            return new_y;
        } else {
            return start.getY();
        }
    }

    public void draw(Canvas canvas, ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        if(exists) {
            g.setColor(shortening ? Color.WHITE : Color.BLACK);
            GraphicsUtil.switchToWidth(g, 3);
            g.drawLine(start.getX(), start.getY(), cur.getX(), cur.getY());
        } else if(canvas.getShowGhosts() && inCanvas) {
            g.setColor(Color.GRAY);
            g.fillOval(cur.getX() - 2, cur.getY() - 2, 5, 5);
        }
    }

    public void mouseEntered(Canvas canvas, Graphics g,
            MouseEvent e) {
        inCanvas = true;
        canvas.getProject().repaintCanvas();
    }

    public void mouseExited(Canvas canvas, Graphics g,
            MouseEvent e) {
        inCanvas = false;
        canvas.getProject().repaintCanvas();
    }

    public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
        Canvas.snapToGrid(e);
        inCanvas = true;
        int curX = e.getX();
        int curY = e.getY();
        if(cur.getX() != curX || cur.getY() != curY) {
            cur = Location.create(curX, curY);
        }
        canvas.getProject().repaintCanvas();
    }

    public void mousePressed(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(!canvas.getProject().getLogisimFile().contains(canvas.getCircuit())) {
            exists = false;
            canvas.setErrorMessage(Strings.getter("cannotModifyError"));
            return;
        }

        Canvas.snapToGrid(e);
        start = Location.create(e.getX(), e.getY());
        cur = start;
        exists = true;
        
        startShortening = !canvas.getCircuit().getWires(start).isEmpty();
        shortening = startShortening;

        super.mousePressed(canvas, g, e);
        canvas.getProject().repaintCanvas();
    }

    public void mouseDragged(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(exists) {
            Canvas.snapToGrid(e);
            int x = e.getX();
            int y = e.getY();
            int curX = findX(x, y);
            int curY = findY(x, y);
            if(cur.getX() == curX && cur.getY() == curY) return;
    
            Rectangle rect = new Rectangle();
            rect.add(start.getX(), start.getY());
            rect.add(cur.getX(), cur.getY());
            rect.add(curX, curY);
            rect.grow(3, 3);
    
            cur = Location.create(curX, curY);
            super.mouseDragged(canvas, g, e);
            
            shortening = false;
            if(startShortening) {
                for(Iterator it = canvas.getCircuit().getWires(start).iterator(); it.hasNext(); ) {
                    Wire w = (Wire) it.next();
                    if(w.contains(cur)) { shortening = true; break; }
                }
            }
            if(!shortening) {
                for(Iterator it = canvas.getCircuit().getWires(cur).iterator(); it.hasNext(); ) {
                    Wire w = (Wire) it.next();
                    if(w.contains(start)) { shortening = true; break; }
                }
            }
    
            canvas.repaint(rect);
        }
    }

    public void mouseReleased(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(!exists) return;

        Canvas.snapToGrid(e);
        int x = e.getX();
        int y = e.getY();
        int curX = findX(x, y);
        int curY = findY(x, y);
        if(cur.getX() != curX || cur.getY() != curY) {
            cur = Location.create(curX, curY);
        }
        exists = false;

        super.mouseReleased(canvas, g, e);

        // compute endpoints
        Wire w = Wire.create(cur, start);
        if(w.getEnd0().equals(w.getEnd1())) return;

        // See whether the drag needs to be repaired
checkForRepairs:
        for(int i = 1; i >= 0; i--) { // it happens that the larger end should have higher priority
            if(w.getLength() <= 10) break; // don't repair a short wire to nothing
            Location end = w.getEndLocation(i);
            if(!canvas.getCircuit().getNonWires(end).isEmpty()) continue;

            int delta = (i == 0 ? 10 : -10);
            Location cand;
            if(w.isVertical()) {
                cand = Location.create(end.getX(), end.getY() + delta);
            } else {
                cand = Location.create(end.getX() + delta, end.getY());
            }

            for(Iterator it = canvas.getCircuit().getNonWires(cand).iterator();
                    it.hasNext(); ) {
                Component comp = (Component) it.next();
                if(comp.getBounds().contains(end)) {
                    WireRepair repair = (WireRepair) comp.getFeature(WireRepair.class);
                    if(repair != null && repair.shouldRepairWire(new WireRepairData(w, cand))) {
                        w = Wire.create(w.getOtherEnd(end), cand);
                        canvas.repaint(end.getX() - 13, end.getY() - 13, 26, 26);
                        continue checkForRepairs;
                    }
                }
            }
        }
            
        if(w.getEnd0().equals(w.getEnd1())) return;
        Action act = CircuitActions.addComponent(canvas.getCircuit(), w, true);
        canvas.getProject().doAction(act);
        lastAction = act;
    }
    
    public void keyPressed(Canvas canvas, KeyEvent event) {
        switch(event.getKeyCode()) {
        case KeyEvent.VK_BACK_SPACE:
            if(lastAction != null && canvas.getProject().getLastAction() == lastAction) {
                canvas.getProject().undoAction();
                lastAction = null;
            }
        }
    }

    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Graphics g = c.getGraphics();
        if(toolIcon != null) {
            toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
        } else {
            g.setColor(java.awt.Color.black);
            g.drawLine(x + 3, y + 13, x + 17, y + 7);
            g.fillOval(x + 1, y + 11, 5, 5);
            g.fillOval(x + 15, y + 5, 5, 5);
        }
    }

    public Cursor getCursor() { return cursor; }
}
