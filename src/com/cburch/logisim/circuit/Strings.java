/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.circuit;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

class Strings {
    private static LocaleManager source
        = new LocaleManager("com/cburch/logisim/resources", "circuit");

    public static String get(String key) {
        return source.get(key);
    }
    public static StringGetter getter(String key) {
        return source.getter(key);
    }
    public static StringGetter getter(String key, String arg) {
        return source.getter(key, arg);
    }
}
