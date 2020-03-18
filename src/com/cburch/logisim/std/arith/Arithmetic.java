/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.arith;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

public class Arithmetic extends Library {
    public static final Attribute data_attr
        = Attributes.forBitWidth("width", Strings.getter("arithmeticDataWidthAttr"));
    public static final Object data_dflt = BitWidth.create(1);
    
    private List tools = null;

    public Arithmetic() { }

    public String getName() { return "Arithmetic"; }

    public String getDisplayName() { return Strings.get("arithmeticLibrary"); }

    public List getTools() {
        if(tools == null) {
            tools = Arrays.asList(new Object[] {
                new AddTool(Adder.factory),
                new AddTool(Subtractor.factory),
                new AddTool(Multiplier.factory),
                new AddTool(Divider.factory),
                new AddTool(Negator.factory),
                new AddTool(Comparator.factory),
            });
        }
        return tools;
    }
}
