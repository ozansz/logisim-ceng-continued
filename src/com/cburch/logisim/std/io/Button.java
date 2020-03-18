/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.comp.TextFieldListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.TextEditable;
import com.cburch.logisim.util.Icons;

class Button extends ManagedComponent
        implements AttributeListener, TextFieldListener, Pokable,
            TextEditable, Loggable {
    public static final ComponentFactory factory = new Factory();

    private static final Attribute[] ATTRIBUTES
        = { Io.ATTR_FACING, Io.ATTR_COLOR,
            Io.ATTR_LABEL, Io.ATTR_LABEL_LOC, Io.ATTR_LABEL_FONT, Io.ATTR_LABEL_COLOR };
    private static final Object[] DEFAULTS
        = { Direction.EAST, Color.white,
            "", Io.LABEL_CENTER, Io.DEFAULT_LABEL_FONT, Color.black };

    private static final Icon toolIcon = Icons.getIcon("button.gif");
    
    private static final int DEPTH = 3;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() {
            return "Button";
        }

        public String getDisplayName() {
            return Strings.get("buttonComponent");
        }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Button(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            Direction facing = (Direction) attrs.getValue(Io.ATTR_FACING);
            return Bounds.create(-20, -10, 20, 20).rotate(Direction.EAST, facing, 0, 0);
        }

        //
        // user interface methods
        //
        public void paintIcon(ComponentDrawContext context,
                int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            if(toolIcon != null) {
                toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
            }
        }

        public Object getFeature(Object key, AttributeSet attrs) {
            if(key == FACING_ATTRIBUTE_KEY) return Io.ATTR_FACING;
            return super.getFeature(key, attrs);
        }
    }
    
    private class PokeCaret extends AbstractCaret {
        CircuitState circState;
    
        PokeCaret(CircuitState circState) {
            this.circState = circState;
        }
        
        public void mousePressed(MouseEvent e) {
            circState.setData(Button.this, Value.TRUE);
            circState.setValue(Button.this.getLocation(), Value.TRUE, Button.this, 1);
        }
        
        public void mouseReleased(MouseEvent e) {
            circState.setData(Button.this, Value.FALSE);
            circState.setValue(Button.this.getLocation(), Value.FALSE, Button.this, 1);
        }
    }
    
    private TextField field;

    private Button(Location loc, AttributeSet attrs) {
        super(loc, attrs, 1);

        setEnd(0, getLocation(), BitWidth.ONE, EndData.OUTPUT_ONLY);

        attrs.addAttributeListener(this);
        String text = (String) attrs.getValue(Io.ATTR_LABEL);
        if(text != null && !text.equals("")) createTextField();
    }

    private void createTextField() {
        AttributeSet attrs = getAttributeSet();
        Direction facing = (Direction) attrs.getValue(Io.ATTR_FACING);
        Object labelLoc = attrs.getValue(Io.ATTR_LABEL_LOC);

        Bounds bds = getBounds();
        int x = bds.getX() + bds.getWidth() / 2;
        int y = bds.getY() + bds.getHeight() / 2;
        int halign = TextField.H_CENTER;
        int valign = TextField.V_CENTER;
        if(labelLoc == Io.LABEL_CENTER) {
            x = bds.getX() + (bds.getWidth() - DEPTH) / 2;
            y = bds.getY() + (bds.getHeight() - DEPTH) / 2;
        } else if(labelLoc == Direction.NORTH) {
            y = bds.getY() - 2;
            valign = TextField.V_BOTTOM;
        } else if(labelLoc == Direction.SOUTH) {
            y = bds.getY() + bds.getHeight() + 2;
            valign = TextField.V_TOP;
        } else if(labelLoc == Direction.EAST) {
            x = bds.getX() + bds.getWidth() + 2;
            halign = TextField.H_LEFT;
        } else if(labelLoc == Direction.WEST) {
            x = bds.getX() - 2;
            halign = TextField.H_RIGHT;
        }
        if(labelLoc == facing) {
            if(labelLoc == Direction.NORTH || labelLoc == Direction.SOUTH) {
                x += 2;
                halign = TextField.H_LEFT;
            } else {
                y -= 2;
                valign = TextField.V_BOTTOM;
            }
        }

        if(field == null) {
            field = new TextField(x, y, halign, valign,
                (Font) attrs.getValue(Io.ATTR_LABEL_FONT));
            field.addTextFieldListener(this);
        } else {
            field.setLocation(x, y, halign, valign);
            field.setFont((Font) attrs.getValue(Io.ATTR_LABEL_FONT));
        }
        String text = (String) attrs.getValue(Io.ATTR_LABEL);
        field.setText(text == null ? "" : text);
    }

    public ComponentFactory getFactory() {
        return factory;
    }

    public void propagate(CircuitState state) {
        Value val = (Value) state.getData(this);
        if(val == null) val = Value.FALSE;
        state.setValue(getLocation(), val, this, 1);
    }

    public void attributeListChanged(AttributeEvent e) { }
    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == Io.ATTR_FACING) {
            Location loc = getLocation();
            setBounds(getFactory().getOffsetBounds(getAttributeSet()).translate(loc.getX(), loc.getY()));
            if(field != null) createTextField();
        } else if(attr == Io.ATTR_LABEL) {
            String val = (String) e.getValue();
            if(val == null || val.equals("")) {
                field = null;
            } else {
                if(field == null) createTextField();
                else field.setText(val);
            }
        } else if(attr == Io.ATTR_LABEL_LOC) {
            if(field != null) createTextField();
        } else if(attr == Io.ATTR_LABEL_FONT) {
            if(field != null) createTextField();
        }
    }

    public void textChanged(TextFieldEvent e) {
        AttributeSet attrs = getAttributeSet();
        String prev = (String) attrs.getValue(Io.ATTR_LABEL);
        String next = e.getText();
        if(!prev.equals(next)) {
            attrs.setValue(Io.ATTR_LABEL, next);
        }
    }
    
    //
    // user interface methods
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

    public void draw(ComponentDrawContext context) {
        Bounds bds = getBounds();
        int x = bds.getX();
        int y = bds.getY();
        int w = bds.getWidth();
        int h = bds.getHeight();

        Value val;
        if(context.getShowState()) val = (Value) context.getCircuitState().getData(this);
        else val = Value.FALSE;
        
        Color color = (Color) getAttributeSet().getValue(Io.ATTR_COLOR);
        
        Graphics g = context.getGraphics();
        int depress;
        if(val == Value.TRUE) {
            x += DEPTH;
            y += DEPTH;
            depress = DEPTH;
            g.setColor(color);
            g.fillRect(x, y, w - DEPTH, h - DEPTH);
            g.setColor(Color.black);
            g.drawRect(x, y, w - DEPTH, h - DEPTH);
        } else {
            depress = 0;
            int[] xp = new int[] { x, x + w - DEPTH, x + w, x + w, x + DEPTH, x };
            int[] yp = new int[] { y, y, y + DEPTH, y + h, y + h, y + h - DEPTH };
            g.setColor(color.darker());
            g.fillPolygon(xp, yp, xp.length);
            g.setColor(color);
            g.fillRect(x, y, w - DEPTH, h - DEPTH);
            g.setColor(Color.black);
            g.drawRect(x, y, w - DEPTH, h - DEPTH);
            g.drawLine(x + w - DEPTH, y + h - DEPTH, x + w, y + h);
            g.drawPolygon(xp, yp, xp.length);
        }
        if(field != null) {
            g.setColor((Color) getAttributeSet().getValue(Io.ATTR_LABEL_COLOR));
            g.translate(depress, depress);
            field.draw(g);
            g.translate(-depress, -depress);
        }
        context.drawPins(this);
    }
    
    public Object getFeature(Object key) {
        if(key == Pokable.class) return this;
        if(key == TextEditable.class) return this;
        if(key == Loggable.class) return this;
        return super.getFeature(key);
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        Bounds bds = getBounds();
        CircuitState circState = event.getCircuitState();
        PokeCaret ret = new PokeCaret(circState);
        ret.setBounds(bds);
        ret.mousePressed(null);
        return ret;
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
        return (String) getAttributeSet().getValue(Io.ATTR_LABEL);
    }

    public Value getLogValue(CircuitState state, Object option) {
        Value ret = (Value) state.getData(this);
        return ret == null ? Value.FALSE : ret; 
    }
}
