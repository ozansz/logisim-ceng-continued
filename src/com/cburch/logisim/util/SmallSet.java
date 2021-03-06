/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SmallSet extends AbstractSet {
    private static final int HASH_POINT = 4;

    private class ArrayIterator implements Iterator {
        int itVersion = version;
        int pos = 0; // position of next item to return
        boolean hasNext = true;
        boolean removeOk = false;
        
        public boolean hasNext() {
            return hasNext;
        }
        
        public Object next() {
            if(itVersion != version) {
                throw new ConcurrentModificationException();
            } else if(!hasNext) {
                throw new NoSuchElementException();
            } else if(size == 1) {
                pos = 1;
                hasNext = false;
                removeOk = true;
                return values;
            } else {
                Object ret = ((Object[]) values)[pos];
                ++pos;
                hasNext = pos < size;
                removeOk = true;
                return ret;
            }
        }
        
        public void remove() {
            if(itVersion != version) {
                throw new ConcurrentModificationException();
            } else if(!removeOk) {
                throw new IllegalStateException();
            } else if(size == 1) {
                values = null;
                size = 0;
                ++version;
                itVersion = version;
                removeOk = false;
            } else {
                Object[] vals = (Object[]) values;
                if(size == 2) {
                    values = (pos == 2 ? vals[0] : vals[1]);
                    size = 1;
                } else {
                    for(int i = pos; i < size; i++) {
                        vals[i - 1] = vals[i];
                    }
                    --pos;
                    --size;
                    vals[size] = null;
                }
                ++version;
                itVersion = version;
                removeOk = false;
            }
        }
    }
    
    private int size = 0;
    private int version = 0;
    private Object values = null;
    
    public SmallSet() { }
    
    public Object clone() {
        SmallSet ret = new SmallSet();
        ret.size = this.size;
        if(size == 1) {
            ret.values = this.values;
        } else if(size <= HASH_POINT) {
            Object[] oldVals = (Object[]) this.values;
            Object[] retVals = new Object[size];
            for(int i = size - 1; i >= 0; i--) retVals[i] = oldVals[i];
        } else {
            HashSet oldVals = (HashSet) this.values;
            values = oldVals.clone();
        }
        return ret;
    }
    
    public Object[] toArray() {
        if(size == 1) {
            return new Object[] { this.values };
        } else if(size <= HASH_POINT) {
            Object[] ret = new Object[size];
            System.arraycopy((Object[]) this.values, 0, ret, 0, size);
            return ret;
        } else {
            HashSet hash = (HashSet) this.values;
            return hash.toArray();
        }
    }
    
    public void clear() {
        size = 0;
        values = null;
        ++version;
    }
    
    public boolean isEmpty() {
        if(size <= HASH_POINT) {
            return size == 0;
        } else {
            return ((HashSet) values).isEmpty();
        }
    }

    public int size() {
        if(size <= HASH_POINT) {
            return size;
        } else {
            return ((HashSet) values).size();
        }
    }
    
    public boolean add(Object value) {
        if(size < 2) {
            if(size == 0) {
                values = value;
                size = 1;
                ++version;
                return true;
            } else {
                if(values.equals(value)) {
                    return false;
                } else {
                    Object[] newValues = new Object[HASH_POINT];
                    newValues[0] = values;
                    newValues[1] = value;
                    values = newValues;
                    size = 2;
                    ++version;
                    return true;
                }
            }
        } else if(size <= HASH_POINT) {
            Object[] vals = (Object[]) values;
            for(int i = 0; i < size; i++) {
                if(vals[i].equals(value)) return false;
            }
            if(size < HASH_POINT) {
                vals[size] = value;
                ++size;
                ++version;
                return true;
            } else {
                HashSet newValues = new HashSet();
                for(int i = 0; i < size; i++) newValues.add(vals[i]);
                newValues.add(value);
                values = newValues;
                ++size;
                ++version;
                return true;
            }
        } else {
            HashSet vals = (HashSet) values;
            if(vals.add(value)) {
                ++version;
                return true;
            } else {
                return false;
            }
        }
    }
    
    public boolean contains(Object value) {
        if(size <= 2) {
            if(size == 0) {
                return false;
            } else {
                return values.equals(value);
            }
        } else if(size <= HASH_POINT) {
            Object[] vals = (Object[]) values;
            for(int i = 0; i < size; i++) {
                if(vals[i].equals(value)) return true;
            }
            return false;
        } else {
            HashSet vals = (HashSet) values;
            return vals.contains(value);
        }
    }

    public Iterator iterator() {
        if(size <= HASH_POINT) {
            if(size == 0) {
                return IteratorUtil.EMPTY_ITERATOR;
            } else {
                return new ArrayIterator();
            }
        } else {
            return ((HashSet) values).iterator();
        }
    }
    
    public static void main(String[] args) throws java.io.IOException {
        SmallSet set = new SmallSet();
        java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(System.in));
        while(true) {
            System.out.print(set.size() + ":"); //OK
            for(Iterator it = set.iterator(); it.hasNext(); ) {
                System.out.print(" " + it.next()); //OK
            }
            System.out.println(); //OK
            System.out.print("> "); //OK
            String cmd = in.readLine();
            if(cmd == null) break;
            cmd = cmd.trim();
            if(cmd.equals("")) {
                ;
            } else if(cmd.startsWith("+")) {
                set.add(cmd.substring(1));
            } else if(cmd.startsWith("-")) {
                set.remove(cmd.substring(1));
            } else if(cmd.startsWith("?")) {
                boolean ret = set.contains(cmd.substring(1));
                System.out.println("  " + ret); //OK
            } else {
                System.out.println("unrecognized command"); //OK
            }
        }
    }
}
