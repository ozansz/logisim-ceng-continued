/*
 * Derived from OptionActions class
 * author: kerem hadimli

 Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.tools;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.StringUtil;

class ZoomActions {
    private ZoomActions() { }

    public static Action setZoom(AttributeSet attrs, Attribute attr, Object value) {
        Object oldValue = attrs.getValue(attr);
        if(!oldValue.equals(value)) {
            return new ZoomAction(attrs, attr, value);
        } else {
            return null;
        }
    }
    
    private static class ZoomAction extends Action {
        private AttributeSet attrs;
        private Attribute attr;
        private Object newval;
        private Object oldval;

        ZoomAction(AttributeSet attrs, Attribute attr,
                Object value) {
            this.attrs = attrs;
            this.attr = attr;
            this.newval = value;
        }

        public String getName() {
            return StringUtil.format("Zoom Action",
                attr.getDisplayName());
        }

        public void doIt(Project proj) {
            oldval = attrs.getValue(attr);
            attrs.setValue(attr, newval);
        }

        public void undo(Project proj) {
            attrs.setValue(attr, oldval);
        }
    }

}
