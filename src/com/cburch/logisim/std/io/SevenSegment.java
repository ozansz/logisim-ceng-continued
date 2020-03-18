/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.io;

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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.IntegerFactory;

class SevenSegment extends ManagedComponent {
    public static final ComponentFactory factory = new Factory();

    private static final Attribute[] ATTRIBUTES
        = { Io.ATTR_COLOR };
    private static final Object[] DEFAULTS
        = { new Color(240, 0, 0) };
    private static Bounds[] SEGMENTS = null;
    private static Color OFF_COLOR = null;

    private static final Icon toolIcon = Icons.getIcon("7seg.gif");

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "7-Segment Display";
        }

        public String getDisplayName() {
            return Strings.get("sevenSegmentComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new SevenSegment(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            return Bounds.create(-5, 0, 40, 60);
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

    private SevenSegment(Location loc, AttributeSet attrs) {
        super(loc, attrs, 8);
        setPins();
    }

    private void setPins() {
        Location p = getLocation();
        setEnd(0, p.translate(20,  0), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(1, p.translate(30,  0), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(2, p.translate(20, 60), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(3, p.translate(10, 60), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(4, p.translate( 0, 60), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(5, p.translate(10,  0), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(6, p.translate( 0,  0), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(7, p.translate(30, 60), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        int summary = 0;
        for(int i = 0; i < 8; i++) {
            Value val = state.getValue(getEndLocation(i));
            if(val == Value.TRUE) summary |= 1 << i;
        }
        state.setData(this, IntegerFactory.create(summary));
    }
    
    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        if(SEGMENTS == null) {
            OFF_COLOR = new Color(220, 220, 220);
            SEGMENTS = new Bounds[] {
                    Bounds.create( 3,  8, 19,  4),
                    Bounds.create(23, 10,  4, 19),
                    Bounds.create(23, 30,  4, 19),
                    Bounds.create( 3, 47, 19,  4),
                    Bounds.create(-2, 30,  4, 19),
                    Bounds.create(-2, 10,  4, 19),
                    Bounds.create( 3, 28, 19,  4)
            };
        }
        Integer data = (Integer) context.getCircuitState().getData(this);
        int summ = (data == null ? 0 : data.intValue());
        Color color = (Color) getAttributeSet().getValue(Io.ATTR_COLOR);
        Location loc = getLocation();
        int x = loc.getX();
        int y = loc.getY();
        
        Graphics g = context.getGraphics();
        context.drawBounds(this);
        g.setColor(Color.BLACK);
        for(int i = 0; i < 7; i++) {
            Bounds seg = SEGMENTS[i];
            if(context.getShowState()) g.setColor(((summ >> i) & 1) == 1 ? color : OFF_COLOR);
            g.fillRect(x + seg.getX(), y + seg.getY(), seg.getWidth(), seg.getHeight());
        }
        if(context.getShowState()) g.setColor(((summ >> 7) & 1) == 1 ? color : OFF_COLOR);
        g.fillOval(x + 28, y + 48, 5, 5);
        context.drawPins(this);
    }
}
