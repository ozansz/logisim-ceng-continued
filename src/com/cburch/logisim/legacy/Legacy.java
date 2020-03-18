/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.legacy;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;


public class Legacy extends Library {
    private List tools = null;

    public Legacy() { }

    public String getName() { return "Legacy"; }

    public String getDisplayName() { return Strings.get("legacyLibrary"); }

    public List getTools() {
        if(tools == null) {
            tools = Arrays.asList(new Object[] {
                new AddTool(DFlipFlop.instance),
                new AddTool(JKFlipFlop.instance),
                new AddTool(Register.instance),
            });
        }
        return tools;
    }
}
