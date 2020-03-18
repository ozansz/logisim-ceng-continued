/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Dependencies;
import com.cburch.logisim.util.StringUtil;

public class AddTool extends Tool {
    private static int SHOW_NONE    = 0;
    private static int SHOW_GHOST   = 1;
    private static int SHOW_ADD     = 2;
    private static int SHOW_ADD_NO  = 3;

    private static Cursor cursor
        = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    private class MyAttributeListener implements AttributeListener {
        public void attributeListChanged(AttributeEvent e) {
            recomputeBounds();
        }
        public void attributeValueChanged(AttributeEvent e) {
            recomputeBounds();
        }
    }

    private ComponentFactory source;
    private AttributeSet attrs;
    private Bounds bounds;
    private boolean should_snap;
    private int last_x = Integer.MIN_VALUE;
    private int last_y = Integer.MIN_VALUE;
    private int state = SHOW_GHOST;
    private Action lastAddition;

    public AddTool(ComponentFactory source) {
        this(source, source.createAttributeSet());
    }

    private AddTool(AddTool base) {
        this(base.source, (AttributeSet) base.attrs.clone());
    }

    private AddTool(ComponentFactory source, AttributeSet attrs) {
        this.source = source;
        this.attrs = attrs;
        this.bounds = source.getOffsetBounds(attrs).expand(5);
        attrs.addAttributeListener(new MyAttributeListener());
        
        Boolean value = (Boolean) source.getFeature(ComponentFactory.SHOULD_SNAP, attrs);
        should_snap = value == null ? true : value.booleanValue(); 
    }
    
    public boolean equals(Object other) {
        return other instanceof AddTool
            && source.equals(((AddTool) other).source);
    }
    
    public int hashCode() {
        return source.hashCode();
    }
    
    public boolean sharesSource(Tool other) {
        if(!(other instanceof AddTool)) return false;
        AddTool o = (AddTool) other;
        return this.source.equals(o.source);
    }

    public ComponentFactory getFactory() {
        return source;
    }

    public String getName() {
        return source.getName();
    }

    public String getDisplayName() {
        return source.getDisplayName();
    }

    public String getDescription() {
        String ret = (String) source.getFeature(ComponentFactory.TOOL_TIP, attrs);
        if(ret == null) {
            ret = StringUtil.format(Strings.get("addToolText"),
                    source.getDisplayName());
        }
        return ret;
    }

    public Tool cloneTool() {
        return new AddTool(this);
    }

    public AttributeSet getAttributeSet() {
        return attrs;
    }

    public void draw(Canvas canvas, ComponentDrawContext context) {
        if(state == SHOW_GHOST) {
            source.drawGhost(context,
                Color.GRAY, last_x, last_y, attrs);
        } else if(state == SHOW_ADD) {
            source.drawGhost(context,
                Color.BLACK, last_x, last_y, attrs);
        }
    }

    public void cancelOp() { }

    public void select(Canvas canvas) {
        setState(canvas, SHOW_GHOST);
        recomputeBounds();
    }

    public void deselect(Canvas canvas) {
        setState(canvas, SHOW_GHOST);
        moveTo(canvas, canvas.getGraphics(),
            Integer.MAX_VALUE, Integer.MAX_VALUE);
        recomputeBounds();
        lastAddition = null;
    }

    private synchronized void moveTo(Canvas canvas, Graphics g,
            int x, int y) {
        if(state != SHOW_NONE) expose(canvas, last_x, last_y);
        last_x = x;
        last_y = y;
        if(state != SHOW_NONE) expose(canvas, last_x, last_y);
    }

    public void mouseEntered(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(state == SHOW_GHOST || state == SHOW_NONE) {
            setState(canvas, SHOW_GHOST);
            canvas.grabFocus();
        } else if(state == SHOW_ADD_NO) {
            setState(canvas, SHOW_ADD);
            canvas.grabFocus();
        }
    }

    public void mouseExited(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(state == SHOW_GHOST) {
            moveTo(canvas, canvas.getGraphics(),
                Integer.MAX_VALUE, Integer.MAX_VALUE);
            setState(canvas, SHOW_NONE);
        } else if(state == SHOW_ADD) {
            moveTo(canvas, canvas.getGraphics(),
                Integer.MAX_VALUE, Integer.MAX_VALUE);
            setState(canvas, SHOW_ADD_NO);
        }
    }

