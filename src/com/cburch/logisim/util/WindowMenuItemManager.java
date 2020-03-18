/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JRadioButtonMenuItem;

public abstract class WindowMenuItemManager {
    private class MyListener implements WindowListener {
        public void windowOpened(WindowEvent event) { }
        public void windowClosing(WindowEvent event) {
            JFrame frame = getJFrame(false);
            if(frame.getDefaultCloseOperation() == JFrame.HIDE_ON_CLOSE) {
                removeFromManager();
            }
        }
        
        public void windowClosed(WindowEvent event) {
            removeFromManager();
        }
        
        public void windowDeiconified(WindowEvent event) { }

        public void windowIconified(WindowEvent event) {
            addToManager();
            WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
        }

        public void windowActivated(WindowEvent event) {
            addToManager();
            WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
        }

        public void windowDeactivated(WindowEvent event) {
            WindowMenuManager.unsetCurrentManager(WindowMenuItemManager.this);
        }
    }
    
    private MyListener myListener = new MyListener();
    private String text;
    private boolean persistent;
    private boolean listenerAdded = false;
    private boolean inManager = false;
    private HashMap menuItems = new HashMap(); // MenuWindow -> JRadioButtonMenuItem
    
    public WindowMenuItemManager(String text, boolean persistent) {
        this.text = text;
        this.persistent = persistent;
        if(persistent) {
            WindowMenuManager.addManager(this);
        }
    }
    
    public abstract JFrame getJFrame(boolean create);
    
    public void frameOpened(JFrame frame) {
        if(!listenerAdded) {
            frame.addWindowListener(myListener);
            listenerAdded = true;
        }
        addToManager();
        WindowMenuManager.setCurrentManager(this);
    }

    public void frameClosed(JFrame frame) {
        if(!persistent) {
            if(listenerAdded) {
                frame.removeWindowListener(myListener);
                listenerAdded = false;
            }
            removeFromManager();
        }
    }
    
    private void addToManager() {
        if(!persistent && !inManager) {
            WindowMenuManager.addManager(this);
            inManager = true;
        }
    }
    
    private void removeFromManager() {
        if(!persistent && inManager) {
            inManager = false;
            for(Iterator it = WindowMenuManager.getMenus().iterator(); it.hasNext(); ) {
                WindowMenu menu = (WindowMenu) it.next();
                JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) menuItems.get(menu);
                menu.removeMenuItem(this, menuItem);
            }
            WindowMenuManager.removeManager(this);
        }
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String value) {
        text = value;
        for(Iterator it = menuItems.values().iterator(); it.hasNext(); ) {
            JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) it.next();
            menuItem.setText(text);
        }
    }
    
    JRadioButtonMenuItem getMenuItem(WindowMenu key) {
        return (JRadioButtonMenuItem) menuItems.get(key);
    }
    
    void createMenuItem(WindowMenu menu) {
        WindowMenuItem ret = new WindowMenuItem(this);
        menuItems.put(menu, ret);
        menu.addMenuItem(this, ret, persistent);
    }
    
    void removeMenuItem(WindowMenu menu) {
        JRadioButtonMenuItem item = (JRadioButtonMenuItem) menuItems.remove(menu);
        if(item != null) menu.removeMenuItem(this, item);
    }

    void setSelected(boolean selected) {
        for(Iterator it = menuItems.values().iterator(); it.hasNext(); ) {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem) it.next();
            item.setSelected(selected);
        }
    }
}
