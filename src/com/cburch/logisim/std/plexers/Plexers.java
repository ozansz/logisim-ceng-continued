/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.plexers;

import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.GraphicsUtil;

public class Plexers extends Library {
    
    public static final Attribute facing_attr
        = Attributes.forDirection("facing", Strings.getter("plexerFacingAttr"));

    public static final Attribute data_attr
        = Attributes.forBitWidth("width", Strings.getter("plexerDataWidthAttr"));
    public static final Object data_dflt = BitWidth.create(1);

    public static final Attribute select_attr
        = Attributes.forBitWidth("select", Strings.getter("plexerSelectBitsAttr"), 1, 4);
    public static final Object select_dflt = BitWidth.create(1);

    public static final Attribute threeState_attr
        = Attributes.forBoolean("tristate", Strings.getter("plexerThreeStateAttr"));
    public static final Object threeState_dflt = Boolean.FALSE;

    protected static final int DELAY = 3;

    private List tools = null;

    public Plexers() { }

    public String getName() { return "Plexers"; }

    public String getDisplayName() { return Strings.get("plexerLibrary"); }

    public List getTools() {
        if(tools == null) {
            tools = Arrays.asList(new Object[] {
                new AddTool(Multiplexer.factory),
                new AddTool(Demultiplexer.factory),
                new AddTool(Decoder.factory),
                new AddTool(BitSelector.factory),
            });
        }
        return tools;
    }

    static void drawTrapezoid(Graphics g, Bounds bds, Direction facing,
            int facingLean) {
        int wid = bds.getWidth();
        int ht = bds.getHeight();
        int x0 = bds.getX(); int x1 = x0 + wid;
        int y0 = bds.getY(); int y1 = y0 + ht;
        int[] xp = { x0, x1, x1, x0 };
        int[] yp = { y0, y0, y1, y1 };
        if(facing == Direction.WEST) {
            yp[0] += facingLean; yp[3] -= facingLean;
        } else if(facing == Direction.NORTH) {
            xp[0] += facingLean; xp[1] -= facingLean;
        } else if(facing == Direction.SOUTH) {
            xp[2] -= facingLean; xp[3] += facingLean;
        } else {
            yp[1] += facingLean; yp[2] -= facingLean;
        }
        GraphicsUtil.switchToWidth(g, 2);
        g.drawPolygon(xp, yp, 4);
    }
}
