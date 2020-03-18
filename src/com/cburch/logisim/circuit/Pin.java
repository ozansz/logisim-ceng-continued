/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretListener;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.util.GraphicsUtil;

public class Pin extends Probe implements Pokable {
    public static final Attribute facing_attr
        = Attributes.forDirection("facing", Strings.getter("pinFacingAttr"));
    public static final Attribute threeState_attr
        = Attributes.forBoolean("tristate", Strings.getter("pinThreeStateAttr"));
    public static final Attribute width_attr
        = Attributes.forBitWidth("width", Strings.getter("pinBitWidthAttr"));
    public static final Attribute type_attr
        = Attributes.forBoolean("output", Strings.getter("pinOutputAttr"));
    public static final Attribute label_attr
        = Attributes.forString("label", Strings.getter("pinLabelAttr"));
    public static final Attribute labelloc_attr
        = Attributes.forDirection("labelloc", Strings.getter("pinLabelLocAttr"));
    public static final Attribute labelfont_attr
        = Attributes.forFont("labelfont", Strings.getter("pinLabelFontAttr"));
    
    public static final Object pull_none
        = new AttributeOption("none", Strings.getter("pinPullNoneOption"));
    public static final Object pull_up
        = new AttributeOption("up", Strings.getter("pinPullUpOption"));
    public static final Object pull_down
        = new AttributeOption("down", Strings.getter("pinPullDownOption"));
    private static final Object[] pull_options = { pull_none, pull_up, pull_down };
    public static final Attribute pull_attr
        = Attributes.forOption("pull", Strings.getter("pinPullAttr"), pull_options);

    private static class State implements ComponentState, Cloneable {
        Value sending;
        Value receiving;
        
        public Object clone() {
            try { return super.clone(); }
            catch(CloneNotSupportedException e) { return null; }
        }
    }

    private static class PokeCaret implements Caret {
        Pin pin;
        Canvas canvas;
        int bit_pressed = -1;

        PokeCaret(Pin pin, Canvas canvas) {
            this.pin = pin;
            this.canvas = canvas;
        }

        public void addCaretListener(CaretListener e) { }
        public void removeCaretListener(CaretListener e) { }

        // query/Graphics methods
        public String getText() { return ""; }
        public Bounds getBounds(Graphics g) {
            return pin.getBounds();
        }
        public void draw(Graphics g) { }

        // finishing
        public void commitText(String text) { }
        public void cancelEditing() { }
        public void stopEditing() { }

