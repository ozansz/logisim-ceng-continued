/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;

public class InputEventUtil {
    public static String CTRL    = "Ctrl";
    public static String SHIFT   = "Shift";
    public static String BUTTON1 = "Button1";
    public static String BUTTON2 = "Button2";
    public static String BUTTON3 = "Button3";

    private InputEventUtil() { }

    public static int fromString(String str) {
        int ret = 0;
        StringTokenizer toks = new StringTokenizer(str);
        while(toks.hasMoreTokens()) {
            String s = toks.nextToken();
            if(s.equals(CTRL))          ret |= InputEvent.CTRL_MASK;
            else if(s.equals(SHIFT))    ret |= InputEvent.SHIFT_MASK;
            else if(s.equals(BUTTON1))  ret |= InputEvent.BUTTON1_MASK;
            else if(s.equals(BUTTON2))  ret |= InputEvent.BUTTON2_MASK;
            else if(s.equals(BUTTON3))  ret |= InputEvent.BUTTON3_MASK;
            else throw new NumberFormatException("InputEventUtil");
        }
        return ret;
    }

    public static String toString(int mods) {
        ArrayList arr = new ArrayList();
        if((mods & InputEvent.CTRL_MASK)    != 0) arr.add(CTRL);
        if((mods & InputEvent.SHIFT_MASK)   != 0) arr.add(SHIFT);
        if((mods & InputEvent.BUTTON1_MASK) != 0) arr.add(BUTTON1);
        if((mods & InputEvent.BUTTON2_MASK) != 0) arr.add(BUTTON2);
        if((mods & InputEvent.BUTTON3_MASK) != 0) arr.add(BUTTON3);

        if(arr.isEmpty()) return "";

        StringBuffer ret = new StringBuffer();
        Iterator it = arr.iterator();
        ret.append((String) it.next());
        while(it.hasNext()) {
            ret.append(" ");
            ret.append((String) it.next());
        }
        return ret.toString();
    }

    public static int fromDisplayString(String str) {
        int ret = 0;
        StringTokenizer toks = new StringTokenizer(str);
        while(toks.hasMoreTokens()) {
            String s = toks.nextToken();
            if(s.equals(Strings.get("ctrlMod")))            ret |= InputEvent.CTRL_MASK;
            else if(s.equals(Strings.get("shiftMod")))      ret |= InputEvent.SHIFT_MASK;
            else if(s.equals(Strings.get("button1Mod")))    ret |= InputEvent.BUTTON1_MASK;
            else if(s.equals(Strings.get("button2Mod")))    ret |= InputEvent.BUTTON2_MASK;
            else if(s.equals(Strings.get("button3Mod")))    ret |= InputEvent.BUTTON3_MASK;
            else throw new NumberFormatException("InputEventUtil");
        }
        return ret;
    }

    public static String toDisplayString(int mods) {
        ArrayList arr = new ArrayList();
        if((mods & InputEvent.CTRL_MASK)    != 0) arr.add(Strings.get("ctrlMod"));
        if((mods & InputEvent.SHIFT_MASK)   != 0) arr.add(Strings.get("shiftMod"));
        if((mods & InputEvent.BUTTON1_MASK) != 0) arr.add(Strings.get("button1Mod"));
        if((mods & InputEvent.BUTTON2_MASK) != 0) arr.add(Strings.get("button2Mod"));
        if((mods & InputEvent.BUTTON3_MASK) != 0) arr.add(Strings.get("button3Mod"));

        if(arr.isEmpty()) return "";

        StringBuffer ret = new StringBuffer();
        Iterator it = arr.iterator();
        ret.append((String) it.next());
        while(it.hasNext()) {
            ret.append(" ");
            ret.append((String) it.next());
        }
        return ret.toString();
    }

    public static String toKeyDisplayString(int mods) {
        ArrayList arr = new ArrayList();
        if((mods & InputEvent.META_MASK) != 0) arr.add(Strings.get("metaMod"));
        if((mods & InputEvent.ALT_MASK) != 0) arr.add(Strings.get("altMod"));
        if((mods & InputEvent.CTRL_MASK)    != 0) arr.add(Strings.get("ctrlMod"));
        if((mods & InputEvent.SHIFT_MASK)   != 0) arr.add(Strings.get("shiftMod"));

        if(arr.isEmpty()) return "";

        StringBuffer ret = new StringBuffer();
        Iterator it = arr.iterator();
        ret.append((String) it.next());
        while(it.hasNext()) {
            ret.append(" ");
            ret.append((String) it.next());
        }
        return ret.toString();
    }

}
