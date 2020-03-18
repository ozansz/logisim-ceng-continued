/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretListener;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.TextEditable;
import com.cburch.logisim.util.GraphicsUtil;

public class Clock extends ManagedComponent
        implements Pokable, TextEditable, Loggable {
    private static class State implements ComponentState, Cloneable {
        Value sending = Value.FALSE;
        int clicks = 0;
        
        public Object clone() {
            try { return super.clone(); }
            catch(CloneNotSupportedException e) { return null; }
        }
    }

    private static class PokeCaret implements Caret {
        Clock clock;
        CircuitState state;
        boolean isPressed = true;

        PokeCaret(Clock clock, CircuitState state) {
            this.clock = clock;
            this.state = state;
        }

        public void addCaretListener(CaretListener e) { }
        public void removeCaretListener(CaretListener e) { }

        // query/Graphics methods
        public String getText() { return ""; }
        public Bounds getBounds(Graphics g) {
            return clock.getBounds();
        }
        public void draw(Graphics g) { }

        // finishing
        public void commitText(String text) { }
        public void cancelEditing() { }
        public void stopEditing() { }

        // events to handle
        public void mousePressed(MouseEvent e) {
            isPressed = isInside(e.getX(), e.getY());
        }
        public void mouseDragged(MouseEvent e) { }
        public void mouseReleased(MouseEvent e) {
            boolean isIn = isInside(e.getX(), e.getY());
            if(isPressed && isIn) {
                State myState = clock.getState(state);
                myState.sending = myState.sending.not();
                myState.clicks++;
                state.setValue(clock.getLocation(), myState.sending, clock, 1);
            }
        }
        public void keyPressed(KeyEvent e) { }
        public void keyReleased(KeyEvent e) { }
        public void keyTyped(KeyEvent e) { }

        private boolean isInside(int x, int y) {
            return clock.getBounds().contains(x, y);
        }
    }

    private class MyListener
            implements AttributeListener, TextFieldListener {
        public void attributeListChanged(AttributeEvent e) { }
        public void attributeValueChanged(AttributeEvent e) {
            Attribute attr = e.getAttribute();
            if(attr == ClockFactory.high_attr) {
                durationHigh = ((Integer) e.getValue()).intValue();
            } else if(attr == ClockFactory.low_attr) {
                durationLow = ((Integer) e.getValue()).intValue();
            } else if(attr == Pin.label_attr) {
                String val = (String) e.getValue();
                if(val == null || val.equals("")) {
                    field = null;
                } else {
                    if(field == null) createTextField();
                    else field.setText(val);
                }
            } else if(attr == Pin.labelloc_attr) {
                if(field != null) createTextField();
            } else if(attr == Pin.labelfont_attr) {
                if(field != null) createTextField();
            } else if(attr == Pin.facing_attr) {
                Location loc = getLocation();
                dir = (Direction) e.getValue();
                setBounds(ClockFactory.instance.getOffsetBounds(getAttributeSet()).translate(loc.getX(), loc.getY()));
                if(field != null) createTextField();
            }
        }

        public void textChanged(TextFieldEvent e) {
            AttributeSet attrs = getAttributeSet();
            String prev = (String) attrs.getValue(Pin.label_attr);
            String next = e.getText();
            if(!prev.equals(next)) {
                attrs.setValue(Pin.label_attr, next);
            }
        }
    }

    private MyListener myListener = new MyListener();
    private Direction dir;
    private int durationHigh = 1;
    private int durationLow = 1;
    private TextField field;

    public Clock(Location loc, AttributeSet attrs) {
        super(loc, attrs, 1);
        attrs.addAttributeListener(myListener);

        dir = (Direction) attrs.getValue(Pin.facing_attr);
        durationHigh = ((Integer) attrs.getValue(ClockFactory.high_attr)).intValue();
        durationLow  = ((Integer) attrs.getValue(ClockFactory.low_attr )).intValue();
        setEnd(0, loc, BitWidth.ONE, EndData.OUTPUT_ONLY);

        String text = (String) attrs.getValue(Pin.label_attr);
        if(text != null && !text.equals("")) createTextField();
    }

    //
    // abstract ManagedComponent methods
    //
    public ComponentFactory getFactory() {
        return ClockFactory.instance;
    }

    public void propagate(CircuitState state) {
        Location pt = getEndLocation(0);
        Value val = state.getValue(pt);
        State q = getState(state);
        if(!val.equals(q.sending)) { // ignore if no change
            state.setValue(pt, q.sending, this, 1);
        }
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
    // package methods
    //
    boolean tick(CircuitState circState, int ticks) {
        State state = getState(circState);
        boolean curValue = ticks % (durationHigh + durationLow) < durationLow;
        if(state.clicks % 2 == 1) curValue = !curValue;
        Value desired = (curValue ? Value.FALSE : Value.TRUE);
        if(!state.sending.equals(desired)) {
            state.sending = desired;
            circState.setValue(getLocation(), desired, this, 1);
            return true;
        } else {
            return false;
        }
    }

    //
    // private methods
    //
    private State getState(CircuitState state) {
        State ret = (State) state.getData(this);
        if(ret == null) {
            ret = new State();
            state.setData(this, ret);
        }
        return ret;
    }

    private void createTextField() {
        AttributeSet attrs = getAttributeSet();
        Direction labelloc = (Direction) attrs.getValue(Pin.labelloc_attr);

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
            if(dir == labelloc) {
                halign = TextField.H_LEFT;
                x += 2;
            }
        } else if(labelloc == Direction.SOUTH) {
            halign = TextField.H_CENTER;
            valign = TextField.V_TOP;
            x = bds.getX() + bds.getWidth() / 2;
            y = bds.getY() + bds.getHeight() + 2;
            if(dir == labelloc) {
                halign = TextField.H_LEFT;
                x += 2;
            }
        } else if(labelloc == Direction.EAST) {
            halign = TextField.H_LEFT;
            valign = TextField.V_CENTER;
            x = bds.getX() + bds.getWidth() + 2;
            y = bds.getY() + bds.getHeight() / 2;
            if(dir == labelloc) {
                valign = TextField.V_BOTTOM;
                y -= 2;
            }
        } else { // WEST
            halign = TextField.H_RIGHT;
            valign = TextField.V_CENTER;
            x = bds.getX() - 2;
            y = bds.getY() + bds.getHeight() / 2;
            if(dir == labelloc) {
                valign = TextField.V_BOTTOM;
                y -= 2;
            }
        }

        if(field == null) {
            field = new TextField(x, y, halign, valign,
                (Font) attrs.getValue(Pin.labelfont_attr));
            field.addTextFieldListener(myListener);
        } else {
            field.setLocation(x, y, halign, valign);
            field.setFont((Font) attrs.getValue(Pin.labelfont_attr));
        }
        String text = (String) attrs.getValue(Pin.label_attr);
        field.setText(text == null ? "" : text);
    }

    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        java.awt.Graphics g = context.getGraphics();
        Bounds bds = getBounds(); // intentionally with no graphics object - we don't want label included
        int x = bds.getX();
        int y = bds.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.black);
        g.drawRect(x, y, bds.getWidth(), bds.getHeight());

        if(field != null) field.draw(g);

        boolean drawUp;
        if(context.getShowState()) {
            CircuitState circState = context.getCircuitState();
            State state = getState(circState);
            g.setColor(state.sending.getColor());
            drawUp = state.sending == Value.TRUE;
        } else {
            g.setColor(Color.BLACK);
            drawUp = true;
        }
        x += 10;
        y += 10;
        int[] xs = { x - 6, x - 6, x, x, x + 6, x + 6 };
        int[] ys;
        if(drawUp) {
            ys = new int[] { y, y - 4, y - 4, y + 4, y + 4, y };
        } else {
            ys = new int[] { y, y + 4, y + 4, y - 4, y - 4, y };
        }
        g.drawPolyline(xs, ys, xs.length);

        context.drawPins(this);
    }
    
    public Object getFeature(Object key) {
        if(key == Pokable.class) return this;
        if(key == Loggable.class) return this;
        if(key == TextEditable.class) return this;
        return super.getFeature(key);
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        if(getBounds().contains(event.getX(), event.getY())) {
            return new PokeCaret(this, event.getCircuitState());
        } else {
            return null;
        }
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
        return (String) getAttributeSet().getValue(Pin.label_attr);
    }

    public Value getLogValue(CircuitState state, Object option) {
        State s = getState(state);
        return s.sending;
    }

    // CENG Logisim addition
    public void changeValue(CircuitState state, Value value) {
        State myState = getState(state);
        myState.sending = value;
        myState.clicks++;
        state.setValue(getLocation(), myState.sending, this, 1);
    }

}
