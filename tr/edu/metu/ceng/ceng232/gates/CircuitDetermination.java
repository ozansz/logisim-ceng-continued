/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

import java.util.ArrayList;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.ExpressionVisitor;
import com.cburch.logisim.comp.ComponentFactory;

/** This represents the actual gate selection used corresponding
 * to an expression, without any correspondence to how they would
 * be laid down in a circuit. This intermediate representation permits
 * easy manipulation of an expression's translation. */
abstract class CircuitDetermination {
    /** Ensures that all gates have only two inputs. */
    void convertToTwoInputs() { }

    /** Converts all gates to NANDs. Note that this will fail with an
     * exception if any XOR/XNOR gates are used. */ 
    void convertToNands() { }

    /** Repairs two errors that may have cropped up in creating the
     * circuit. First, if there are gates with more inputs than their
     * capacity, we repair them. Second, any XOR/XNOR gates with
     * more than 2 inputs should really be Odd/Even Parity gates. */
    void repair() { }
    
    /** A utility method for determining whether this fits the
     * pattern of a NAND representing a NOT. */
    boolean isNandNot() { return false; }
    
    //
    // static members
    //
    static class Gate extends CircuitDetermination {
        private ComponentFactory factory;
        private ArrayList inputs = new ArrayList();
        
        private Gate(ComponentFactory factory) { this.factory = factory; }
        
        ComponentFactory getFactory() { return factory; }
        ArrayList getInputs() { return inputs; }
        
        void convertToTwoInputs() {
            if(inputs.size() <= 2) {
                for(int i = 0; i < inputs.size(); i++) {
                    CircuitDetermination a = (CircuitDetermination) inputs.get(i);
                    a.convertToTwoInputs();
                }
            } else {
                ComponentFactory subFactory;
                if(factory == NorGate.instance) subFactory = OrGate.instance;
                else if(factory == NandGate.instance) subFactory = AndGate.instance;
                else subFactory = factory;
                    
                int split = (inputs.size() + 1) / 2;
                CircuitDetermination a = convertToTwoInputsSub(0, split, subFactory);
                CircuitDetermination b = convertToTwoInputsSub(split, inputs.size(), subFactory);
                inputs.clear();
                inputs.add(a);
                inputs.add(b);
            }
        }
        
        private CircuitDetermination convertToTwoInputsSub(int start, int stop,
                ComponentFactory subFactory) {
            if(stop - start == 1) {
                CircuitDetermination a = (CircuitDetermination) inputs.get(start);
                a.convertToTwoInputs();
                return a;
            } else {
                int split = (start + stop + 1) / 2;
                CircuitDetermination a = convertToTwoInputsSub(start, split, subFactory);
                CircuitDetermination b = convertToTwoInputsSub(split, stop, subFactory);
                Gate ret = new Gate(subFactory);
                ret.inputs.add(a);
                ret.inputs.add(b);
                return ret;
            }
        }
        
        void convertToNands() {
            // first recurse to clean up any children
            int num = inputs.size();
            for(int i = 0; i < num; i++) {
                CircuitDetermination sub = (CircuitDetermination) inputs.get(i);
                sub.convertToNands();
            }
            
            // repair large XOR/XNORs to odd/even parity gates
            if(factory == NotGate.factory) {
                inputs.add(inputs.get(0));
            } else if(factory == AndGate.instance) {
                notOutput();
            } else if(factory == OrGate.instance) {
                notAllInputs();
            } else if(factory == NorGate.instance) {
                notAllInputs(); // the order of these two lines is significant
                notOutput();
            } else if(factory == NandGate.instance) {
                ;
            } else {
                throw new IllegalArgumentException("Cannot handle " + factory.getDisplayName());
            }
            factory = NandGate.instance;
        }
        
        private void notOutput() {
            Gate sub = new Gate(NandGate.instance);
            sub.inputs = this.inputs;
            this.inputs = new ArrayList();
            inputs.add(sub);
            inputs.add(sub);
        }
            
        private void notAllInputs() {
            for(int i = 0; i < inputs.size(); i++) {
                CircuitDetermination old = (CircuitDetermination) inputs.get(i);
                if(old.isNandNot()) {
                    inputs.set(i, ((Gate) old).inputs.get(0));
                } else {
                    Gate now = new Gate(NandGate.instance);
                    now.inputs.add(old);
                    now.inputs.add(old);
                    inputs.set(i, now);
                }
            }
        }
        