        // events to handle
        public void mousePressed(MouseEvent e) {
            bit_pressed = getBit(e.getX(), e.getY());
        }
        public void mouseDragged(MouseEvent e) { }
        public void mouseReleased(MouseEvent e) {
            if(!pin.isInputPin()) return;

            int bit = getBit(e.getX(), e.getY());
            if(bit < 0 || bit != bit_pressed) {
                bit_pressed = -1;
                return;
            }
            bit_pressed = -1;

            CircuitState state = canvas.getCircuitState();
            if(state.isSubstate()) {
                int choice = JOptionPane.showConfirmDialog(canvas.getProject().getFrame(),
                        Strings.get("pinFrozenQuestion"),
                        Strings.get("pinFrozenTitle"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if(choice != JOptionPane.OK_OPTION) return;
                state = state.cloneState();
                canvas.getProject().setCircuitState(state);
            }

            State pin_state = pin.getState(state);
            Value val = pin_state.sending.get(bit);
            if(val == Value.FALSE) {
                val = Value.TRUE;
            } else if(val == Value.TRUE) {
                PinAttributes attrs = (PinAttributes) pin.getAttributeSet();
                val = attrs.threeState ? Value.UNKNOWN : Value.FALSE;
            } else {
                val = Value.FALSE;
            }
            pin_state.sending = pin_state.sending.set(bit, val);
            state.setValue(pin.getLocation(), pin_state.sending,
                pin, 1);
        }
        public void keyPressed(KeyEvent e) { }
        public void keyReleased(KeyEvent e) { }
        public void keyTyped(KeyEvent e) { }

        private int getBit(int x, int y) {
            int which;
            BitWidth width = pin.getWidth();
            if(width.getWidth() == 1) {
                return 0;
            } else {
                Bounds bds = pin.getBounds(); // intentionally with no graphics object - we don't want label included
                int i = (bds.getX() + bds.getWidth() - x) / 10;
                int j = (bds.getY() + bds.getHeight() - y) / 20;
                which = 8 * j + i;
                if(which < 0 || which >= width.getWidth()) {
                    return -1;
                } else {
                    return which;
                }
            }
        }
    }

    public Pin(Location loc, AttributeSet attrs) {
        super(loc, attrs);

        setEnd((PinAttributes) attrs);
    }

    private void setEnd(PinAttributes attrs) {
        int endType = attrs.type;
        if(attrs.type == EndData.OUTPUT_ONLY) endType = EndData.INPUT_ONLY;
        else if(attrs.type == EndData.INPUT_ONLY) endType = EndData.OUTPUT_ONLY;

        setEnd(0, getLocation(), attrs.width, endType);
    }

    //
    // abstract ManagedComponent methods
    //
    public ComponentFactory getFactory() {
        return PinFactory.instance;
    }

    public void propagate(CircuitState state) {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        Location pt = getEndLocation(0);
        Value val = state.getValue(pt);

        State q = getState(state);
        if(attrs.type == EndData.OUTPUT_ONLY) {
            q.sending = val;
            q.receiving = val;
            state.setValue(pt, Value.createUnknown(attrs.width), this, 1);
        } else {
            q.receiving = val;
            if(!val.equals(q.sending)) { // ignore if no change
                state.setValue(pt, q.sending, this, 1);
            }
        }
    }

    //
    // basic information methods
    //
    public BitWidth getWidth() {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        return attrs.width;
    }
    public int getType() {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        return attrs.type;
    }
    public boolean isInputPin() {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        return attrs.type != EndData.OUTPUT_ONLY;
    }

    //
    // state information methods
    //
    Value getValue(CircuitState state) {
        return getState(state).sending;
    }

    // CENG Logisim addition
    public void changeValue(CircuitState state, Value value) {
        setValue(state, value);
        State myState = getState(state);
        state.setValue(getLocation(), myState.sending, this, 1);
    }

    void setValue(CircuitState state, Value value) {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        Object pull = attrs.pull;
        if(pull != pull_none && pull != null && !value.isFullyDefined()) {
            Value[] bits = value.getAll();
            if(pull == pull_up) {
                for(int i = 0; i < bits.length; i++) {
                    if(bits[i] != Value.FALSE) bits[i] = Value.TRUE;
                }
            } else if(pull == pull_down) {
                for(int i = 0; i < bits.length; i++) {
                    if(bits[i] != Value.TRUE) bits[i] = Value.FALSE;
                }
            }
            value = Value.create(bits);
        }
        
        State myState = getState(state);
        if(value == Value.NIL) {
            myState.sending = Value.createUnknown(attrs.width);
        } else {
            myState.sending = value;
        }
    }

    private State getState(CircuitState state) {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        BitWidth width = attrs.width;
        State ret = (State) state.getData(this);
        if(ret == null) {
            ret = new State();
            Value val = attrs.threeState ? Value.UNKNOWN : Value.FALSE;
            if(width.getWidth() > 1) {
                Value[] arr = new Value[width.getWidth()];
                java.util.Arrays.fill(arr, val);
                val = Value.create(arr);
            }
            ret.sending = val;
            ret.receiving = val;
            state.setData(this, ret);
        }
        if(ret.sending.getWidth() != width.getWidth()) {
            ret.sending = ret.sending.extendWidth(width.getWidth(),
                    attrs.threeState ? Value.UNKNOWN : Value.FALSE);
        }
        if(ret.receiving.getWidth() != width.getWidth()) {
            ret.receiving = ret.receiving.extendWidth(width.getWidth(), Value.UNKNOWN);
        }
        return ret;
    }
    
    //
    // user interface methods
    //
    public void draw(ComponentDrawContext context) {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        java.awt.Graphics g = context.getGraphics();
        Bounds bds = getBounds(); // intentionally with no graphics object - we don't want label included
        int x = bds.getX();
        int y = bds.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.black);
        if(attrs.type == EndData.OUTPUT_ONLY) {
            if(attrs.width.getWidth() == 1) {
                g.drawOval(x + 1, y + 1,
                    bds.getWidth() - 1, bds.getHeight() - 1);
            } else {
                g.drawRoundRect(x + 1, y + 1,
                    bds.getWidth() - 1, bds.getHeight() - 1, 6, 6);
            }
        } else {
            g.drawRect(x + 1, y + 1,
                bds.getWidth() - 1, bds.getHeight() - 1);
        }

        TextField field = getTextField();
        if(field != null) field.draw(g);

        if(!context.getShowState()) {
            g.setColor(Color.black);
            GraphicsUtil.drawCenteredText(g, "x" + attrs.width.getWidth(),
                    bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
        } else {
            CircuitState circ_state = context.getCircuitState();
            State state = getState(circ_state);
            if(attrs.width.getWidth() <= 1) {
                Value receiving = state.receiving;
                g.setColor(receiving.getColor());
                g.fillOval(x + 4, y + 4, 13, 13);

                if(attrs.width.getWidth() == 1) {
                    g.setColor(Color.white);
                    GraphicsUtil.drawCenteredText(g,
                        state.sending.toDisplayString(), x + 11, y + 9);
                }
            } else {
                drawValue(context, state.sending);
            }
        }

        context.drawPins(this);
    }
    
    public Object getFeature(Object key) {
        if(key == Pokable.class) return this;
        return super.getFeature(key);
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        if(getBounds().contains(event.getX(), event.getY())) {
            return new PokeCaret(this, event.getCanvas());
        } else {
            return null;
        }
    }
    
    public String getLogName(Object option) {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        String ret = attrs.label;
        if(ret == null || ret.equals("")) {
            String type = attrs.type == EndData.INPUT_ONLY
                ? Strings.get("pinInputName") : Strings.get("pinOutputName");
            return type + getLocation();
        } else {
            return ret;
        }
    }

    public Value getLogValue(CircuitState state, Object option) {
        State s = getState(state);
        return s.sending;
    }

    // listener methods
    void attributeValueChanged(ProbeAttributes probeAttrs, Attribute attr, Object value) {
        PinAttributes attrs = (PinAttributes) probeAttrs;
        if(attr == width_attr || attr == type_attr) {
            setEnd(attrs);
        } else {
            super.attributeValueChanged(attrs, attr, value);
        }
        if(attr == width_attr || attr == facing_attr) {
            Location loc = getLocation();
            setBounds(PinFactory.instance.getOffsetBounds(attrs)
                    .translate(loc.getX(), loc.getY()));
            if(getTextField() != null) createTextField();
        }
    }

    public void textChanged(TextFieldEvent e) {
        PinAttributes attrs = (PinAttributes) getAttributeSet();
        String next = e.getText();
        if(!attrs.label.equals(next)) {
            attrs.setValue(label_attr, next);
        }
    }
}
