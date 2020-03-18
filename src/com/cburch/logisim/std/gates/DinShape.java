/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.IntegerFactory;

class DinShape {
    private DinShape() { }
    
    static final int AND = 0;
    static final int OR = 1;
    static final int XOR = 2;
    static final int XNOR = 3;
    
    private static HashMap orLenArrays = new HashMap();
    
    static void draw(ComponentDrawContext context, int x, int y,
            int width, int height, boolean drawBubble, int dinType) {
        Graphics g = context.getGraphics();
        int xMid = x - width;
        int xBase = x;
        int y0 = y - height / 2;
        if(drawBubble) {
            x -= 4;
            width -= 8;
        }
        int diam = Math.min(height, 2 * width);
        if(dinType == AND) {
            ; // nothing to do
        } else if(dinType == OR) {
            // TODO
        } else if(dinType == XOR || dinType == XNOR) {
            int elen = Math.min(diam / 2 - 10, 20);
            int ex0 = xMid + (diam / 2 - elen) / 2;
            int ex1 = ex0 + elen;
            g.drawLine(ex0, y - 5, ex1, y - 5);
            g.drawLine(ex0, y, ex1, y);
            g.drawLine(ex0, y + 5, ex1, y + 5);
            if(dinType == XOR) {
                int exMid = ex0 + elen / 2;
                g.drawLine(exMid, y - 8, exMid, y + 8);
            }
        } else {
            throw new IllegalArgumentException("unrecognized shape");
        }
        GraphicsUtil.switchToWidth(g, 2);
        int x0 = xMid - diam / 2;
        Color oldColor = g.getColor();
        if(context.getShowState()) {
            CircuitState state = context.getCircuitState();
            if(state != null) {
                Location loc = Location.create(xBase, y);
                Value val = state.getValue(loc);
                g.setColor(val.getColor());
            }
        }
        g.drawLine(x0 + diam, y, x, y);
        g.setColor(oldColor);
        if(height <= diam) {
            g.drawArc(x0, y0, diam, diam, -90, 180);
        } else {
            int x1 = x0 + diam;
            int yy0 = y - (height - diam) / 2;
            int yy1 = y + (height - diam) / 2;
            g.drawArc(x0, y0, diam, diam, 0, 90);
            g.drawLine(x1, yy0, x1, yy1);
            g.drawArc(x0, y0 + height - diam, diam, diam, -90, 90);
        }
        g.drawLine(xMid, y0, xMid, y0 + height);
        if(drawBubble) {
            g.fillOval(x0 + diam - 4, y - 4, 8, 8);
        }
    }

    static void drawOrLines(ComponentDrawContext context, int rx, int cy,
            int width, int height, int inputs, AbstractGate gate,
            boolean hasBubble) {
        int x0 = rx - width;
        if(hasBubble) {
            rx -= 4;
            width -= 8;
        }
        Graphics g = context.getGraphics();
        // draw state if appropriate
        // ignore lines if in print view
        int dy = (height - 10) / (inputs - 1);
        int r = Math.min(height / 2, width);
        Integer hash = IntegerFactory.create(r << 4 | inputs);
        int[] lens = (int[]) orLenArrays.get(hash);
        if(lens == null) {
            lens = new int[inputs];
            orLenArrays.put(hash, lens);
            int y = cy - height / 2 + 5;
            if(height <= 2 * r) {
                for(int i = 0; i < inputs; i++) {
                    int a = y - cy;
                    lens[i] = (int) (Math.sqrt(r * r - a * a) + 0.5);
                    y += dy;
                }
            } else {
                for(int i = 0; i < inputs; i++) {
                    lens[i] = r;
                }
                int yy0 = cy - height / 2 + r;
                for(int i = 0; y < yy0; i++, y += dy) {
                    int a = y - yy0;
                    lens[i] = (int) (Math.sqrt(r * r - a * a) + 0.5);
                    lens[lens.length - 1 - i] = lens[i];
                }
            }
        }
        boolean printView = context.isPrintView() && gate != null;
        GraphicsUtil.switchToWidth(g, 2);
        int y = cy - height / 2 + 5;
        for(int i = 0; i < inputs; i++, y += dy) {
            if(printView) {
                Location loc = gate.getEndLocation(i + 1);
                if(context.getCircuit().getComponents(loc).size() <= 1) {
                    continue; // don't draw this line
                }
            }
            g.drawLine(x0, y, x0 + lens[i], y);
        }
    }
}
