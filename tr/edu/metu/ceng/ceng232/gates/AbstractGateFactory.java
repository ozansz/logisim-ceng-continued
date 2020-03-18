/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

abstract class AbstractGateFactory extends AbstractComponentFactory {
    private static final int ATTEMPT_SHAPED = 1;
    private static final int ATTEMPT_RECTANGULAR = 2;
    private static final int ATTEMPT_DIN40700 = 4;
    
    private String name;
    private StringGetter desc;
    private int iconAttempts = 0;
    private Icon iconShaped = null;
    private Icon iconRect = null;
    private Icon iconDin = null;
    int bonus_width = 0;
    boolean has_dongle = false;
    String rect_label = "";

    protected AbstractGateFactory(String name, StringGetter desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() { return name; }

    public String getDisplayName() { return desc.get(); }

    public AttributeSet createAttributeSet() {
        return new GateAttributes();
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new AbstractGate(loc, attrs, this);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return computeBounds((GateAttributes) attrs);
    }

    //
    // user interface methods
    //
    public void drawGhost(ComponentDrawContext context,
            Color color, int x, int y, AttributeSet baseAttrs) {
        GateAttributes attrs = (GateAttributes) baseAttrs;
        Bounds bounds = computeBounds(attrs);
        context.getGraphics().setColor(color);
        AbstractGate.drawBase(context, this, null, attrs, x, y,
                bounds.getWidth(), bounds.getHeight());
    }
    
    public Object getFeature(Object key, AttributeSet attrs) {
        if(key == FACING_ATTRIBUTE_KEY) return GateAttributes.facing_attr;
        return super.getFeature(key, attrs);
    }
    
    public abstract Icon getIconShaped();
    public abstract Icon getIconRectangular();
    public abstract Icon getIconDin40700();
    public abstract void paintIconShaped(ComponentDrawContext context,
            int x, int y, AttributeSet attrs);
    public void paintIconRectangular(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        g.drawRect(x + 1, y + 2, 16, 16);
        if(has_dongle) g.drawOval(x + 16, y + 8, 4, 4);
        GraphicsUtil.drawCenteredText(g, rect_label, x + 9, y + 8);
    }

    public final void paintIcon(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        g.setColor(Color.black);
        if(context.getGateShape() == LogisimPreferences.SHAPE_RECTANGULAR) {
            if(iconRect == null && (iconAttempts & ATTEMPT_RECTANGULAR) == 0) {
                iconRect = getIconRectangular();
                iconAttempts |= ATTEMPT_RECTANGULAR;
            }
            if(iconRect != null) {
                iconRect.paintIcon(context.getDestination(), g, x + 2, y + 2);
            } else {
                paintIconRectangular(context, x, y, attrs);
            }
        } else if(context.getGateShape() == LogisimPreferences.SHAPE_DIN40700) {
            if(iconDin == null && (iconAttempts & ATTEMPT_DIN40700) == 0) {
                iconDin = getIconDin40700();
                iconAttempts |= ATTEMPT_DIN40700;
            }
            if(iconDin != null) {
                iconDin.paintIcon(context.getDestination(), g, x + 2, y + 2);
            } else {
                paintIconRectangular(context, x, y, attrs);
            }
        } else {
            if(iconShaped == null && (iconAttempts & ATTEMPT_SHAPED) == 0) {
                iconShaped = getIconShaped();
                iconAttempts |= ATTEMPT_SHAPED;
            }
            if(iconShaped != null) {
                iconShaped.paintIcon(context.getDestination(), g, x + 2, y + 2);
            } else {
                paintIconShaped(context, x, y, attrs);
            }
        }
    }

    protected void setAdditionalWidth(int value) {
        bonus_width = value;
    }

    protected void setHasDongle(boolean value) {
        has_dongle = value;
    }

    protected void setRectangularLabel(String value) {
        rect_label = value;
    }

    protected String getRectangularLabel() {
        return rect_label;
    }

    //
    // protected methods intended to be overridden
    //
    protected void drawInputLines(ComponentDrawContext context,
            AbstractGate comp, int inputs,
            int x, int yTop, int width, int height) {
    }

    protected abstract void drawShape(ComponentDrawContext context,
            int x, int y, int width, int height);

    protected void drawRectangular(ComponentDrawContext context,
            int x, int y, int width, int height) {
        int don = has_dongle ? 10 : 0;
        context.drawRectangle(x - width, y - height / 2, width - don, height,
                rect_label);
        if(has_dongle) {
            context.drawDongle(x - 5, y);
        }
    }

    protected abstract void drawDinShape(ComponentDrawContext context,
            int x, int y, int width, int height, int inputs, AbstractGate gate);
    
    protected abstract Value computeOutput(Value[] inputs,
            int num_inputs);
    
    protected abstract Expression computeExpression(Expression[] inputs,
            int numInputs);

    protected boolean shouldRepairWire(Component comp, WireRepairData data) {
        return false;
    }

    //
    // private helper methods
    //
    private Bounds computeBounds(GateAttributes attrs) {
        Direction facing = attrs.facing;
        int size = ((Integer) attrs.size.getValue()).intValue();
        int width = size + bonus_width + (has_dongle ? 10 : 0);
        int height = Math.max(10 * attrs.inputs, size);
        if(facing == Direction.SOUTH) {
            return Bounds.create(-height / 2, -width, height, width);
        } else if(facing == Direction.NORTH) {
            return Bounds.create(-height / 2, 0, height, width);
        } else if(facing == Direction.WEST) {
            return Bounds.create(0, -height / 2, width, height);
        } else {
            return Bounds.create(-width, -height / 2, width, height);
        }
    }
}
