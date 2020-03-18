/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.Collections;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

public class CircuitActions {
    private CircuitActions() { }

    public static Action setCircuitName(Circuit circuit, String name) {
        return new SetCircuitName(circuit, name);
    }

    public static Action setAttributeValue(Circuit circuit,
            Component comp, Attribute attr, Object value) {
        return new SetAttributeValue(circuit, comp, attr, value);
    }
    
    public static ComponentAction addComponent(Circuit circuit, Component comp,
            boolean tryShortening) {
        if(tryShortening && comp instanceof Wire) {
            ComponentAction ret = ActionShorten.create(circuit, (Wire) comp);
            if(ret != null) return ret;
        }
        return addComponents(circuit, Collections.singleton(comp));
    }
    
    public static ComponentAction addComponents(Circuit circuit, Collection comps) {
        return ActionAdd.create(circuit, comps);
    }

    public static ComponentAction removeComponent(Circuit circuit, Component comp) {
        return removeComponents(circuit, Collections.singleton(comp));
    }
    
    public static ComponentAction removeComponents(Circuit circuit, Collection comps) {
        return ActionRemove.create(circuit, comps);
    }

    private static class SetCircuitName extends Action {
        private Circuit circuit;
        private String newval;
        private String oldval;

        SetCircuitName(Circuit circuit, String name) {
            this.circuit = circuit;
            this.newval = name;
        }

        public String getName() {
            return Strings.get("renameCircuitAction");
        }

        public void doIt(Project proj) {
            oldval = circuit.getName();
            circuit.setName(newval);
        }

        public void undo(Project proj) {
            circuit.setName(oldval);
        }
    }

    private static class SetAttributeValue extends Action {
        private Circuit circuit;
        private Component comp;
        private Attribute attr;
        private Object newval;
        private Object oldval;

        SetAttributeValue(Circuit circuit, Component comp,
                Attribute attr, Object value) {
            this.circuit = circuit;
            this.comp = comp;
            this.attr = attr;
            this.newval = value;
        }

        public String getName() {
            return Strings.get("changeAttributeAction");
        }

        public void doIt(Project proj) {
            AttributeSet attrs = comp.getAttributeSet();
            oldval = attrs.getValue(attr);
            attrs.setValue(attr, newval);
            circuit.componentChanged(comp);
        }

        public void undo(Project proj) {
            AttributeSet attrs = comp.getAttributeSet();
            attrs.setValue(attr, oldval);
            circuit.componentChanged(comp);
        }
    }
}
