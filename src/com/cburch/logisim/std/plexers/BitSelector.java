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
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class BitSelector extends ManagedComponent
        implements AttributeListener, ToolTipMaker {
    public static final ComponentFactory factory = new Factory();

    public static final Attribute GROUP_ATTR
        = Attributes.forBitWidth("group", Strings.getter("bitSelectorGroupAttr"));

    private static final Attribute[] ATTRIBUTES = {
        Plexers.facing_attr, Plexers.data_attr, GROUP_ATTR
    };
    private static final Object[] VALUES = {
        Direction.EAST, BitWidth.create(8), BitWidth.ONE
    };
    private static final Icon toolIcon = Icons.getIcon("bitSelector.gif");

    private static class Factory extends AbstractComponentFactory {

        private Factory() { }

        public String getName() {
            return "BitSelector";
        }

        public String getDisplayName() {
            return Strings.get("bitSelectorComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, VALUES);
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new BitSelector(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(Plexers.facing_attr);
            return Bounds.create(-30, -15, 30, 30).rotate(Direction.EAST, facing, 0, 0);
        }
        
        //
        // user interface methods
        //
        public void drawGhost(ComponentDrawContext context,
                Color color, int x, int y, AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(Plexers.facing_attr);
            Graphics g = context.getGraphics();
            g.setColor(color);
            Plexers.drawTrapezoid(g, getOffsetBounds(attrs).translate(x, y), facing, 9);
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

    private BitSelector(Location loc, AttributeSet attrs) {
        super(loc, attrs, 3);
        attrs.addAttributeListener(this);
        attrs.setReadOnly(Plexers.facing_attr, true);
        setPins();
    }

    private void setPins() {
        Direction facing = (Direction) getAttributeSet().getValue(Plexers.facing_attr);
        BitWidth data = (BitWidth) getAttributeSet().getValue(Plexers.data_attr);
        BitWidth group = (BitWidth) getAttributeSet().getValue(GROUP_ATTR);
        int groups = (data.getWidth() + group.getWidth() - 1) / group.getWidth() - 1;
        int selectBits = 1;
        if(groups > 0) {
            while(groups != 1) { groups >>= 1; selectBits++; }
        }
        BitWidth select = BitWidth.create(selectBits);

        Location outPt = getLocation();
        Location inPt;
        Location selPt;
        if(facing == Direction.WEST) {
            inPt  = outPt.translate(30, 0);
            selPt = outPt.translate(10, 10);
        } else if(facing == Direction.NORTH) {
            inPt  = outPt.translate(  0, 30);
            selPt = outPt.translate(-10, 10);
        } else if(facing == Direction.SOUTH) {
            inPt  = outPt.translate(  0, -30);
            selPt = outPt.translate(-10, -10);
        } else {
            inPt  = outPt.translate(-30, 0);
            selPt = outPt.translate(-10, 10);
        }
        
        setEnd(0, outPt, group,  EndData.OUTPUT_ONLY);
        setEnd(1, inPt,  data,   EndData.INPUT_ONLY);
        setEnd(2, selPt, select, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        Value data = state.getValue(getEndLocation(1));
        Value select = state.getValue(getEndLocation(2));
        BitWidth groupBits = (BitWidth) getAttributeSet().getValue(GROUP_ATTR);
        Value group;
        if(!select.isFullyDefined()) {
            group = Value.createUnknown(groupBits);
        } else {
            int shift = select.toIntValue() * groupBits.getWidth();
            if(shift >= data.getWidth()) {
                group = Value.createKnown(groupBits, 0);
            } else if(groupBits.getWidth() == 1) {
                group = data.get(shift);
            } else {
                Value[] bits = new Value[groupBits.getWidth()];
                for(int i = 0; i < bits.length; i++) {
                    if(shift + i >= data.getWidth()) {
                        bits[i] = Value.FALSE;
                    } else {
                        bits[i] = data.get(shift + i);
                    }
                }
                group = Value.create(bits);
            }
        }
        state.setValue(getEndLocation(0), group,
            this, Plexers.DELAY);
    }

    public void attributeListChanged(AttributeEvent e) { }
    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == Plexers.data_attr) {
            setPins();
        } else if(attr == GROUP_ATTR) {
            setPins();
        }
    }
    
    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        Direction facing = (Direction) getAttributeSet().getValue(Plexers.facing_attr);

        Plexers.drawTrapezoid(g, getBounds(), facing, 9);
        Bounds bds = getBounds();
        g.setColor(Color.BLACK);
        GraphicsUtil.drawCenteredText(g, "Sel",
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
        
        if(end == 0) {
            return Strings.get("bitSelectorOutputTip");
        } else if(end == 1) {
            return Strings.get("bitSelectorDataTip");
        } else if(end == 2){
            return Strings.get("bitSelectorSelectTip");
        } else {
            return null;
        }
    }
}
