/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class WindowMenuManager {
    private WindowMenuManager() { }
    
    private static ArrayList menus = new ArrayList();
    private static ArrayList managers = new ArrayList();
    private static WindowMenuItemManager currentManager = null;
    
    public static void addMenu(WindowMenu menu) {
        for(Iterator it = managers.iterator(); it.hasNext(); ) {
            WindowMenuItemManager manager = (WindowMenuItemManager) it.next();
            manager.createMenuItem(menu);
        }
        menus.add(menu);
    }
    
    // TODO frames should call removeMenu when they're destroyed
    
    public static void addManager(WindowMenuItemManager manager) {
        for(Iterator it = menus.iterator(); it.hasNext(); ) {
            WindowMenu menu = (WindowMenu) it.next();
            manager.createMenuItem(menu);
        }
        managers.add(manager);
    }
    
    public static void removeManager(WindowMenuItemManager manager) {
        for(Iterator it = menus.iterator(); it.hasNext(); ) {
            WindowMenu menu = (WindowMenu) it.next();
            manager.removeMenuItem(menu);
        }
        managers.remove(manager);
    }
    
    static List getMenus() {
        return menus;
    }
    
    static WindowMenuItemManager getCurrentManager() {
        return currentManager;
    }
    
    static void setCurrentManager(WindowMenuItemManager value) {
        if(value == currentManager) return;
        
        boolean doEnable = (currentManager == null) != (value == null);
        if(currentManager == null) setNullItems(false); else currentManager.setSelected(false);
        currentManager = value;
        if(currentManager == null) setNullItems(true); else currentManager.setSelected(true);
        if(doEnable) enableAll();
    }
    
    static void unsetCurrentManager(WindowMenuItemManager value) {
        if(value != currentManager) return;
        setCurrentManager(null);
    }
    
    private static void setNullItems(boolean value) {
        for(Iterator it = menus.iterator(); it.hasNext(); ) {
            WindowMenu menu = (WindowMenu) it.next();
            menu.setNullItemSelected(value);
        }
    }
    
    private static void enableAll() {
        for(Iterator it = menus.iterator(); it.hasNext(); ) {
            WindowMenu menu = (WindowMenu) it.next();
            menu.computeEnabled();
        }
    }
}
