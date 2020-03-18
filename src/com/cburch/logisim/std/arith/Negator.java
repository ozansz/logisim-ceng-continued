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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;

class Negator extends ManagedComponent implements AttributeListener {
    public static final ComponentFactory factory = new Factory();

    private static final Attribute[] ATTRIBUTES = { Arithmetic.data_attr };
    private static final Icon toolIcon = Icons.getIcon("negator.gif");

    private static final int IN    = 0;
    private static final int OUT   = 1;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "Negator";
        }

        public String getDisplayName() {
            return Strings.get("negatorComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES,
                    new Object[] { Arithmetic.data_dflt });
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Negator(loc, attrs);
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

    private Negator(Location loc, AttributeSet attrs) {
        super(loc, attrs, 2);

        attrs.addAttributeListener(this);
        setPins();
    }

    private void setPins() {
        BitWidth data = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        Location pt = getLocation();
        setEnd(IN,    pt.translate(-40, 0), data, EndData.INPUT_ONLY);
        setEnd(OUT,   pt                  , data, EndData.OUTPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        BitWidth dataWidth = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        // compute outputs
        Value in = state.getValue(getEndLocation(IN));
        Value out;
        if(in.isFullyDefined()) {
            out = Value.createKnown(in.getBitWidth(), -in.toIntValue());
        } else {
            Value[] bits = in.getAll();
            Value fill = Value.FALSE;
            int pos = 0;
            while(pos < bits.length) {
                if(bits[pos] == Value.FALSE) {
                    bits[pos] = fill;
                } else if(bits[pos] == Value.TRUE) {
                    if(fill != Value.FALSE) bits[pos] = fill;
                    pos++;
                    break;
                } else if(bits[pos] == Value.ERROR) {
                    fill = Value.ERROR;
                } else {
                    if(fill == Value.FALSE) fill = bits[pos];
                    else bits[pos] = fill;
                }
                pos++;
            }
            while(pos < bits.length) {
                if(bits[pos] == Value.TRUE) {
                    bits[pos] = Value.FALSE;
                } else if(bits[pos] == Value.FALSE) {
                    bits[pos] = Value.TRUE;
                }
                pos++;
            }
            out = Value.create(bits);
        }

        // propagate them
        int delay = (dataWidth.getWidth() + 2) * Adder.PER_DELAY;
        state.setValue(getEndLocation(OUT), out, this, delay);
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
        context.drawPin(this, IN);
        context.drawPin(this, OUT, "-x", Direction.WEST);
    }
}
