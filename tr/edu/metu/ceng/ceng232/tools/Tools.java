
package tr.edu.metu.ceng.ceng232.tools;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.Library;

public class Tools extends Library {
    private List tools = null;

    public Tools() {
        tools = Arrays.asList(new Object[] {
            new ZoomInTool(),
            new ZoomOutTool(),
        });
    }

    public String getName() { return "CENG232 Tools"; }

    public String getDisplayName() { return getName(); }

    public List getTools() {
        return tools;
    }
}
