/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.util.ArrayList;
import java.util.Iterator;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;


public class LogisimFileActions {
    private LogisimFileActions() { }

    public static Action addCircuit(Circuit circuit) {
        return new AddCircuit(circuit);
    }

    public static Action removeCircuit(Circuit circuit) {
        return new RemoveCircuit(circuit);
    }
    
    public static Action moveCircuit(AddTool tool, int toIndex) {
        return new MoveCircuit(tool, toIndex);
    }

    public static Action loadLibrary(Library lib) {
        return new LoadLibraries(new Library[] { lib });
    }

    public static Action loadLibraries(Library[] libs) {
        return new LoadLibraries(libs);
    }

    public static Action unloadLibrary(Library lib) {
        return new UnloadLibraries(new Library[] { lib });
    }

    public static Action unloadLibraries(Library[] libs) {
        return new UnloadLibraries(libs);
    }

    public static Action setMainCircuit(Circuit circuit) {
        return new SetMainCircuit(circuit);
    }

    public static Action revertDefaults() {
        return new RevertDefaults();
    }

    private static class AddCircuit extends Action {
        private Circuit circuit;

        AddCircuit(Circuit circuit) {
            this.circuit = circuit;
        }

        public String getName() {
            return Strings.get("addCircuitAction");
        }

        public void doIt(Project proj) {
            proj.getLogisimFile().addCircuit(circuit);
        }

        public void undo(Project proj) {
            proj.getLogisimFile().removeCircuit(circuit);
        }
    }

    private static class RemoveCircuit extends Action {
        private Circuit circuit;

        RemoveCircuit(Circuit circuit) {
            this.circuit = circuit;
        }

        public String getName() {
            return Strings.get("removeCircuitAction");
        }

        public void doIt(Project proj) {
            proj.getLogisimFile().removeCircuit(circuit);
        }

        public void undo(Project proj) {
            proj.getLogisimFile().addCircuit(circuit);
        }
    }

    private static class MoveCircuit extends Action {
        private AddTool tool;
        private int fromIndex;
        private int toIndex;

        MoveCircuit(AddTool tool, int toIndex) {
            this.tool = tool;
            this.toIndex = toIndex;
        }

        public String getName() {
            return Strings.get("moveCircuitAction");
        }

        public void doIt(Project proj) {
            fromIndex = proj.getLogisimFile().getTools().indexOf(tool);
            proj.getLogisimFile().moveCircuit(tool, toIndex);
        }

        public void undo(Project proj) {
            proj.getLogisimFile().moveCircuit(tool, fromIndex);
        }
    }

    private static class LoadLibraries extends Action {
        private Library[] libs;

        LoadLibraries(Library[] libs) {
            this.libs = libs;
        }

        public String getName() {
            if(libs.length == 1) {
                return Strings.get("loadLibraryAction");
            } else {
                return Strings.get("loadLibrariesAction");
            }
        }

        public void doIt(Project proj) {
            for(int i = 0; i < libs.length; i++) {
                proj.getLogisimFile().addLibrary(libs[i]);
            }
        }

        public void undo(Project proj) {
            for(int i = libs.length - 1; i >= 0; i--) {
                proj.getLogisimFile().removeLibrary(libs[i]);
            }
        }
    }
    
    private static class UnloadLibraries extends Action {
        private Library[] libs;

        UnloadLibraries(Library[] libs) {
            this.libs = libs;
        }

        public String getName() {
            if(libs.length == 1) {
                return Strings.get("unloadLibraryAction");
            } else {
                return Strings.get("unloadLibrariesAction");
            }
        }

        public void doIt(Project proj) {
            for(int i = libs.length - 1; i >= 0; i--) {
                proj.getLogisimFile().removeLibrary(libs[i]);
            }
        }

        public void undo(Project proj) {
            for(int i = 0; i < libs.length; i++) {
                proj.getLogisimFile().addLibrary(libs[i]);
            }
        }
    }

    private static class SetMainCircuit extends Action {
        private Circuit oldval;
        private Circuit newval;

        SetMainCircuit(Circuit circuit) {
            newval = circuit;
        }

        public String getName() {
            return Strings.get("setMainCircuitAction");
        }

        public void doIt(Project proj) {
            oldval = proj.getLogisimFile().getMainCircuit();
            proj.getLogisimFile().setMainCircuit(newval);
        }

        public void undo(Project proj) {
            proj.getLogisimFile().setMainCircuit(oldval);
        }
    }
    
    private static class RevertAttributeValue {
        private AttributeSet attrs;
        private Attribute attr;
        private Object value;
        
        RevertAttributeValue(AttributeSet attrs, Attribute attr, Object value) {
            this.attrs = attrs;
            this.attr = attr;
            this.value = value;
        }
    }
    
    private static class RevertDefaults extends Action {
        private Options oldOpts;
        private ArrayList libraries = null;
        private ArrayList attrValues = new ArrayList();

        RevertDefaults() { }

        public String getName() {
            return Strings.get("revertDefaultsAction");
        }

        public void doIt(Project proj) {
            LogisimFile src = LogisimPreferences.getTemplate(proj.getLogisimFile().getLoader());
            LogisimFile dst = proj.getLogisimFile();
            
            copyToolAttributes(src, dst);
            for(Iterator it = src.getLibraries().iterator(); it.hasNext(); ) {
                Library srcLib = (Library) it.next();
                Library dstLib = dst.getLibrary(srcLib.getName());
                if(dstLib == null) {
                    String desc = src.getLoader().getDescriptor(srcLib);
                    dstLib = dst.getLoader().loadLibrary(desc);
                    proj.getLogisimFile().addLibrary(dstLib);
                    if(libraries == null) libraries = new ArrayList();
                    libraries.add(dstLib);
                }
                copyToolAttributes(srcLib, dstLib);
            }
            
            Options newOpts = proj.getOptions();
            oldOpts = new Options();
            oldOpts.copyFrom(newOpts, dst);
            newOpts.copyFrom(src.getOptions(), dst);
        }
        
        private void copyToolAttributes(Library srcLib, Library dstLib) {
            for(Iterator it = srcLib.getTools().iterator(); it.hasNext(); ) {
                Tool srcTool = (Tool) it.next();
                AttributeSet srcAttrs = srcTool.getAttributeSet();
                Tool dstTool = dstLib.getTool(srcTool.getName());
                if(srcAttrs != null && dstTool != null) {
                    AttributeSet dstAttrs = dstTool.getAttributeSet();
                    for(Iterator it2 = srcAttrs.getAttributes().iterator(); it2.hasNext(); ) {
                        Attribute attr = (Attribute) it2.next();
                        Object srcValue = srcAttrs.getValue(attr);
                        Object dstValue = dstAttrs.getValue(attr);
                        if(!dstValue.equals(srcValue)) {
                            dstAttrs.setValue(attr, srcValue);
                            attrValues.add(new RevertAttributeValue(dstAttrs, attr, dstValue));
                        }
                    }
                }
            }
        }

        public void undo(Project proj) {
            proj.getOptions().copyFrom(oldOpts, proj.getLogisimFile());
            
            for(Iterator it = attrValues.iterator(); it.hasNext(); ) {
                RevertAttributeValue attrValue = (RevertAttributeValue) it.next();
                attrValue.attrs.setValue(attrValue.attr, attrValue.value);
            }

            if(libraries != null) {
                for(Iterator it = libraries.iterator(); it.hasNext(); ) {
                    Library lib = (Library) it.next();
                    proj.getLogisimFile().removeLibrary(lib);
                }
            }
        }
    }
}
