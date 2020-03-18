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

class XnorGate extends AbstractGateFactory {
    public static XnorGate instance = new XnorGate();

    private XnorGate() {
        super("XNOR Gate", Strings.getter("xnorGateComponent"));
        setHasDongle(true);
        setAdditionalWidth(10);
        setRectangularLabel(XorGate.instance.getRectangularLabel());
    }

    public Icon getIconShaped() {
        return Icons.getIcon("xnorGate.gif");
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("xnorGateRect.gif");
    }
    public Icon getIconDin40700() {
        return Icons.getIcon("dinXnorGate.gif");
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        GraphicsUtil.drawCenteredArc(g, x, y -  5, 22, -90,  53);
        GraphicsUtil.drawCenteredArc(g, x, y + 23, 22,  90, -53);
        GraphicsUtil.drawCenteredArc(g, x - 8, y + 9, 16, -30, 60);
        GraphicsUtil.drawCenteredArc(g, x - 10, y + 9, 16, -30, 60);
        g.drawOval(x + 16, y + 8, 4, 4);
    }

    protected void drawInputLines(ComponentDrawContext context,
            AbstractGate comp, int inputs, int x, int yTop, int width, int height) {
        OrGate.instance.drawInputLines(context, comp, inputs,
                x, yTop, width, height);
    }

    protected void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height) {
        XorGate.instance.drawShape(context, x, y, width, height);
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        DinShape.draw(context, x, y, width, height, false, DinShape.XNOR);
    }

    protected Value computeOutput(Value[] inputs, int num_inputs) {
        return XorGate.instance.computeOutput(inputs, num_inputs).not();
    }

    protected boolean shouldRepairWire(Component comp, WireRepairData data) {
        return !data.getPoint().equals(comp.getLocation());
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        return Expressions.not(XorGate.xorExpression(inputs, numInputs));
    }
}
