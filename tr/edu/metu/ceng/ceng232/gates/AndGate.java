/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class AndGate extends AbstractGateFactory {
    public static AndGate instance = new AndGate();

    private static final String LABEL = "&";

    private AndGate() {
        super("AND Gate", Strings.getter("andGateComponent"));
        setRectangularLabel(LABEL);
    }
    
    public Icon getIconShaped() {
        return Icons.getIcon("andGate.gif");
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("andGateRect.gif");
    }
    public Icon getIconDin40700() {
        return Icons.getIcon("dinAndGate.gif");
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        int[] xp = new int[4];
        int[] yp = new int[4];
        xp[0] = x + 10; yp[0] = y + 2;
        xp[1] = x + 2;  yp[1] = y + 2;
        xp[2] = x + 2;  yp[2] = y + 18;
        xp[3] = x + 10; yp[3] = y + 18;
        g.drawPolyline(xp, yp, 4);
        GraphicsUtil.drawCenteredArc(g, x + 10, y + 10, 8, -90, 180);
    }

    protected void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height) {
        Graphics g = context.getGraphics();
        GraphicsUtil.switchToWidth(g, 2);
        int[] xp = new int[4];
        int[] yp = new int[4];
        xp[0] = x - width / 2; yp[0] = y - width / 2;
        xp[1] = x - width + 1; yp[1] = y - width / 2;
        xp[2] = x - width + 1; yp[2] = y + width / 2;
        xp[3] = x - width / 2; yp[3] = y + width / 2;
        GraphicsUtil.drawCenteredArc(g, x - width / 2, y, width / 2, -90, 180);

        g.drawPolyline(xp, yp, 4);
        if(height > width) {
            g.drawLine(x - width + 1, y - height / 2,
                x - width + 1, y + height / 2);
        }
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        DinShape.draw(context, x, y, width, height, false, DinShape.AND);
    }

    protected Value computeOutput(Value[] inputs, int num_inputs) {
        if(num_inputs == 0) {
            return Value.NIL;
        } else {
            Value ret = inputs[0];
            for(int i = 1; i < num_inputs; i++) {
                ret = ret.and(inputs[i]);
            }
            return ret;
        }
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        Expression ret = inputs[0];
        for(int i = 1; i < numInputs; i++) {
            ret = Expressions.and(ret, inputs[i]);
        }
        return ret;
    }

}
