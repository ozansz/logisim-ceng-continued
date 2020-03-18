/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.log;

import java.util.Locale;
import javax.swing.JMenuItem;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

class Strings {
    private static LocaleManager source
        = new LocaleManager("com/cburch/logisim/resources", "log");

    public static String get(String key) {
        return source.get(key);
    }
    public static StringGetter getter(String key) {
        return source.getter(key);
    }
    public static Locale[] getLocaleOptions() {
        return source.getLocaleOptions();
    }
    public static JMenuItem createLocaleMenuItem() {
        return source.createLocaleMenuItem();
    }
}
