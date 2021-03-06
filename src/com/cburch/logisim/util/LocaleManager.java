/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

public class LocaleManager {
    // static members
    private static final String SETTINGS_NAME = "settings";
    private static ArrayList managers = new ArrayList();

    private static class LocaleItem extends JRadioButtonMenuItem
            implements ActionListener {
        private Locale locale;
        LocaleItem(Locale locale, ButtonGroup bgroup) {
            this.locale = locale;
            bgroup.add(this);
            addActionListener(this);
            setSelected(locale.equals(LocaleManager.getLocale()));
        }
        public void actionPerformed(ActionEvent evt) {
            if(isSelected()) LocaleManager.setLocale(locale);
        }
    }

    private static class LocaleMenu extends JMenu
            implements LocaleListener {
        LocaleItem[] items;
        LocaleMenu(Locale[] locales) {
            ButtonGroup bgroup = new ButtonGroup();
            items = new LocaleItem[locales.length];
            for(int i = 0; i < locales.length; i++) {
                items[i] = new LocaleItem(locales[i], bgroup);
                add(items[i]);
            }
            LocaleManager.addLocaleListener(this);
            localeChanged();
        }
        public void localeChanged() {
            this.setText(Strings.get("localeMenuItem"));
            Locale current = LocaleManager.getLocale();
            for(int i = 0; i < items.length; i++) {
                LocaleItem it = items[i];
                it.setText(it.locale.getDisplayName(current));
                it.setSelected(it.locale.equals(current));
            }
        }
    }
    
    private static class LocaleGetter implements StringGetter {
        private LocaleManager source;
        private String key;

        LocaleGetter(LocaleManager source, String key) {
            this.source = source;
            this.key = key;
        }

        public String get() {
            return source.get(key);
        }
        
        public String toString() { return get(); }
    }
    
    private static ArrayList listeners = new ArrayList();
    private static boolean replaceAccents = false; 
    private static HashMap repl = null;

    public static Locale getLocale() {
        return Locale.getDefault();
    }

    public static void setLocale(Locale loc) {
        Locale.setDefault(loc);
        Iterator it = managers.iterator();
        while(it.hasNext()) {
            ((LocaleManager) it.next()).loadDefault();
        }
        repl = replaceAccents ? fetchReplaceAccents() : null;
        fireLocaleChanged();
    }
    
    public static boolean canReplaceAccents() {
        return fetchReplaceAccents() != null;
    }
    
    public static void setReplaceAccents(boolean value) {
        HashMap newRepl = value ? fetchReplaceAccents() : null;
        replaceAccents = value;
        repl = newRepl;
        fireLocaleChanged();
    }
    
    private static HashMap fetchReplaceAccents() {
        HashMap ret = null;
        String val;
        try {
            val = Strings.source.locale.getString("accentReplacements");
        } catch(MissingResourceException e) {
            return null;
        }
        StringTokenizer toks = new StringTokenizer(val, "/");
        while(toks.hasMoreTokens()) {
            String tok = toks.nextToken().trim();
            char c = '\0';
            String s = null;
            if(tok.length() == 1) {
                c = tok.charAt(0);
                s = "";
            } else if(tok.length() >= 2 && tok.charAt(1) == ' ') {
                c = tok.charAt(0);
                s = tok.substring(2).trim();
            }
            if(s != null) {
                if(ret == null) ret = new HashMap();
                ret.put(new Character(c), s);
            }
        }
        return ret;
    }

    public static void addLocaleListener(LocaleListener l) {
        listeners.add(l);
    }

    public static void removeLocaleListener(LocaleListener l) {
        listeners.remove(l);
    }

    private static void fireLocaleChanged() {
        Iterator it = listeners.iterator();
        while(it.hasNext()) ((LocaleListener) it.next()).localeChanged();
    }

    // instance members
    private String dir_name;
    private String file_start;
    private ResourceBundle settings = null;
    private ResourceBundle locale = null;
    private ResourceBundle dflt_locale = null;

    public LocaleManager(String dir_name, String file_start) {
        this.dir_name = dir_name;
        this.file_start = file_start;
        loadDefault();
        managers.add(this);
    }

    private void loadDefault() {
        if(settings == null) {
            try {
                settings = ResourceBundle.getBundle(dir_name + "/" + SETTINGS_NAME);
            } catch(java.util.MissingResourceException e) { }
        }

        try {
            loadLocale(Locale.getDefault());
            if(locale != null) return;
        } catch(java.util.MissingResourceException e) { }
        try {
            loadLocale(Locale.ENGLISH);
            if(locale != null) return;
        } catch(java.util.MissingResourceException e) { }
        Locale[] choices = getLocaleOptions();
        if(choices != null && choices.length > 0) loadLocale(choices[0]);
        if(locale != null) return;
        throw new RuntimeException("No locale bundles are available");
    }

    private void loadLocale(Locale loc) {
        locale = ResourceBundle.getBundle(dir_name + "/" + loc + "/" + file_start,
            loc);
        Locale.setDefault(loc);
        if(dflt_locale == null) dflt_locale = locale;
    }

    public String get(String key) {
        String ret;
        try {
            ret = locale.getString(key);
        } catch(MissingResourceException e) {
            try {
                ret = dflt_locale.getString(key);
            } catch(MissingResourceException e2) {
                ret = key;
            }
        }
        HashMap repl = LocaleManager.repl;
        if(repl != null) ret = replaceAccents(ret, repl);
        return ret;
    }

    public StringGetter getter(String key) {
        return new LocaleGetter(this, key);
    }
    
    public StringGetter getter(String key, String arg) {
        return StringUtil.formatter(getter(key), arg);
    }

    public Locale[] getLocaleOptions() {
        String locs = null;
        try {
            if(settings != null) locs = settings.getString("locales");
        } catch(java.util.MissingResourceException e) { }
        if(locs == null) return new Locale[] { };

        ArrayList retl = new ArrayList();
        StringTokenizer toks = new StringTokenizer(locs);
        while(toks.hasMoreTokens()) {
            String f = toks.nextToken();
            String language;
            String country;
            if(f.length() >= 2) {
                language = f.substring(0, 2);
                country = (f.length() >= 5 ? f.substring(3, 5) : null);
            } else {
                language = null;
                country = null;
            }
            if(language != null) {
                Locale loc = country == null ? new Locale(language) : new Locale(language, country);
                retl.add(loc);
            }
        }

        Locale[] ret = new Locale[retl.size()];
        for(int i = 0; i < retl.size(); i++) {
            ret[i] = (Locale) retl.get(i);
        }
        return ret;
    }

    public JMenuItem createLocaleMenuItem() {
        Locale[] locales = getLocaleOptions();
        if(locales == null || locales.length == 0) return null;
        else return new LocaleMenu(locales);
    }
    
    private static String replaceAccents(String src, HashMap repl) {
        // find first non-standard character - so we can avoid the
        // replacement process if possible
        int i = 0;
        int n = src.length();
        for(; i < n; i++) {
            char ci = src.charAt(i);
            if(ci < 32 || ci >= 127) break;
        }
        if(i == n) return src;
        
        // ok, we'll have to consider replacing accents
        char[] cs = src.toCharArray();
        StringBuffer ret = new StringBuffer(src.substring(0, i));
        for(int j = i; j < cs.length; j++) {
            char cj = cs[j];
            if(cj < 32 || cj >= 127) {
                String out = (String) repl.get(new Character(cj));
                if(out != null) {
                    ret.append(out);
                } else {
                    ret.append(cj);
                }
            } else {
                ret.append(cj);
            }
        }
        return ret.toString();
    }
}
