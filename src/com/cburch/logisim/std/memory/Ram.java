/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.IntegerFactory;

class Ram extends Mem {
    // pin-related constants
    private static final int NUM_INPUTS = 6;
    private static final int WE = 3;
    private static final int OE = 4;
    private static final int CLR = 5;

    private static Object[][] logOptions = new Object[9][];
    
    private static class RamState extends MemState
            implements AttributeListener {
        private Ram parent;
        private MemListener listener;
        private HexFrame hexFrame = null;
        private Value lastWE = Value.FALSE;

        RamState(Ram parent, MemContents contents, MemListener listener) {
            super(contents);
            this.parent = parent;
            this.listener = listener;
            if(parent != null) parent.getAttributeSet().addAttributeListener(this);
            contents.addHexModelListener(listener);
        }
        
        void setRam(Ram value) {
            if(parent == value) return;
            if(parent != null) parent.getAttributeSet().removeAttributeListener(this);
            parent = value;
            if(value != null) value.getAttributeSet().addAttributeListener(this);
        }
        
        public Object clone() {
            RamState ret = (RamState) super.clone();
            ret.parent = null;
            ret.getContents().addHexModelListener(listener);
            return ret;
        }
        
        // Retrieves a HexFrame for editing within a separate window
        public HexFrame getHexFrame(Project proj) {
            if(hexFrame == null) {
                hexFrame = new HexFrame(proj, getContents());
                hexFrame.addWindowListener(new WindowAdapter() {
                    public void windowClosed(WindowEvent e) {
                        hexFrame = null;
                    }
                });
            }
            return hexFrame;
        }
        
        //
        // methods for accessing the write-enable data
        //
        public Value getLastWriteEnable() {
            return lastWE;
        }
        
        public void setLastWriteEnable(Value value) {
            lastWE = value;
        }

        public void attributeListChanged(AttributeEvent e) { }

        public void attributeValueChanged(AttributeEvent e) {
            AttributeSet attrs = e.getSource();
            BitWidth addrBits = (BitWidth) attrs.getValue(Mem.ADDR_ATTR);
            BitWidth dataBits = (BitWidth) attrs.getValue(Mem.DATA_ATTR);
            getContents().setDimensions(addrBits.getWidth(), dataBits.getWidth());
        }
    }

    Ram(Location loc, AttributeSet attrs) {
        super(loc, attrs, NUM_INPUTS);
    }

    void setPins() {
        super.setPins();
        
        Location loc = getLocation();
        setEnd(WE, loc.translate(-70, 40), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(OE, loc.translate(-50, 40), BitWidth.ONE, EndData.INPUT_ONLY);
        setEnd(CLR, loc.translate(-30, 40), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public ComponentFactory getFactory() {
        return RamFactory.INSTANCE;
    }

    public void propagate(CircuitState state) {
        RamState myState = (RamState) getState(state);
        BitWidth dataBits = (BitWidth) getAttributeSet().getValue(DATA_ATTR);

        Value addrValue = state.getValue(getEndLocation(ADDR));
        boolean chipSelect = state.getValue(getEndLocation(CS)) != Value.FALSE;
        boolean writeEnabled = state.getValue(getEndLocation(WE)) == Value.TRUE;
        boolean outputEnabled = state.getValue(getEndLocation(OE)) != Value.FALSE;
        boolean shouldClear = state.getValue(getEndLocation(CLR)) == Value.TRUE;
        
        if(!chipSelect) {
            myState.setCurrent(-1);
            state.setValue(getEndLocation(DATA),
                    Value.createUnknown(dataBits), this, DELAY);
            return;
        }

        if(shouldClear) {
            myState.getContents().clear();
        }

        int addr = addrValue.toIntValue();
        if(!addrValue.isFullyDefined() || addr < 0)
            return;
        if(addr != myState.getCurrent()) {
            myState.setCurrent(addr);
            myState.scrollToShow(addr);
        }

        if(!shouldClear && !outputEnabled && writeEnabled
                && myState.getLastWriteEnable() == Value.FALSE) {
            Value dataValue = state.getValue(getEndLocation(DATA));
            myState.getContents().set(addr, dataValue.toIntValue());
        }
        myState.setLastWriteEnable(writeEnabled ? Value.TRUE : Value.FALSE);

        if(outputEnabled) {
            state.setValue(getEndLocation(DATA),
                    Value.createKnown(dataBits, myState.getContents().get(addr)),
                    this, DELAY);
        } else {
            state.setValue(getEndLocation(DATA),
                    Value.createUnknown(dataBits), this, DELAY);
        }
    }

    //
    // user interface methods
    //
    public void draw(ComponentDrawContext context) {
        super.draw(context);
        context.drawClock(this, WE, Direction.NORTH);
        context.drawPin(this, OE, Strings.get("ramOELabel"), Direction.SOUTH);
        context.drawPin(this, CLR, Strings.get("ramClrLabel"), Direction.SOUTH);
    }

    public Object getFeature(Object key) {
        if(key == Loggable.class) return this;
        return super.getFeature(key);
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
        case DATA: return Strings.get("memDataTip");
        case ADDR: return Strings.get("memAddrTip");
        case CS:   return Strings.get("memCSTip");
        case WE:   return Strings.get("ramWETip");
        case OE:   return Strings.get("ramOETip");
        case CLR:  return Strings.get("ramClrTip");
        default:   return null;
        }
    }

    MemState getState(CircuitState state) {
        BitWidth addrBits = (BitWidth) getAttributeSet().getValue(ADDR_ATTR);
        BitWidth dataBits = (BitWidth) getAttributeSet().getValue(DATA_ATTR);

        RamState myState = (RamState) state.getData(this);
        if(myState == null) {
            MemContents contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth());
            myState = new RamState(this, contents, new MemListener());
            state.setData(this, myState);
        } else {
            myState.setRam(this);
        }
        return myState;
    }

    public Object[] getLogOptions(CircuitState state) {
        int addrBits = ((BitWidth) getAttributeSet().getValue(ADDR_ATTR)).getWidth();
        if(addrBits >= logOptions.length) addrBits = logOptions.length - 1;
        synchronized(logOptions) {
            Object[] ret = logOptions[addrBits];
            if(ret == null) {
                ret = new Object[1 << addrBits];
                logOptions[addrBits] = ret;
                for(int i = 0; i < ret.length; i++) {
                    ret[i] = IntegerFactory.create(i);
                }
            }
            return ret;
        }
    }

    public String getLogName(Object option) {
        if(option instanceof Integer) {
            return getFactory().getDisplayName() + getLocation()
                + "[" + option + "]";
        } else {
            return null;
        }
    }

    public Value getLogValue(CircuitState state, Object option) {
        if(option instanceof Integer) {
            MemState s = getState(state);
            int addr = ((Integer) option).intValue();
            return Value.createKnown(BitWidth.create(s.getDataBits()),
                    s.getContents().get(addr));
        } else {
            return Value.NIL;
        }
    }

    HexFrame getHexFrame(Project proj, CircuitState circState) {
        RamState state = (RamState) getState(circState);
        return state.getHexFrame(proj);
    }

}
