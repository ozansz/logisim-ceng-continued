/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.Icons;

import java.util.Iterator;

public class PokeTool extends Tool {
    private static final Icon toolIcon = Icons.getIcon("poke.gif");
    private static final Color caretColor = new Color(255, 255, 150);

    private static class WireCaret extends AbstractCaret {
        AttributeSet opts;
        Canvas canvas;
        Wire wire;
        int x;
        int y;

        WireCaret(Canvas c, Wire w, int x, int y, AttributeSet opts) {
            canvas = c;
            wire = w;
            this.x = x;
            this.y = y;
            this.opts = opts;
        }

        public void draw(Graphics g) {
            Value v = canvas.getCircuitState().getValue(wire.getEnd0());
            RadixOption radix1 = (RadixOption) opts.getValue(Options.ATTR_RADIX_1);
            RadixOption radix2 = (RadixOption) opts.getValue(Options.ATTR_RADIX_2);
            if(radix1 == null) radix1 = RadixOption.RADIX_2;
            String vStr = radix1.toString(v);
            if(radix2 != null && v.getWidth() > 1) {
                vStr += " / " + radix2.toString(v);
            }
            
            FontMetrics fm = g.getFontMetrics();
            g.setColor(caretColor);
            g.fillRect(x + 2, y + 2, fm.stringWidth(vStr) + 4, 
                    fm.getAscent() + fm.getDescent() + 4);
            g.setColor(Color.BLACK);
            g.drawRect(x + 2, y + 2, fm.stringWidth(vStr) + 4, 
                    fm.getAscent() + fm.getDescent() + 4);
            g.fillOval(x - 2, y - 2, 5, 5);
            g.drawString(vStr, x + 4, y + 4 + fm.getAscent());
        }
    }

    private static Cursor cursor
        = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    private Caret caret;

    public PokeTool() { }
    
    public boolean equals(Object other) {
        return other instanceof PokeTool;
    }
    
    public int hashCode() {
        return PokeTool.class.hashCode();
    }

    public String getName() {
        return "Poke Tool";
    }

    public String getDisplayName() {
        return Strings.get("pokeTool");
    }

    public String getDescription() {
        return Strings.get("pokeToolDesc");
    }

    public void draw(Canvas canvas, ComponentDrawContext context) {
        if(caret != null) caret.draw(context.getGraphics());
    }

    public void deselect(Canvas canvas) {
        if(caret != null) {
            caret.stopEditing();
            caret = null;
        }
    }

    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Location loc = Location.create(x, y);
        boolean dirty = false;
        if(caret != null && !caret.getBounds(g).contains(loc)) {
            dirty = true;
            caret.stopEditing();
            caret = null;
        }
        if(caret == null) {
            ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);
            Circuit circ = canvas.getCircuit();
            Iterator it = circ.getAllContaining(loc, g).iterator();
            while(caret == null && it.hasNext()) {
                Component c = (Component) it.next();
                if(c instanceof Wire) {
                    caret = new WireCaret(canvas, (Wire) c, x, y,
                        canvas.getProject().getOptions().getAttributeSet());
                } else {
                    Pokable p = (Pokable) c.getFeature(Pokable.class);
                    if(p != null) {
                        caret = p.getPokeCaret(event);
                        AttributeSet attrs = c.getAttributeSet();
                        if(attrs != null && attrs.getAttributes().size() > 0) {
                            Project proj = canvas.getProject();
                            proj.getFrame().viewComponentAttributes(circ, c);
                        }
                    }
                }
            }
        }
        if(caret != null) {
            dirty = true;
            caret.mousePressed(e);
        }
        if(dirty) canvas.getProject().repaintCanvas();
    }

    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        if(caret != null) {
            caret.mouseDragged(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        if(caret != null) {
            caret.mouseReleased(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public void keyTyped(Canvas canvas, KeyEvent e) {
        if(caret != null) {
            caret.keyTyped(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public void keyPressed(Canvas canvas, KeyEvent e) {
        if(caret != null) {
            caret.keyPressed(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public void keyReleased(Canvas canvas, KeyEvent e) {
        if(caret != null) {
            caret.keyReleased(e);
            canvas.getProject().repaintCanvas();
        }
    }


    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Graphics g = c.getGraphics();
        if(toolIcon != null) {
            toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
        } else {
            g.setColor(java.awt.Color.black);
            g.drawLine(x + 4, y +  2, x + 4, y + 17);
            g.drawLine(x + 4, y + 17, x + 1, y + 11);
            g.drawLine(x + 4, y + 17, x + 7, y + 11);

            g.drawLine(x + 15, y +  2, x + 15, y + 17);
            g.drawLine(x + 15, y +  2, x + 12, y + 8);
            g.drawLine(x + 15, y +  2, x + 18, y + 8);
        }
    }

    public Cursor getCursor() { return cursor; }
}

