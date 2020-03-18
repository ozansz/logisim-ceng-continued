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

class Divider extends ManagedComponent implements AttributeListener {
    public static final ComponentFactory factory = new Factory();
    
    static final int PER_DELAY = 1;

    private static final Attribute[] ATTRIBUTES = { Arithmetic.data_attr };
    private static final Icon toolIcon = Icons.getIcon("divider.gif");

    private static final int IN0   = 0;
    private static final int IN1   = 1;
    private static final int OUT   = 2;
    private static final int UPPER = 3;
    private static final int REM   = 4;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "Divider";
        }

        public String getDisplayName() {
            return Strings.get("dividerComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES,
                    new Object[] { Arithmetic.data_dflt });
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Divider(loc, attrs);
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

    private Divider(Location loc, AttributeSet attrs) {
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
        setEnd(UPPER, pt.translate(-20, -20), data, EndData.INPUT_ONLY);
        setEnd(REM,   pt.translate(-20,  20), data, EndData.OUTPUT_ONLY);
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
        Value upper = state.getValue(getEndLocation(UPPER));
        Value[] outs = Divider.computeResult(dataWidth, a, b, upper);

        // propagate them
        int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2) * PER_DELAY;
        state.setValue(getEndLocation(OUT), outs[0], this, delay);
        state.setValue(getEndLocation(REM), outs[1], this, delay);
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
        context.drawPin(this, UPPER, Strings.get("dividerUpperInput"),  Direction.NORTH);
        context.drawPin(this, REM, Strings.get("dividerRemainderOutput"), Direction.SOUTH);

        Location loc = getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.fillOval(x - 12, y - 7, 4, 4);
        g.drawLine(x - 15, y, x - 5, y);
        g.fillOval(x - 12, y + 3, 4, 4);
        GraphicsUtil.switchToWidth(g, 1);
    }

    static Value[] computeResult(BitWidth width, Value a, Value b, Value upper) {
        int w = width.getWidth();
        if(upper == Value.NIL || upper.isUnknown()) upper = Value.createKnown(width, 0);
        if(a.isFullyDefined() && b.isFullyDefined() && upper.isFullyDefined()) {
            long num = ((long) upper.toIntValue() << w)
                | ((long) a.toIntValue() & 0xFFFFFFFFL);
            long den = (long) b.toIntValue() & 0xFFFFFFFFL;
            if(den == 0) den = 1;
            long result = num / den;
            long rem = num % den;
            if(rem < 0) {
                if(den >= 0) {
                    rem += den;
                    result--;
                } else {
                    rem -= den;
                    result++;
                }
            }
            return new Value[] { Value.createKnown(width, (int) result),
                    Value.createKnown(width, (int) rem) };
        } else if(a.isErrorValue() || b.isErrorValue() || upper.isErrorValue()) {
            return new Value[] { Value.createError(width), Value.createError(width) };
        } else {
            return new Value[] { Value.createUnknown(width), Value.createUnknown(width) };
        }
    }
}
