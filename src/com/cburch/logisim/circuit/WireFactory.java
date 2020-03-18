/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Graphics;
import java.awt.Color;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;

class WireFactory extends AbstractComponentFactory {
    public static final WireFactory instance = new WireFactory();

    private WireFactory() { }

    public String getName() { return "Wire"; }

    public String getDisplayName() {
        return Strings.get("wireComponent");
    }

    public AttributeSet createAttributeSet() {
        return Wire.create(Location.create(0, 0), Location.create(100, 0));
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        Object dir = attrs.getValue(Wire.dir_attr);
        int len = ((Integer) attrs.getValue(Wire.len_attr)).intValue();

        if(dir == Wire.horz_value) {
            return Wire.create(loc, loc.translate(len, 0));
        } else {
            return Wire.create(loc, loc.translate(0, len));
        }
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        Object dir = attrs.getValue(Wire.dir_attr);
        int len = ((Integer) attrs.getValue(Wire.len_attr)).intValue();

        if(dir == Wire.horz_value) {
            return Bounds.create(0, -2, len, 5);
        } else {
            return Bounds.create(-2, 0, 5, len);
        }
    }

    //
    // user interface methods
    //
    public void drawGhost(ComponentDrawContext context,
            Color color, int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        Object dir = attrs.getValue(Wire.dir_attr);
        int len = ((Integer) attrs.getValue(Wire.len_attr)).intValue();

        g.setColor(color);
        GraphicsUtil.switchToWidth(g, 3);
        if(dir == Wire.horz_value) {
            g.drawLine(x, y, x + len, y);
        } else {
            g.drawLine(x, y, x, y + len);
        }
    }
}
