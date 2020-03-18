/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

public class ListUtil {
    private static class JoinedList extends AbstractList {
        List a;
        List b;

        JoinedList(List a, List b) {
            this.a = a;
            this.b = b;
        }

        public int size() {
            return a.size() + b.size();
        }

        public Object get(int index) {
            if(index < a.size())    return a.get(index);
            else                    return b.get(index - a.size());
        }

        public Iterator iterator() {
            return IteratorUtil.createJoinedIterator(a.iterator(),
                b.iterator());
        }
                
    }

    private ListUtil() { }

    public static List joinImmutableLists(List a, List b) {
        return new JoinedList(a, b);
    }
}
