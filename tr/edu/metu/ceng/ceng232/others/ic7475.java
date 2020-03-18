/*
 * Quad Bistable Latch (7475)
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
class ic7475 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1D"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1C,2C"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2D"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "3D"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "3C,4C"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "4D"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "3Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/3Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "4Q"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/4Q"),
            };
            ICDraw.ICPin[] northPins = {};
            ICDraw.ICPin[] southPins = {};

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "7475");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic7475.descriptor,toolIcon);
        }

        public String getName() {
            return "4-bit Latch (7475)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic7475(loc, attrs);
        }
    }

    private ic7475(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;

        Location pt = getLocation();

        for(int i = 0; i < 6; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 8; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);

    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        InternalState is = getInternalState(state);

        BitWidth data = BitWidth.ONE;

        Value d1 = state.getValue(getWestPinLoc(0));
        Value c1 = state.getValue(getWestPinLoc(1));

        Value d2 = state.getValue(getWestPinLoc(2));
        Value c2 = c1;
        
        Value d3 = state.getValue(getWestPinLoc(3));
        Value c3 = state.getValue(getWestPinLoc(4));

        Value d4 = state.getValue(getWestPinLoc(5));
        Value c4 = c3;

        Value q1 = is.Q1;
        Value q2 = is.Q2;
        Value q3 = is.Q3;
        Value q4 = is.Q4;

        if (d1.isFullyDefined() && c1.isFullyDefined()) {
            if (c1.toIntValue() == 1)
                q1 = d1;
        }
        state.setValue(getEastPinLoc(0), q1, this, ic.DELAY);
        state.setValue(getEastPinLoc(1), q1.not(), this, ic.DELAY);

        if (d2.isFullyDefined() && c2.isFullyDefined()) {
            if (c2.toIntValue() == 1)
                q2 = d2;
        }
        state.setValue(getEastPinLoc(2), q2, this, ic.DELAY);
        state.setValue(getEastPinLoc(3), q2.not(), this, ic.DELAY);

        if (d3.isFullyDefined() && c3.isFullyDefined()) {
            if (c3.toIntValue() == 1)
                q3 = d3;
        }
        state.setValue(getEastPinLoc(4), q3, this, ic.DELAY);
        state.setValue(getEastPinLoc(5), q3.not(), this, ic.DELAY);

        if (d4.isFullyDefined() && c4.isFullyDefined()) {
            if (c4.toIntValue() == 1)
                q4 = d4;
        }
        state.setValue(getEastPinLoc(6), q4, this, ic.DELAY);
        state.setValue(getEastPinLoc(7), q4.not(), this, ic.DELAY);

        state.setData(this, new InternalState(q1, q2, q3, q4));
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
            state = new InternalState(Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
            circuitState.setData(this, state);
        }
        return state;
    }

    public class InternalState implements ComponentState, Cloneable {
        public Value Q1;
        public Value Q2;
        public Value Q3;
        public Value Q4;

        public InternalState(Value q1, Value q2, Value q3, Value q4) {
            Q1 = q1;
            Q2 = q2;
            Q3 = q3;
            Q4 = q4;
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
