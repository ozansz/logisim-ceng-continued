/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import javax.swing.Icon;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.IntegerFactory;

public class TextClass extends AbstractComponentFactory {
    public static TextClass instance = new TextClass();

    public static Attribute text_attr = Attributes.forString("text",
        Strings.getter("textTextAttr"));
    public static Attribute font_attr = Attributes.forFont("font",
        Strings.getter("textFontAttr"));
    public static Attribute halign_attr = Attributes.forOption("halign",
        Strings.getter("textHorzAlignAttr"), new AttributeOption[] {
            new AttributeOption(IntegerFactory.create(TextField.H_LEFT),
                "left", Strings.getter("textHorzAlignLeftOpt")),
            new AttributeOption(IntegerFactory.create(TextField.H_RIGHT),
                "right", Strings.getter("textHorzAlignRightOpt")),
            new AttributeOption(IntegerFactory.create(TextField.H_CENTER),
                "center", Strings.getter("textHorzAlignCenterOpt")),
        });
    public static Attribute valign_attr = Attributes.forOption("valign",
        Strings.getter("textVertAlignAttr"), new AttributeOption[] {
            new AttributeOption(IntegerFactory.create(TextField.V_TOP),
                "top", Strings.getter("textVertAlignTopOpt")),
            new AttributeOption(IntegerFactory.create(TextField.V_BASELINE),
                "base", Strings.getter("textVertAlignBaseOpt")),
            new AttributeOption(IntegerFactory.create(TextField.V_BOTTOM),
                "bottom", Strings.getter("textVertAlignBottomOpt")),
            new AttributeOption(IntegerFactory.create(TextField.H_CENTER),
                "center", Strings.getter("textVertAlignCenterOpt")),
        });
    
    private static final Attribute[] ATTRIBUTES = {
        text_attr, font_attr,
        halign_attr, valign_attr
    };
    private static final Object[] DEFAULTS = {
        "", new Font("SansSerif", Font.PLAIN, 12),
        halign_attr.parse("center"), valign_attr.parse("base"),
    };
    private static final Icon toolIcon = Icons.getIcon("text.gif");

    private TextClass() { }

    public String getName() { return "Text"; }

    public String getDisplayName() {
        return Strings.get("textComponent");
    }

    public AttributeSet createAttributeSet() {
        return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new Text(loc, attrs);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return Bounds.EMPTY_BOUNDS;
    }
    
    //
    // user interface methods
    //
    public Object getFeature(Object key, AttributeSet attrs) {
        if(key == SHOULD_SNAP) return Boolean.FALSE;
        return super.getFeature(key, attrs);
    }

    public void drawGhost(ComponentDrawContext context,
            Color color, int x, int y, AttributeSet attrs) {
        AttributeOption ha = (AttributeOption) attrs.getValue(halign_attr);
        AttributeOption va = (AttributeOption) attrs.getValue(valign_attr);
        int h = ((Integer) ha.getValue()).intValue();
        int v = ((Integer) va.getValue()).intValue();

        Graphics g = context.getGraphics();
        String text = (String) attrs.getValue(text_attr);
        if(text == null || text.equals("")) return;
        g.setColor(color);
        Font old = g.getFont();
        g.setFont((Font) attrs.getValue(font_attr));
        GraphicsUtil.drawText(g, text, x, y, h, v);
        g.setFont(old);
    }

    public void paintIcon(ComponentDrawContext c,
            int x, int y, AttributeSet attrs) {
        Graphics g = c.getGraphics();
        if(toolIcon != null) {
            toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
        } else {
            g.setColor(Color.black);
            GraphicsUtil.switchToWidth(g, 2);
            g.drawLine(x + 5, y + 16, x + 10, y + 2);
            g.drawLine(x + 15, y + 16, x + 10, y + 2);
            g.drawLine(x + 7, y + 11, x + 13, y + 11);
        }
    }

}
