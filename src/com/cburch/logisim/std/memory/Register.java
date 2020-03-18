/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

class Register extends ManagedComponent
        implements AttributeListener, Pokable, ToolTipMaker, Loggable {
    public static final Attribute width_attr
        = Attributes.forBitWidth("width", Strings.getter("registerWidthAttr"),
                1, 32);

    public static ComponentFactory factory = new Factory();

    private static Attribute[] ATTRIBUTES = { width_attr };
    private static Object[] DEFAULTS = { BitWidth.create(8) };
    private static final Bounds OFFSET_BOUNDS = Bounds.create(-30, -20, 30, 40);
    private static final int DELAY = 8;
    private static final int OUT = 0;
    private static final int IN  = 1;
    private static final int CK  = 2;
    private static final int CLR = 3;

    private static class Factory extends AbstractComponentFactory {
        private Factory() { }

        public String getName() { return "Register"; }

        public String getDisplayName() { return Strings.get("registerComponent"); }

        public AttributeSet createAttributeSet() {
            return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
        }

        public Component createComponent(Location loc, AttributeSet attrs) {
            return new Register(loc, attrs);
        }

        public Bounds getOffsetBounds(AttributeSet attrs) {
            return OFFSET_BOUNDS;
        }
        
        //
        // user interface methods
        //
        public void paintIcon(ComponentDrawContext context,
                int x, int y, AttributeSet attrs) {
            Graphics g = context.getGraphics();
            Font old = g.getFont();
            g.setFont(old.deriveFont(9.0f));
            GraphicsUtil.drawCenteredText(g, "Reg", x + 10, y + 9);
            g.setFont(old);
            g.drawRect(x, y + 4, 19, 12);
            for(int dx = 2; dx < 20; dx += 5) {
                g.drawLine(x + dx, y +  2, x + dx, y +  4);
                g.drawLine(x + dx, y + 16, x + dx, y + 18);
            }
        }

    }
    
    private static class State implements ComponentState, Cloneable {
        private int value;
        private Value lastClock;
    
        public State() {
            value = 0;
            lastClock = Value.FALSE;
        }
        
        public Object clone() {
            try { return super.clone(); }
            catch(CloneNotSupportedException e) { return null; }
        }
    }
    
    private class CompCaret extends AbstractCaret {
        State state;
        int initValue;
        int curValue;
        CircuitState circState;
    
        CompCaret(State state, CircuitState circState) {
            this.state = state;
            this.circState = circState;
            initValue = state.value;
            curValue = initValue;
        }
    
        public void draw(Graphics g) {
            Bounds bds = Register.this.getBounds();
            int len = (Register.this.dataWidth.getWidth() + 3) / 4;
    
            g.setColor(Color.RED);
            if(len > 4) {
                g.drawRect(bds.getX(), bds.getY() + 3, bds.getWidth(), 25);
            } else {
                int wid = 7 * len + 2;
                g.drawRect(bds.getX() + (bds.getWidth() - wid) / 2, bds.getY() + 4, wid, 15);
            }
            g.setColor(Color.BLACK);
        }
    
        public void stopEditing() { }
        public void cancelEditing() {
            state.value = initValue;
        }
    
        public void keyTyped(KeyEvent e) {
            int val = Character.digit(e.getKeyChar(), 16);
            if(val < 0) return;
    
            curValue = (curValue * 16 + val) & Register.this.dataWidth.getMask();
            state.value = curValue;
    
            circState.setValue(Register.this.getEndLocation(OUT),
                Value.createKnown(Register.this.dataWidth, state.value),
                Register.this, 1);
        }
    }

    private BitWidth dataWidth;

    public Register(Location loc, AttributeSet attrs) {
        super(loc, attrs, 4);
        attrs.addAttributeListener(this);
        setPins();
    }

    private void setPins() {
        Location loc = getLocation();
        dataWidth = (BitWidth) getAttributeSet().getValue(width_attr);
        setEnd(OUT, loc,                    dataWidth, EndData.OUTPUT_ONLY);
        setEnd(IN,  loc.translate(-30,  0), dataWidth, EndData.INPUT_ONLY);
        setEnd(CK,  loc.translate(-20, 20), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(CLR, loc.translate(-10, 20), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return Register.factory;
    }

    public void propagate(CircuitState state) {
        State myState = getState(state);

        Value ckValue  = state.getValue(getEndLocation(CK));
        Value clrValue = state.getValue(getEndLocation(CLR));
        if(clrValue == Value.TRUE) {
            myState.value = 0;
        } else if(myState.lastClock == Value.FALSE
                && ckValue == Value.TRUE) {
            Value inValue = state.getValue(getEndLocation(IN));
            if(inValue.isFullyDefined()) {
                myState.value = inValue.toIntValue();
            }
        } 

        myState.lastClock = ckValue;

        state.setValue(getEndLocation(OUT),
            Value.createKnown(dataWidth, myState.value),
            this, DELAY);
    }

    private State getState(CircuitState state) {
        State myState = (State) state.getData(this);
        if(myState == null) {
            myState = new State();
            state.setData(this, myState);
        }
        return myState;
    }

    public void attributeListChanged(AttributeEvent e) { }
    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == width_attr) {
            setPins();
        }
    }
    
    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        Bounds bds = getBounds();
        State state = getState(context.getCircuitState());
        int width = dataWidth.getWidth();


        // draw boundary
        context.drawBounds(this);

        // draw input and output pins
        if(width <= 16 || !context.getShowState()) {
            context.drawPin(this,   IN,  "D", Direction.EAST);
            context.drawPin(this,   OUT, "Q", Direction.WEST);
        } else {
            context.drawPin(this,   IN);
            context.drawPin(this,   OUT);
        }
        g.setColor(Color.GRAY);
        context.drawPin(this,   CLR, "clr", Direction.SOUTH);
        g.setColor(Color.BLACK);
        context.drawClock(this, CK ,      Direction.NORTH);

        // draw contents
        if(context.getShowState()) {
            String str = StringUtil.toHexString(width, state.value);
            if(str.length() <= 4) {
                GraphicsUtil.drawText(g, str,
                        bds.getX() + 15, bds.getY() + 4,
                        GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
            } else {
                int split = str.length() - 4;
                GraphicsUtil.drawText(g, str.substring(0, split),
                        bds.getX() + 15, bds.getY() + 3,
                        GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
                GraphicsUtil.drawText(g, str.substring(split),
                        bds.getX() + 15, bds.getY() + 15,
                        GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
            }
        }
    }
    
    public Object getFeature(Object key) {
        if(key == Pokable.class) return this;
        if(key == Loggable.class) return this;
        if(key == ToolTipMaker.class) return this;
        return super.getFeature(key);
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        Bounds bds = getBounds();
        CircuitState circState = event.getCircuitState();
        State state = getState(circState);
        CompCaret ret = new CompCaret(state, circState);
        ret.setBounds(bds);
        return ret;
    }
    
    public String getToolTip(ComponentUserEvent e) {
        int end = -1;
        for(int i = getEnds().size() - 1; i >= 0; i--) {
            if(getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10) {
                end = i;
                break;
            }
        }
        switch(end) {
        case OUT:  return Strings.get("registerQTip");
        case IN:   return Strings.get("registerDTip");
        case CK:   return Strings.get("registerClkTip");
        case CLR:  return Strings.get("registerClrTip");
        default:   return null;
        }
    }

    public Object[] getLogOptions(CircuitState state) {
        return null;
    }

    public String getLogName(Object option) {
        return null;
    }

    public Value getLogValue(CircuitState state, Object option) {
        return Value.createKnown(dataWidth, getState(state).value);
    }
}

