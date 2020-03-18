/*
 * DUAL J-K FLIP FLOP WITH PRESET AND CLEAR (74112)
 */

package tr.edu.metu.ceng.ceng232.others;

import java.awt.Color;
import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;


/**
 *
 * @author kerem
 */
class ic74112 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "/1R"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1J"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1CLK"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1K"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "/1S"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Q"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "/2R"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2J"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2CLK"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2K"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "/2S"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2Q"),
            };
            ICDraw.ICPin[] northPins = {};
            ICDraw.ICPin[] southPins = {};

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "74112");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic74112.descriptor,toolIcon);
        }

        public String getName() {
            return "Dual J-K Flip Flop (74112)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic74112(loc, attrs);
        }
    }

    private ic74112(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;

        Location pt = getLocation();

        for(int i = 0; i < 5; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);
        for(int i = 5; i < 7; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 5; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.INPUT_ONLY);
        for(int i = 5; i < 7; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        InternalState is = getInternalState(state);

        BitWidth data = BitWidth.ONE;

        Value s1    = state.getValue(getWestPinLoc(0));
        Value j1    = state.getValue(getWestPinLoc(1));
        Value clk1  = state.getValue(getWestPinLoc(2));
        Value k1    = state.getValue(getWestPinLoc(3));
        Value r1    = state.getValue(getWestPinLoc(4));

        Value s2    = state.getValue(getEastPinLoc(0));
        Value j2    = state.getValue(getEastPinLoc(1));
        Value clk2  = state.getValue(getEastPinLoc(2));
        Value k2    = state.getValue(getEastPinLoc(3));
        Value r2    = state.getValue(getEastPinLoc(4));

        Value q1 = is.Q1;

        if (s1.isFullyDefined() && j1.isFullyDefined() &&
                clk1.isFullyDefined() &&
                k1.isFullyDefined() && r1.isFullyDefined() ) {

            if (s1.toIntValue() == 0 && r1.toIntValue() == 1)
                q1 = Value.FALSE;
            else if (s1.toIntValue() == 1 && r1.toIntValue() == 0)
                q1 = Value.TRUE;
            else if (s1.toIntValue() == 0 && r1.toIntValue() == 0)
                q1 = Value.ERROR; // Show as: Q1 High /Q1 High
            else if (is.CLK1.toIntValue() == 1 && clk1.toIntValue() == 0) {
                if (j1.toIntValue() == 1 && k1.toIntValue() == 0)
                    q1 = Value.TRUE;
                else if (j1.toIntValue() == 0 && k1.toIntValue() == 1)
                    q1 = Value.FALSE;
                else if (j1.toIntValue() == 1 && k1.toIntValue() == 1)
                    q1 = q1.not();
            }
        }

        if (q1.isErrorValue()) {
            state.setValue(getWestPinLoc(5), Value.TRUE, this, ic.DELAY);
            state.setValue(getWestPinLoc(6), Value.TRUE, this, ic.DELAY);
        }
        else {
            state.setValue(getWestPinLoc(5), q1, this, ic.DELAY);
            state.setValue(getWestPinLoc(6), q1.not(), this, ic.DELAY);
        }

        Value q2 = is.Q2;

        if (s2.isFullyDefined() && j2.isFullyDefined() &&
                clk2.isFullyDefined() &&
                k2.isFullyDefined() && r2.isFullyDefined() ) {

            if (s2.toIntValue() == 0 && r2.toIntValue() == 1)
                q2 = Value.FALSE;
            else if (s2.toIntValue() == 1 && r2.toIntValue() == 0)
                q2 = Value.TRUE;
            else if (s2.toIntValue() == 0 && r2.toIntValue() == 0)
                q2 = Value.ERROR; // Show as: Q2 High /Q2 High
            else if (is.CLK2.toIntValue() == 1 && clk2.toIntValue() == 0) {
                if (j2.toIntValue() == 1 && k2.toIntValue() == 0)
                    q2 = Value.TRUE;
                else if (j2.toIntValue() == 0 && k2.toIntValue() == 1)
                    q2 = Value.FALSE;
                else if (j2.toIntValue() == 1 && k2.toIntValue() == 1)
                    q2 = q2.not();
            }

        }

        if (q2.isErrorValue()) {
            state.setValue(getEastPinLoc(5), Value.TRUE, this, ic.DELAY);
            state.setValue(getEastPinLoc(6), Value.TRUE, this, ic.DELAY);
        }
        else {
            state.setValue(getEastPinLoc(5), q2, this, ic.DELAY);
            state.setValue(getEastPinLoc(6), q2.not(), this, ic.DELAY);
        }

        state.setData(this, new InternalState(q1, clk1, q2, clk2));
    }

    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        factory.drawGhost(context, Color.BLACK, getLocation().getX(), getLocation().getY(), getAttributeSet());
        context.drawPins(this);
    }

    protected InternalState getInternalState(CircuitState circuitState) {
        InternalState state = (InternalState) circuitState.getData(this);
        if(state == null) {
            state = new InternalState(Value.FALSE, Value.UNKNOWN, Value.FALSE, Value.UNKNOWN);
            circuitState.setData(this, state);
        }
        return state;
    }

    public class InternalState implements ComponentState, Cloneable {
        public Value Q1;
        public Value CLK1;
        public Value Q2;
        public Value CLK2;

        public InternalState(Value q1, Value clk1, Value q2, Value clk2) {
            Q1 = q1;
            CLK1 = clk1;
            Q2 = q2;
            CLK2 = clk2;
        }

        public Object clone() {
            // We can just use what super.clone() returns: The only instance variables are
            // Value objects, which are immutable, so we don't care that both the copy
            // and the copied refer to the same Value objects. If we had mutable instance
            // variables, then of course we would need to clone them.
            try { return super.clone(); }
            catch(CloneNotSupportedException e) { return null; }
        }
    }

}
