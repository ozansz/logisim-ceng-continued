/*
 * Dual D flip flop (7474)
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
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;


/**
 *
 * @author kerem
 */
class ic7474 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1CLR"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1D"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1CLK"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1PRE"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Q"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2CLR"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2D"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2CLK"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2PRE"),
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
                "7474");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic7474.descriptor,toolIcon);
        }

        public String getName() {
            return "Dual D Flip Flop (7474)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic7474(loc, attrs);
        }
    }

    private ic7474(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;

        Location pt = getLocation();

        for(int i = 0; i < 4; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);
        for(int i = 4; i < 6; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 4; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.INPUT_ONLY);
        for(int i = 4; i < 6; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);

    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        InternalState is = getInternalState(state);

        BitWidth data = BitWidth.ONE;

        Value _clr1 = state.getValue(getWestPinLoc(0));
        Value d1    = state.getValue(getWestPinLoc(1));
        Value clk1  = state.getValue(getWestPinLoc(2));
        Value _pre1 = state.getValue(getWestPinLoc(3));

        Value _clr2 = state.getValue(getEastPinLoc(0));
        Value d2    = state.getValue(getEastPinLoc(1));
        Value clk2  = state.getValue(getEastPinLoc(2));
        Value _pre2 = state.getValue(getEastPinLoc(3));

        Value q1 = is.Q1;

        if (_clr1.isFullyDefined() && d1.isFullyDefined() &&
                clk1.isFullyDefined() && _pre1.isFullyDefined() ) {

            if (_pre1.toIntValue() == 0 && _clr1.toIntValue() == 1)
                q1 = Value.TRUE;
            else if (_pre1.toIntValue() == 1 && _clr1.toIntValue() == 0)
                q1 = Value.FALSE;
            else if (_pre1.toIntValue() == 0 && _clr1.toIntValue() == 0)
                q1 = is.Q1; // undefined
            else if (is.CLK1.toIntValue() == 0 && clk1.toIntValue() == 1)
                q1 = d1;
            else
                q1 = is.Q1;
        }
        state.setValue(getWestPinLoc(4), q1, this, ic.DELAY);
        state.setValue(getWestPinLoc(5), q1.not(), this, ic.DELAY);

        Value q2 = is.Q2;
        if (_clr2.isFullyDefined() && d2.isFullyDefined() &&
                clk2.isFullyDefined() && _pre2.isFullyDefined() ) {

            if (_pre2.toIntValue() == 0 && _clr2.toIntValue() == 1)
                q2 = Value.TRUE;
            else if (_pre2.toIntValue() == 1 && _clr2.toIntValue() == 0)
                q2 = Value.FALSE;
            else if (_pre2.toIntValue() == 0 && _clr2.toIntValue() == 0)
                q2 = is.Q2; // undefined
            else if (is.CLK2.toIntValue() == 0 && clk2.toIntValue() == 1)
                q2 = d2;
            else
                q2 = is.Q2;
        }

        state.setValue(getEastPinLoc(4), q2, this, ic.DELAY);
        state.setValue(getEastPinLoc(5), q2.not(), this, ic.DELAY);

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
