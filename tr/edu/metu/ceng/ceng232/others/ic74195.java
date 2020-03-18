/*
 * Universal 4 bit shift register with parallel load (74195)
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
public class ic74195 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "P3"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "P2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "P1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "P0"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/PE"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "Q3"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "Q2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "Q1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "Q0"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/MR"),
            };
            ICDraw.ICPin[] northPins = {};
            ICDraw.ICPin[] southPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "J"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "CP"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/K"),
            };

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "74195");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic74195.descriptor,toolIcon);
        }

        public String getName() {
            return "4-bit shift register (74195)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic74195(loc, attrs);
        }
    }

    private ic74195(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;
        Direction facing = (Direction) getAttributeSet().getValue(ic.facing_attr);

        Location pt = getLocation();

        for(int i = 0; i < 5; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 4; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);
        setEnd(getEastPin(4), getEastPinLoc(4), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 3; i++)
            setEnd(getSouthPin(i), getSouthPinLoc(i), data, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        InternalState is = getInternalState(state);

        BitWidth data = BitWidth.ONE;

        Value p3 = state.getValue(getWestPinLoc(0));
        Value p2 = state.getValue(getWestPinLoc(1));
        Value p1 = state.getValue(getWestPinLoc(2));
        Value p0 = state.getValue(getWestPinLoc(3));
        Value _pe = state.getValue(getWestPinLoc(4));

        Value j = state.getValue(getSouthPinLoc(0));
        Value cp = state.getValue(getSouthPinLoc(1));
        Value _k = state.getValue(getSouthPinLoc(2));

        Value _mr = state.getValue(getEastPinLoc(4));

        boolean problem = false;

        if (p0.isFullyDefined() && p1.isFullyDefined() && p2.isFullyDefined() && p3.isFullyDefined() &&
                _pe.isFullyDefined() &&
                cp.isFullyDefined() && j.isFullyDefined() && _k.isFullyDefined()
                && _mr.isFullyDefined() ) {
        }
        else
        {
            state.setValue(getEastPinLoc(3), is.value.get(0), this, ic.DELAY);
            state.setValue(getEastPinLoc(2), is.value.get(1), this, ic.DELAY);
            state.setValue(getEastPinLoc(1), is.value.get(2), this, ic.DELAY);
            state.setValue(getEastPinLoc(0), is.value.get(3), this, ic.DELAY);
            return;
        }

        Value outputs = is.value;
        if (_mr.toIntValue() == 0) {
            Value[] newValues = {
                        Value.FALSE,
                        Value.FALSE,
                        Value.FALSE,
                        Value.FALSE,
                };
            outputs = Value.create(newValues);
        }
        else if (cp.toIntValue() == 1 && is.CP.toIntValue() == 0) {
            if (_pe.toIntValue() == 1) {
                Value newFirst = null;
                if (j.toIntValue() == _k.toIntValue())
                    newFirst = j;
                else if (j.toIntValue() == 1)
                    newFirst = is.value.get(0).not();
                else
                    newFirst = is.value.get(0);

                Value[] newValues = {
                            newFirst,
                            is.value.get(0),
                            is.value.get(1),
                            is.value.get(2),
                    };
                outputs = Value.create(newValues);
            }
            else {
                Value[] newValues = {
                            p0, p1, p2, p3,
                    };
                outputs = Value.create(newValues);
            }
        }
        else
            outputs = is.value;

        state.setValue(getEastPinLoc(3), outputs.get(0), this, ic.DELAY);
        state.setValue(getEastPinLoc(2), outputs.get(1), this, ic.DELAY);
        state.setValue(getEastPinLoc(1), outputs.get(2), this, ic.DELAY);
        state.setValue(getEastPinLoc(0), outputs.get(3), this, ic.DELAY);
        
        state.setData(this, new InternalState(cp, outputs));
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
            state = new InternalState(Value.createUnknown(BitWidth.ONE), Value.createKnown(BitWidth.create(4), 0));
            circuitState.setData(this, state);
        }
        return state;
    }

    public class InternalState implements ComponentState, Cloneable {
        public Value CP;
        public Value value;

        public InternalState(Value cp, Value v) {
            this.CP = cp;
            value = v;
        }

        @Override
        public Object clone() {
            // We can just use what super.clone() returns: The only instance variables are
            // Value objects, which are immutable, so we don't care that both the copy
            // and the copied refer to the same Value objects. If we had mutable instance
            // variables, then of course we would need to clone them.
            try { return super.clone(); }
            catch(CloneNotSupportedException e) { return null; }
        }
    }

    public Value fetchValue(CircuitState state) {
        InternalState is = getInternalState(state);
        return is.value;
    }

    public void modifyValue(CircuitState state, Value outputs) {
        InternalState is = getInternalState(state);

        state.setData(this, new InternalState(is.CP, outputs));

        state.setValue(getEastPinLoc(3), outputs.get(0), this, ic.DELAY);
        state.setValue(getEastPinLoc(2), outputs.get(1), this, ic.DELAY);
        state.setValue(getEastPinLoc(1), outputs.get(2), this, ic.DELAY);
        state.setValue(getEastPinLoc(0), outputs.get(3), this, ic.DELAY);
    }

}
