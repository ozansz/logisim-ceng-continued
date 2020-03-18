/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorUtil {
    public static Iterator EMPTY_ITERATOR = new EmptyIterator();

    private static class EmptyIterator implements Iterator {
        private EmptyIterator() { }
        public Object next() { throw new NoSuchElementException(); }
        public boolean hasNext() { return false; }
        public void remove() {
            throw new UnsupportedOperationException("EmptyIterator.remove");
        }
    }

    private static class UnitIterator implements Iterator {
        private Object data;
        private boolean taken = false;

        private UnitIterator(Object data) { this.data = data; }

        public Object next() {
            if(taken) throw new NoSuchElementException();
            taken = true;
            return data;
        }

        public boolean hasNext() {
            return !taken;
        }

        public void remove() {
            throw new UnsupportedOperationException("UnitIterator.remove");
        }
    }

    private static class ArrayIterator implements Iterator {
        private Object[] data;
        private int i = -1;

        private ArrayIterator(Object[] data) { this.data = data; }

        public Object next() {
            if(!hasNext()) throw new NoSuchElementException();
            i++;
            return data[i];
        }

        public boolean hasNext() {
            return i + 1 < data.length;
        }

        public void remove() {
            throw new UnsupportedOperationException("ArrayIterator.remove");
        }
    }

    private static class IteratorUnion implements Iterator {
        Iterator cur;
        Iterator next;

        private IteratorUnion(Iterator cur, Iterator next) {
            this.cur = cur;
            this.next = next;
        }

        public Object next() {
            if(!cur.hasNext()) {
                if(next == null) throw new NoSuchElementException();
                cur = next;
                if(!cur.hasNext()) throw new NoSuchElementException();
            }
            return cur.next();
        }

        public boolean hasNext() {
            return cur.hasNext() || (next != null && next.hasNext());
        }

        public void remove() {
            cur.remove();
        }
    }

    public static Iterator createEmptyIterator() {
        return EMPTY_ITERATOR;
    }

    public static Iterator createUnitIterator(Object data) {
        return new UnitIterator(data);
    }

    public static Iterator createArrayIterator(Object[] data) {
        return new ArrayIterator(data);
    }

    public static Iterator createJoinedIterator(Iterator i0,
            Iterator i1) {
        if(!i0.hasNext()) return i1;
        if(!i1.hasNext()) return i0;
        return new IteratorUnion(i0, i1);
    }

}
