/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.plexers;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentUserEvent;
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
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringUtil;

class Decoder extends ManagedComponent
        implements AttributeListener, ToolTipMaker {
    public static final ComponentFactory factory = new Factory();

    private static final Attribute[] ATTRIBUTES = {
        Plexers.facing_attr, Plexers.select_attr, Plexers.threeState_attr
    };
    private static final Object[] VALUES = {
        Direction.EAST, Plexers.select_dflt, Plexers.threeState_dflt
    };
    private static final Icon toolIcon = Icons.getIcon("decoder.gif");
    
    private static class Factory extends AbstractComponentFactory {

        private Factory() { }
        
        public String getName() {
            return "Decoder";
        }

        public String getDisplayName() {
            return Strings.get("decoderComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, VALUES);
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Decoder(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(Plexers.facing_attr);
            BitWidth select = (BitWidth) attrs.getValue(Plexers.select_attr);
            int outputs = 1 << select.getWidth();
            if(outputs == 2) {
                boolean reversed = facing == Direction.WEST || facing == Direction.NORTH
                    || facing == Direction.SOUTH;
                int y = reversed ? 0 : -40;
                return Bounds.create(-20, y, 30, 40).rotate(Direction.EAST, facing, 0, 0);
            } else {
                boolean reversed = facing == Direction.NORTH
                    || facing == Direction.SOUTH || facing == Direction.WEST;
                int x = -20;
                int y = reversed ? -10 : -(outputs * 10 + 10);
                return Bounds.create(x, y,
                        40, outputs * 10 + 20).rotate(Direction.EAST, facing, 0, 0);
            }
        }

        //
        // user interface methods
        //
        public void drawGhost(ComponentDrawContext context,
                Color color, int x, int y, AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(Plexers.facing_attr);
            BitWidth select = (BitWidth) attrs.getValue(Plexers.select_attr);
            Graphics g = context.getGraphics();
            g.setColor(color);
            Plexers.drawTrapezoid(g, getOffsetBounds(attrs).translate(x, y),
                    facing.reverse(), select.getWidth() == 1 ? 10 : 20);
        }

        public void paintIcon(ComponentDrawContext context,
                int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            if(toolIcon != null) {
                toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
            }
        }

        public Object getFeature(Object key, AttributeSet attrs) {
            if(key == FACING_ATTRIBUTE_KEY) return Plexers.facing_attr;
            return super.getFeature(key, attrs);
        }
    }

    private Decoder(Location loc, AttributeSet attrs) {
        super(loc, attrs, 4);
        attrs.setReadOnly(Plexers.facing_attr, true);
        attrs.setReadOnly(Plexers.select_attr, true);
        attrs.addAttributeListener(this);
        setPins();
    }

    private void setPins() {
        BitWidth data = BitWidth.ONE;
        Direction facing = (Direction) getAttributeSet().getValue(Plexers.facing_attr);
        BitWidth select = (BitWidth) getAttributeSet().getValue(Plexers.select_attr);
        int outputs = 1 << select.getWidth();

        Location pt = getLocation();
        if(outputs == 2) {
            Location end0;
            Location end1;
            if(facing == Direction.WEST) {
                end0 = pt.translate(-10, -30);
                end1 = pt.translate(-10, -10);
            } else if(facing == Direction.NORTH) {
                end0 = pt.translate(10, -10);
                end1 = pt.translate(30, -10);
            } else if(facing == Direction.SOUTH) {
                end0 = pt.translate(10, 10);
                end1 = pt.translate(30, 10);
            } else {
                end0 = pt.translate(10, -30);
                end1 = pt.translate(10, -10);
            }
            setEnd(0, end0, data, EndData.OUTPUT_ONLY);
            setEnd(1, end1, data, EndData.OUTPUT_ONLY);
        } else {
            int dx = 0;
            int ddx = 10;
            int dy = -outputs * 10;
            int ddy = 10;
            if(facing == Direction.WEST) {
                dx = -20; ddx = 0;
            } else if(facing == Direction.NORTH) {
                dy = -20; ddy = 0;
            } else if(facing == Direction.SOUTH) {
                dy = 20; ddy = 0;
            } else {
                dx = 20; ddx = 0;
            }
            for(int i = 0; i < outputs; i++) {
                setEnd(i, pt.translate(dx, dy), data, EndData.OUTPUT_ONLY);
                dx += ddx;
                dy += ddy;
            }
        }
        setEnd(outputs, pt, select, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        // get attributes
        BitWidth data = BitWidth.ONE;
        BitWidth select = (BitWidth) getAttributeSet().getValue(Plexers.select_attr);
        Boolean threeState = (Boolean) getAttributeSet().getValue(Plexers.threeState_attr);
        int outputs = 1 << select.getWidth();
        Value sel = state.getValue(getEndLocation(outputs));

        // determine output values
        Value others; // the default output
        if(threeState.booleanValue()) {
            others = Value.UNKNOWN;
        } else {
            others = Value.FALSE;
        }
        int outIndex = -1; // the special output
        Value out = null;
        if(sel.isFullyDefined()) {
            outIndex = sel.toIntValue();
            out = Value.TRUE;
        } else if(sel.isErrorValue()) {
            others = Value.createError(data);
        } else {
            others = Value.createUnknown(data);
        }

        // now propagate them
        for(int i = 0; i < outputs; i++) {
            state.setValue(getEndLocation(i), i == outIndex ? out : others,
                this, Plexers.DELAY);
        }
    }

    public void attributeListChanged(AttributeEvent e) { }
    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == Plexers.select_attr) {
            setPins();
        } else if(attr == Plexers.threeState_attr) {
            fireComponentInvalidated(new ComponentEvent(this));
        }
    }
    
    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        Direction facing = (Direction) getAttributeSet().getValue(Plexers.facing_attr);
        BitWidth select = (BitWidth) getAttributeSet().getValue(Plexers.select_attr);
        int outputs = 1 << select.getWidth();

        if(outputs == 2) { // draw select wire
            GraphicsUtil.switchToWidth(g, 3);
            EndData e = (EndData) getEnd(outputs);
            Location pt = e.getLocation();
            if(context.getShowState()) {
                CircuitState state = context.getCircuitState();
                g.setColor(state.getValue(pt).getColor());
            }
            boolean vertical = facing == Direction.NORTH || facing == Direction.SOUTH;
            int dx = vertical ? 3 : 0;
            int dy = vertical ? 0 : -3;
            g.drawLine(pt.getX(), pt.getY(), pt.getX() + dx, pt.getY() + dy);
            GraphicsUtil.switchToWidth(g, 1);
            g.setColor(Color.BLACK);
        }
        Plexers.drawTrapezoid(g, getBounds(), facing.reverse(), outputs == 2 ? 10 : 20);
        Bounds bds = getBounds();
        int x0;
        int y0;
        int halign;
        if(facing == Direction.WEST) {
            x0 = 3;
            y0 = 15;
            halign = GraphicsUtil.H_LEFT;
        } else if(facing == Direction.NORTH) {
            x0 = 10;
            y0 = 15;
            halign = GraphicsUtil.H_CENTER;
        } else if(facing == Direction.SOUTH) {
            x0 = 10;
            y0 = bds.getHeight() - 3;
            halign = GraphicsUtil.H_CENTER;
        } else {
            x0 = bds.getWidth() - 3;
            y0 = 15;
            halign = GraphicsUtil.H_RIGHT;
        }
        GraphicsUtil.drawText(g, "0", bds.getX() + x0, bds.getY() + y0,
                halign, GraphicsUtil.V_BASELINE);
        g.setColor(Color.BLACK);
        GraphicsUtil.drawCenteredText(g, "Decd",
                bds.getX() + bds.getWidth() / 2,
                bds.getY() + bds.getHeight() / 2);
        context.drawPins(this);
    }

    public Object getFeature(Object key) {
        if(key == ToolTipMaker.class) return this;
        return null;
    }
    
    public String getToolTip(ComponentUserEvent e) {
        int end = -1;
        for(int i = getEnds().size() - 1; i >= 0; i--) {
            if(getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10) {
                end = i;
                break;
            }
        }
        if(end < 0) return null;
        
        BitWidth select = (BitWidth) getAttributeSet().getValue(Plexers.select_attr);
        int outputs = 1 << select.getWidth();
        if(end == outputs) {
            return Strings.get("decoderSelectTip");
        } else if(end >= 0 && end < outputs){
            return StringUtil.format(Strings.get("decoderOutTip"), "" + end);
        } else {
            return null;
        }
    }
}

