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

class Multiplier extends ManagedComponent implements AttributeListener {
    public static final ComponentFactory factory = new Factory();
    
    static final int PER_DELAY = 1;

    private static final Attribute[] ATTRIBUTES = { Arithmetic.data_attr };
    private static final Icon toolIcon = Icons.getIcon("multiplier.gif");

    private static final int IN0   = 0;
    private static final int IN1   = 1;
    private static final int OUT   = 2;
    private static final int C_IN  = 3;
    private static final int C_OUT = 4;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "Multiplier";
        }

        public String getDisplayName() {
            return Strings.get("multiplierComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES,
                    new Object[] { Arithmetic.data_dflt });
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Multiplier(loc, attrs);
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

    private Multiplier(Location loc, AttributeSet attrs) {
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
        setEnd(C_IN,  pt.translate(-20, -20), data, EndData.INPUT_ONLY);
        setEnd(C_OUT, pt.translate(-20,  20), data, EndData.OUTPUT_ONLY);
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
        Value[] outs = Multiplier.computeProduct(dataWidth, a, b, c_in);

        // propagate them
        int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2) * PER_DELAY;
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
        g.drawLine(x - 15, y - 5, x - 5, y + 5);
        g.drawLine(x - 15, y + 5, x - 5, y - 5);
        GraphicsUtil.switchToWidth(g, 1);
    }


    static Value[] computeProduct(BitWidth width, Value a, Value b, Value c_in) {
        int w = width.getWidth();
        if(c_in == Value.NIL || c_in.isUnknown()) c_in = Value.createKnown(width, 0);
        if(a.isFullyDefined() && b.isFullyDefined() && c_in.isFullyDefined()) {
            long sum = (long) a.toIntValue() * (long) b.toIntValue()
                + (long) c_in.toIntValue();
            return new Value[] { Value.createKnown(width, (int) sum),
                Value.createKnown(width, (int) (sum >> w)) };
        } else {
            Value[] avals = a.getAll();
            int aOk = findUnknown(avals);
            int aErr = findError(avals);
            int ax = getKnown(avals);
            Value[] bvals = b.getAll();
            int bOk = findUnknown(bvals);
            int bErr = findError(bvals);
            int bx = getKnown(bvals);
            Value[] cvals = c_in.getAll();
            int cOk = findUnknown(cvals);
            int cErr = findError(cvals);
            int cx = getKnown(cvals);
            
            int known = Math.min(Math.min(aOk, bOk), cOk);
            int error = Math.min(Math.min(aErr, bErr), cErr);
            int ret = ax * bx + cx;

            Value[] bits = new Value[w];
            for(int i = 0; i < w; i++) {
                if(i < known) {
                    bits[i] = ((ret & (1 << i)) != 0 ? Value.TRUE : Value.FALSE);
                } else if(i < error) {
                    bits[i] = Value.UNKNOWN;
                } else {
                    bits[i] = Value.ERROR;
                }
            }
            return new Value[] { Value.create(bits),
                    error < w ? Value.createError(width) : Value.createUnknown(width) };
        }
    }
    
    private static int findUnknown(Value[] vals) {
        for(int i = 0; i < vals.length; i++) {
            if(!vals[i].isFullyDefined()) return i;
        }
        return vals.length;
    }
    
    private static int findError(Value[] vals) {
        for(int i = 0; i < vals.length; i++) {
            if(vals[i].isErrorValue()) return i;
        }
        return vals.length;
    }
    
    private static int getKnown(Value[] vals) {
        int ret = 0;
        for(int i = 0; i < vals.length; i++) {
            int val = vals[i].toIntValue();
            if(val < 0) return ret;
            ret |= val << i;
        }
        return ret;
    }
}
