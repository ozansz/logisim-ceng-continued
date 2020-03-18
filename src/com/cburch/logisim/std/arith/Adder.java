/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.arith;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class Adder extends ManagedComponent implements AttributeListener {
    public static final ComponentFactory factory = new Factory();
    
    static final int PER_DELAY = 1;

    private static final Attribute[] ATTRIBUTES = { Arithmetic.data_attr };
    private static final Icon toolIcon = Icons.getIcon("adder.gif");

    private static final int IN0   = 0;
    private static final int IN1   = 1;
    private static final int OUT   = 2;
    private static final int C_IN  = 3;
    private static final int C_OUT = 4;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "Adder";
        }

        public String getDisplayName() {
            return Strings.get("adderComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES,
                    new Object[] { Arithmetic.data_dflt });
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Adder(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            return Bounds.create(-40, -20, 40, 40);
        }

        //
        // user interface methods
        //
        public void paintIcon(ComponentDrawContext context,
                int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            if(toolIcon != null) {
                toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
            }
        }
    }

    private Adder(Location loc, AttributeSet attrs) {
        super(loc, attrs, 5);

        attrs.addAttributeListener(this);
        setPins();
    }

    private void setPins() {
        BitWidth data = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        Location pt = getLocation();
        setEnd(IN0,   pt.translate(-40, -10), data, EndData.INPUT_ONLY);
        setEnd(IN1,   pt.translate(-40,  10), data, EndData.INPUT_ONLY);
        setEnd(OUT,   pt                    , data, EndData.OUTPUT_ONLY);
        setEnd(C_IN,  pt.translate(-20, -20), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(C_OUT, pt.translate(-20,  20), BitWidth.ONE, EndData.OUTPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        BitWidth dataWidth = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        // compute outputs
        Value a = state.getValue(getEndLocation(IN0));
        Value b = state.getValue(getEndLocation(IN1));
        Value c_in = state.getValue(getEndLocation(C_IN));
        Value[] outs = Adder.computeSum(dataWidth, a, b, c_in);

        // propagate them
        int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
        state.setValue(getEndLocation(OUT),   outs[0], this, delay);
        state.setValue(getEndLocation(C_OUT), outs[1], this, delay);
    }

    public void attributeListChanged(AttributeEvent e) { }
    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == Arithmetic.data_attr) {
            setPins();
        }
    }
    
    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        context.drawBounds(this);

        g.setColor(Color.GRAY);
        context.drawPin(this, IN0);
        context.drawPin(this, IN1);
        context.drawPin(this, OUT);
        context.drawPin(this, C_IN,  "c in",  Direction.NORTH);
        context.drawPin(this, C_OUT, "c out", Direction.SOUTH);

        Location loc = getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawLine(x - 15, y, x - 5, y);
        g.drawLine(x - 10, y - 5, x - 10, y + 5);
        GraphicsUtil.switchToWidth(g, 1);
    }


    static Value[] computeSum(BitWidth width, Value a, Value b, Value c_in) {
        int w = width.getWidth();
        if(c_in == Value.UNKNOWN || c_in == Value.NIL) c_in = Value.FALSE;
        if(a.isFullyDefined() && b.isFullyDefined() && c_in.isFullyDefined()) {
            if(w >= 32) {
                long mask = (1L << w) - 1;
                long ax = (long) a.toIntValue() & mask;
                long bx = (long) b.toIntValue() & mask;
                long cx = (long) c_in.toIntValue() & mask;
                long sum = ax + bx + cx;
                return new Value[] { Value.createKnown(width, (int) sum),
                    ((sum >> w) & 1) == 0 ? Value.FALSE : Value.TRUE };
            } else {
                int sum = a.toIntValue() + b.toIntValue() + c_in.toIntValue();
                return new Value[] { Value.createKnown(width, sum),
                    ((sum >> w) & 1) == 0 ? Value.FALSE : Value.TRUE };
            }
        } else {
            Value[] bits = new Value[w];
            Value carry = c_in;
            for(int i = 0; i < w; i++) {
                if(carry == Value.ERROR) {
                    bits[i] = Value.ERROR;
                } else if(carry == Value.UNKNOWN) {
                    bits[i] = Value.UNKNOWN;
                } else {
                    Value ab = a.get(i);
                    Value bb = b.get(i);
                    if(ab == Value.ERROR || bb == Value.ERROR) {
                        bits[i] = Value.ERROR;
                        carry = Value.ERROR;
                    } else if(ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
                        bits[i] = Value.UNKNOWN;
                        carry = Value.UNKNOWN;
                    } else {
                        int sum = (ab == Value.TRUE ? 1 : 0)
                            + (bb == Value.TRUE ? 1 : 0)
                            + (carry == Value.TRUE ? 1 : 0);
                        bits[i] = (sum & 1) == 1 ? Value.TRUE : Value.FALSE;
                        carry = (sum >= 2) ? Value.TRUE : Value.FALSE;
                    }
                }
            }
            return new Value[] { Value.create(bits), carry };
        }
    }
}
