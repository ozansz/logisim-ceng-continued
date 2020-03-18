/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretListener;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

abstract class AbstractFlipFlop extends AbstractComponentFactory {
    private static class StateData implements ComponentState, Cloneable {
        Value lastClock = Value.FALSE;
        Value curValue  = Value.FALSE;
        Location propLocation = null; // so that moved component emits again
        
        public Object clone() {
            try { return super.clone(); }
            catch(CloneNotSupportedException e) { return null; }
        }
    }

    private static class PokeCaret implements Caret {
        Comp comp;
        CircuitState state;
        boolean isPressed = true;

        PokeCaret(Comp comp, CircuitState state) {
            this.comp = comp;
            this.state = state;
        }

        public void addCaretListener(CaretListener e) { }
        public void removeCaretListener(CaretListener e) { }

        // query/Graphics methods
        public String getText() { return ""; }
        public Bounds getBounds(Graphics g) {
            return comp.getBounds();
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
            if(isPressed && isInside(e.getX(), e.getY())) {
                AbstractFlipFlop src = (AbstractFlipFlop) comp.getFactory();
                StateData myState = (StateData) state.getData(comp);
                if(myState == null) return;

                myState.curValue = myState.curValue.not();
                state.setValue(comp.getLocation(),
                    myState.curValue, comp, 1);
                state.setValue(comp.getEndLocation(src.getNumInputs() + 2),
                    myState.curValue.not(), comp, 1);
            }
            isPressed = false;
        }
        public void keyPressed(KeyEvent e) { }
        public void keyReleased(KeyEvent e) { }
        public void keyTyped(KeyEvent e) { }

        private boolean isInside(int x, int y) {
            Location loc = comp.getLocation();
            int dx = x - (loc.getX() - 20);
            int dy = y - (loc.getY() + 10);
            int d2 = dx * dx + dy * dy;
            return d2 < 8 * 8;
        }
    }

