/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std;


import java.awt.Font;
import java.awt.Graphics;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.comp.TextFieldListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.TextEditable;

public class Text extends ManagedComponent implements TextEditable {

    private class MyListener
            implements AttributeListener, TextFieldListener {
        public void attributeListChanged(AttributeEvent e) { }
        public void attributeValueChanged(AttributeEvent e) {
            Attribute attr = e.getAttribute();
            if(attr == TextClass.text_attr) {
                String text = (String) e.getValue();
                if(!text.equals(field.getText())) {
                    field.setText((String) e.getValue());
                }
            } else if(attr == TextClass.font_attr) {
                field.setFont((Font) e.getValue());
            } else if(attr == TextClass.halign_attr) {
                AttributeOption ha = (AttributeOption) e.getValue();
                int h = ((Integer) ha.getValue()).intValue();
                field.setHorzAlign(h);
            } else if(attr == TextClass.valign_attr) {
                AttributeOption va = (AttributeOption) e.getValue();
                int v = ((Integer) va.getValue()).intValue();
                field.setVertAlign(v);
            }
        }
        public void textChanged(TextFieldEvent e) {
            AttributeSet attrs = getAttributeSet();
            String prev = (String) attrs.getValue(TextClass.text_attr);
            String next = e.getText();
            if(!prev.equals(next)) {
                attrs.setValue(TextClass.text_attr, next);
            }
        }
    }

    private TextField field;

    public Text(Location loc, AttributeSet attrs) {
        super(loc, attrs, 0);

        AttributeOption ha = (AttributeOption) attrs.getValue(TextClass.halign_attr);
        AttributeOption va = (AttributeOption) attrs.getValue(TextClass.valign_attr);
        int h = ((Integer) ha.getValue()).intValue();
        int v = ((Integer) va.getValue()).intValue();
        field = new TextField(loc.getX(), loc.getY(), h, v,
            (Font) attrs.getValue(TextClass.font_attr));
        field.setText((String) attrs.getValue(TextClass.text_attr));

        MyListener l = new MyListener();
        attrs.addAttributeListener(l);
        field.addTextFieldListener(l);
    }

    public ComponentFactory getFactory() {
        return TextClass.instance;
    }

    public void propagate(CircuitState state) { }

    public Bounds getBounds(Graphics g) {
        return field.getBounds(g);
    }
    
    //
    // user interface methods
    //
    public void draw(ComponentDrawContext context) {
        field.draw(context.getGraphics());
    }
    
    public Object getFeature(Object key) {
        if(key == TextEditable.class) return this;
        return super.getFeature(key);
    }
    
    public Caret getTextCaret(ComponentUserEvent event) {
        Graphics g = event.getCanvas().getGraphics();
        Bounds bds = field.getBounds(g);
        if(bds.getWidth() < 4 || bds.getHeight() < 4) {
            Location loc = getLocation();
            bds = bds.add(Bounds.create(loc).expand(2));
        }

        int x = event.getX();
        int y = event.getY();
        if(bds.contains(x, y))  return field.getCaret(g, x, y);
        else                    return null;
    }

}
