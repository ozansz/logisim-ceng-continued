/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class OrGate extends AbstractGateFactory {
    public static OrGate instance = new OrGate();

    private static final String LABEL = ">0";

    private OrGate() {
        super("OR Gate", Strings.getter("orGateComponent"));
        setRectangularLabel(LABEL);
    }

    public Icon getIconShaped() {
        return Icons.getIcon("orGate.gif");
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("orGateRect.gif");
    }
    public Icon getIconDin40700() {
        return Icons.getIcon("dinOrGate.gif");
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        GraphicsUtil.drawCenteredArc(g, x, y -  5, 22, -90,  53);
        GraphicsUtil.drawCenteredArc(g, x, y + 23, 22,  90, -53);
        GraphicsUtil.drawCenteredArc(g, x - 12, y + 9, 16, -30, 60);
    }
    
    protected void drawInputLines(ComponentDrawContext context,
            AbstractGate comp, int inputs, 
            int x, int yTop, int width, int height) {
        int center_break = (inputs - width / 10) / 2;
        int wing_len = (height - width) / 2;
        
        int dy = 10;
        if(dy * inputs < height) {
            dy = (height - 10) / (inputs - 1);
        }

        CircuitState state = context.getCircuitState();
        Graphics g = context.getGraphics();
        GraphicsUtil.switchToWidth(g, 3);
        int y0 = yTop; 
        int y1 = yTop + (inputs - 1) * dy;
        for(int i = 0; y0 <= y1; i++, y0 += dy, y1 -= dy) {
            int off_dy; // how far vertically this is from arc's center
            int r; // arc's radius
            if(i < center_break) { // on wing
                off_dy = -wing_len / 2 + 10 * i + 5;
                r = wing_len;
            } else { // on main shape
                off_dy = -width / 2 + (i - center_break) * 10 + 5;
                r = width;
            }
            double lenf = Math.sqrt(r * r - off_dy * off_dy)
                        - r * Math.sqrt(3) / 2;
            int len = (int) Math.round(lenf);

            if(len > 1) {
                Location loc0 = comp.getEndLocation(i + 1);
                if(!context.isPrintView() || context.getCircuit().getComponents(loc0).size() > 1) {
                    if(context.getShowState()) {
                        g.setColor(state.getValue(loc0).getColor());
                    }
                    g.drawLine(x, y0, x + len - 1, y0);
                }
                
                if(y0 != y1) {
                    Location loc1 = comp.getEndLocation(inputs - i);                    if(!context.isPrintView() || context.getCircuit().getComponents(loc1).size() > 1) { 
                        if(context.getShowState()) {
                            g.setColor(state.getValue(loc1).getColor());
                        }
                        g.drawLine(x, y1, x + len - 1, y1);
                    }
                }
            }
        }
        g.setColor(Color.black);
    }

    protected void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height) {
        Graphics g = context.getGraphics();
        GraphicsUtil.switchToWidth(g, 2);
        if(width == 30) {
            GraphicsUtil.drawCenteredArc(g, x - 30, y - 21, 36, -90, 53);
            GraphicsUtil.drawCenteredArc(g, x - 30, y + 21, 36, 90, -53);
        } else {
            GraphicsUtil.drawCenteredArc(g, x - 50, y - 37, 62, -90, 53);
            GraphicsUtil.drawCenteredArc(g, x - 50, y + 37, 62, 90, -53);
        }
        drawShield(g, x - width, y, width, height);
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        DinShape.drawOrLines(context, x, y, width, height, inputs, gate, false);
        DinShape.draw(context, x, y, width, height, false, DinShape.OR);
    }

    protected void drawShield(Graphics g, int x, int y,
            int width, int height) {
        GraphicsUtil.switchToWidth(g, 2);
        if(width == 30) {
            GraphicsUtil.drawCenteredArc(g, x - 26, y, 30, -30, 60);
        } else {
            GraphicsUtil.drawCenteredArc(g, x - 43, y, 50, -30, 60);
        }
        if(height > width) {
            int extra = (height - width) / 2;
            int dx = (int) Math.round(extra * (Math.sqrt(3) / 2));
            GraphicsUtil.drawCenteredArc(g,
                x - dx, y - (width + extra) / 2,
                extra, -30, 60);
            GraphicsUtil.drawCenteredArc(g,
                x - dx, y + (width + extra) / 2,
                extra, -30, 60);
        }
    }

    protected Value computeOutput(Value[] inputs, int num_inputs) {
        if(num_inputs == 0) {
            return Value.NIL;
        } else {
            Value ret = inputs[0];
            for(int i = 1; i < num_inputs; i++) {
                ret = ret.or(inputs[i]);
            }
            return ret;
        }
    }

    protected boolean shouldRepairWire(Component comp, WireRepairData data) {
        boolean ret = !data.getPoint().equals(comp.getLocation());
        return ret;
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        Expression ret = inputs[0];
        for(int i = 1; i < numInputs; i++) {
            ret = Expressions.or(ret, inputs[i]);
        }
        return ret;
    }
}
