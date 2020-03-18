/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.circuit.ClockFactory;
import com.cburch.logisim.circuit.PinFactory;
import com.cburch.logisim.circuit.ProbeFactory;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.MenuTool;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.WiringTool;

public class Base extends Library {
    private List tools = null;

    Base() {
        tools = Arrays.asList(new Object[] {
            new PokeTool(),
            new SelectTool(),
            new WiringTool(),
            new TextTool(),
            new MenuTool(),
            new AddTool(SplitterFactory.instance),
            new AddTool(PinFactory.instance),
            new AddTool(ProbeFactory.instance),
            new AddTool(ClockFactory.instance),
            new AddTool(TextClass.instance),
        });
    }

    public String getName() { return "Base"; }

    public String getDisplayName() { return Strings.get("baseLibrary"); }

    public List getTools() {
        return tools;
    }
}
