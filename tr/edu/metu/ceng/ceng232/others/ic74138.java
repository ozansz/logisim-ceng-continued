/*
 * 3-to-8 Decoder (74138)
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
class ic74138 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S0"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S2"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y0"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y1"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y2"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y3"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y4"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y5"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y6"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/Y7"),
            };
            ICDraw.ICPin[] northPins = {};
            ICDraw.ICPin[] southPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "E0"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "/E1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "/E2"),
            };

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "74138");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic74138.descriptor,toolIcon);
        }

        public String getName() {
            return "3-to-8 decoder (74138)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic74138(loc, attrs);
        }
    }

    private ic74138(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;
        Direction facing = (Direction) getAttributeSet().getValue(ic.facing_attr);

        Location pt = getLocation();

        for(int i = 0; i < 3; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 8; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 3; i++)
            setEnd(getSouthPin(i), getSouthPinLoc(i), data, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        BitWidth data = BitWidth.ONE;
        
        Value s0 = state.getValue(getWestPinLoc(0));
        Value s1 = state.getValue(getWestPinLoc(1));
        Value s2 = state.getValue(getWestPinLoc(2));

        Value e1 = state.getValue(getSouthPinLoc(0));
        Value _e2 = state.getValue(getSouthPinLoc(1));
        Value _e3 = state.getValue(getSouthPinLoc(2));

        int out = -1;
        boolean enabled;
        boolean problem = false;

        if (e1.isFullyDefined() && _e2.isFullyDefined() && _e3.isFullyDefined())
            enabled = (e1.toIntValue() + _e2.not().toIntValue() + _e3.not().toIntValue()) == 3;
        else {
            enabled = false;
            problem = true;
        }

        if (s0.isFullyDefined() && s1.isFullyDefined() && s2.isFullyDefined())
            out = (s2.toIntValue() << 2) + (s1.toIntValue() << 1) + s0.toIntValue();
        else
            problem = true;

        if (problem) {
            for (int i = 0; i < 8; i++)
                state.setValue(getEastPinLoc(i), Value.ERROR, this, ic.DELAY);
        }
        else {
            for (int i = 0; i < 8; i++)
                state.setValue(getEastPinLoc(i),
                                    (enabled && out==i) ? Value.FALSE : Value.TRUE,
                                    this,
                                    ic.DELAY);
        }
    }

    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        factory.drawGhost(context, Color.BLACK, getLocation().getX(), getLocation().getY(), getAttributeSet());
        context.drawPins(this);
    }

}
