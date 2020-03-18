/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;

class ProbeAttributes extends AbstractAttributeSet {
    public static ProbeAttributes instance = new ProbeAttributes();

    static final Font labelfont_dflt = new Font("SansSerif", Font.PLAIN, 12);

    private static final List ATTRIBUTES = Arrays.asList(new Attribute[] {
        Pin.facing_attr, Pin.label_attr, Pin.labelloc_attr, Pin.labelfont_attr,
        RadixOption.ATTRIBUTE
    });

    Probe component = null;
    Direction facing = Direction.EAST;
    String label = "";
    Direction labelloc = Direction.WEST;
    Font labelfont = labelfont_dflt;
    RadixOption radix = RadixOption.RADIX_2;

    public ProbeAttributes() { }

    protected void copyInto(AbstractAttributeSet destObj) {
        ProbeAttributes dest = (ProbeAttributes) destObj;
        dest.component = null;
    }

    public List getAttributes() {
        return ATTRIBUTES;
    }

    public Object getValue(Attribute attr) {
        if(attr == Pin.facing_attr) return facing;
        if(attr == Pin.label_attr) return label;
        if(attr == Pin.labelloc_attr) return labelloc;
        if(attr == Pin.labelfont_attr) return labelfont;
        if(attr == RadixOption.ATTRIBUTE) return radix;
        return null;
    }

    public void setValue(Attribute attr, Object value) {
        if(attr == Pin.facing_attr) {
            facing = (Direction) value;
        } else if(attr == Pin.label_attr) {
            label = (String) value;
        } else if(attr == Pin.labelloc_attr) {
            labelloc = (Direction) value;
        } else if(attr == Pin.labelfont_attr) {
            labelfont = (Font) value;
        } else if(attr == RadixOption.ATTRIBUTE) {
            radix = (RadixOption) value;
        } else {
            throw new IllegalArgumentException("unknown attribute");
        }
        if(component != null) component.attributeValueChanged(this, attr, value);
        fireAttributeValueChanged(attr, value);
    }
}


