/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.start;

import java.util.ArrayList;
import java.util.Locale;
import java.io.File;

import javax.swing.UIManager;

import com.cburch.logisim.Main;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Print;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.WindowManagers;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.StringUtil;

public class Startup {
    private static Startup startupTemp = null;

    static void doOpen(File file) {
        if(startupTemp != null) startupTemp.doOpenFile(file);
    }
    static void doPrint(File file) {
        if(startupTemp != null) startupTemp.doPrintFile(file);
    }
    
    private void doOpenFile(File file) {
        if(initialized) {
            ProjectActions.doOpen(null, null, file);
        } else {
            filesToOpen.add(file);
        }
    }

    private void doPrintFile(File file) {
        if(initialized) {
            Project toPrint = ProjectActions.doOpen(null, null, file);
            Print.doPrint(toPrint);
            toPrint.getFrame().dispose();
        } else {
            filesToPrint.add(file);
        }
    }
    
    private static void registerHandler() {
        try {
            Class needed1 = Class.forName("com.apple.eawt.Application");
            if(needed1 == null) return;
            Class needed2 = Class.forName("com.apple.eawt.ApplicationAdapter");
            if(needed2 == null) return;
            MacOsAdapter.register();
            MacOsAdapter.addListeners(true);
        } catch(ClassNotFoundException e) {
            return;
        } catch(Throwable t) {
            try {
                MacOsAdapter.addListeners(false);
            } catch(Throwable t2) { }
        }
    }
    
    // based on command line
    private File templFile = null;
    private boolean templEmpty = false;
    private boolean templPlain = false;
    private ArrayList filesToOpen = new ArrayList();
    private boolean showSplash = true;
    
    // from other sources
    private boolean initialized = false;
    private SplashScreen monitor = null;
    private ArrayList filesToPrint = new ArrayList();

    private Startup() { 
    }

    public void run() {
        // kick off the progress monitor
        // (The values used for progress values are based on a single run where
        // I loaded a large file.)
        if(showSplash) {
            try {
                monitor = new SplashScreen();
                monitor.setVisible(true);
            } catch(Throwable t) {
                monitor = null;
                showSplash = false;
            }
        }
        
        // pre-load the two basic component libraries, just so that the time
        // taken is shown separately in the progress bar.
        if(showSplash) monitor.setProgress(SplashScreen.LIBRARIES);
        Loader templLoader = new Loader(monitor);
        int count = templLoader.getBuiltin().getLibrary("Base").getTools().size()
             + templLoader.getBuiltin().getLibrary("Gates").getTools().size();
        if(count < 0) {
            // this will never happen, but the optimizer doesn't know that...
            System.err.println("FATAL ERROR - no components"); //OK
            System.exit(-1);
        }

        // load in template
        loadTemplate(templLoader, templFile, templEmpty);
        
        // now that the splash screen is almost gone, we do some last-minute
        // interface initialization
        if(showSplash) monitor.setProgress(SplashScreen.GUI_INIT);
        WindowManagers.initialize();
        if(MacCompatibility.isSwingUsingScreenMenuBar()) {
            MacCompatibility.setFramelessJMenuBar(new LogisimMenuBar(null, null));
        } else {
            new LogisimMenuBar(null, null);
            // most of the time occupied here will be in loading menus, which
            // will occur eventually anyway; we might as well do it when the
            // monitor says we are
        }

        // if user has double-clicked a file to open, we'll
        // use that as the file to open now.
        initialized = true;
        
        // load file
        Project proj; // last project created
        if(filesToOpen.isEmpty()) {
            proj = ProjectActions.doNew(monitor);
            proj.setStartupScreen(true); // so that Open will close it.
            if(showSplash) monitor.close();
        } else {
            for(int i = 0; i < filesToOpen.size(); i++) {
                File fileToOpen = (File) filesToOpen.get(i);
                try {
                    proj = ProjectActions.doOpen(monitor, fileToOpen);
                } catch(LoadFailedException ex) {
                    System.err.println(fileToOpen.getName() + ": " + ex.getMessage()); //OK
                    System.exit(-1);
                }
                if(i == 0) { if(showSplash) monitor.close(); monitor = null; }
            }
        }

        for(int i = 0; i < filesToPrint.size(); i++) {
            File fileToPrint = (File) filesToPrint.get(i);
            doPrintFile(fileToPrint);
        }

    }

    private static void setLocale(String lang) {
        Locale[] opts = Strings.getLocaleOptions();
        for(int i = 0; i < opts.length; i++) {
            if(lang.equals(opts[i].toString())) {
                LocaleManager.setLocale(opts[i]);
                return;
            }
        }
        System.err.println(Strings.get("invalidLocaleError")); //OK
        System.err.println(Strings.get("invalidLocaleOptionsHeader")); //OK
        for(int i = 0; i < opts.length; i++) {
            System.err.println("   " + opts[i].toString()); //OK
        }
        System.exit(-1);
    }

