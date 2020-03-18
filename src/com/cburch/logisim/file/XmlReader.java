/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;

class XmlReader {
    private static class UnknownComponent {
        Circuit circuit;
        String comp_name;
        Element elt;

        UnknownComponent(Circuit circuit, String comp_name,
                Element elt) {
            this.circuit = circuit;
            this.comp_name = comp_name;
            this.elt = elt;
        }
    }

    private class ReadContext {
        LogisimFile file;
        HashMap libs = new HashMap();
        ArrayList unknowns = new ArrayList();

        ReadContext(LogisimFile file) {
            this.file = file;
        }

        void toLogisimFile(Element elt) {
            Iterator it;
            String main = null;

            // first load the sublibraries
            it = elt.getChildren("lib").iterator();
            while(it.hasNext()) {
                Element o = (Element) it.next();
                Library lib = toLibrary(o);
                if(lib != null) file.addLibrary(lib);
            }

            // then create the subcircuits
            it = elt.getChildren().iterator();
            while(it.hasNext()) {
                Element sub_elt = (Element) it.next();
                String name = sub_elt.getName();
                if(name.equals("circuit")) {
                    Circuit to_add = toCircuit(sub_elt);
                    file.addCircuit(to_add);
                } else if(name.equals("lib")) {
                    ; // Nothing to do: Done earlier.
                } else if(name.equals("options")) {
                    initOptions(sub_elt);
                } else if(name.equals("mappings")) {
                    initMouseMappings(sub_elt);
                } else if(name.equals("toolbar")) {
                    initToolbarData(sub_elt);
                } else if(name.equals("main")) {
                    main = sub_elt.getAttributeValue("name");
                } else if(name.equals("message")) {
                    file.addMessage(sub_elt.getAttributeValue("value"));
                }
            }

            // set the main circuit
            file.setMainCircuit(file.getCircuit(main));

            // then resolve any unknown components
            it = unknowns.iterator();
            while(it.hasNext()) {
                UnknownComponent unk = (UnknownComponent) it.next();
                AddTool tool = (AddTool) file.getTool(unk.comp_name);
                if(tool == null) {
                    loader.showError(StringUtil.format(
                        Strings.get("compUnknownError"),
                        unk.comp_name));
                } else {
                    addComponent(unk.circuit, tool.getFactory(),
                        unk.elt);
                }
            }
        }

        Library toLibrary(Element elt) {
            String name = elt.getAttributeValue("name");
            if(name == null) {
                loader.showError(Strings.get("libNameMissingError"));
                return null;
            }
            String desc = elt.getAttributeValue("desc");
            if(desc == null) {
                loader.showError(Strings.get("libDescMissingError"));
                return null;
            }
            Library ret = loader.loadLibrary(desc);
            if(ret == null) return null;
            libs.put(name, ret);
            Iterator it = elt.getChildren().iterator();
            while(it.hasNext()) {
                Element sub_elt = (Element) it.next();
                if(sub_elt.getName().equals("tool")) {
                    String tool_str = sub_elt.getAttributeValue("name");
                    if(tool_str == null) {
                        loader.showError(Strings.get("toolNameMissingError"));
                        continue;
                    }
                    Tool tool = ret.getTool(tool_str);
                    if(tool == null) {
                        continue;
                    }
                    initAttributeSet(sub_elt, tool.getAttributeSet());
                }
            }
            return ret;
        }

        void initOptions(Element elt) {
            initAttributeSet(elt, file.getOptions().getAttributeSet());
        }

        void initMouseMappings(Element elt) {
            MouseMappings map = file.getOptions().getMouseMappings();
            Iterator it = elt.getChildren().iterator();
            while(it.hasNext()) {
                Element sub_elt = (Element) it.next();
                if(sub_elt.getName().equals("tool")) {
                    Tool tool = toTool(sub_elt);
                    if(tool == null) continue;

                    String mods_str = sub_elt.getAttributeValue("map");
                    if(mods_str == null) {
                        loader.showError(Strings.get("mappingMissingError"));
                        continue;
                    }
                    int mods;
                    try {
                        mods = InputEventUtil.fromString(mods_str);
                    } catch(NumberFormatException e) {
                        loader.showError(StringUtil.format(
                            Strings.get("mappingBadError"), mods_str));
                        continue;
                    }

                    tool = tool.cloneTool();
                    initAttributeSet(sub_elt, tool.getAttributeSet());

                    map.setToolFor(mods, tool);
                }
            }
        }

        void initToolbarData(Element elt) {
            ToolbarData toolbar = file.getOptions().getToolbarData();
            Iterator it = elt.getChildren().iterator();
            while(it.hasNext()) {
                Element sub_elt = (Element) it.next();
                if(sub_elt.getName().equals("sep")) {
                    toolbar.addSeparator();
                } else if(sub_elt.getName().equals("tool")) {
                    Tool tool = toTool(sub_elt);
                    if(tool == null) continue;
                    tool = tool.cloneTool();
                    initAttributeSet(sub_elt, tool.getAttributeSet());
                    toolbar.addTool(tool);
                }
            }
        }

        Tool toTool(Element elt) {
            Library lib = findLibrary(elt.getAttributeValue("lib"));
            if(lib == null) return null;
            String tool_name = elt.getAttributeValue("name");
            if(tool_name == null) return null;
            return lib.getTool(tool_name);
        }

