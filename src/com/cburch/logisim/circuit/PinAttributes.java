/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;

class PinAttributes extends ProbeAttributes {
    public static PinAttributes instance = new PinAttributes();

    static final Font labelfont_dflt = new Font("SansSerif", Font.PLAIN, 12);

    private static final List ATTRIBUTES = Arrays.asList(new Attribute[] {
        Pin.facing_attr, Pin.type_attr, Pin.width_attr, Pin.threeState_attr,
        Pin.pull_attr, Pin.label_attr, Pin.labelloc_attr, Pin.labelfont_attr
    });

    BitWidth width = BitWidth.ONE;
    boolean threeState = true;
    int type = EndData.INPUT_ONLY;
    Object pull = Pin.pull_none;

    public PinAttributes() { }

    public List getAttributes() {
        return ATTRIBUTES;
    }

    public Object getValue(Attribute attr) {
        if(attr == Pin.width_attr) return width;
        if(attr == Pin.threeState_attr) return threeState ? Boolean.TRUE : Boolean.FALSE;
        if(attr == Pin.type_attr) return type == EndData.OUTPUT_ONLY ? Boolean.TRUE : Boolean.FALSE;
        if(attr == Pin.pull_attr) return pull;
        return super.getValue(attr);
    }

    public void setValue(Attribute attr, Object value) {
        if(attr == Pin.width_attr) {
            width = (BitWidth) value;
        } else if(attr == Pin.threeState_attr) {
            threeState = ((Boolean) value).booleanValue();
        } else if(attr == Pin.type_attr) {
            type = ((Boolean) value).booleanValue() ? EndData.OUTPUT_ONLY : EndData.INPUT_ONLY;
        } else if(attr == Pin.pull_attr) {
            pull = value;
        } else {
            super.setValue(attr, value);
            return;
        }
        if(component != null) component.attributeValueChanged(this, attr, value);
        fireAttributeValueChanged(attr, value);
    }
}


