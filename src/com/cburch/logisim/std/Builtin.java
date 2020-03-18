/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cburch.logisim.legacy.Legacy;
import com.cburch.logisim.std.arith.Arithmetic;
import com.cburch.logisim.std.gates.Gates;
import com.cburch.logisim.std.io.Io;
import com.cburch.logisim.std.memory.Memory;
import com.cburch.logisim.std.plexers.Plexers;
import com.cburch.logisim.tools.Library;

public class Builtin extends Library {
    private List libraries = null;

    public Builtin() {
        libraries = Arrays.asList(new Object[] {
            new Base(),
            new Gates(),
            new Memory(),
            new Plexers(),
            new Arithmetic(),
            new Io(),
            new Legacy(),
            new tr.edu.metu.ceng.ceng232.gates.Gates(),
            new tr.edu.metu.ceng.ceng232.others.Others(),
            new tr.edu.metu.ceng.ceng232.tools.Tools(),
        });
    }

    public String getName() { return "Builtin"; }

    public String getDisplayName() { return Strings.get("builtinLibrary"); }

    public List getTools() { return Collections.EMPTY_LIST; }
    
    public List getLibraries() {
        return libraries;
    }
}
