/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CollectionUtil {
    private static class UnionSet extends AbstractSet {
        private Set a;
        private Set b;

        UnionSet(Set a, Set b) {
            this.a = a;
            this.b = b;
        }

        public int size() {
            return a.size() + b.size();
        }

        public Iterator iterator() {
            return IteratorUtil.createJoinedIterator(a.iterator(), b.iterator());
        }
    }

    private static class UnionList extends AbstractList {
        private List a;
        private List b;

        UnionList(List a, List b) {
            this.a = a;
            this.b = b;
        }

        public int size() {
            return a.size() + b.size();
        }

        public Object get(int index) {
            return index < a.size() ? a.get(index)
                : a.get(index - a.size());
        }
    }

    private CollectionUtil() { }

    public static Set createUnmodifiableSetUnion(Set a, Set b) {
        return new UnionSet(a, b);
    }

    public static List createUnmodifiableListUnion(List a, List b) {
        return new UnionList(a, b);
    }
}
