/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.io;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

class Strings {
    private static LocaleManager source
        = new LocaleManager("com/cburch/logisim/resources", "std");

    public static String get(String key) {
        return source.get(key);
    }
    public static StringGetter getter(String key) {
        return source.getter(key);
    }
}
