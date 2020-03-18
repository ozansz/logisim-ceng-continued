/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.io;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

public class Io extends Library {
    static final AttributeOption LABEL_CENTER = new AttributeOption("center", "center", Strings.getter("ioLabelCenter"));
    static final Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);

    static final Attribute ATTR_FACING = Attributes.forDirection("facing", Strings.getter("ioFacingAttr"));
    static final Attribute ATTR_COLOR = Attributes.forColor("color", Strings.getter("ioColorAttr"));
    static final Attribute ATTR_LABEL = Attributes.forString("label", Strings.getter("ioLabelAttr"));
    static final Attribute ATTR_LABEL_LOC = Attributes.forOption("labelloc", Strings.getter("ioLabelLocAttr"),
            new Object[] { LABEL_CENTER, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST });
    static final Attribute ATTR_LABEL_FONT = Attributes.forFont("labelfont", Strings.getter("ioLabelFontAttr"));
    static final Attribute ATTR_LABEL_COLOR = Attributes.forColor("labelcolor", Strings.getter("ioLabelColorAttr"));

    private List tools = null;

    public Io() { }

    public String getName() { return "I/O"; }

    public String getDisplayName() { return Strings.get("ioLibrary"); }

    public List getTools() {
        if(tools == null) {
            tools = Arrays.asList(new Object[] {
                    new AddTool(Button.factory),
                    new AddTool(Led.factory),
                    new AddTool(SevenSegment.factory),
            });
        }
        return tools;
    }
}
