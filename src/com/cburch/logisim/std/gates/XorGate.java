/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class XorGate extends AbstractGateFactory {
    public static XorGate instance = new XorGate();

    private static final String LABEL = "1";

    private XorGate() {
        super("XOR Gate", Strings.getter("xorGateComponent"));
        setRectangularLabel(LABEL);
        setAdditionalWidth(10);
    }

    public Icon getIconShaped() {
        return Icons.getIcon("xorGate.gif");
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("xorGateRect.gif");
    }
    public Icon getIconDin40700() {
        return Icons.getIcon("dinXorGate.gif");
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        GraphicsUtil.drawCenteredArc(g, x + 2, y -  5, 22, -90,  53);
        GraphicsUtil.drawCenteredArc(g, x + 2, y + 23, 22,  90, -53);
        GraphicsUtil.drawCenteredArc(g, x - 10, y + 9, 16, -30, 60);
        GraphicsUtil.drawCenteredArc(g, x - 12, y + 9, 16, -30, 60);
    }

    protected void drawInputLines(ComponentDrawContext context,
            AbstractGate comp, int inputs, int x, int yTop, int width, int height) {
        OrGate.instance.drawInputLines(context, comp, inputs,
                x, yTop, width - 10, height);
    }

    protected void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height) {
        Graphics g = context.getGraphics();
        OrGate.instance.drawShape(context, x, y, width - 10, width - 10);
        OrGate.instance.drawShield(g, x - width, y, width - 10, height);
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        DinShape.draw(context, x, y, width, height, false, DinShape.XOR);
    }

    protected Value computeOutput(Value[] inputs, int num_inputs) {
        if(num_inputs == 0) {
            return Value.NIL;
        } else {
            boolean allUnknown = true;
            for(int i = 0; i < inputs.length; i++) {
                if(!inputs[i].isUnknown()) { allUnknown = false; break; }
            }
            if(allUnknown) return inputs[0];

            Value[] ret = inputs[0].getAll();
            for(int i = 0; i < ret.length; i++) if(ret[i] == Value.UNKNOWN) ret[i] = Value.FALSE;
            boolean[] found = new boolean[ret.length];
            for(int i = 0; i < ret.length; i++) found[i] = (ret[i] == Value.TRUE);
            for(int i = 1; i < num_inputs; i++) {
                Value[] other = inputs[i].getAll();
                for(int j = 0; j < other.length; j++) {
                    if(other[j] == Value.TRUE) {
                        if(ret[j] == Value.TRUE) {
                            ret[j] = Value.FALSE;
                        } else if(ret[j] == Value.FALSE && !found[j]) {
                            found[j] = true;
                            ret[j] = Value.TRUE;
                        }
                    } else if(other[j] == Value.FALSE || other[j] == Value.UNKNOWN) {
                        ; // don't change anything
                    } else { // whether ERROR or UNKNOWN
                        if(ret[j] != Value.ERROR) { // keep error
                            ret[j] = other[j];
                        }
                    }
                }
            }
            return Value.create(ret);
        }
    }

    protected boolean shouldRepairWire(Component comp, WireRepairData data) {
        return !data.getPoint().equals(comp.getLocation());
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        return xorExpression(inputs, numInputs);
    }
    
    protected static Expression xorExpression(Expression[] inputs, int numInputs) {
        if(numInputs > 2) {
            throw new UnsupportedOperationException("XorGate");
        }
        Expression ret = inputs[0];
        for(int i = 1; i < numInputs; i++) {
            ret = Expressions.xor(ret, inputs[i]);
        }
        return ret;
    }
}
