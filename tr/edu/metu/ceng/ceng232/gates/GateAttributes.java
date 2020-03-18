/*
 * This library is a copy of LOGISIM Gates library, except:
 *     - ParityGates are removed
 *     - All gates are defaulted to bitwidth 1 and number of inputs 2 (with no ability to change)
 *     - All gates are defaulted to narrow
 *
 * Modified by Kerem Hadimli <kerem@ceng.metu.edu.tr>
 *
 */
/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.IntegerFactory;

class GateAttributes extends AbstractAttributeSet {
    static final int MAX_INPUTS = 9;
    static final int DELAY = 1;
    
    public static final Attribute facing_attr
        = Attributes.forDirection("facing", Strings.getter("gateFacingAttr"));
    
    public static final Attribute width_attr
        = Attributes.forBitWidth("width", 1,1/*Strings.getter("gateBitWidthAttr")*/);

    static final AttributeOption SIZE_NARROW
        = new AttributeOption(IntegerFactory.create(30),
            Strings.getter("gateSizeNarrowOpt"));
    static final AttributeOption SIZE_WIDE
        = new AttributeOption(IntegerFactory.create(50),
            Strings.getter("gateSizeWideOpt"));
    public static final Attribute size_attr
        = Attributes.forOption("size", Strings.getter("gateSizeAttr"),
            new AttributeOption[] { SIZE_NARROW, SIZE_WIDE });

    private static final Integer INPUTS_2 = IntegerFactory.create(2);
    private static final Integer INPUTS_3 = IntegerFactory.create(3);
    private static final Integer INPUTS_5 = IntegerFactory.create(5);
    private static final Integer INPUTS_7 = IntegerFactory.create(7);
    private static final Integer INPUTS_9 = IntegerFactory.create(9);
    public static final Attribute inputs_attr
        = Attributes.forOption("inputs", Strings.getter("gateInputsAttr"),
            new Object[] { INPUTS_2 /*, INPUTS_3, INPUTS_5, INPUTS_7, INPUTS_9*/ });

    private static final List ATTRIBUTES = Arrays.asList(new Attribute[] {
            facing_attr, width_attr, size_attr, inputs_attr
    });

    AbstractGate gate = null;
    Direction facing = Direction.EAST;
    BitWidth width = BitWidth.ONE;
    AttributeOption size = SIZE_NARROW;
    int inputs = 2;
    
    GateAttributes() { }

    protected void copyInto(AbstractAttributeSet destObj) {
        GateAttributes dest = (GateAttributes) destObj;
        dest.gate = null;
    }

    public List getAttributes() {
        return ATTRIBUTES;
    }

    public Object getValue(Attribute attr) {
        if(attr == facing_attr) return facing;
        if(attr == width_attr) return width;
        if(attr == size_attr) return size;
        if(attr == inputs_attr) return IntegerFactory.create(inputs);
        return null;
    }
    
    public boolean isReadOnly(Attribute attr) {
        if(gate != null) {
            return attr == facing_attr || attr == size_attr || attr == inputs_attr;
        } else {
            return false;
        }
    }

    public void setValue(Attribute attr, Object value) {
        if(attr == width_attr) {
            width = (BitWidth) value;
        } else if(attr == facing_attr) {
            facing = (Direction) value;
        } else if(attr == size_attr) {
            size = (AttributeOption) value;
        } else if(attr == inputs_attr) {
            inputs = ((Integer) value).intValue();
        } else {
            throw new IllegalArgumentException("unrecognized argument");
        }
        if(gate != null) gate.attributeValueChanged(attr, value);
        fireAttributeValueChanged(attr, value);
    }
}
