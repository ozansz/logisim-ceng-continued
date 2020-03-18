/*
 * This library is a copy of LOGISIM Gates library, except:
 *     - ParityGates are removed
 *     - All gates are defaulted to bitwidth 1 and number of inputs 2 (with no ability to change)
 *     - All gates are defaulted to narrow
 *
 * Modified by Kerem Hadimli <kerem@ceng.metu.edu.tr>
 * 
 */
/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

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
            new AddTool(ControlledBuffer.bufferFactory),
            new AddTool(ControlledBuffer.inverterFactory),
        });
    }

    public String getName() { return "CENG232 Gates"; }

    public String getDisplayName() { return getName(); }

    public List getTools() {
        return tools;
    }
}
