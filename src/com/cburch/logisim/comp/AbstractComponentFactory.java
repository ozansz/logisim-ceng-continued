/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.comp;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

public abstract class AbstractComponentFactory implements ComponentFactory {
    private static final Icon toolIcon = Icons.getIcon("subcirc.gif");

    protected AbstractComponentFactory() { }

    public String toString() { return getName(); }

    public abstract String getName();
    public abstract String getDisplayName();
    public abstract Component createComponent(Location loc, AttributeSet attrs);
    public abstract Bounds getOffsetBounds(AttributeSet attrs);

    public AttributeSet createAttributeSet() {
        return AttributeSets.EMPTY;
    }

    //
    // user interface methods
    //
    public void drawGhost(ComponentDrawContext context, Color color,
                int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        Bounds bds = getOffsetBounds(attrs);
        g.setColor(color);
        GraphicsUtil.switchToWidth(g, 2);
        g.drawRect(x + bds.getX(), y + bds.getY(),
            bds.getWidth(), bds.getHeight());
    }

    public void paintIcon(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        if(toolIcon != null) {
            toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
        } else {
            g.setColor(Color.black);
            g.drawRect(x + 5, y + 2, 11, 17);
            Value[] v = { Value.TRUE, Value.FALSE };
            for(int i = 0; i < 3; i++) {
                g.setColor(v[i % 2].getColor());
                g.fillOval(x + 5 - 1, y + 5 + 5 * i - 1, 3, 3);
                g.setColor(v[(i + 1) % 2].getColor());
                g.fillOval(x + 16 - 1, y + 5 + 5 * i - 1, 3, 3);
            }
        }
    }
    
    public Object getFeature(Object key, AttributeSet attrs) {
        return null;
    }

}
