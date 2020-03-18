/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.IntegerFactory;

class SplitterAttributes extends AbstractAttributeSet {
    public static final Attribute facing_attr
        = Attributes.forDirection("facing", Strings.getter("splitterFacingAttr"));
    public static final Attribute width_attr
        = Attributes.forBitWidth("incoming", Strings.getter("splitterBitWidthAttr"));
    public static final Attribute fanout_attr
        = Attributes.forIntegerRange("fanout", Strings.getter("splitterFanOutAttr"), 1, 32);

    private static final List INIT_ATTRIBUTES = Arrays.asList(new Attribute[] {
        facing_attr, fanout_attr, width_attr 
    });

    private static final String unchosen_val = "none";

    private static class BitOutOption {
        int value;
        boolean isVertical;
        boolean isLast;

        BitOutOption(int value, boolean isVertical, boolean isLast) {
            this.value = value;
            this.isVertical = isVertical;
            this.isLast = isLast;
        }

        public String toString() {
            if(value < 0) {
                return Strings.get("splitterBitNone");
            } else {
                String ret = "" + value;
                if(value == 0) {
                    String note = isVertical ? Strings.get("splitterBitNorth")
                            : Strings.get("splitterBitEast");
                    ret += " (" + note + ")";
                } else if(isLast) {
                    String note = isVertical ? Strings.get("splitterBitSouth")
                            : Strings.get("splitterBitWest");
                    ret += " (" + note + ")";
                }
                return ret;
            }
        }
    }

    static class BitOutAttribute extends Attribute {
        int which;
        BitOutOption[] options;

        private BitOutAttribute(int which, BitOutOption[] options) {
            super("bit" + which, Strings.getter("splitterBitAttr", "" + which));
            this.which = which;
            this.options = options;
        }
        
        private BitOutAttribute createCopy() {
            return new BitOutAttribute(which, options);
        }

        public Object parse(String value) {
            if(value.equals(unchosen_val)) {
                return IntegerFactory.ZERO;
            } else {
                return IntegerFactory.create(1 + Integer.parseInt(value));
            }
        }

        public String toDisplayString(Object value) {
            int index = ((Integer) value).intValue();
            return options[index].toString();
        }

        public String toStandardString(Object value) {
            int index = ((Integer) value).intValue();
            if(index == 0) {
                return unchosen_val;
            } else {
                return "" + (index - 1);
            }
        }

        public java.awt.Component getCellEditor(Object value) {
            int index = ((Integer) value).intValue();
            javax.swing.JComboBox combo = new javax.swing.JComboBox(options);
            combo.setSelectedIndex(index);
            return combo;
        }
    }

    private ArrayList attrs = new ArrayList(INIT_ATTRIBUTES);
    boolean frozen;
    Direction facing = Direction.EAST;
    byte fanout = 2;                 // number of ends this splits into
    byte[] bit_end = new byte[2];    // how each bit maps to an end (0 if nowhere);
                                     //   other values will be between 1 and fanout
    BitOutOption[] options = null;

    SplitterAttributes() {
        configureOptions();
        configureDefaults();
    }

    protected void copyInto(AbstractAttributeSet destObj) {
        SplitterAttributes dest = (SplitterAttributes) destObj;
        
        dest.attrs = new ArrayList(this.attrs.size());
        dest.attrs.addAll(INIT_ATTRIBUTES);
        for(int i = INIT_ATTRIBUTES.size(), n = this.attrs.size(); i < n; i++) {
            BitOutAttribute attr = (BitOutAttribute) this.attrs.get(i);
            dest.attrs.add(attr.createCopy());
        }

        dest.frozen = this.frozen;
        dest.facing = this.facing;
        dest.fanout = this.fanout;
        dest.bit_end = (byte[]) this.bit_end.clone();
        dest.options = this.options;
    }

    public List getAttributes() {
        return attrs;
    }

