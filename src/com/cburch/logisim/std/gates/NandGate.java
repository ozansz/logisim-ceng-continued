/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class NandGate extends AbstractGateFactory {
    public static NandGate instance = new NandGate();

    private NandGate() {
        super("NAND Gate", Strings.getter("nandGateComponent"));
        setHasDongle(true);
        setRectangularLabel(AndGate.instance.getRectangularLabel());
    }

    public Icon getIconShaped() {
        return Icons.getIcon("nandGate.gif");
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("nandGateRect.gif");
    }
    public Icon getIconDin40700() {
        return Icons.getIcon("dinNandGate.gif");
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        int[] xp = new int[4];
        int[] yp = new int[4];
        xp[0] = x + 8; yp[0] = y + 2;
        xp[1] = x;  yp[1] = y + 2;
        xp[2] = x;  yp[2] = y + 18;
        xp[3] = x + 8; yp[3] = y + 18;
        g.drawPolyline(xp, yp, 4);
        GraphicsUtil.drawCenteredArc(g, x + 8, y + 10, 8, -90, 180);
        g.drawOval(x + 16, y + 8, 4, 4);
    }

    protected void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height) {
        AndGate.instance.drawShape(context, x, y, width, height);
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        DinShape.draw(context, x, y, width, height, true, DinShape.AND);
    }

    protected Value computeOutput(Value[] inputs, int num_inputs) {
        return AndGate.instance.computeOutput(inputs, num_inputs).not();
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        Expression ret = inputs[0];
        for(int i = 1; i < numInputs; i++) {
            ret = Expressions.and(ret, inputs[i]);
        }
        return Expressions.not(ret);
    }
}
