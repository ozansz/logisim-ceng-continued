/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tr.edu.metu.ceng.ceng232.others;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Icon;

import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.ToolTipMaker;


/**
 *
 * @author kerem
 */
abstract public class ic extends ManagedComponent implements ToolTipMaker {
    protected static final int DELAY = 3;
    public static final Attribute facing_attr = Attributes.forDirection("Facing");

    private ICDraw.ICDescriptor descriptor;

    private static final Attribute[] ATTRIBUTES = {
        facing_attr
    };
    private static final Object[] VALUES = {
        Direction.EAST
    };

    public ic(Location loc, AttributeSet attrs, ICDraw.ICDescriptor desc) {
        super(loc,attrs, desc.getE()+desc.getN()+desc.getW()+desc.getS());
        attrs.setReadOnly(facing_attr, true);

        descriptor = desc;
    }

    public int getWestPin(int i) {
        return i;
    }
    public int getEastPin(int i) {
        return descriptor.getW()+i;
    }
    public int getNorthPin(int i) {
        return descriptor.getW()+descriptor.getE()+i;
    }
    public int getSouthPin(int i) {
        return descriptor.getW()+descriptor.getE()+descriptor.getN()+i;
    }
    public int getPinW() {
        return descriptor.getPinW();
    }
    public int getPinH() {
        return descriptor.getPinH();
    }

    public Location getPinLoc(Direction dir, int i) {
        Direction facing = (Direction) getAttributeSet().getValue(facing_attr);
        return ICDraw.getPinLoc(descriptor, getLocation().getX(), getLocation().getY(), facing, dir, i);
    }

    public Location getWestPinLoc(int i) {
        return getPinLoc(Direction.WEST, i);
    }

    public Location getEastPinLoc(int i) {
        return getPinLoc(Direction.EAST, i);
    }

    public Location getNorthPinLoc(int i) {
        return getPinLoc(Direction.NORTH, i);
    }

    public Location getSouthPinLoc(int i) {
        return getPinLoc(Direction.SOUTH, i);
    }

    public abstract static class ICFactory extends AbstractComponentFactory {
        private Icon toolIcon;
        private ICDraw.ICDescriptor desc;

        public ICFactory(ICDraw.ICDescriptor desc, Icon toolicon) {
            this.desc = desc;
            toolIcon = toolicon;
        }

        public String getDisplayName() {
            return getName();
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, VALUES);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(facing_attr);
            return ICDraw.getBounds(desc, facing);
        }

        //
        // user interface methods
        //
        public void drawGhost(ComponentDrawContext context,
                Color color, int x, int y, AttributeSet attrs) {

            Direction facing = (Direction) attrs.getValue(facing_attr);
            ICDraw.draw(desc, context, color, x, y, facing);
        }

        public void paintIcon(ComponentDrawContext context,
                int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            if(toolIcon != null) {
                toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
            }
        }

        public Object getFeature(Object key, AttributeSet attrs) {
            if(key == FACING_ATTRIBUTE_KEY) return facing_attr;
            return super.getFeature(key, attrs);
        }
    }

    public Object getFeature(Object key) {
        if(key == ToolTipMaker.class) return this;
        return super.getFeature(key);
    }

    public String getToolTip(ComponentUserEvent e) {
        for (int i = 0; i < descriptor.pinsWest.length; i++)
            if (getEndLocation(getWestPin(i)).manhattanDistanceTo(e.getX(), e.getY()) < 10)
                return descriptor.pinsWest[i].name;
        for (int i = 0; i < descriptor.pinsEast.length; i++)
            if (getEndLocation(getEastPin(i)).manhattanDistanceTo(e.getX(), e.getY()) < 10)
                return descriptor.pinsEast[i].name;
        for (int i = 0; i < descriptor.pinsNorth.length; i++)
            if (getEndLocation(getNorthPin(i)).manhattanDistanceTo(e.getX(), e.getY()) < 10)
                return descriptor.pinsNorth[i].name;
        for (int i = 0; i < descriptor.pinsSouth.length; i++)
            if (getEndLocation(getSouthPin(i)).manhattanDistanceTo(e.getX(), e.getY()) < 10)
                return descriptor.pinsSouth[i].name;
        return null;
    }

}
