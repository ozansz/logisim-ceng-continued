/*
7483
4-bit binary full adder with fast carry.

    +----------+
 A4 |1  +--+ 16| B4          S=A+B+CIN
 S3 |2       15| S4
 A3 |3       14| COUT
 B3 |4       13| CIN
VCC |5  7483 12| GND
 S2 |6       11| B1
 B2 |7       10| A1
 A2 |8        9| S1
    +----------+
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
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;


/**
 *
 * @author kerem
 */
class ic7483 extends ic {
    public static final ComponentFactory factory = Factory.getFactory();

    private static Icon toolIcon;
    private static ICDraw.ICDescriptor descriptor;

    private static class Factory extends ICFactory {
        private static Factory instance = null;
        private static Factory getFactory() {
            if (instance!= null)
                return instance;

            ICDraw.ICPin[] westPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "A1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "A2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "A3"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "A4"),
            };
            ICDraw.ICPin[] eastPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S3"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "S4"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "CO"),
            };
            ICDraw.ICPin[] northPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "B1"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "B2"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "B3"),
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "B4"),
            };
            ICDraw.ICPin[] southPins = {
                new ICDraw.ICPin(ICDraw.ICPinType.PIN, "CI"),
            };

            descriptor = new ICDraw.ICDescriptor(
                westPins,
                eastPins,
                northPins,
                southPins,
                "7483");

            toolIcon = Icons.getIcon("decoder.gif");

            return (instance = new Factory());
        }

        private Factory() {
            super(ic7483.descriptor,toolIcon);
        }

        public String getName() {
            return "4 bit full adder (7483)";
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new ic7483(loc, attrs);
        }
    }

    private ic7483(Location loc, AttributeSet attrs) {
        super(loc, attrs, descriptor);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;

        for(int i = 0; i < 4; i++)
            setEnd(getWestPin(i), getWestPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 5; i++)
            setEnd(getEastPin(i), getEastPinLoc(i), data, EndData.OUTPUT_ONLY);

        for(int i = 0; i < 4; i++)
            setEnd(getNorthPin(i), getNorthPinLoc(i), data, EndData.INPUT_ONLY);

        for(int i = 0; i < 1; i++)
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
            state.getValue(getNorthPinLoc(0)),
            state.getValue(getNorthPinLoc(1)),
            state.getValue(getNorthPinLoc(2)),
            state.getValue(getNorthPinLoc(3)),
        };
        Value ci = state.getValue(getSouthPinLoc(0));

        Value[] s = {
            state.getValue(getEastPinLoc(0)),
            state.getValue(getEastPinLoc(1)),
            state.getValue(getEastPinLoc(2)),
            state.getValue(getEastPinLoc(3)),
            state.getValue(getEastPinLoc(4)), // CARRY OUT
        };

        int outVal = 0;
        boolean problem = false;

        if ( a[0].isFullyDefined() && a[1].isFullyDefined() && a[2].isFullyDefined() && a[3].isFullyDefined() &&
                b[0].isFullyDefined() && b[1].isFullyDefined() && b[2].isFullyDefined() && b[3].isFullyDefined() &&
                ci.isFullyDefined() )
            outVal = (a[3].toIntValue() << 3) +
                     (a[2].toIntValue() << 2) +
                     (a[1].toIntValue() << 1) +
                     (a[0].toIntValue() << 0) +
                     (b[3].toIntValue() << 3) +
                     (b[2].toIntValue() << 2) +
                     (b[1].toIntValue() << 1) +
                     (b[0].toIntValue() << 0) +
                     ci.toIntValue();
        else
            problem = true;

        for (int i = 0; i < 5; i++) {
            if (problem)
                state.setValue(getEastPinLoc(i), Value.ERROR, this, ic.DELAY);
            else
                state.setValue(getEastPinLoc(i),
                                ((outVal & (1<<i)) != 0) ? Value.TRUE : Value.FALSE,
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