        boolean isNandNot() {
            return factory == NandGate.instance
                && inputs.size() == 2 && inputs.get(0) == inputs.get(1);
        }
        
        void repair() {
            // check whether we need to split ourself up.
            int num = inputs.size();
            if(num > GateAttributes.MAX_INPUTS) {
                int newNum = (num + GateAttributes.MAX_INPUTS - 1) / GateAttributes.MAX_INPUTS;
                ArrayList oldInputs = inputs;
                inputs = new ArrayList();
                
                ComponentFactory subFactory = factory;
                if(subFactory == NandGate.instance) subFactory = AndGate.instance;
                if(subFactory == NorGate.instance) subFactory = OrGate.instance;
                
                int per = num / newNum;
                int numExtra = num - per * newNum;
                int k = 0;
                for(int i = 0; i < newNum; i++) {
                    Gate sub = new Gate(subFactory);
                    int subCount = per + (i < numExtra ? 1 : 0);
                    for(int j = 0; j < subCount; j++) {
                        sub.inputs.add(oldInputs.get(k));
                        k++;
                    }
                    inputs.add(sub);
                }
            }

            // repair large XOR/XNORs to odd/even parity gates
            if(inputs.size() > 2) {
                if(factory == XorGate.instance) {
                    factory = OddParityGate.instance;
                } else if(factory == XnorGate.instance) {
                    factory = EvenParityGate.instance;
                }
            }

            // finally, recurse to clean up any children
            for(int i = 0; i < inputs.size(); i++) {
                CircuitDetermination sub = (CircuitDetermination) inputs.get(i);
                sub.repair();
            }
        }
    }
    
    static class Input extends CircuitDetermination {
        private String name;
        
        private Input(String name) { this.name = name; }
        
        String getName() { return name; }
    }
    
    static class Value extends CircuitDetermination {
        private int value;
        
        private Value(int value) { this.value = value; }
        
        int getValue() { return value; }
    }
    
    static CircuitDetermination create(Expression expr) {
        if(expr == null) return null;
        return (CircuitDetermination) expr.visit(new Determine());
    }
    
    private static class Determine implements ExpressionVisitor {
        public Object visitAnd(Expression a, Expression b) {
            return binary(a.visit(this), b.visit(this), AndGate.instance);
        }

        public Object visitOr(Expression a, Expression b) {
            return binary(a.visit(this), b.visit(this), OrGate.instance);
        }

        public Object visitXor(Expression a, Expression b) {
            return binary(a.visit(this), b.visit(this), XorGate.instance);
        }
        
        private Gate binary(Object aret, Object bret, ComponentFactory factory) {
            if(aret instanceof Gate) {
                Gate a = (Gate) aret;
                if(a.factory == factory) {
                    if(bret instanceof Gate) {
                        Gate b = (Gate) bret;
                        if(b.factory == factory) {
                            a.inputs.addAll(b.inputs);
                            return a;
                        }
                    }
                    a.inputs.add(bret);
                    return a;
                }
            }
            
            if(bret instanceof Gate) {
                Gate b = (Gate) bret;
                if(b.factory == factory) {
                    b.inputs.add(aret);
                    return b;
                }
            }
            
            Gate ret = new Gate(factory);
            ret.inputs.add(aret);
            ret.inputs.add(bret);
            return ret;
        }

        public Object visitNot(Expression aBase) {
            Object aret = aBase.visit(this);
            if(aret instanceof Gate) {
                Gate a = (Gate) aret;
                if(a.factory == AndGate.instance) {
                    a.factory = NandGate.instance;
                    return a;
                } else if(a.factory == OrGate.instance) {
                    a.factory = NorGate.instance;
                    return a;
                } else if(a.factory == XorGate.instance) {
                    a.factory = XnorGate.instance;
                    return a;
                }
            }
            
            Gate ret = new Gate(NotGate.factory);
            ret.inputs.add(aret);
            return ret;
        }

        public Object visitVariable(String name) {
            return new Input(name);
        }

        public Object visitConstant(int value) {
            return new Value(value);
        }           
    }
}
