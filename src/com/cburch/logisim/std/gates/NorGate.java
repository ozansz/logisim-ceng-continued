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

class NorGate extends AbstractGateFactory {
    public static NorGate instance = new NorGate();

    private NorGate() {
        super("NOR Gate", Strings.getter("norGateComponent"));
        setHasDongle(true);
        setRectangularLabel(OrGate.instance.getRectangularLabel());
    }

    public Icon getIconShaped() {
        return Icons.getIcon("norGate.gif");
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("norGateRect.gif");
    }
    public Icon getIconDin40700() {
        return Icons.getIcon("dinNorGate.gif");
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        GraphicsUtil.drawCenteredArc(g, x, y -  5, 22, -90,  53);
        GraphicsUtil.drawCenteredArc(g, x, y + 23, 22,  90, -53);
        GraphicsUtil.drawCenteredArc(g, x - 12, y + 9, 16, -30, 60);
        g.drawOval(x + 16, y + 8, 4, 4);
    }

    protected void drawInputLines(ComponentDrawContext context,
            AbstractGate comp, int inputs, int x, int y, int width, int height) {
        OrGate.instance.drawInputLines(context, comp, inputs, x, y, width, height);
    }

    protected void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height) {
        OrGate.instance.drawShape(context, x, y, width, height);
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        DinShape.drawOrLines(context, x, y, width, height, inputs, gate, true);
        DinShape.draw(context, x, y, width, height, true, DinShape.OR);
    }

    protected Value computeOutput(Value[] inputs, int num_inputs) {
        return OrGate.instance.computeOutput(inputs, num_inputs).not();
    }

    protected boolean shouldRepairWire(Component comp, WireRepairData data) {
        return !data.getPoint().equals(comp.getLocation());
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        Expression ret = inputs[0];
        for(int i = 1; i < numInputs; i++) {
            ret = Expressions.or(ret, inputs[i]);
        }
        return Expressions.not(ret);
    }
}
