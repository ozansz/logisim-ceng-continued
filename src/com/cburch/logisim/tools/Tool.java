/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.main.Canvas;

//
// DRAWING TOOLS
//
public abstract class Tool {
    private static Cursor dflt_cursor
        = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    public abstract String getName();
    public abstract String getDisplayName();
    public abstract String getDescription();
    public Tool cloneTool() { return this; }
    public boolean sharesSource(Tool other) { return this == other; }
    public AttributeSet getAttributeSet() { return null; }
    public void setAttributeSet(AttributeSet attrs) { }
    public void paintIcon(ComponentDrawContext c, int x, int y) { }
    public String toString() { return getName(); }

    // This was the draw method until 2.0.4 - As of 2.0.5, you should
    // use the other draw method.
    public void draw(ComponentDrawContext context) { }
    public void draw(Canvas canvas, ComponentDrawContext context) {
        draw(context);
    }
    public void select(Canvas canvas) { }
    public void deselect(Canvas canvas) { }

    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) { }
    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) { }
    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) { }
    public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) { }
    public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) { }
    public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) { }

    public void keyTyped(Canvas canvas, KeyEvent e) { }
    public void keyPressed(Canvas canvas, KeyEvent e) { }
    public void keyReleased(Canvas canvas, KeyEvent e) { }
    public Cursor getCursor() { return dflt_cursor; }

}