        Circuit toCircuit(Element circuit_elt) {
            String circuit_name = circuit_elt.getAttributeValue("name");
            if(circuit_name == null) {
                loader.showError(Strings.get("circNameMissingError"));
            }
            Circuit ret = new Circuit(circuit_name);

            Iterator it = circuit_elt.getChildren().iterator();
            while(it.hasNext()) {
                Element sub_elt = (Element) it.next();
                if(sub_elt.getName().equals("comp")) {
                    addComponent(ret, sub_elt);
                } else if(sub_elt.getName().equals("wire")) {
                    addWire(ret, sub_elt);
                }
            }
            return ret;
        }

        void addComponent(Circuit circuit, Element elt) {
            // Determine component class
            String comp_name = elt.getAttributeValue("name");
            if(comp_name == null) {
                loader.showError(Strings.get("compNameMissingError"));
                return;
            }

            Library lib = findLibrary(elt.getAttributeValue("lib"));
            tr.edu.metu.ceng.ceng232.grader.Settings.useComponent(lib.getName() + ":" + comp_name);

            if(lib == null) return;

            Tool tool = lib.getTool(comp_name);
            if(tool == null || !(tool instanceof AddTool)) {
                if(lib == file) {
                    unknowns.add(new UnknownComponent(circuit,
                        comp_name, elt));
                } else {
                    loader.showError(StringUtil.format(
                        Strings.get("compAbsentError"), comp_name,
                        lib.getName()));
                }
            } else {
                ComponentFactory source = ((AddTool) tool).getFactory();
                addComponent(circuit, source, elt);
            }
        }

        void addComponent(Circuit circuit,
                ComponentFactory source, Element elt) {
            // Determine attributes
            String loc_str = elt.getAttributeValue("loc");
            AttributeSet attrs = source.createAttributeSet();
            initAttributeSet(elt, attrs);

            // Create component if location known
            if(loc_str == null) {
                loader.showError(StringUtil.format(
                    Strings.get("compLocMissingError"), source.getName()));
            } else {
                try {
                    Location loc = Location.parse(loc_str);
                    circuit.add(source.createComponent(loc, attrs));
                } catch(NumberFormatException e) {
                    loader.showError(StringUtil.format(
                        Strings.get("compLocInvalidError"),
                        source.getName(), loc_str));
                }
            }

        }

        void addWire(Circuit circuit, Element elt) {
            Location pt0;
            try {
                String str = elt.getAttributeValue("from");
                if(str == null) {
                    loader.showError(Strings.get("wireStartMissingError"));
                }
                pt0 = Location.parse(str);
            } catch(NumberFormatException e) {
                loader.showError(Strings.get("wireStartInvalidError"));
                return;
            }

            Location pt1;
            try {
                String str = elt.getAttributeValue("to");
                if(str == null) {
                    loader.showError(Strings.get("wireEndMissingError"));
                }
                pt1 = Location.parse(str);
            } catch(NumberFormatException e) {
                loader.showError(Strings.get("wireEndInvalidError"));
                return;
            }

            circuit.add(Wire.create(pt0, pt1));
        }

        void initAttributeSet(Element elt, AttributeSet attrs) {
            Iterator it = elt.getChildren().iterator();
            while(it.hasNext()) {
                Element attr_elt = (Element) it.next();
                if(!attr_elt.getName().equals("a")) {
                    // ignore unrecognized element
                    continue;
                }
                String attr_name = attr_elt.getAttributeValue("name");
                if(attr_name == null) {
                    loader.showError(Strings.get("attrNameMissingError"));
                    continue;
                }
                com.cburch.logisim.data.Attribute attr = attrs.getAttribute(attr_name);
                if(attr != null) {
                    String attr_val = attr_elt.getAttributeValue("val");
                    if(attr_val == null) {
                        attr_val = attr_elt.getText();
                    }
                    if(attr_val != null) {
                        Object val = null;
                        try {
                            val = attr.parse(attr_val);
                        } catch(NumberFormatException e) {
                            loader.showError(StringUtil.format(
                                Strings.get("attrValueInvalidError"),
                                attr_val, attr_name));
                            continue;
                        }
                        attrs.setValue(attr, val);
                    }
                }
            }
        }

        Library findLibrary(String lib_name) {
            if(lib_name == null) {
                return file;
            }

            Library ret = (Library) libs.get(lib_name);
            if(ret == null) {
                loader.showError(StringUtil.format(
                    Strings.get("libMissingError"), lib_name));
                return null;
            } else {
                return ret;
            }
        }

    }

    private SAXBuilder builder = new SAXBuilder();
    private LibraryLoader loader;

    XmlReader(Loader loader) {
        this.loader = loader;
    }

    LogisimFile readLibrary(java.io.Reader reader)
            throws java.io.IOException {
        try {
            Document doc = builder.build(reader);
            Element elt = doc.getRootElement();
            LogisimFile file = new LogisimFile((Loader) loader);
            ReadContext context = new ReadContext(file);
            context.toLogisimFile(elt);
            return file;
        } catch(JDOMException e) {
            loader.showError(StringUtil.format(
                Strings.get("xmlFormatError"), e.toString()));
            return null;
        }
    }

}
