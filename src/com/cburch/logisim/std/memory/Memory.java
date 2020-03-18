/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

public class Memory extends Library {
    protected static final int DELAY = 5;

    private List tools = null;

    public Memory() { }

    public String getName() { return "Memory"; }

    public String getDisplayName() { return Strings.get("memoryLibrary"); }

    public List getTools() {
        if(tools == null) {
            tools = Arrays.asList(new Object[] {
                new AddTool(DFlipFlop.instance),
                new AddTool(TFlipFlop.instance),
                new AddTool(JKFlipFlop.instance),
                new AddTool(SRFlipFlop.instance),
                new AddTool(Register.factory),
                new AddTool(RamFactory.INSTANCE),
                new AddTool(RomFactory.INSTANCE),
            });
        }
        return tools;
    }
}