    public Object getValue(Attribute attr) {
        if(attr == facing_attr) {
            return facing;
        } else if(attr == fanout_attr) {
            return IntegerFactory.create(fanout);
        } else if(attr == width_attr) {
            return BitWidth.create(bit_end.length);
        } else if(attr instanceof BitOutAttribute) {
            BitOutAttribute bitOut = (BitOutAttribute) attr;
            return IntegerFactory.create(bit_end[bitOut.which]);
        } else {
            return null;
        }
    }
    
    public boolean isReadOnly(Attribute attr) {
        if(frozen) {
            return attr == facing_attr || attr == fanout_attr;
        } else {
            return false;
        }
    }

    public void setValue(Attribute attr, Object value) {
        if(attr == facing_attr) {
            facing = (Direction) value;
            configureOptions();
        } else if(attr == fanout_attr) {
            fanout = (byte) ((Integer) value) .intValue();
            configureOptions();
            configureDefaults();
        } else if(attr == width_attr) {
            BitWidth width = (BitWidth) value;
            bit_end = new byte[width.getWidth()];
            configureOptions();
            configureDefaults();
        } else if(attr instanceof BitOutAttribute) {
            BitOutAttribute bitOutAttr = (BitOutAttribute) attr;
            int val;
            if(value instanceof Integer) {
                val = ((Integer) value).intValue();
            } else {
                val= ((BitOutOption) value).value + 1;
            }
            if(val >= 0 && val <= fanout) {
                bit_end[bitOutAttr.which] = (byte) val;
            }
        } else {
            throw new IllegalArgumentException("unknown attribute " + attr);
        }
        fireAttributeValueChanged(attr, value);
    }

    private void configureOptions() {
        // compute the set of options for BitOutAttributes
        options = new BitOutOption[fanout + 1];
        boolean isVertical = facing == Direction.EAST || facing == Direction.WEST;
        for(int i = -1; i < fanout; i++) {
            options[i + 1] = new BitOutOption(i, isVertical, i == fanout - 1);
        }

        // go ahead and set the options for the existing attributes
        int offs = INIT_ATTRIBUTES.size();
        int curNum = attrs.size() - offs;
        for(int i = 0; i < curNum; i++) {
            BitOutAttribute attr = (BitOutAttribute) attrs.get(offs + i);
            attr.options = options;
        }
    }
    
    private void configureDefaults() {
        int offs = INIT_ATTRIBUTES.size();
        int curNum = attrs.size() - offs;

        // compute default values
        byte[] dflt = new byte[bit_end.length];
        if(fanout >= bit_end.length) {
            for(int i = 0; i < bit_end.length; i++) dflt[i] = (byte) (i + 1);
        } else {
            int threads_per_end = dflt.length / fanout;
            int ends_with_extra = dflt.length % fanout;
            int cur_end = -1; // immediately increments
            int left_in_end = 0;
            for(int i = 0; i < dflt.length; i++) {
                if(left_in_end == 0) {
                    ++cur_end;
                    left_in_end = threads_per_end;
                    if(ends_with_extra > 0) {
                        ++left_in_end;
                        --ends_with_extra;
                    }
                }
                dflt[i] = (byte) (1 + cur_end);
                --left_in_end;
            }
        }

        boolean changed = curNum != bit_end.length;
        
        // remove excess attributes
        while(curNum > bit_end.length) {
            curNum--;
            attrs.remove(offs + curNum);
        }

        // set existing attributes
        for(int i = 0; i < curNum; i++) {
            if(bit_end[i] != dflt[i]) {
                BitOutAttribute attr = (BitOutAttribute) attrs.get(offs + i);
                bit_end[i] = dflt[i];
                fireAttributeValueChanged(attr, IntegerFactory.create(bit_end[i]));
            }
        }

        // add new attributes
        for(int i = curNum; i < bit_end.length; i++) {
            BitOutAttribute attr = new BitOutAttribute(i, options);
            bit_end[i] = dflt[i];
            attrs.add(attr);
        }
        
        if(changed) fireAttributeListChanged();
    }
}
