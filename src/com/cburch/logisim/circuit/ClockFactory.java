/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JTextField;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.IntegerFactory;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class ClockFactory extends AbstractComponentFactory {
    public static ClockFactory instance = new ClockFactory();

    public static final Attribute high_attr
        = new FrequencyAttribute("highDuration", Strings.getter("clockHighAttr"));

    public static final Attribute low_attr
        = new FrequencyAttribute("lowDuration", Strings.getter("clockLowAttr"));
    
    private static final Attribute[] ATTRIBUTES = {
        Pin.facing_attr, high_attr, low_attr,
        Pin.label_attr, Pin.labelloc_attr, Pin.labelfont_attr
    };
    private static final Object[] DEFAULTS = {
        Direction.EAST, IntegerFactory.ONE, IntegerFactory.ONE,
        "", Direction.WEST, PinAttributes.labelfont_dflt
    };
    private static final Icon toolIcon = Icons.getIcon("clock.gif");

    private static class FrequencyAttribute extends Attribute {
        private FrequencyAttribute(String name, StringGetter disp) {
            super(name, disp);
        }

        public Object parse(String value) {
            try {
                Integer ret = Integer.valueOf(value);
                if(ret.intValue() <= 0) {
                    throw new NumberFormatException(Strings.get("freqNegativeMessage"));
                }
                return ret;
            } catch(NumberFormatException e) {
                throw new NumberFormatException(Strings.get("freqInvalidMessage"));
            }
        }

        public String toDisplayString(Object value) {
            if(value.equals(IntegerFactory.ONE)) {
                return Strings.get("clockDurationOneValue");
            } else {
                return StringUtil.format(Strings.get("clockDurationValue"),
                        value.toString());
            }
        }

        public java.awt.Component getCellEditor(Object value) {
            JTextField field = new JTextField();
            field.setText(value.toString());
            return field;
        }
    }

    public ClockFactory() { }

    public String getName() { return "Clock"; }

    public String getDisplayName() { return Strings.get("clockComponent"); }

    public AttributeSet createAttributeSet() {
        return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new Clock(loc, attrs);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return ProbeFactory.getOffsetBounds(
                (Direction) attrs.getValue(Pin.facing_attr),
                BitWidth.ONE, RadixOption.RADIX_2);
    }

    //
    // user interface methods
    //
    public void paintIcon(ComponentDrawContext c,
            int x, int y, AttributeSet attrs) {
        Graphics g = c.getGraphics();
        if(toolIcon != null) {
            toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
        } else {
            g.drawRect(x + 4, y + 4, 13, 13);
            g.setColor(Value.FALSE.getColor());
            g.drawPolyline(new int[] { x + 6, x + 6, x + 10, x + 10, x + 14, x + 14 },
                    new int[] { y + 10, y + 6, y + 6, y + 14, y + 14, y + 10 },
                    6);
        }

        Direction dir = (Direction) attrs.getValue(Pin.facing_attr);
        int pinx = x + 15; int piny = y + 8;
        if(dir == Direction.EAST) { // keep defaults
        } else if(dir == Direction.WEST) { pinx = x + 3;
        } else if(dir == Direction.NORTH) { pinx = x + 8; piny = y + 3;
        } else if(dir == Direction.SOUTH) { pinx = x + 8; piny = y + 15;
        }
        g.setColor(Value.TRUE.getColor());
        g.fillOval(pinx, piny, 3, 3);
    }
    
    public Object getFeature(Object key, AttributeSet attrs) {
        if(key == FACING_ATTRIBUTE_KEY) return Pin.facing_attr;
        return super.getFeature(key, attrs);
    }

}