    private void loadTemplate(Loader loader, File templFile,
            boolean templEmpty) {
        if(showSplash) monitor.setProgress(SplashScreen.TEMPLATE_OPEN);
        if(templFile != null) {
            try {
                LogisimFile templ = loader.openLogisimFile(templFile);
                LogisimPreferences.setTemplateFile(templFile, templ);
                LogisimPreferences.setTemplateType(LogisimPreferences.TEMPLATE_CUSTOM);
            } catch(LoadFailedException e) {
                if(showSplash) monitor.close();
                System.exit(-1);
            }
        } else if(templEmpty) {
            LogisimPreferences.setTemplateType(LogisimPreferences.TEMPLATE_EMPTY);
        } else if(templPlain) {
            LogisimPreferences.setTemplateType(LogisimPreferences.TEMPLATE_PLAIN);
        }
        LogisimPreferences.getTemplate(loader);
    }
    
    public static Startup parseArgs(String[] args) {
        // set up the Look&Feel to match the platform
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Logisim");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        
        LocaleManager.setReplaceAccents(false);

        // Initializate graphics acceleration if appropriate
        LogisimPreferences.handleGraphicsAcceleration();
        
        Startup ret = new Startup();
        startupTemp = ret;
        registerHandler();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception ex) { }

        // parse arguments
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(arg.equals("-empty")) {
                if(ret.templFile != null || ret.templEmpty || ret.templPlain) {
                    System.err.println(Strings.get("argOneTemplateError")); //OK
                    return null;
                }
                ret.templEmpty = true;
            } else if(arg.equals("-plain")) {
                if(ret.templFile != null || ret.templEmpty || ret.templPlain) {
                    System.err.println(Strings.get("argOneTemplateError")); //OK
                    return null;
                }
                ret.templPlain = true;
            } else if(arg.equals("-version")) {
                System.out.println(Main.VERSION_NAME); //OK
                return null;
            } else if(arg.equals("-gates")) {
                i++;
                if(i >= args.length) printUsage();
                String a = args[i];
                if(a.equals("shaped")) {
                    LogisimPreferences.setGateShape(LogisimPreferences.SHAPE_SHAPED);
                } else if(a.equals("rectangular")) {
                    LogisimPreferences.setGateShape(LogisimPreferences.SHAPE_RECTANGULAR);
                } else {
                    System.err.println(Strings.get("argGatesOptionError")); //OK
                    System.exit(-1);
                }
            } else if(arg.equals("-locale")) {
                i++;
                if(i >= args.length) printUsage();
                setLocale(args[i]);
            } else if(arg.equals("-accents")) {
                i++;
                if(i >= args.length) printUsage();
                String a = args[i];
                if(a.equals("yes")) {
                    LogisimPreferences.setAccentsReplace(false);
                } else if(a.equals("no")) {
                    LogisimPreferences.setAccentsReplace(true);
                } else {
                    System.err.println(Strings.get("argAccentsOptionError")); //OK
                    System.exit(-1);
                }
            } else if(arg.equals("-template")) {
                if(ret.templFile != null || ret.templEmpty || ret.templPlain) {
                    System.err.println(Strings.get("argOneTemplateError")); //OK
                    return null;
                }
                i++;
                if(i >= args.length) printUsage();
                ret.templFile = new File(args[i]);
                if(!ret.templFile.exists()) {
                    System.err.println(StringUtil.format( //OK
                            Strings.get("templateMissingError"), args[i]));
                } else if(!ret.templFile.canRead()) {
                    System.err.println(StringUtil.format( //OK
                            Strings.get("templateCannotReadError"), args[i]));
                }
            } else if(arg.equals("-nosplash")) {
                ret.showSplash = false;
            } else if(arg.equals("-grader")) {
                i++;
                if(i >= args.length) printUsage();
                tr.edu.metu.ceng.ceng232.grader.Settings.loadGradingFile(new File(args[i]));
            } else if(arg.charAt(0) == '-') {
                printUsage();
                return null;
            } else {
                ret.filesToOpen.add(new File(arg));
            }
        }
        return ret;
    }

    private static void printUsage() {
        System.err.println(StringUtil.format(Strings.get("argUsage"), Startup.class.getName())); //OK
        System.err.println(); //OK
        System.err.println(Strings.get("argOptionHeader")); //OK
        System.err.println("   " + Strings.get("argEmptyOption")); //OK
        System.err.println("   " + Strings.get("argPlainOption")); //OK
        System.err.println("   " + Strings.get("argTemplateOption")); //OK
        System.err.println("   " + Strings.get("argGatesOption")); //OK
        System.err.println("   " + Strings.get("argLocaleOption")); //OK
        System.err.println("   " + Strings.get("argAccentsOption")); //OK
        System.err.println("   " + Strings.get("argNoSplashOption")); //OK
        System.err.println("   " + Strings.get("argVersionOption")); //OK
        System.err.println("   " + Strings.get("argHelpOption")); //OK
        System.exit(-1);
    }
}
