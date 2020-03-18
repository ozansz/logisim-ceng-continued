
package tr.edu.metu.ceng.ceng232.others;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

public class Others extends Library {
    private List tools = null;

    public Others() {
        tools = Arrays.asList(new Object[] {
            new AddTool(ic74138.factory),
            new AddTool(ic74153.factory),
            new AddTool(ic74155.factory),
            new AddTool(ic7474.factory),
            new AddTool(ic7475.factory),
            new AddTool(ic7483.factory),
            new AddTool(ic7495.factory),
            new AddTool(ic74112.factory),
            new AddTool(ic74195.factory),
        });
    }

    public String getName() { return "CENG232 ICs"; }

    public String getDisplayName() { return "CENG232 ICs"; }

    public List getTools() {
        return tools;
    }
}
