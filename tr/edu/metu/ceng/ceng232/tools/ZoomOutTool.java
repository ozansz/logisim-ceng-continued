
package tr.edu.metu.ceng.ceng232.tools;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.MouseEvent;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.Tool;

public class ZoomOutTool extends Tool {

    public ZoomOutTool() { }

    public boolean equals(Object other) {
        return other instanceof ZoomOutTool;
    }

    public int hashCode() {
        return ZoomOutTool.class.hashCode();
    }

    public String getName() { return "Zoom-out Tool"; }

    public String getDisplayName() { return getName(); }

    public String getDescription() { return getName(); }

    public void select(Canvas canvas) {
        double change = 1/1.41421356; // 1/sqrt(2)

        AttributeSet attrs = canvas.getProject().getOptions().getAttributeSet();

        canvas.getProject().doAction(ZoomActions.setZoom(attrs,
                            Options.zoom_attr,
                            (Double) attrs.getValue(Options.zoom_attr) * change
                            ));

        canvas.getProject().setTool(null);
    }

    @Override
    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Graphics g = c.getGraphics();
        g.drawOval(x+1, y+1, 10, 10);
        g.drawLine(x+10, y+10, x+14, y+14);
        g.drawLine(x+10, y+11, x+14, y+15);
        g.drawLine(x+4, y+6, x+8, y+6);
    }
}
