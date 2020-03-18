/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SelectTool extends Tool {
    private static final Cursor selectCursor
        = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor rectSelectCursor
        = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor moveCursor
        = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

    private static final int IDLE = 0;
    private static final int MOVING = 1;
    private static final int RECT_SELECT = 2;
    private static final Icon toolIcon = Icons.getIcon("select.gif");

    private Location start = null;
    private int state = IDLE;
    private int cur_dx;
    private int cur_dy;
    private Set connectPoints;

    public SelectTool() { }
    
    public boolean equals(Object other) {
        return other instanceof SelectTool;
    }
    
    public int hashCode() {
        return SelectTool.class.hashCode();
    }

    public String getName() {
        return "Select Tool";
    }

    public String getDisplayName() {
        return Strings.get("selectTool");
    }

    public String getDescription() {
        return Strings.get("selectToolDesc");
    }

    public void draw(Canvas canvas, ComponentDrawContext context) {
        Project proj = canvas.getProject();
        if(state == MOVING) {
            if(connectPoints != null && ((cur_dx != 0 && cur_dy == 0) || (cur_dx == 0 && cur_dy != 0))) {
                Graphics g = context.getGraphics();
                g.setColor(Color.gray);
                GraphicsUtil.switchToWidth(g, 3);
                for(Iterator it = connectPoints.iterator(); it.hasNext(); ) {
                    Location loc = (Location) it.next();
                    int x = loc.getX();
                    int y = loc.getY();
                    g.drawLine(x, y, x + cur_dx, y + cur_dy);
                }
                GraphicsUtil.switchToWidth(g, 1);
            }
            proj.getSelection().drawGhostsShifted(context, cur_dx, cur_dy);
        } else if(state == RECT_SELECT) {
            int left = start.getX();
            int right = left + cur_dx;
            if(left > right) { int i = left; left = right; right = i; }
            int top = start.getY();
            int bot = top + cur_dy;
            if(top > bot) { int i = top; top = bot; bot = i; }

            Graphics g = context.getGraphics();
            g.setColor(Color.gray);
            g.drawRect(left, top, right - left, bot - top);
        }
    }

    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        Project proj = canvas.getProject();
        Selection sel = proj.getSelection();
        Circuit circuit = canvas.getCircuit();
        start = Location.create(e.getX(), e.getY());
        cur_dx = 0;
        cur_dy = 0;

        // if the user clicks into the selection,
        // selection is being modified
        Collection in_sel = sel.getComponentsContaining(start, g);
        if(!in_sel.isEmpty()) {
            if((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
                setState(proj, MOVING);
                proj.repaintCanvas();
                return;
            } else {
                Iterator it = in_sel.iterator();
                while(it.hasNext()) {
                    Component comp = (Component) it.next();
                    sel.remove(comp);
                }
            }
        }

        // if the user clicks into a component outside selection, user
        // wants to add/reset selection
        Collection clicked = circuit.getAllContaining(start, g);
        if(!clicked.isEmpty()) {
            if((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
                if(sel.getComponentsContaining(start).isEmpty()) {
                    sel.clear();
                }
            }
            boolean isFirst = true;
            Iterator it = clicked.iterator();
            while(it.hasNext()) {
                Component comp = (Component) it.next();
                if(!in_sel.contains(comp)) {
                    sel.add(comp);
                }
                if(isFirst) {
                    AttributeSet attrs = comp.getAttributeSet();
                    if(attrs != null && attrs.getAttributes().size() > 0) {
                        isFirst = false;
                        proj.getFrame().viewComponentAttributes(circuit, comp);
                    }
                }
            }
            setState(proj, MOVING);
            proj.repaintCanvas();
            return;
        }

        // The user clicked on the background. This is a rectangular
        // selection (maybe with the shift key down).
        if((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
            sel.clear();
        }
        setState(proj, RECT_SELECT);
        proj.repaintCanvas();
    }

    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        if(state == MOVING) {
            Project proj = canvas.getProject();
            computeDxDy(proj, e, g);
            if(connectPoints == null && (cur_dx != 0 || cur_dy != 0)) {
                connectPoints = computeConnectPoints(canvas);
            }
            proj.repaintCanvas();
        } else if(state == RECT_SELECT) {
            Project proj = canvas.getProject();
            cur_dx = e.getX() - start.getX();
            cur_dy = e.getY() - start.getY();
            proj.repaintCanvas();
        }
    }

    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        Project proj = canvas.getProject();
        if(state == MOVING) {
            setState(proj, IDLE);
            computeDxDy(proj, e, g);
            if(cur_dx != 0 || cur_dy != 0) {
                if(!proj.getLogisimFile().contains(canvas.getCircuit())) {
                    canvas.setErrorMessage(Strings.getter("cannotModifyError"));
                } else if(proj.getSelection().hasConflictWhenMoved(cur_dx, cur_dy)) {
                    canvas.setErrorMessage(Strings.getter("exclusiveError"));
                } else {
                    if(!LogisimPreferences.getStretchWires()) connectPoints = null;
                    proj.doAction(SelectionActions.move(cur_dx, cur_dy, connectPoints));
                }
            }
            connectPoints = null;
            proj.repaintCanvas();
        } else if(state == RECT_SELECT) {
            Bounds bds = Bounds.create(start).add(start.getX() + cur_dx,
                start.getY() + cur_dy);
            Circuit circuit = canvas.getCircuit();
            Selection sel = proj.getSelection();
            Collection in_sel = sel.getComponentsWithin(bds, g);
            Iterator it = circuit.getAllWithin(bds, g).iterator();
            while(it.hasNext()) {
                Component comp = (Component) it.next();
                if(!in_sel.contains(comp)) sel.add(comp);
            }
            it = in_sel.iterator();
            while(it.hasNext()) {
                Component comp = (Component) it.next();
                sel.remove(comp);
            }
            setState(proj, IDLE);
            proj.repaintCanvas();
        }
    }

    private void computeDxDy(Project proj, MouseEvent e, Graphics g) {
        Bounds bds = proj.getSelection().getBounds(g);
        if(bds == Bounds.EMPTY_BOUNDS) {
            cur_dx = e.getX() - start.getX();
            cur_dy = e.getY() - start.getY();
        } else {
            cur_dx = Math.max(e.getX() - start.getX(), -bds.getX());
            cur_dy = Math.max(e.getY() - start.getY(), -bds.getY());
        }

        Selection sel = proj.getSelection();
        if(sel.shouldSnap()) {
            cur_dx = Canvas.snapXToGrid(cur_dx);
            cur_dy = Canvas.snapYToGrid(cur_dy);
        }
    }

    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Graphics g = c.getGraphics();
        if(toolIcon != null) {
            toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
        } else {
            int[] xp = { x+ 5, x+ 5, x+ 9, x+12, x+14, x+11, x+16 };
            int[] yp = { y   , y+17, y+12, y+18, y+18, y+12, y+12 };
            g.setColor(java.awt.Color.black);
            g.fillPolygon(xp, yp, xp.length);
        }
    }

    public Cursor getCursor() {
        return state == IDLE ? selectCursor :
            (state == RECT_SELECT ? rectSelectCursor : moveCursor);
    }

    private void setState(Project proj, int new_state) {
        if(state == new_state) return; // do nothing if state not new

        state = new_state;
        proj.getSelection().setVisible(state != MOVING);
        proj.getFrame().getCanvas().setCursor(getCursor());
    }

    private Set computeConnectPoints(Canvas canvas) {
        if(!LogisimPreferences.getStretchWires()) return null;
        Selection sel = canvas.getProject().getSelection();
        if(sel == null) return null;
        Circuit circ = canvas.getCircuit();
        Collection anchored = sel.getAnchoredComponents();
        if(anchored == null || anchored.isEmpty()) return null;
        
        HashSet ret = new HashSet();
        for(Iterator it = anchored.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            for(Iterator it2 = comp.getEnds().iterator(); it2.hasNext(); ) {
                EndData end = (EndData) it2.next();
                Location loc = end.getLocation();
                for(Iterator it3 = circ.getComponents(loc).iterator(); it3.hasNext(); ) {
                    Component comp2 = (Component) it3.next();
                    if(!anchored.contains(comp2)) {
                        ret.add(loc);
                    }
                }
            }
        }
        return ret.isEmpty() ? null : ret;
    }
}
