/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import javax.swing.JPopupMenu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Caret;

class Rom extends Mem {
    private static final int NUM_INPUTS = 3;
    
    private MemListener memListener = new MemListener();

    Rom(Location loc, AttributeSet attrs) {
        super(loc, attrs, NUM_INPUTS);
        
        MemContents contents = getMemContents();
        contents.addHexModelListener(memListener);
    }

    public ComponentFactory getFactory() {
        return RomFactory.INSTANCE;
    }

    public void propagate(CircuitState state) {
        MemState myState = getState(state);
        BitWidth dataBits = (BitWidth) getAttributeSet().getValue(DATA_ATTR);

        Value addrValue = state.getValue(getEndLocation(ADDR));
        boolean chipSelect = state.getValue(getEndLocation(CS)) != Value.FALSE;
        
        if(!chipSelect) {
            myState.setCurrent(-1);
            state.setValue(getEndLocation(DATA),
                    Value.createUnknown(dataBits), this, DELAY);
            return;
        }

        int addr = addrValue.toIntValue();
        if(!addrValue.isFullyDefined() || addr < 0)
            return;
        if(addr != myState.getCurrent()) {
            myState.setCurrent(addr);
            myState.scrollToShow(addr);
        }

        state.setValue(getEndLocation(DATA),
                Value.createKnown(dataBits, myState.getContents().get(addr)),
                this, DELAY);
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        Canvas canvas = event.getCanvas();
        if(canvas != null) {
            RomAttributes attrs = (RomAttributes) getAttributeSet();
            attrs.setProject(canvas.getProject());
        }
        return super.getPokeCaret(event);
    }

    public void configureMenu(JPopupMenu menu, Project proj) {
        RomAttributes attrs = (RomAttributes) getAttributeSet();
        attrs.setProject(proj);
        super.configureMenu(menu, proj);
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
        default:   return null;
        }
    }
    
    MemContents getMemContents() {
        return (MemContents) getAttributeSet().getValue(RomFactory.CONTENTS_ATTR);
    }

    MemState getState(CircuitState state) {
        MemState ret = (MemState) state.getData(this);
        if(ret == null) {
            MemContents contents = getMemContents();
            ret = new MemState(contents);
            state.setData(this, ret);
        }
        return ret;
    }

    HexFrame getHexFrame(Project proj, CircuitState state) {
        return RomAttributes.getHexFrame(getMemContents(), proj);
    }
}
