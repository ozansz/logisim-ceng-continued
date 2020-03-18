/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.arith;

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
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;

class Comparator extends ManagedComponent implements AttributeListener {
    public static final ComponentFactory factory = new Factory();
    
    private static final AttributeOption SIGNED_OPTION
        = new AttributeOption("twosComplement", "twosComplement", Strings.getter("twosComplementOption"));
    private static final AttributeOption UNSIGNED_OPTION
        = new AttributeOption("unsigned", "unsigned", Strings.getter("unsignedOption"));
    private static final Attribute MODE_ATTRIBUTE
        = Attributes.forOption("mode", Strings.getter("comparatorType"),
                new AttributeOption[] { SIGNED_OPTION, UNSIGNED_OPTION });
    
    private static final Attribute[] ATTRIBUTES = { Arithmetic.data_attr, MODE_ATTRIBUTE };
    private static final Icon toolIcon = Icons.getIcon("comparator.gif");

    private static final int IN0   = 0;
    private static final int IN1   = 1;
    private static final int GT    = 2;
    private static final int EQ    = 3;
    private static final int LT    = 4;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "Comparator";
        }

        public String getDisplayName() {
            return Strings.get("comparatorComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES,
                    new Object[] { Arithmetic.data_dflt, SIGNED_OPTION });
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Comparator(loc, attrs);
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

    private Comparator(Location loc, AttributeSet attrs) {
        super(loc, attrs, 5);

        attrs.addAttributeListener(this);
        setPins();
    }

    private void setPins() {
        BitWidth one = BitWidth.ONE;
        BitWidth data = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        Location pt = getLocation();
        setEnd(IN0, pt.translate(-40, -10), data, EndData.INPUT_ONLY);
        setEnd(IN1, pt.translate(-40,  10), data, EndData.INPUT_ONLY);
        setEnd(GT,  pt.translate(  0, -10), one,  EndData.OUTPUT_ONLY);
        setEnd(EQ,  pt,                     one,  EndData.OUTPUT_ONLY);
        setEnd(LT,  pt.translate(  0,  10), one,  EndData.OUTPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        BitWidth dataWidth = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        // compute outputs
        Value gt = Value.FALSE;
        Value eq = Value.TRUE;
        Value lt = Value.FALSE;

        Value a = state.getValue(getEndLocation(IN0));
        Value b = state.getValue(getEndLocation(IN1));
        Value[] ax = a.getAll();
        Value[] bx = b.getAll();
        for(int pos = ax.length - 1; pos >= 0; pos--) {
            Value ab = ax[pos];
            Value bb = bx[pos];
            if(pos == ax.length - 1 && ab != bb) {
                Object mode = getAttributeSet().getValue(MODE_ATTRIBUTE);
                if(mode != UNSIGNED_OPTION) {
                    Value t = ab;
                    ab = bb;
                    bb = t;
                }
            }

            if(ab == Value.ERROR || bb == Value.ERROR) {
                gt = Value.ERROR;
                eq = Value.ERROR;
                lt = Value.ERROR;
                break;
            } else if(ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
                gt = Value.UNKNOWN;
                eq = Value.UNKNOWN;
                lt = Value.UNKNOWN;
                break;
            } else if(ab != bb) {
                eq = Value.FALSE;
                if(ab == Value.TRUE) gt = Value.TRUE;
                else                 lt = Value.TRUE;
                break;
            }
        }

        // propagate them
        int delay = (dataWidth.getWidth() + 2) * Adder.PER_DELAY;
        state.setValue(getEndLocation(GT), gt, this, delay);
        state.setValue(getEndLocation(EQ), eq, this, delay);
        state.setValue(getEndLocation(LT), lt, this, delay);
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
        context.drawBounds(this);
        context.drawPin(this, IN0);
        context.drawPin(this, IN1);
        context.drawPin(this, GT, ">", Direction.WEST);
        context.drawPin(this, EQ, "=", Direction.WEST);
        context.drawPin(this, LT, "<", Direction.WEST);
    }
}