    private class Comp extends ManagedComponent
            implements Pokable, ToolTipMaker, Loggable {
        private AbstractFlipFlop src;

        public Comp(Location loc, AttributeSet attrs, AbstractFlipFlop src) {
            super(loc, attrs, 4);
            this.src = src;

            Location pt = getLocation();
            BitWidth w = BitWidth.ONE;
            int n = getNumInputs();
            if(n == 1) {
                setEnd(0, pt.translate(-40, 20), w, EndData.INPUT_ONLY);
                setEnd(1, pt.translate(-40,  0), w, EndData.INPUT_ONLY);
            } else if(n == 2) {
                setEnd(0, pt.translate(-40,  0), w, EndData.INPUT_ONLY);
                setEnd(1, pt.translate(-40, 20), w, EndData.INPUT_ONLY);
                setEnd(2, pt.translate(-40, 10), w, EndData.INPUT_ONLY);
            } else {
                throw new RuntimeException("flip-flop input > 1");
            }
            setEnd(n + 1, pt,                    w, EndData.OUTPUT_ONLY);
            setEnd(n + 2, pt.translate(  0, 20), w, EndData.OUTPUT_ONLY);
            setEnd(n + 3, pt.translate(-10, 30), w, EndData.INPUT_ONLY);
            setEnd(n + 4, pt.translate(-30, 30), w, EndData.INPUT_ONLY);
        }

        public ComponentFactory getFactory() {
            return src;
        }
        
        public void propagate(CircuitState state) {
            boolean changed = false;
            StateData myState = (StateData) state.getData(this);
            if(myState == null) {
                changed = true;
                myState = new StateData();
                state.setData(this, myState);
            }

            int n = getNumInputs();
            Value lastClock = myState.lastClock;
            Value clock = state.getValue(getEndLocation(n));
            myState.lastClock = clock;

            if(state.getValue(getEndLocation(n + 3)) == Value.TRUE) {
                // clear pin is set: clear everything
                changed |= myState.curValue != Value.FALSE;
                myState.curValue = Value.FALSE;
            } else if(state.getValue(getEndLocation(n + 4)) == Value.TRUE) {
                changed |= myState.curValue != Value.TRUE;
                myState.curValue = Value.TRUE;
            } else {
                // it is not being cleared: Check for clock's state
                if(lastClock == Value.FALSE && clock == Value.TRUE) {
                    Value[] inputs = new Value[n];
                    for(int i = 0; i < n; i++) {
                        inputs[i] = state.getValue(getEndLocation(i));
                    }

                    Value newVal = computeValue(inputs, myState.curValue);
                    if(newVal == Value.TRUE || newVal == Value.FALSE) {
                        changed |= myState.curValue != newVal;
                        myState.curValue = newVal;
                    }
                }
            }

            Location loc = getLocation();
            if(changed || !loc.equals(myState.propLocation)) {
                myState.propLocation = loc;
                state.setValue(getEndLocation(n + 1),
                        myState.curValue,
                        this, Memory.DELAY);
                state.setValue(getEndLocation(n + 2),
                        myState.curValue.not(),
                        this, Memory.DELAY);
            }
        }
        
        //
        // user interface methods
        //

        public void draw(ComponentDrawContext context) {
            Graphics g = context.getGraphics();
            AbstractFlipFlop src = (AbstractFlipFlop) getFactory();
            Location loc = getLocation();
            Bounds bds = getBounds();
            context.drawRectangle(bds.getX(), bds.getY(),
                bds.getWidth(), bds.getHeight(), "");
            if(context.getShowState()) {
                StateData myState = (StateData) context.getCircuitState().getData(this);
                if(myState != null) {
                    int x = loc.getX();
                    int y = loc.getY();
                    g.setColor(myState.curValue.getColor());
                    g.fillOval(x - 26, y + 4, 13, 13);
                    g.setColor(Color.WHITE);
                    GraphicsUtil.drawCenteredText(g,
                        myState.curValue.toDisplayString(), x - 19, y + 9);
                    g.setColor(Color.BLACK);
                }
            }
            int n = src.getNumInputs();
            for(int i = 0; i < n; i++) {
                context.drawPin(this, i, src.getInputName(i),
                        Direction.EAST);
            }
            context.drawClock(this, n, Direction.EAST);
            context.drawPin(this, n + 1, "Q", Direction.WEST);
            context.drawPin(this, n + 2);
            context.drawPin(this, n + 3);
            context.drawPin(this, n + 4);
        }
        
        public Object getFeature(Object key) {
            if(key == Pokable.class) return this;
            if(key == Loggable.class) return this;
            if(key == ToolTipMaker.class) return this;
            return super.getFeature(key);
        }

        public Caret getPokeCaret(ComponentUserEvent event) {
            if(getBounds().contains(event.getX(), event.getY())) {
                PokeCaret ret = new PokeCaret(this, event.getCircuitState());
                ret.isPressed = ret.isInside(event.getX(), event.getY());
                return ret;
            } else {
                return null;
            }
        }

        public String getToolTip(ComponentUserEvent e) {
            int end = -1;
            for(int i = getEnds().size() - 1; i >= 0; i--) {
                if(getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10) {
                    end = i;
                    break;
                }
            }
            int n = getNumInputs();
            if(end == n) {
                return Strings.get("flipFlopClockTip");
            } else if(end == n + 1) {
                return Strings.get("flipFlopQTip");
            } else if(end == n + 2) {
                return Strings.get("flipFlopNotQTip");
            } else if(end == n + 3) {
                return Strings.get("flipFlopResetTip");
            } else if(end == n + 4) {
                return Strings.get("flipFlopPresetTip");
            } else {
                return null;
            }
        }

        public Object[] getLogOptions(CircuitState state) {
            return null;
        }

        public String getLogName(Object option) {
            return null;
        }

        public Value getLogValue(CircuitState state, Object option) {
            StateData s = (StateData) state.getData(this);
            return s == null ? Value.FALSE : s.curValue;
        }
    }

    private String name;
    private StringGetter desc;
    private Bounds offsetBounds = Bounds.create(-40, -10, 40, 40);

    protected AbstractFlipFlop(String name, StringGetter desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() { return name; }

    public String getDisplayName() { return desc.get(); }

    public AttributeSet createAttributeSet() {
        return AttributeSets.EMPTY;
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new Comp(loc, attrs, this);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return offsetBounds;
    }
    
    //
    // user interface methods
    //
    public void drawGhost(ComponentDrawContext context,
            Color color, int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        g.setColor(color);
        Bounds bds = getOffsetBounds(attrs);
        context.drawRectangle(this,
            x + bds.getX(), y + bds.getY(),
            bds.getWidth(), bds.getHeight(),
            "");
    }

    public abstract void paintIcon(ComponentDrawContext context,
            int x, int y, AttributeSet attrs);

    //
    // protected methods intended to be overridden
    //
    protected abstract int getNumInputs();

    protected abstract String getInputName(int index);

    protected abstract Value computeValue(Value[] inputs,
            Value curValue);
}
