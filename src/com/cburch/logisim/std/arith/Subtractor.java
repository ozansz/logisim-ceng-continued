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

class Subtractor extends ManagedComponent implements AttributeListener {
    public static final ComponentFactory factory = new Factory();

    private static final Attribute[] ATTRIBUTES = { Arithmetic.data_attr };
    private static final Icon toolIcon = Icons.getIcon("subtractor.gif");

    private static final int IN0   = 0;
    private static final int IN1   = 1;
    private static final int OUT   = 2;
    private static final int B_IN  = 3;
    private static final int B_OUT = 4;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }
        
        public String getName() {
            return "Subtractor";
        }

        public String getDisplayName() {
            return Strings.get("subtractorComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES,
                    new Object[] { Arithmetic.data_dflt });
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Subtractor(loc, attrs);
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

    private Subtractor(Location loc, AttributeSet attrs) {
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
        setEnd(B_IN,  pt.translate(-20, -20), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(B_OUT, pt.translate(-20,  20), BitWidth.ONE, EndData.OUTPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        BitWidth data = (BitWidth) getAttributeSet().getValue(Arithmetic.data_attr);

        // compute outputs
        Value a = state.getValue(getEndLocation(IN0));
        Value b = state.getValue(getEndLocation(IN1));
        Value b_in = state.getValue(getEndLocation(B_IN));
        if(b_in == Value.UNKNOWN || b_in == Value.NIL) b_in = Value.FALSE;
        Value[] outs = Adder.computeSum(data, a, b.not(), b_in.not());

        // propagate them
        int delay = (data.getWidth() + 4) * Adder.PER_DELAY;
        state.setValue(getEndLocation(OUT),   outs[0],       this, delay);
        state.setValue(getEndLocation(B_OUT), outs[1].not(), this, delay);
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
        context.drawPin(this, B_IN,  "b in",  Direction.NORTH);
        context.drawPin(this, B_OUT, "b out", Direction.SOUTH);

        Location loc = getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawLine(x - 15, y, x - 5, y);
        GraphicsUtil.switchToWidth(g, 1);
    }

}
