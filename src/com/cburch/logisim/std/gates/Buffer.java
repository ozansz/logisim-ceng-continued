/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
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

class Buffer extends ManagedComponent
        implements AttributeListener, ExpressionComputer {
    public static ComponentFactory factory = new Factory();

    private static final Attribute[] ATTRIBUTES
        = { GateAttributes.facing_attr, GateAttributes.width_attr };
    
    private static final Object[] DEFAULTS = { Direction.EAST, BitWidth.ONE };
    private static final Icon toolIcon = Icons.getIcon("bufferGate.gif");

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() { return "Buffer"; }

        public String getDisplayName() { return Strings.get("bufferComponent"); }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Buffer(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(GateAttributes.facing_attr);
            if(facing == Direction.SOUTH) return Bounds.create(-9, -20, 18, 20);
            if(facing == Direction.NORTH) return Bounds.create(-9, 0, 18, 20);
            if(facing == Direction.WEST) return Bounds.create(0, -9, 20, 18);
            return Bounds.create(-20, -9, 20, 18);
        }
        
        //
        // user interface methods
        //
        public void drawGhost(ComponentDrawContext context,
                Color color, int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            g.setColor(color);
            drawBase(g, attrs, x, y);
        }

        public void paintIcon(ComponentDrawContext context,
                int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            g.setColor(Color.black);
            if(toolIcon != null) {
                toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
            } else {
                g.setColor(Color.black);
                int[] xp = new int[4];
                int[] yp = new int[4];
                xp[0] = x + 17; yp[0] = y + 10;
                xp[1] = x +  3; yp[1] = y +  3;
                xp[2] = x +  3; yp[2] = y + 17;
                xp[3] = x + 17; yp[3] = y + 10;
                g.drawPolyline(xp, yp, 4);
            }
        }

        public Object getFeature(Object key, AttributeSet attrs) {
            if(key == FACING_ATTRIBUTE_KEY) return GateAttributes.facing_attr;
            return super.getFeature(key, attrs);
        }
    }

    public Buffer(Location loc, AttributeSet attrs) {
        super(loc, attrs, 2);
        attrs.addAttributeListener(this);
        attrs.setReadOnly(GateAttributes.facing_attr, true);
        setPins();
    }

    private void setPins() {
        AttributeSet attrs = getAttributeSet();
        Direction dir = (Direction) attrs.getValue(GateAttributes.facing_attr);
        BitWidth w = (BitWidth) attrs.getValue(GateAttributes.width_attr);
        Location loc0 = getLocation();
        Location loc1 = loc0.translate(dir.reverse(), 20);
        
        setEnd(0, loc0, w, EndData.OUTPUT_ONLY);
        setEnd(1, loc1, w, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        Value in = state.getValue(getEndLocation(1));
        state.setValue(getEndLocation(0), in, this, GateAttributes.DELAY);
    }

    public void attributeListChanged(AttributeEvent e) { }
    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == GateAttributes.width_attr) {
            setPins();
        }
    }
    
    //
    // user interface methods
    //
    public void draw(ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        Location loc = getLocation();
        int x = loc.getX();
        int y = loc.getY();

        // draw gate
        g.setColor(Color.BLACK);
        drawBase(g, getAttributeSet(), x, y);
        context.drawPins(this);
    }
    
    public Object getFeature(Object key) {
        if(key == ExpressionComputer.class) return this;
        return super.getFeature(key);
    }

    public void computeExpression(Map expressionMap) {
        Expression e = (Expression) expressionMap.get(getEndLocation(1));
        if(e != null) {
            expressionMap.put(getEndLocation(0), e);
        }
    }

    private static void drawBase(Graphics oldG, AttributeSet attrs,
            int x, int y) {
        Direction facing = (Direction) attrs.getValue(GateAttributes.facing_attr);
        Graphics g = oldG;
        if(facing != Direction.EAST && oldG instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(-facing.toRadians(), x, y);
            g = g2;
        }

        GraphicsUtil.switchToWidth(g, 2);
        int[] xp = new int[4];
        int[] yp = new int[4];
        xp[0] = x;      yp[0] = y;
        xp[1] = x - 19; yp[1] = y - 7;
        xp[2] = x - 19; yp[2] = y + 7;
        xp[3] = x;      yp[3] = y;
        g.drawPolyline(xp, yp, 4);
    }
}
