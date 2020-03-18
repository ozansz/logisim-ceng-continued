/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.util.Iterator;
import java.util.HashMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;

class XmlWriter {
    private class WriteContext {
        LogisimFile file;
        HashMap libs = new HashMap();

        WriteContext(LogisimFile file) {
            this.file = file;
        }

        Element fromLogisimFile() {
            Element ret = new Element("project");
            ret.addContent("This file is intended to be loaded by Logisim (www.logisim.com).");
            ret.setAttribute("version", "1.0");
            ret.setAttribute("source", Main.VERSION_NAME);

            Iterator it = file.getLibraries().iterator();
            while(it.hasNext()) {
                Library lib = (Library) it.next();
                Element elt = fromLibrary(lib);
                if(elt != null) ret.addContent(elt);
            }

            if(file.getMainCircuit() != null) {
                Element mainElt = new Element("main");
                mainElt.setAttribute("name", file.getMainCircuit().getName());
                ret.addContent(mainElt);
            }

            ret.addContent(fromOptions());
            ret.addContent(fromMouseMappings());
            ret.addContent(fromToolbarData());

            it = file.getTools().iterator();
            while(it.hasNext()) {
                AddTool tool = (AddTool) it.next();
                Circuit circ = (Circuit) tool.getFactory();
                ret.addContent(fromCircuit(circ));
            }
            return ret;
        }

        Element fromLibrary(Library lib) {
            Element ret = new Element("lib");
            if(libs.containsKey(lib)) return null;
            String name = "" + libs.size();
            String desc = loader.getDescriptor(lib);
            if(desc == null) {
                loader.showError("library location unknown: "
                    + lib.getName());
                return null;
            }
            libs.put(lib, name);
            ret.setAttribute("name", name);
            ret.setAttribute("desc", desc);
            Iterator it = lib.getTools().iterator();
            while(it.hasNext()) {
                Tool t = (Tool) it.next();
                AttributeSet attrs = t.getAttributeSet();
                if(attrs != null) {
                    Element to_add = new Element("tool");
                    to_add.setAttribute("name", t.getName());
                    addAttributeSetContent(to_add, attrs);
                    ret.addContent(to_add);
                }
            }
            return ret;
        }

        Element fromOptions() {
            Element elt = new Element("options");
            addAttributeSetContent(elt, file.getOptions().getAttributeSet());
            return elt;
        }

        Element fromMouseMappings() {
            Element elt = new Element("mappings");
            MouseMappings map = file.getOptions().getMouseMappings();
            Iterator it = map.getMappings().keySet().iterator();
            while(it.hasNext()) {
                Integer mods = (Integer) it.next();
                Tool tool = map.getToolFor(mods);
                Element to_add = fromTool(tool);
                to_add.setAttribute("map",
                    InputEventUtil.toString(mods.intValue()));
                elt.addContent(to_add);
            }
            return elt;
        }

        Element fromToolbarData() {
            Element elt = new Element("toolbar");
            ToolbarData toolbar = file.getOptions().getToolbarData();
            Iterator it = toolbar.getContents().iterator();
            while(it.hasNext()) {
                Object item = it.next();
                if(item instanceof ToolbarData.Separator) {
                    elt.addContent(new Element("sep"));
                } else if(item instanceof Tool) {
                    elt.addContent(fromTool((Tool) item));
                }
            }
            return elt;
        }

        Element fromTool(Tool tool) {
            Library lib = findLibrary(tool);
            String lib_name;
            if(lib == null) {
                loader.showError(StringUtil.format("tool `%s' not found",
                    tool.getDisplayName()));
                return null;
            } else if(lib == file) {
                lib_name = null;
            } else {
                lib_name = (String) libs.get(lib);
                if(lib_name == null) {
                    loader.showError("unknown library within file");
                    return null;
                }
            }

            Element elt = new Element("tool");
            if(lib_name != null) elt.setAttribute("lib", lib_name);
            elt.setAttribute("name", tool.getName());
            addAttributeSetContent(elt, tool.getAttributeSet());
            return elt;
        }

        Element fromCircuit(Circuit circuit) {
            Element ret = new Element("circuit");
            ret.setAttribute("name", circuit.getName());
            for(Iterator it = circuit.getWires().iterator(); it.hasNext(); ) {
                Wire w = (Wire) it.next();
                ret.addContent(fromWire(w));
            }
            for(Iterator it = circuit.getNonWires().iterator(); it.hasNext(); ) {
                Component comp = (Component) it.next();
                Element elt = fromComponent(comp);
                if(elt != null) ret.addContent(elt);
            }
            return ret;
        }

        Element fromComponent(Component comp) {
            ComponentFactory source = comp.getFactory();
            Library lib = findLibrary(source);
            String lib_name;
            if(lib == null) {
                loader.showError(source.getName() + " component not found");
                return null;
            } else if(lib == file) {
                lib_name = null;
            } else {
                lib_name = (String) libs.get(lib);
                if(lib_name == null) {
                    loader.showError("unknown library within file");
                    return null;
                }
            }

            Element ret = new Element("comp");
            if(lib_name != null) ret.setAttribute("lib", lib_name);
            ret.setAttribute("name", source.getName());
            ret.setAttribute("loc", comp.getLocation().toString());
            addAttributeSetContent(ret, comp.getAttributeSet());
            return ret;
        }

        Element fromWire(Wire w) {
            Element ret = new Element("wire");
            ret.setAttribute("from", w.getEnd0().toString());
            ret.setAttribute("to", w.getEnd1().toString());
            return ret;
        }

        void addAttributeSetContent(Element elt, AttributeSet attrs) {
            if(attrs == null) return;
            Iterator it = attrs.getAttributes().iterator();
            while(it.hasNext()) {
                com.cburch.logisim.data.Attribute attr
                    = (com.cburch.logisim.data.Attribute) it.next();
                Object val = attrs.getValue(attr);
                if(val != null) {
                    Element a = new Element("a");
                    a.setAttribute("name", attr.getName());
                    String value = attr.toStandardString(val);
                    if(value.indexOf("\n") >= 0) {
                        a.addContent(value);
                    } else {
                        a.setAttribute("val", attr.toStandardString(val));
                    }
                    elt.addContent(a);
                }
            }
        }

        Library findLibrary(Tool tool) {
            if(libraryContains(file, tool)) {
                return file;
            }
            for(Iterator it = file.getLibraries().iterator(); it.hasNext(); ) {
                Library lib = (Library) it.next();
                if(libraryContains(lib, tool)) return lib;
            }
            return null;
        }

        Library findLibrary(ComponentFactory source) {
            if(file.contains(source)) {
                return file;
            }
            Iterator it = file.getLibraries().iterator();
            while(it.hasNext()) {
                Library lib = (Library) it.next();
                if(lib.contains(source)) return lib;
            }
            return null;
        }

        boolean libraryContains(Library lib, Tool query) {
            for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
                Tool tool = (Tool) it.next();
                if(tool.sharesSource(query)) return true;
            }
            return false;
        }
    }

    private XMLOutputter outputter;
    private LibraryLoader loader;

    XmlWriter(LibraryLoader loader) {
        this.loader = loader;
        Format format = Format.getPrettyFormat();
//      format.setLineSeparator("\r\n");
//      format.setIndent(" ");
        outputter = new XMLOutputter(format);
    }

    Object initialize(LogisimFile file) {
        WriteContext context = new WriteContext(file);
        return new Document(context.fromLogisimFile());
    }

    void output(Object data, java.io.Writer writer)
            throws java.io.IOException {
        outputter.output((Document) data, writer);
    }

}
