/*
 * (74153)
 * 8-to-2 line noninverting data selector/multiplexer with separate enables.
 */

package tr.edu.metu.ceng.ceng232.others;

import java.awt.Color;
import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
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
class ic74153 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1A0"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1A1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1A2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1A3"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2A0"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2A1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2A2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "2A3"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "1Y"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "2Y"),
            };
            ICDraw.ICPin[] northPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1E"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2E"),
            };
            ICDraw.ICPin[] southPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S0"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S1"),
            };

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "74153");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic74153.descriptor,toolIcon);
        }

        public String getName() {
            return "4-to-1 MUX (x2) (74153)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic74153(loc, attrs);
        }
    }

    private ic74153(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;

        for(int i = 0; i < 8; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 2; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 2; i++)
            setEnd(getNorthPin(i), getNorthPinLoc(i), data, EndData.INPUT_ONLY);
        
        for(int i = 0; i < 2; i++)
            setEnd(getSouthPin(i), getSouthPinLoc(i), data, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        Value[] a = {
            state.getValue(getWestPinLoc(0)),
            state.getValue(getWestPinLoc(1)),
            state.getValue(getWestPinLoc(2)),
            state.getValue(getWestPinLoc(3)),
        };
        Value[] b = {
            state.getValue(getWestPinLoc(4)),
            state.getValue(getWestPinLoc(5)),
            state.getValue(getWestPinLoc(6)),
            state.getValue(getWestPinLoc(7)),
        };

        Value s0 = state.getValue(getSouthPinLoc(0));
        Value s1 = state.getValue(getSouthPinLoc(1));

        Value _ea = state.getValue(getNorthPinLoc(0));
        Value _eb = state.getValue(getNorthPinLoc(1));

        int out = -1;
        boolean enableda = false;
        boolean enabledb = false;
        boolean problema = false;
        boolean problemb = false;

        if (s0.isFullyDefined() && s1.isFullyDefined())
            out = (s1.toIntValue() << 1) + s0.toIntValue();
        else {
            problema = true;
            problemb = true;
        }

        if (_ea.isFullyDefined())
            enableda = (_ea.toIntValue() == 0);
        else {
            enableda = false;
            problema = true;
        }

        if (_eb.isFullyDefined())
            enabledb = (_eb.toIntValue() == 0);
        else {
            enabledb = false;
            problemb = true;
        }

        if (problema)
            state.setValue(getEastPinLoc(0), Value.ERROR, this, ic.DELAY);
        else if (enableda)
            state.setValue(getEastPinLoc(0), a[out], this, ic.DELAY);
        else
            state.setValue(getEastPinLoc(0), Value.FALSE, this, ic.DELAY);

        if (problemb)
            state.setValue(getEastPinLoc(1), Value.ERROR, this, ic.DELAY);
        else if (enabledb)
            state.setValue(getEastPinLoc(1), b[out], this, ic.DELAY);
        else
            state.setValue(getEastPinLoc(1), Value.FALSE, this, ic.DELAY);
    }

    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        factory.drawGhost(context, Color.BLACK, getLocation().getX(), getLocation().getY(), getAttributeSet());
        context.drawPins(this);
    }

}
