/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Implicant implements Comparable {
    static Implicant MINIMAL_IMPLICANT = new Implicant(0, -1);
    static List MINIMAL_LIST = Arrays.asList(new Object[] { MINIMAL_IMPLICANT });

    private static class TermIterator implements Iterator {
        Implicant source;
        int currentMask = 0;
        
        TermIterator(Implicant source) {
            this.source = source;
        }
        
        public boolean hasNext() {
            return currentMask >= 0;
        }
        
        public Object next() {
            int ret = currentMask | source.values;
            int diffs = currentMask ^ source.unknowns;
            int diff = diffs ^ ((diffs - 1) & diffs);
            if(diff == 0) {
                currentMask = -1;
            } else {
                currentMask = (currentMask & ~(diff - 1)) | diff;
            }
            return new Implicant(0, ret);
        }
        
        public void remove() { }
    }
    
    private int unknowns;
    private int values;
    
    private Implicant(int unknowns, int values) {
        this.unknowns = unknowns;
        this.values = values;
    }
    
    public boolean equals(Object other) {
        if(!(other instanceof Implicant)) return false;
        Implicant o = (Implicant) other;
        return this.unknowns == o.unknowns && this.values == o.values;
    }
    
    public int compareTo(Object other) {
        Implicant o = (Implicant) other;
        if(this.values < o.values) return -1;
        if(this.values > o.values) return  1;
        if(this.unknowns < o.unknowns) return -1;
        if(this.unknowns > o.unknowns) return  1;
        return 0;
    }
    
    public int hashCode() {
        return (unknowns << 16) | values;
    }
    
    public Iterator getTerms() {
        return new TermIterator(this);
    }
    
    public int getRow() {
        if(unknowns != 0) return -1;
        return values;
    }
    
    private Expression toExpression(TruthTable source) {
        Expression term = null;
        int cols = source.getInputColumnCount();
        for(int i = cols - 1; i >= 0; i--) {
            if((unknowns & (1 << i)) == 0) {
                Expression literal = Expressions.variable(source.getInputHeader(cols - 1 - i));
                if((values & (1 << i)) == 0) literal = Expressions.not(literal);
                term = Expressions.and(term, literal);
            }
        }
        return term == null ? Expressions.constant(1) : term;
    }
    
    static Expression toExpression(AnalyzerModel model, List implicants) {
        if(implicants == null) return null;
        TruthTable table = model.getTruthTable();
        Expression sum = null;
        for(Iterator it = implicants.iterator(); it.hasNext(); ) {
            Implicant imp = (Implicant) it.next();
            sum = Expressions.or(sum, imp.toExpression(table));
        }
        return sum == null ? Expressions.constant(0) : sum;
    }
    
    static List computeSum(AnalyzerModel model, String variable) {
        TruthTable table = model.getTruthTable();
        int column = model.getOutputs().indexOf(variable);
        if(column < 0) return Collections.EMPTY_LIST;

        ArrayList ret = new ArrayList();
        for(int i = 0; i < table.getRowCount(); i++) {
            if(table.getOutputEntry(i, column) == Entry.ONE) {
                ret.add(new Implicant(0, i));
            }
        }
        return ret;
    }
    
    static List computeMinimal(AnalyzerModel model, String variable) {
        TruthTable table = model.getTruthTable();
        int column = model.getOutputs().indexOf(variable);
        if(column < 0) return Collections.EMPTY_LIST;

        // determine the first-cut implicants, as well as the rows
        // that we need to cover.
        HashMap base = new HashMap();
        HashSet toCover = new HashSet();
        boolean knownFound = false;
        for(int i = 0; i < table.getRowCount(); i++) {
            Entry entry = table.getOutputEntry(i, column);
            if(entry == Entry.ZERO) {
                knownFound = true;
            } else if(entry == Entry.ONE) {
                knownFound = true;
                Implicant imp = new Implicant(0, i);
                base.put(imp, entry);
                toCover.add(imp);
            } else {
                Implicant imp = new Implicant(0, i);
                base.put(imp, entry);
            }
        }
        if(!knownFound) return null;
        
        // work up to more general implicants, discovering
        // any prime implicants.
        HashSet primes = new HashSet();
        HashMap current = base;
        while(current.size() > 1) {
            HashSet toRemove = new HashSet();
            HashMap next = new HashMap();
            for(Iterator it = current.keySet().iterator(); it.hasNext(); ) {
                Implicant imp = (Implicant) it.next();
                Entry detEntry = (Entry) current.get(imp);
                for(int j = 1; j <= imp.values; j *= 2) {
                    if((imp.values & j) != 0) {
                        Implicant opp = new Implicant(imp.unknowns, imp.values ^ j);
                        Entry oppEntry = (Entry) current.get(opp);
                        if(oppEntry != null) {
                            toRemove.add(imp);
                            toRemove.add(opp);
                            next.put(new Implicant(opp.unknowns | j, opp.values),
                                oppEntry == Entry.DONT_CARE && detEntry == Entry.DONT_CARE
                                    ? Entry.DONT_CARE : Entry.ONE);
                        }
                    }
                }
            }
            
            for(Iterator it = current.keySet().iterator(); it.hasNext(); ) {
                Implicant det = (Implicant) it.next();
                if(!toRemove.contains(det) && current.get(det) == Entry.ONE) {
                    primes.add(det);
                }
            }
            
            current = next;
        }
        
        // we won't have more than one implicant left, but it
        // is probably prime.
        for(Iterator it = current.keySet().iterator(); it.hasNext(); ) {
            Implicant imp = (Implicant) it.next();
            if(current.get(imp) == Entry.ONE) {
                primes.add(imp);
            }
        }
        
        // determine the essential prime implicants
        HashSet retSet = new HashSet();
        HashSet covered = new HashSet();
        for(Iterator it = toCover.iterator(); it.hasNext(); ) {
            Implicant required = (Implicant) it.next();
            if(covered.contains(required)) continue;
            int row = required.getRow();
            Implicant essential = null;
            for(Iterator it2 = primes.iterator(); it2.hasNext(); ) {
                Implicant imp = (Implicant) it2.next();
                if((row & ~imp.unknowns) == imp.values) {
                    if(essential == null) essential = imp;
                    else { essential = null; break; }
                }
            }
            if(essential != null) {
                retSet.add(essential);
                primes.remove(essential);
                for(Iterator it2 = essential.getTerms(); it2.hasNext(); ) {
                    covered.add(it2.next());
                }
            }
        }
        toCover.removeAll(covered);
        
        // This is an unusual case, but it's possible that the
        // essential prime implicants don't cover everything.
        // In that case, greedily pick out prime implicants
        // that cover the most uncovered rows.
        while(!toCover.isEmpty()) {
            // find the implicant covering the most rows
            Implicant max = null;
            int maxCount = 0;
            for(Iterator it = primes.iterator(); it.hasNext(); ) {
                Implicant imp = (Implicant) it.next();
                int count = 0;
                for(Iterator it2 = imp.getTerms(); it2.hasNext(); ) {
                    if(toCover.contains(it2.next())) ++count;
                }
                if(count == 0) {
                    it.remove();
                } else if(count > maxCount) {
                    max = imp;
                    maxCount = count;
                }
            }
            
            // add it to our choice, and remove the covered rows
            retSet.add(max);
            primes.remove(max);
            for(Iterator it = max.getTerms(); it.hasNext(); ) {
                toCover.remove(it.next());
            }
        }

        // Now build up our sum-of-products expression
        // from the remaining terms
        ArrayList ret = new ArrayList();
        ret.addAll(retSet);
        Collections.sort(ret);
        return ret;
    }
}
