/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.comp.TextFieldListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.TextEditable;
import com.cburch.logisim.util.GraphicsUtil;

class Probe extends ManagedComponent
        implements TextFieldListener, TextEditable, Loggable {
    private TextField field;
    private BitWidth width = BitWidth.ONE;

    public Probe(Location loc, AttributeSet attrs) {
        super(loc, attrs, 1);
        
        ProbeAttributes probeAttrs = (ProbeAttributes) attrs;
        probeAttrs.component = this;

        String text = probeAttrs.label;
        if(text != null && !text.equals("")) createTextField();
        
        setEnd(0, getLocation(), BitWidth.UNKNOWN, EndData.INPUT_ONLY);
    }

    //
    // abstract ManagedComponent methods
    //
    public ComponentFactory getFactory() {
        return ProbeFactory.instance;
    }

    public void propagate(CircuitState state) {
        Value newValue = state.getValue(getEndLocation(0));
        state.setData(this, newValue);
        if(newValue.getBitWidth() != width) {
            width = newValue.getBitWidth();
            ProbeAttributes attrs = (ProbeAttributes) getAttributeSet();
            Bounds bds = ProbeFactory.getOffsetBounds(attrs.facing, width, attrs.radix);
            Location loc = getLocation();
            setBounds(bds.translate(loc.getX(), loc.getY()));
            if(field != null) createTextField();
            fireComponentInvalidated(new ComponentEvent(this));
        }
        /*DEBUGGING - comment out
        ProbeAttributes attrs = (ProbeAttributes) getAttributeSet();
        if(attrs != null && attrs.label.equals("Err")
                && newValue.equals(Value.TRUE)) {
            throw new RuntimeException("Err probe leads to simulation abort");
        } end DEBUGGING */
    }

    //
    // overridden ManagedComponent methods
    //
    public Bounds getBounds(Graphics g) {
        Bounds ret = super.getBounds();
        if(field != null) ret = ret.add(field.getBounds(g));
        return ret;
    }

    public boolean contains(Location pt, Graphics g) {
        return super.contains(pt)
            || (field != null && field.getBounds(g).contains(pt));
    }

    //
    // basic information methods
    //
    public Direction getDirection() {
        ProbeAttributes attrs = (ProbeAttributes) getAttributeSet();
        return attrs.facing;
    }
    public String getLabel() {
        return field == null ? null : field.getText();
    }

    //
    // state information methods
    //
    Value getValue(CircuitState state) {
        Value ret = (Value) state.getData(this);
        return ret == null ? Value.NIL : ret;
    }
    
    //
    // user interface methods
    //
    public void draw(ComponentDrawContext context) {
        CircuitState circuitState = context.getCircuitState();
        Value value = circuitState == null ? Value.NIL : getValue(circuitState);
        
        Graphics g = context.getGraphics();
        Bounds bds = getBounds(); // intentionally with no graphics object - we don't want label included
        int x = bds.getX();
        int y = bds.getY();
        g.setColor(Color.WHITE);
        g.fillRect(x + 5, y + 5, bds.getWidth() - 10, bds.getHeight() - 10);
        g.setColor(Color.GRAY);
        if(value.getWidth() <= 1) {
            g.drawOval(x + 1, y + 1,
                bds.getWidth() - 2, bds.getHeight() - 2);
        } else {
            g.drawRoundRect(x + 1, y + 1,
                bds.getWidth() - 2, bds.getHeight() - 2, 6, 6);
        }

        g.setColor(Color.BLACK);
        if(field != null) field.draw(g);

        if(!context.getShowState()) {
            if(value.getWidth() > 0) {
                GraphicsUtil.drawCenteredText(g, "x" + value.getWidth(),
                    bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
            }
        } else {
            drawValue(context, value);
        }

        context.drawPins(this);
    }
    
    void drawValue(ComponentDrawContext context, Value value) {
        Graphics g = context.getGraphics();
        Bounds bds = getBounds(); // intentionally with no graphics object - we don't want label included

        RadixOption radix = (RadixOption) getAttributeSet().getValue(RadixOption.ATTRIBUTE);
        if(radix == null || radix == RadixOption.RADIX_2) {
            int x = bds.getX();
            int y = bds.getY();
            int wid = value.getWidth();
            if(wid == 0) {
                x += bds.getWidth() / 2;
                y += bds.getHeight() / 2;
                GraphicsUtil.switchToWidth(g, 2);
                g.drawLine(x - 4, y, x + 4, y);
                return;
            }
            int x0 = bds.getX() + bds.getWidth() - 5;
            int compWidth = wid * 10;
            if(compWidth < bds.getWidth() - 3) {
                x0 = bds.getX() + (bds.getWidth() + compWidth) / 2 - 5;
            }
            int cx = x0;
            int cy = bds.getY() + bds.getHeight() - 12;
            int cur = 0;
            for(int k = 0; k < wid; k++) {
                GraphicsUtil.drawCenteredText(g,
                    value.get(k).toDisplayString(), cx, cy);
                ++cur;
                if(cur == 8) {
                    cur = 0;
                    cx = x0;
                    cy -= 20;
                } else {
                    cx -= 10;
                }
            }
        } else {
            String text = radix.toString(value);
            GraphicsUtil.drawCenteredText(g, text,
                    bds.getX() + bds.getWidth() / 2,
                    bds.getY() + bds.getHeight() / 2);
        }
    }
    
    public Object getFeature(Object key) {
        if(key == Loggable.class) return this;
        if(key == TextEditable.class) return this;
        return super.getFeature(key);
    }

    public Caret getTextCaret(ComponentUserEvent event) {
        Graphics g = event.getCanvas().getGraphics();

        // if field is absent, create it
        if(field == null) {
            createTextField();
            return field.getCaret(g, 0);
        }

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

    public Object[] getLogOptions(CircuitState state) {
        return null;
    }
    
    public String getLogName(Object option) {
        ProbeAttributes attrs = (ProbeAttributes) getAttributeSet();
        String ret = attrs.label;
        if(ret == null || ret.equals("")) {
            return null;
        } else {
            return ret;
        }
    }

    public Value getLogValue(CircuitState state, Object option) {
        return getValue(state);
    }

    //
    // private methods
    //
    TextField getTextField() {
        return field;
    }
    
    void createTextField() {
        ProbeAttributes attrs = (ProbeAttributes) getAttributeSet();
        Direction labelloc = attrs.labelloc;

        Bounds bds = getBounds();
        int x;
        int y;
        int halign;
        int valign;
        if(labelloc == Direction.NORTH) {
            halign = TextField.H_CENTER;
            valign = TextField.V_BOTTOM;
            x = bds.getX() + bds.getWidth() / 2;
            y = bds.getY() - 2;
            if(attrs.facing == labelloc) {
                halign = TextField.H_LEFT;
                x += 2;
            }
        } else if(labelloc == Direction.SOUTH) {
            halign = TextField.H_CENTER;
            valign = TextField.V_TOP;
            x = bds.getX() + bds.getWidth() / 2;
            y = bds.getY() + bds.getHeight() + 2;
            if(attrs.facing == labelloc) {
                halign = TextField.H_LEFT;
                x += 2;
            }
        } else if(labelloc == Direction.EAST) {
            halign = TextField.H_LEFT;
            valign = TextField.V_CENTER;
            x = bds.getX() + bds.getWidth() + 2;
            y = bds.getY() + bds.getHeight() / 2;
            if(attrs.facing == labelloc) {
                valign = TextField.V_BOTTOM;
                y -= 2;
            }
        } else { // WEST
            halign = TextField.H_RIGHT;
            valign = TextField.V_CENTER;
            x = bds.getX() - 2;
            y = bds.getY() + bds.getHeight() / 2;
            if(attrs.facing == labelloc) {
                valign = TextField.V_BOTTOM;
                y -= 2;
            }
        }

        if(field == null) {
            field = new TextField(x, y, halign, valign, attrs.labelfont);
            field.addTextFieldListener(this);
        } else {
            field.setLocation(x, y, halign, valign);
            field.setFont(attrs.labelfont);
        }
        String text = attrs.label;
        field.setText(text == null ? "" : text);
    }

    // listener methods
    void attributeValueChanged(ProbeAttributes attrs, Attribute attr, Object value) {
        if(attr == Pin.label_attr) {
            String val = (String) value;
            if(val == null || val.equals("")) {
                field = null;
            } else {
                if(field == null) createTextField();
                else field.setText(val);
            }
            fireComponentInvalidated(new ComponentEvent(this, null, value));
        } else if(attr == Pin.labelloc_attr) {
            if(field != null) createTextField();
        } else if(attr == Pin.labelfont_attr) {
            if(field != null) createTextField();
        } else if(attr == Pin.facing_attr || attr == RadixOption.ATTRIBUTE) {
            Location loc = getLocation();
            Bounds offs = ProbeFactory.getOffsetBounds(attrs.facing, width, attrs.radix);
            setBounds(offs.translate(loc.getX(), loc.getY()));
            if(field != null) createTextField();
        }
    }

    public void textChanged(TextFieldEvent e) {
        ProbeAttributes attrs = (ProbeAttributes) getAttributeSet();
        String next = e.getText();
        if(!attrs.label.equals(next)) {
            attrs.setValue(Pin.label_attr, next);
        }
    }
}
