/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;

import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.legacy.Version1Support;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.ListUtil;
import com.cburch.logisim.util.StringUtil;

public class LogisimFile extends Library implements LibraryEventSource {
    private static class WritingThread extends Thread {
        Writer writer;
        LogisimFile file;

        WritingThread(Writer writer, LogisimFile file) {
            this.writer = writer;
            this.file = file;
        }

        public void run() {
            try {
                file.write(writer, file.loader);
            } catch(IOException e) {
                file.loader.showError(StringUtil.format(
                    Strings.get("fileDuplicateError"), e.toString()));
            }
            try {
                writer.close();
            } catch(IOException e) {
                file.loader.showError(StringUtil.format(
                    Strings.get("fileDuplicateError"), e.toString()));
            }
        }
    }

    private EventSourceWeakSupport listeners = new EventSourceWeakSupport();
    private Loader loader;
    private LinkedList messages = new LinkedList();
    private Options options = new Options();
    private LinkedList tools = new LinkedList();
    private LinkedList libraries = new LinkedList();
    private Circuit main = null;
    private String name;
    private boolean dirty = false;

    LogisimFile(Loader loader) {
        this.loader = loader;
        
        name = Strings.get("defaultProjectName");
        if(Projects.windowNamed(name)) {
            for(int i = 2; true; i++) {
                if(!Projects.windowNamed(name + " " + i)) {
                    name += " " + i;
                    break;
                }
            }
        }

    }

    //
    // access methods
    //
    public String getName() { return name; }
    
    public boolean isDirty() { return dirty; }

    public String getMessage() {
        if(messages.size() == 0) return null;
        return (String) messages.removeFirst();
    }

    public Loader getLoader() {
        return loader;
    }

    public Options getOptions() {
        return options;
    }

    public List getTools() {
        return tools;
    }

    public List getLibraries() {
        return libraries;
    }

    public List getElements() {
        return ListUtil.joinImmutableLists(tools, libraries);
    }

    public Circuit getCircuit(String name) {
        if(name == null) return null;
        Iterator it = tools.iterator();
        while(it.hasNext()) {
            AddTool tool = (AddTool) it.next();
            Circuit circ = (Circuit) tool.getFactory();
            if(name.equals(circ.getName())) return circ;
        }
        return null;
    }

    public Circuit getMainCircuit() {
        return main;
    }

    public int getCircuitCount() {
        return tools.size();
    }

    //
    // listener methods
    //
    public void addLibraryListener(LibraryListener what) {
        listeners.add(what);
    }

    public void removeLibraryListener(LibraryListener what) {
        listeners.remove(what);
    }

    private void fireEvent(int action, Object data) {
        LibraryEvent e = new LibraryEvent(this, action, data);
        Iterator it = listeners.iterator();
        while(it.hasNext()) {
            LibraryListener what = (LibraryListener) it.next();
            what.libraryChanged(e);
        }
    }


    //
    // modification actions
    //
    public void addMessage(String msg) {
        messages.addLast(msg);
    }
    
