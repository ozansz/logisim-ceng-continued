/*
 * 2-to-4 Decoder (x2) (74155)
 *
 * /1G
 * 1C
 * A
 * B
 * /2G
 * /2C
 *
 * 1Y0
 * 1Y1
 * 1Y2
 * 1Y3
 *
 * 2Y0
 * 2Y1
 * 2Y2
 * 2Y3
 *
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
class ic74155 extends ic {
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
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Y0"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Y1"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Y2"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1Y3"),
            };
            ICDraw.ICPin[] northPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2Y0"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2Y1"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2Y2"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2Y3"),
            };
            ICDraw.ICPin[] southPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/1E0"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "1E1"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2E0"),
                new ICDraw.ICPin(ICDraw.ICPinType.INVERSEPIN, "/2E1"),
            };

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "74155");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic74155.descriptor,toolIcon);
        }

        public String getName() {
            return "2-to-4 Decoder (x2) (74155)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic74155(loc, attrs);
        }
    }

    private ic74155(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;
        Direction facing = (Direction) getAttributeSet().getValue(ic.facing_attr);

        Location pt = getLocation();

        for(int i = 0; i < 2; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 4; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 4; i++)
            setEnd(getNorthPin(i), getNorthPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 4; i++)
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

        Value a_e0 = state.getValue(getSouthPinLoc(0));
        Value ae1 = state.getValue(getSouthPinLoc(1));
        Value b_e0 = state.getValue(getSouthPinLoc(2));
        Value b_e1 = state.getValue(getSouthPinLoc(3));

        int out = -1;
        boolean enableda;
        boolean enabledb;
        boolean problema = false;
        boolean problemb = false;

        if (a_e0.isFullyDefined() && ae1.isFullyDefined())
            enableda = (a_e0.not().toIntValue() + ae1.toIntValue()) == 2;
        else {
            enableda = false;
            problema = true;
        }

        if (b_e0.isFullyDefined() && b_e1.isFullyDefined())
            enabledb = (b_e0.not().toIntValue() + b_e1.not().toIntValue()) == 2;
        else {
            enabledb = false;
            problemb = true;
        }

        if (s0.isFullyDefined() && s1.isFullyDefined())
            out = (s1.toIntValue() << 1) + s0.toIntValue();
        else {
            problema = true;
            problemb = true;
        }

        if (problema) {
            for (int i = 0; i < 4; i++)
                state.setValue(getEastPinLoc(i), Value.ERROR, this, ic.DELAY);
        }
        else {
            for (int i = 0; i < 4; i++)
                state.setValue(getEastPinLoc(i),
                                    (enableda && out==i) ? Value.FALSE : Value.TRUE,
                                    this,
                                    ic.DELAY);
        }
        
        if (problemb) {
            for (int i = 0; i < 4; i++)
                state.setValue(getNorthPinLoc(i), Value.ERROR, this, ic.DELAY);
        }
        else {
            for (int i = 0; i < 4; i++)
                state.setValue(getNorthPinLoc(i),
                                    (enabledb && out==i) ? Value.FALSE : Value.TRUE,
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
