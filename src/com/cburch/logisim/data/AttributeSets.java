/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AttributeSets {
    private AttributeSets() { }
    
    public static final AttributeSet EMPTY = new AttributeSet() {
        public Object clone() { return this; }
        public void addAttributeListener(AttributeListener l) { }
        public void removeAttributeListener(AttributeListener l) { }

        public List getAttributes() { return Collections.EMPTY_LIST; }
        public boolean containsAttribute(Attribute attr) { return false; }
        public Attribute getAttribute(String name) { return null; }

        public boolean isReadOnly(Attribute attr) { return true; }
        public void setReadOnly(Attribute attr, boolean value) {
            throw new UnsupportedOperationException();
        }

        public Object getValue(Attribute attr) { return null; }
        public void setValue(Attribute attr, Object value) { }
    };
    
    public static AttributeSet fixedSet(Attribute attr, Object initValue) {
        return new SingletonSet(attr, initValue);
    }

    public static AttributeSet fixedSet(Attribute[] attrs, Object[] initValues) {
        if(attrs.length > 1) {
            return new FixedSet(attrs, initValues);
        } else if(attrs.length == 1) {
            return new SingletonSet(attrs[0], initValues[0]);
        } else {
            return EMPTY;
        }
    }
    
    public static void copy(AttributeSet src, AttributeSet dst) {
        if(src == null || src.getAttributes() == null) return;
        for(Iterator it = src.getAttributes().iterator(); it.hasNext(); ) {
            Attribute attr = (Attribute) it.next();
            Object value = src.getValue(attr);
            dst.setValue(attr, value);
        }
    }

    private static class SingletonSet extends AbstractAttributeSet {
        private List attrs;
        private Object value;
        private boolean readOnly = false;
        
        SingletonSet(Attribute attr, Object initValue) {
            this.attrs = Collections.singletonList(attr);
            this.value = initValue;
        }

        protected void copyInto(AbstractAttributeSet destSet) {
            SingletonSet dest = (SingletonSet) destSet;
            dest.attrs = this.attrs;
            dest.value = this.value;
            dest.readOnly = this.readOnly;
        }

        public List getAttributes() {
            return attrs;
        }
        
        public boolean isReadOnly(Attribute attr) {
            return readOnly;
        }
        
        public void setReadOnly(Attribute attr, boolean value) {
            int index = attrs.indexOf(attr);
            if(index < 0) throw new IllegalArgumentException("attribute absent");
            readOnly = value;
        }

        public Object getValue(Attribute attr) {
            int index = attrs.indexOf(attr);
            return index >= 0 ? value : null;
        }

        public void setValue(Attribute attr, Object value) {
            int index = attrs.indexOf(attr);
            if(index < 0) throw new IllegalArgumentException("attribute absent");
            if(readOnly) throw new IllegalArgumentException("read only");
            this.value = value;
            fireAttributeValueChanged(attr, value);
        }
    }
    
    private static class FixedSet extends AbstractAttributeSet {
        private List attrs;
        private Object[] values;
        private int readOnly = 0;
        
        FixedSet(Attribute[] attrs, Object[] initValues) {
            if(attrs.length != initValues.length) {
                throw new IllegalArgumentException("attribute and value arrays must have same length");
            }
            if(attrs.length > 32) {
                throw new IllegalArgumentException("cannot handle more than 32 attributes");
            }
            this.attrs = Arrays.asList(attrs);
            this.values = (Object[]) initValues.clone();
        }

        protected void copyInto(AbstractAttributeSet destSet) {
            FixedSet dest = (FixedSet) destSet;
            dest.attrs = this.attrs;
            dest.values = (Object[]) this.values.clone();
            dest.readOnly = this.readOnly;
        }

        public List getAttributes() {
            return attrs;
        }
        
        public boolean isReadOnly(Attribute attr) {
            int index = attrs.indexOf(attr);
            if(index < 0) return true;
            return isReadOnly(index);
        }
        
        public void setReadOnly(Attribute attr, boolean value) {
            int index = attrs.indexOf(attr);
            if(index < 0) throw new IllegalArgumentException("attribute absent");
            
            if(value) readOnly |= (1 << index);
            else readOnly &= ~(1 << index);
        }

        public Object getValue(Attribute attr) {
            int index = attrs.indexOf(attr);
            if(index < 0) {
                return null;
            } else {
                return values[index];
            }
        }

        public void setValue(Attribute attr, Object value) {
            int index = attrs.indexOf(attr);
            if(index < 0) throw new IllegalArgumentException("attribute absent");
            if(isReadOnly(index)) throw new IllegalArgumentException("read only");
            values[index] = value;
            fireAttributeValueChanged(attr, value);
        }
        
        private boolean isReadOnly(int index) {
            return ((readOnly >> index) & 1) == 1;
        }
    }

}
