/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;


public class ToolActions {
    private ToolActions() { }

    public static AttributeTable.Listener createTableListener(Project proj,
            Tool tool) {
        return new AttributeListener(proj, tool);
    }

    public static Action setAttributeValue(Tool tool, Attribute attr,
            Object value) {
        return new SetAttributeValue(tool, attr, value);
    }

    private static class AttributeListener
            implements AttributeTable.Listener {
        Project proj;
        Tool tool;

        AttributeListener(Project proj, Tool tool) {
            this.proj = proj;
            this.tool = tool;
        }

        public void valueChangeRequested(Attribute attr, Object value) {
            proj.doAction(ToolActions.setAttributeValue(tool,
                attr, value));
        }
    }

    private static class SetAttributeValue extends Action {
        private Tool tool;
        private Attribute attr;
        private Object newval;
        private Object oldval;

        SetAttributeValue(Tool tool, Attribute attr, Object value) {
            this.tool = tool;
            this.attr = attr;
            this.newval = value;
        }

        public String getName() {
            return Strings.get("changeToolAttrAction");
        }

        public void doIt(Project proj) {
            AttributeSet attrs = tool.getAttributeSet();
            oldval = attrs.getValue(attr);
            attrs.setValue(attr, newval);
        }

        public void undo(Project proj) {
            AttributeSet attrs = tool.getAttributeSet();
            attrs.setValue(attr, oldval);
        }
    }
}
