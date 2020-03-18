/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;

class CircuitAttributes extends AbstractAttributeSet {
    public static final Attribute FACING_ATTR = Pin.facing_attr;
    public static final Attribute NAME_ATTR = Attributes.forString("circuit", Strings.getter("circuitName"));

    private static final List ATTRIBUTES = Arrays.asList(new Attribute[] { FACING_ATTR, NAME_ATTR });

    private Circuit source;
    private Subcircuit comp;
    private Direction facing = Direction.EAST;
    
    public CircuitAttributes(Circuit source) {
        this.source = source;
    }
    
    void setSubcircuit(Subcircuit value) {
        comp = value;
    }
    
    public Direction getFacing() {
        return facing;
    }

    protected void copyInto(AbstractAttributeSet dest) {
        CircuitAttributes other = (CircuitAttributes) dest;
        other.comp = null;
    }
    
    public boolean isReadOnly(Attribute attr) {
        return comp != null;
    }

    public List getAttributes() {
        return ATTRIBUTES;
    }

    public Object getValue(Attribute attr) {
        if(attr == FACING_ATTR) return facing;
        if(attr == NAME_ATTR) return source.getName();
        return null;
    }

    public void setValue(Attribute attr, Object value) {
        if(attr == FACING_ATTR) {
            facing = (Direction) value;
            fireAttributeValueChanged(FACING_ATTR, value);
        }
    }
}
