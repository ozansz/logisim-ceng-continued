/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.start;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import net.roydesign.event.ApplicationEvent;
import net.roydesign.mac.MRJAdapter;

//MAC import com.apple.eawt.Application;
//MAC import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.ProjectActions;

class MacOsAdapter extends ApplicationAdapter {
    
    private static class MyListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            ApplicationEvent event2 = (ApplicationEvent) event;
            int type = event2.getType();
            switch(type) {
            case ApplicationEvent.ABOUT:
                About.showAboutDialog(null);
                break;
            case ApplicationEvent.QUIT_APPLICATION:
                ProjectActions.doQuit();
                break;
            case ApplicationEvent.OPEN_DOCUMENT:
                Startup.doOpen(event2.getFile());
                break;
            case ApplicationEvent.PRINT_DOCUMENT:
                Startup.doPrint(event2.getFile());
                break;
            case ApplicationEvent.PREFERENCES:
                PreferencesFrame.showPreferences();
                break;
            }
        }
    }
    
    static void addListeners(boolean added) {
        MyListener myListener = new MyListener();
        if(!added) MRJAdapter.addOpenDocumentListener(myListener);
        if(!added) MRJAdapter.addPrintDocumentListener(myListener);
        MRJAdapter.addPreferencesListener(myListener);
        MRJAdapter.addQuitApplicationListener(myListener);
        MRJAdapter.addAboutListener(myListener);
    }
    
    public void handleOpenFile(com.apple.eawt.ApplicationEvent event) {
        Startup.doOpen(new File(event.getFilename()));
    }
    
    public void handlePrintFile(com.apple.eawt.ApplicationEvent event) {
        Startup.doPrint(new File(event.getFilename()));
    }
    
    public void handlePreferences(com.apple.eawt.ApplicationEvent event) {
        PreferencesFrame.showPreferences();
    }

    public static void register() {
        Application.getApplication().addApplicationListener(new MacOsAdapter());
    }
}