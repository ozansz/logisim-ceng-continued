/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.Font;
import java.awt.Graphics;

import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;

class RamFactory extends AbstractComponentFactory {
    public static ComponentFactory INSTANCE = new RamFactory();
    
    private static Attribute[] ATTRIBUTES = { Mem.ADDR_ATTR, Mem.DATA_ATTR };
    private static Object[] DEFAULTS = { BitWidth.create(8), BitWidth.create(8) };

    private RamFactory() {
    }

    public String getName() {
        return "RAM";
    }

    public String getDisplayName() {
        return Strings.get("ramComponent");
    }

    public AttributeSet createAttributeSet() {
        return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new Ram(loc, attrs);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return Mem.OFFSET_BOUNDS;
    }

    //
    // user interface methods
    //
    public void paintIcon(ComponentDrawContext context, int x, int y,
            AttributeSet attrs) {
        Graphics g = context.getGraphics();
        Font old = g.getFont();
        g.setFont(old.deriveFont(9.0f));
        GraphicsUtil.drawCenteredText(g, "RAM", x + 10, y + 9);
        g.setFont(old);
        g.drawRect(x, y + 4, 19, 12);
        for(int dx = 2; dx < 20; dx += 5) {
            g.drawLine(x + dx, y + 2, x + dx, y + 4);
            g.drawLine(x + dx, y + 16, x + dx, y + 18);
        }
    }
}
