/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;

class EvenParityGate extends AbstractGateFactory {
    public static EvenParityGate instance = new EvenParityGate();

    private static final String LABEL = "2k";

    private EvenParityGate() {
        super("Even Parity", Strings.getter("evenParityComponent"));
        setRectangularLabel(LABEL);
    }

    public Icon getIconShaped() {
        return getIconRectangular();
    }
    public Icon getIconRectangular() {
        return Icons.getIcon("parityEvenGate.gif");
    }
    public Icon getIconDin40700() {
        return getIconRectangular();
    }
    public void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        paintIconRectangular(context, x, y, attrs);
    }

    protected boolean shouldDrawShaped(ComponentDrawContext c) {
        return false;
    }

    protected void drawShape(ComponentDrawContext context, int x, int y,
            int width, int height) {
        drawRectangular(context, x, y, width, height);
    }

    protected void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate) {
        drawRectangular(context, x, y, width, height);
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

            Value[] ret = inputs[0].not().getAll();
            for(int i = 0; i < ret.length; i++) if(ret[i] == Value.UNKNOWN) ret[i] = Value.TRUE;
            for(int i = 1; i < num_inputs; i++) {
                Value[] other = inputs[i].getAll();
                for(int j = 0; j < other.length; j++) {
                    if(other[j] == Value.TRUE) {
                        if(ret[j] == Value.TRUE) {
                            ret[j] = Value.FALSE;
                        } else if(ret[j] == Value.FALSE) {
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
            Value r = Value.create(ret);
            return r;
        }
    }

    protected Expression computeExpression(Expression[] inputs, int numInputs) {
        Expression ret = inputs[0];
        for(int i = 1; i < numInputs; i++) {
            ret = Expressions.xor(ret, inputs[i]);
        }
        return Expressions.not(ret);
    }
}
