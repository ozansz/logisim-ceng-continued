/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

public class Gates extends Library {
    private List tools = null;

    public Gates() {
        tools = Arrays.asList(new Object[] {
            new AddTool(Constant.factory),
            new AddTool(NotGate.factory),
            new AddTool(Buffer.factory),
            new AddTool(AndGate.instance),
            new AddTool(OrGate.instance),
            new AddTool(NandGate.instance),
            new AddTool(NorGate.instance),
            new AddTool(XorGate.instance),
            new AddTool(XnorGate.instance),
            new AddTool(OddParityGate.instance),
            new AddTool(EvenParityGate.instance),
            new AddTool(ControlledBuffer.bufferFactory),
            new AddTool(ControlledBuffer.inverterFactory),
        });
    }

    public String getName() { return "Gates"; }

    public String getDisplayName() { return Strings.get("gatesLibrary"); }

    public List getTools() {
        return tools;
    }
}
