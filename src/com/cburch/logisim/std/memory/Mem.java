/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JPopupMenu;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

abstract class Mem extends ManagedComponent
        implements AttributeListener, Pokable, MenuExtender, ToolTipMaker {
    // Note: The code is meant to be able to handle up to 32-bit addresses, but it
    // hasn't been debugged thoroughly. There are two definite changes I would
    // make if I were to extend the address bits: First, there would need to be some
    // modification to the memory's graphical representation, because there isn't
    // room in the box to include such long memory addresses with the current font
    // size. And second, I'd alter the MemContents class's PAGE_SIZE_BITS constant
    // to 14 so that its "page table" isn't quite so big.
    public static final Attribute ADDR_ATTR = Attributes.forBitWidth(
            "addrWidth", Strings.getter("ramAddrWidthAttr"), 2, 24);
    public static final Attribute DATA_ATTR = Attributes.forBitWidth(
            "dataWidth", Strings.getter("ramDataWidthAttr"));

    static final Bounds OFFSET_BOUNDS = Bounds.create(-140, -40, 140, 80);
    
    // pin-related constants
    static final int DATA = 0;

    static final int ADDR = 1;

    static final int CS = 2;

    // other constants

    static final int DELAY = 10;

    private class PokeCaret extends AbstractCaret {
        CircuitState circState;
        MemState state;
        int initValue;
        int curValue;

        PokeCaret(MemState state, CircuitState circState) {
            this.state = state;
            this.circState = circState;
            computeBounds();

            initValue = state.getContents().get(state.getCursor());
            curValue = initValue;
        }

        private void computeBounds() {
            setBounds(state.getBounds(state.getCursor(), Mem.this.getBounds()));
        }

        public void draw(Graphics g) {
            Bounds bds = getBounds(g);
            g.setColor(Color.RED);
            g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
            g.setColor(Color.BLACK);
        }

        public void stopEditing() {
            state.setCursor(-1);
        }

        public void cancelEditing() {
            state.getContents().set(state.getCursor(), initValue);
            state.setCursor(-1);
        }

        public void keyTyped(KeyEvent e) {
            char c = e.getKeyChar();
            int val = Character.digit(e.getKeyChar(), 16);
            if(val >= 0) {
                curValue = curValue * 16 + val;
                state.getContents().set(state.getCursor(), curValue);
                Mem.this.propagate(circState);
            } else if(c == ' ' || c == '\t') {
                moveTo(state.getCursor() + 1);
            } else if(c == '\r' || c == '\n') {
                moveTo(state.getCursor() + state.getColumns());
            } else if(c == '\u0008' || c == '\u007f') {
                moveTo(state.getCursor() - 1);
            }
        }

        private void moveTo(long addr) {
            if(state.isValidAddr(addr)) {
                state.setCursor(addr);
                state.scrollToShow(addr);
                initValue = state.getContents().get(addr);
                curValue = initValue;
                computeBounds();
            }
        }
    }

    private class AddrCaret extends AbstractCaret {
        CircuitState circState;
        MemState state;

        AddrCaret(MemState state, CircuitState circState) {
            this.state = state;
            this.circState = circState;

            setBounds(state.getBounds(-1, Mem.this.getBounds()));
        }

        public void draw(Graphics g) {
            Bounds bds = getBounds(g);
            g.setColor(Color.RED);
            g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
            g.setColor(Color.BLACK);
        }

        public void keyTyped(KeyEvent e) {
            char c = e.getKeyChar();
            int val = Character.digit(e.getKeyChar(), 16);
            if(val >= 0) {
                long newScroll = (state.getScroll() * 16 + val) & (state.getLastAddress());
                state.setScroll(newScroll);
            } else if(c == ' ') {
                state.setScroll(state.getScroll() + (state.getRows() - 1) * state.getColumns());
            } else if(c == '\r' || c == '\n') {
                state.setScroll(state.getScroll() + state.getColumns());
            } else if(c == '\u0008' || c == '\u007f') {
                state.setScroll(state.getScroll() - state.getColumns());
            }
        }
    }
    
    class MemListener implements HexModelListener {
        public void metainfoChanged(HexModel source) { }

        public void bytesChanged(HexModel source, long start,
                long numBytes, int[] values) {
            Mem.this.fireComponentInvalidated(new ComponentEvent(Mem.this));
        }
    }

    private File currentImageFile = null;

    Mem(Location loc, AttributeSet attrs, int numInputs) {
        super(loc, attrs, numInputs);
        attrs.addAttributeListener(this);
        setPins();
    }

    void setPins() {
        Location loc = getLocation();
        BitWidth addrBits = (BitWidth) getAttributeSet().getValue(ADDR_ATTR);
        BitWidth dataBits = (BitWidth) getAttributeSet().getValue(DATA_ATTR);

        setEnd(DATA, loc, dataBits, EndData.INPUT_OUTPUT);
        setEnd(ADDR, loc.translate(-140, 0), addrBits, EndData.INPUT_ONLY);
        setEnd(CS, loc.translate(-90, 40), BitWidth.ONE, EndData.INPUT_ONLY);
    }

    public abstract ComponentFactory getFactory();

    public abstract void propagate(CircuitState state);

    public void attributeListChanged(AttributeEvent e) { }

    public void attributeValueChanged(AttributeEvent e) {
        Attribute attr = e.getAttribute();
        if(attr == ADDR_ATTR || attr == DATA_ATTR) {
            setPins();
        }
    }

    //
    // user interface methods
    //
    public void draw(ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        Bounds bds = getBounds();

        // draw boundary
        context.drawBounds(this);

        // draw contents
        if(context.getShowState()) {
            MemState state = getState(context.getCircuitState());
            state.paint(context.getGraphics(), bds.getX(), bds.getY());
        } else {
            BitWidth addr = (BitWidth) getAttributeSet().getValue(ADDR_ATTR);
            int addrBits = addr.getWidth();
            int bytes = 1 << addrBits;
            String label;
            if(this instanceof Rom) {
                if(addrBits >= 30) {
                    label = StringUtil.format(Strings.get("romGigabyteLabel"), ""
                            + (bytes >>> 30));
                } else if(addrBits >= 20) {
                    label = StringUtil.format(Strings.get("romMegabyteLabel"), ""
                            + (bytes >> 20));
                } else if(addrBits >= 10) {
                    label = StringUtil.format(Strings.get("romKilobyteLabel"), ""
                            + (bytes >> 10));
                } else {
                    label = StringUtil.format(Strings.get("romByteLabel"), ""
                            + bytes);
                }
            } else {
                if(addrBits >= 30) {
                    label = StringUtil.format(Strings.get("ramGigabyteLabel"), ""
                            + (bytes >>> 30));
                } else if(addrBits >= 20) {
                    label = StringUtil.format(Strings.get("ramMegabyteLabel"), ""
                            + (bytes >> 20));
                } else if(addrBits >= 10) {
                    label = StringUtil.format(Strings.get("ramKilobyteLabel"), ""
                            + (bytes >> 10));
                } else {
                    label = StringUtil.format(Strings.get("ramByteLabel"), ""
                            + bytes);
                }
            }
            GraphicsUtil.drawCenteredText(g, label, bds.getX() + bds.getWidth()
                    / 2, bds.getY() + bds.getHeight() / 2);
        }

        // draw input and output pins
        context.drawPin(this, DATA, Strings.get("ramDataLabel"), Direction.WEST);
        context.drawPin(this, ADDR, Strings.get("ramAddrLabel"), Direction.EAST);
        g.setColor(Color.GRAY);
        context.drawPin(this, CS, Strings.get("ramCSLabel"), Direction.SOUTH);
    }

    public Object getFeature(Object key) {
        if(key == Pokable.class) return this;
        if(key == MenuExtender.class) return this;
        if(key == ToolTipMaker.class) return this;
        return super.getFeature(key);
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        Bounds bds = getBounds();
        CircuitState circState = event.getCircuitState();
        MemState state = getState(circState);
        long addr = state.getAddressAt(event.getX() - bds.getX(),
                event.getY() - bds.getY());

        // See if outside box
        if(addr < 0) {
            return new AddrCaret(state, circState);
        } else {
            state.setCursor(addr);
            return new PokeCaret(state, circState);
        }
    }

    public void configureMenu(JPopupMenu menu, Project proj) {
        menu.addSeparator();
        MemMenu compMenu = new MemMenu(proj, this);
        compMenu.appendTo(menu);
    }

    public abstract String getToolTip(ComponentUserEvent e);

    File getCurrentImage() {
        return currentImageFile;
    }
    
    void setCurrentImage(File value) {
        currentImageFile = value;
    }

    abstract MemState getState(CircuitState state);
    abstract HexFrame getHexFrame(Project proj, CircuitState state);
}