    public void mouseMoved(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(state != SHOW_NONE) {
            if(should_snap) Canvas.snapToGrid(e);
            moveTo(canvas, g, e.getX(), e.getY());
        }
    }

    public void mousePressed(Canvas canvas, Graphics g,
            MouseEvent e) {
        // verify the addition would be valid
        Circuit circ = canvas.getCircuit();
        if(!canvas.getProject().getLogisimFile().contains(circ)) {
            canvas.setErrorMessage(Strings.getter("cannotModifyError"));
            return;
        }
        Dependencies depends = canvas.getProject().getDependencies();
        if(source instanceof Circuit
                && !depends.canAdd(circ, (Circuit) source)) {
            canvas.setErrorMessage(Strings.getter("circularError"));
            return;
        }

        if(should_snap) Canvas.snapToGrid(e);
        moveTo(canvas, g, e.getX(), e.getY());
        setState(canvas, SHOW_ADD);
    }

    public void mouseDragged(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(state != SHOW_NONE) {
            if(should_snap) Canvas.snapToGrid(e);
            moveTo(canvas, g, e.getX(), e.getY());
        }
    }

    public void mouseReleased(Canvas canvas, Graphics g,
            MouseEvent e) {
        if(state == SHOW_ADD) {
            Circuit circ = canvas.getCircuit();
            if(!canvas.getProject().getLogisimFile().contains(circ)) return;
            if(should_snap) Canvas.snapToGrid(e);
            moveTo(canvas, g, e.getX(), e.getY());

            Location loc = Location.create(e.getX(), e.getY());
            AttributeSet attrs_copy = (AttributeSet) attrs.clone();
            Component c = source.createComponent(loc, attrs_copy);
            
            if(circ.hasConflict(c)) {
                canvas.setErrorMessage(Strings.getter("exclusiveError"));
                return;
            }
            
            Bounds bds = c.getBounds(g);
            if(bds.getX() < 0 || bds.getY() < 0) {
                canvas.setErrorMessage(Strings.getter("negativeCoordError"));
                return;
            }

            try {
                lastAddition = CircuitActions.addComponent(circ, c, false);
                canvas.getProject().doAction(lastAddition);
            } catch(CircuitException ex) {
                JOptionPane.showMessageDialog(canvas.getProject().getFrame(),
                    ex.getMessage());
            }
            setState(canvas, SHOW_GHOST);
        } else if(state == SHOW_ADD_NO) {
            setState(canvas, SHOW_NONE);
        }
    }
    
    public void keyPressed(Canvas canvas, KeyEvent event) {
        Direction facing = null;
        switch(event.getKeyCode()) {
        case KeyEvent.VK_UP:    facing = Direction.NORTH; break;
        case KeyEvent.VK_DOWN:  facing = Direction.SOUTH; break;
        case KeyEvent.VK_LEFT:  facing = Direction.WEST; break;
        case KeyEvent.VK_RIGHT: facing = Direction.EAST; break;
        case KeyEvent.VK_BACK_SPACE:
            if(lastAddition != null && canvas.getProject().getLastAction() == lastAddition) {
                canvas.getProject().undoAction();
                lastAddition = null;
            }
        }
        if(facing != null) {
            Attribute attr = (Attribute) source.getFeature(ComponentFactory.FACING_ATTRIBUTE_KEY, attrs);
            if(attr != null) {
                attrs.setValue(attr, facing);
                canvas.repaint();
            }
        }
    }

    public void paintIcon(ComponentDrawContext c, int x, int y) {
        source.paintIcon(c, x, y, attrs);
    }

    private void expose(java.awt.Component c, int x, int y) {
        Bounds bds = bounds;
        c.repaint(x + bds.getX(), y + bds.getY(),
            bds.getWidth(), bds.getHeight());
    }

    public Cursor getCursor() { return cursor; }

    private void setState(Canvas canvas, int value) {
        if(value == SHOW_GHOST) {
            if(canvas.getShowGhosts()
                    && canvas.getProject().getLogisimFile().contains(canvas.getCircuit())) {
                state = SHOW_GHOST;
            } else {
                state = SHOW_NONE;
            }
        } else{
            state = value;
        }
    }

    private void recomputeBounds() {
        bounds = source.getOffsetBounds(attrs).expand(5);
    }
}