    public void setDirty(boolean value) {
        if(dirty != value) {
            dirty = value;
            fireEvent(LibraryEvent.DIRTY_STATE, value ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    public void setName(String name) {
        this.name = name;
        fireEvent(LibraryEvent.SET_NAME, name);
    }

    public void addCircuit(Circuit circuit) {
        AddTool tool = new AddTool(circuit);
        tools.add(tool);
        if(tools.size() == 1) setMainCircuit(circuit);
        fireEvent(LibraryEvent.ADD_TOOL, tool);
    }

    public void removeCircuit(Circuit circuit) {
        if(tools.size() <= 1) {
            throw new RuntimeException("Cannot remove last circuit");
        }

        AddTool circuitTool = null;
        for(Iterator it = tools.iterator(); it.hasNext(); ) {
            AddTool tool = (AddTool) it.next();
            if(tool.getFactory() == circuit) {
                it.remove();
                circuitTool = tool;
                break;
            }
        }
        if(circuitTool != null) {
            if(main == circuit) {
                AddTool dflt_tool = (AddTool) tools.get(0);
                Circuit dflt_circ = (Circuit) dflt_tool.getFactory();
                setMainCircuit(dflt_circ);
            }
            fireEvent(LibraryEvent.REMOVE_TOOL, circuitTool);
        }
    }
    
    public void moveCircuit(AddTool tool, int index) {
        int oldIndex = tools.indexOf(tool);
        if(oldIndex < 0) {
            tools.add(index, tool);
            fireEvent(LibraryEvent.ADD_TOOL, tool);
        } else {
            Object value = tools.remove(oldIndex);
            if(index > oldIndex) --index;
            tools.add(index, value);
            fireEvent(LibraryEvent.MOVE_TOOL, tool);
        }
    }

    public void addLibrary(Library lib) {
        libraries.add(lib);
        fireEvent(LibraryEvent.ADD_LIBRARY, lib);
    }

    public void removeLibrary(Library lib) {
        libraries.remove(lib);
        fireEvent(LibraryEvent.REMOVE_LIBRARY, lib);
    }
    
    public String getUnloadLibraryMessage(Library lib) {
        HashSet factories = new HashSet();
        for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
            Object tool = it.next();
            if(tool instanceof AddTool) {
                factories.add(((AddTool) tool).getFactory());
            }
        }
        for(Iterator it = tools.iterator(); it.hasNext(); ) {
            AddTool tool = (AddTool) it.next();
            Circuit circuit = (Circuit) tool.getFactory();
            for(Iterator it2 = circuit.getNonWires().iterator(); it2.hasNext(); ) {
                Component comp = (Component) it2.next();
                if(factories.contains(comp.getFactory())) {
                    return StringUtil.format(Strings.get("unloadUsedError"),
                            circuit.getName());
                }
            }
        }
        
        ToolbarData tb = options.getToolbarData();
        MouseMappings mm = options.getMouseMappings();
        for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
            Tool t = (Tool) it.next();
            if(tb.usesToolFromSource(t)) {
                return Strings.get("unloadToolbarError");
            }
            if(mm.usesToolFromSource(t)) {
                return Strings.get("unloadMappingError");
            }
        }
        
        return null;
    }

    public void setMainCircuit(Circuit circuit) {
        if(circuit == null) return;
        this.main = circuit;
        fireEvent(LibraryEvent.SET_MAIN, circuit);
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    //
    // other methods
    //
    void write(Writer writer, LibraryLoader loader)
            throws java.io.IOException {
        XmlWriter out = new XmlWriter(loader);
        Object data = out.initialize(this);
        out.output(data, writer);
    }

    public LogisimFile cloneLogisimFile(Loader newloader) {
        PipedReader reader = new PipedReader();
        PipedWriter writer = new PipedWriter();
        try {
            reader.connect(writer);
        } catch(IOException e) {
            newloader.showError(StringUtil.format(
                Strings.get("fileDuplicateError"), e.toString()));
            return null;
        }
        new WritingThread(writer, this).start();
        try {
            return LogisimFile.load(reader, newloader);
        } catch(IOException e) {
            newloader.showError(StringUtil.format(
                Strings.get("fileDuplicateError"), e.toString()));
            return null;
        }
    }
    
    Tool findTool(Tool query) {
        for(Iterator it = getLibraries().iterator(); it.hasNext(); ) {
            Library lib = (Library) it.next();
            Tool ret = findTool(lib, query);
            if(ret != null) return ret;
        }
        return null;
    }
    
    private Tool findTool(Library lib, Tool query) {
        for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
            Tool tool = (Tool) it.next();
            if(tool.equals(query)) return tool;
        }
        return null;
    }

    //
    // creation methods
    //
    public static LogisimFile createNew(Loader loader) {
        LogisimFile ret = new LogisimFile(loader);
        ret.main = new Circuit("main");
        // The name will be changed in LogisimPreferences
        ret.tools.add(new AddTool(ret.main));
        return ret;
    }

    public static LogisimFile load(Reader reader, Loader loader)
            throws java.io.IOException {

        // fetch first line and then reset
        BufferedReader buf = new BufferedReader(reader);
        buf.mark(128);
        String firstLine = buf.readLine();
        buf.reset();

        // if this is a 1.0 file, then set up a pipe to translate to
        // 2.0 and then interpret as a 2.0 file
        if(firstLine == null) {
            throw new IOException("File is empty");
        } else if(firstLine.equals("Logisim v1.0")) {
            StringWriter out = new StringWriter();
            Version1Support.translate(buf, out);
            reader = new StringReader(out.toString());
        } else {
            reader = buf;
        }

        XmlReader in = new XmlReader(loader);
        LogisimFile ret = in.readLibrary(reader);
        ret.loader = loader;
        return ret;
    }

}
