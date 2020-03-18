/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public class Analyze {
    private static final int MAX_ITERATIONS = 100;
    
    private Analyze() { }
    
    //
    // getPinLabels
    //
    /** Returns a sorted map from Pin objects to String objects,
     * listed in canonical order (top-down order, with ties
     * broken left-right).
     */
    public static SortedMap getPinLabels(Circuit circuit) {
        SortedMap ret = new TreeMap(new Comparator() {
            public int compare(Object arg0, Object arg1) {
                Location a = ((Pin) arg0).getLocation();
                Location b = ((Pin) arg1).getLocation();
                if(a.getY() < b.getY()) return -1;
                if(a.getY() > b.getY()) return  1;
                if(a.getX() < b.getX()) return -1;
                if(a.getX() > b.getX()) return  1;
                return arg0.hashCode() - arg1.hashCode();
            }
        });

        // Put the pins into the TreeMap, with null labels
        for(Iterator it = circuit.pins.getPins().iterator(); it.hasNext(); ) {
            Object pin = it.next();
            ret.put(pin, null);
        }
        
        // Process first the pins that the user has given labels.
        HashSet labelsTaken = new HashSet();
        for(Iterator it = ret.keySet().iterator(); it.hasNext(); ) {
            Pin pin = (Pin) it.next();
            String label = toValidLabel(pin.getLabel());
            if(label != null) {
                if(labelsTaken.contains(label)) {
                    int i = 2;
                    while(labelsTaken.contains(label + i)) i++;
                    label = label + i;
                }
                ret.put(pin, label);
                labelsTaken.add(label);
            }
        }
        
        // Now process the unlabeled pins.
        for(Iterator it = ret.keySet().iterator(); it.hasNext(); ) {
            Pin pin = (Pin) it.next();
            if(ret.get(pin) != null) continue;
            
            String defaultList;
            if(pin.isInputPin()) {
                defaultList = Strings.get("defaultInputLabels");
                if(defaultList.indexOf(",") < 0) {
                    defaultList = "a,b,c,d,e,f,g,h";
                }
            } else {
                defaultList = Strings.get("defaultOutputLabels");
                if(defaultList.indexOf(",") < 0) {
                    defaultList = "x,y,z,u,v,w,s,t";
                }
            }
            
            String[] options = defaultList.split(",");
            String label = null;
            for(int i = 0; label == null && i < options.length; i++) {
                if(!labelsTaken.contains(options[i])) {
                    label = options[i];
                }
            }
            if(label == null) {
                // This is an extreme measure that should never happen
                // if the default labels are defined properly and the
                // circuit doesn't exceed the maximum number of pins.
                int i = 1;
                do {
                    i++;
                    label = "x" + i;
                } while(labelsTaken.contains(label));
            }
            
            labelsTaken.add(label);
            ret.put(pin, label);
        }
        
        return ret;
    }
    
    private static String toValidLabel(String label) {
        if(label == null) return null;
        StringBuffer end = null;
        StringBuffer ret = new StringBuffer();
        boolean afterWhitespace = false;
        for(int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if(Character.isJavaIdentifierStart(c)) {
                if(afterWhitespace) {
                    // capitalize words after the first one
                    c = Character.toTitleCase(c);
                    afterWhitespace = false;
                }
                ret.append(c);
            } else if(Character.isJavaIdentifierPart(c)) {
                // If we can't place it at the start, we'll dump it
                // onto the end.
                if(ret.length() > 0) {
                    ret.append(c);
                } else {
                    if(end == null) end = new StringBuffer();
                    end.append(c);
                }
                afterWhitespace = false;
            } else if(Character.isWhitespace(c)) {
                afterWhitespace = true;
            } else {
                ; // just ignore any other characters
            }
        }
        if(end != null && ret.length() > 0) ret.append(end.toString());
        if(ret.length() == 0) return null;
        return ret.toString();
    }
    
    //
    // computeExpression
    //
    /** Computes the expression corresponding to the given
     * circuit, or raises ComputeException if difficulties
     * arise.
     */
    public static void computeExpression(AnalyzerModel model, Circuit circuit,
            Map pinNames) throws AnalyzeException {
        ExpressionMap expressionMap = new ExpressionMap(circuit);
        
        ArrayList inputNames = new ArrayList();
        ArrayList outputNames = new ArrayList();
        ArrayList outputPins = new ArrayList();
        for(Iterator it = pinNames.keySet().iterator(); it.hasNext(); ) {
            Pin pin = (Pin) it.next();
            String label = (String) pinNames.get(pin);
            if(pin.isInputPin()) {
                expressionMap.currentCause = pin;
                Expression e = Expressions.variable(label);
                expressionMap.put(pin.getLocation(), e);
                inputNames.add(label);
            } else {
                outputPins.add(pin);
                outputNames.add(label);
            }
        }
        
        propagateComponents(expressionMap, circuit.getNonWires());
        
        for(int iterations = 0; !expressionMap.dirtyPoints.isEmpty(); iterations++) {
            if(iterations > MAX_ITERATIONS) {
                throw new AnalyzeException.Circular();
            }
            
            propagateWires(expressionMap, new HashSet(expressionMap.dirtyPoints));

            HashSet dirtyComponents = getDirtyComponents(circuit, expressionMap.dirtyPoints);
            expressionMap.dirtyPoints.clear();
            propagateComponents(expressionMap, dirtyComponents);
            
            Expression expr = checkForCircularExpressions(expressionMap);
            if(expr != null) throw new AnalyzeException.Circular();
        }
        
        model.setVariables(inputNames, outputNames);
        for(int i = 0; i < outputPins.size(); i++) {
            Pin pin = (Pin) outputPins.get(i);
            model.getOutputExpressions().setExpression((String) outputNames.get(i),
                    (Expression) expressionMap.get(pin.getLocation()));
        }
    }

    private static class ExpressionMap extends HashMap {
        private Circuit circuit;
        private HashSet dirtyPoints = new HashSet();
        private HashMap causes = new HashMap(); // Location -> Component
        private Component currentCause = null;
        
        ExpressionMap(Circuit circuit) {
            this.circuit = circuit;
        }
        
        public Object put(Object point, Object expression) {
            Object ret = super.put(point, expression);
            if(currentCause != null) causes.put(point, currentCause);
            if(ret == null ? expression != null : !ret.equals(expression)) {
                dirtyPoints.add(point);
            }
            return ret;
        }
    }
        
    // propagates expressions down wires
    private static void propagateWires(ExpressionMap expressionMap,
            HashSet pointsToProcess) throws AnalyzeException {
        expressionMap.currentCause = null;
        for(Iterator it = pointsToProcess.iterator(); it.hasNext(); ) {
            Location p = (Location) it.next();
            Expression e = (Expression) expressionMap.get(p);
            expressionMap.currentCause = (Component) expressionMap.causes.get(p);
            WireBundle bundle = expressionMap.circuit.wires.getWireBundle(p);
            if(e != null && bundle != null && bundle.points != null) {
                for(Iterator it2 = bundle.points.iterator(); it2.hasNext(); ) {
                    Location p2 = (Location) it2.next();
                    if(p2.equals(p)) continue;
                    Expression old = (Expression) expressionMap.get(p2);
                    if(old != null) {
                        Component eCause = expressionMap.currentCause;
                        Component oldCause = (Component) expressionMap.causes.get(p2);
                        if(eCause != oldCause && !old.equals(e)) {
                            throw new AnalyzeException.Conflict();
                        }
                    }
                    expressionMap.put(p2, e);
                }
            }
        }
    }
        
    // computes outputs of affected components
    private static HashSet getDirtyComponents(Circuit circuit,
            HashSet pointsToProcess) throws AnalyzeException {
        HashSet dirtyComponents = new HashSet();
        for(Iterator it = pointsToProcess.iterator(); it.hasNext(); ) {
            Location point = (Location) it.next();
            for(Iterator it2 = circuit.getNonWires(point).iterator(); it2.hasNext(); ) {
                Component comp = (Component) it2.next();
                dirtyComponents.add(comp);
            }
        }
        return dirtyComponents;
    }
        
    private static void propagateComponents(ExpressionMap expressionMap,
            Collection components) throws AnalyzeException {
        for(Iterator it = components.iterator(); it.hasNext(); ) {
            Component comp = (Component) it.next();
            ExpressionComputer computer
                = (ExpressionComputer) comp.getFeature(ExpressionComputer.class);
            if(computer != null) {
                try {
                    expressionMap.currentCause = comp;
                    computer.computeExpression(expressionMap);
                } catch(UnsupportedOperationException e) {
                    throw new AnalyzeException.CannotHandle(comp.getFactory().getDisplayName());
                }
            } else if(!(comp instanceof Pin)) {
                // pins are handled elsewhere
                throw new AnalyzeException.CannotHandle(comp.getFactory().getDisplayName());
            }
        }
    }
    
    /** Checks whether any of the recently placed expressions in the
     * expression map are self-referential; if so, return it. */
    private static Expression checkForCircularExpressions(ExpressionMap expressionMap)
            throws AnalyzeException {
        for(Iterator it = expressionMap.dirtyPoints.iterator(); it.hasNext(); ) {
            Location point = (Location) it.next();
            Expression expr = (Expression) expressionMap.get(point);
            if(expr.isCircular()) return expr;
        }
        return null;
    }

    //
    // ComputeTable
    //
    /** Returns a truth table corresponding to the circuit. */
    public static void computeTable(AnalyzerModel model, Project proj,
            Circuit circuit, Map pinLabels) {
        ArrayList inputPins = new ArrayList();
        ArrayList inputNames = new ArrayList();
        ArrayList outputPins = new ArrayList();
        ArrayList outputNames = new ArrayList();
        for(Iterator it = pinLabels.keySet().iterator(); it.hasNext(); ) {
            Pin pin = (Pin) it.next();
            if(pin.isInputPin()) {
                inputPins.add(pin);
                inputNames.add(pinLabels.get(pin));
            } else {
                outputPins.add(pin);
                outputNames.add(pinLabels.get(pin));
            }
        }
        
        int inputCount = inputPins.size();
        int rowCount = 1 << inputCount;
        Entry[][] columns = new Entry[outputPins.size()][rowCount];
        
        for(int i = 0; i < rowCount; i++) {
            CircuitState circuitState = new CircuitState(proj, circuit);
            for(int j = 0; j < inputCount; j++) {
                Pin pin = (Pin) inputPins.get(j);
                boolean value = TruthTable.isInputSet(i, j, inputCount);
                pin.setValue(circuitState, value ? Value.TRUE : Value.FALSE);
            }
            
            Propagator prop = circuitState.getPropagator();
            prop.propagate();
            /* TODO for the SimulatorPrototype class
            do {
                prop.step();
            } while(prop.isPending()); */
            // TODO: Search for circuit state
            
            if(prop.isOscillating()) {
                for(int j = 0; j < columns.length; j++) {
                    columns[j][i] = Entry.OSCILLATE_ERROR;
                }
            } else {
                for(int j = 0; j < columns.length; j++) {
                    Pin pin = (Pin) outputPins.get(j);
                    Entry out;
                    Value outValue = pin.getValue(circuitState).get(0);
                    if(outValue == Value.TRUE) out = Entry.ONE;
                    else if(outValue == Value.FALSE) out = Entry.ZERO;
                    else if(outValue == Value.ERROR) out = Entry.BUS_ERROR;
                    else out = Entry.DONT_CARE;
                    columns[j][i] = out;
                }
            }
        }
        
        model.setVariables(inputNames, outputNames);
        for(int i = 0; i < columns.length; i++) {
            model.getTruthTable().setOutputColumn(i, columns[i]);
        }
    }
}
