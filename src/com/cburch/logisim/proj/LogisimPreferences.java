/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.proj;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.PropertyChangeWeakSupport;

public class LogisimPreferences {
    public static final int TEMPLATE_UNKNOWN = -1;
    public static final int TEMPLATE_EMPTY = 0;
    public static final int TEMPLATE_PLAIN = 1;
    public static final int TEMPLATE_CUSTOM = 2;
    
    public static final String TEMPLATE = "template";
    public static final String TEMPLATE_TYPE = "templateType";
    public static final String TEMPLATE_FILE = "templateFile";
    public static final String ACCENTS_REPLACE = "accentsReplace"; 
    public static final String GATE_SHAPE = "gateShape";
    public static final String GRAPHICS_ACCELERATION = "graphicsAcceleration";
    public static final String STRETCH_WIRES = "stretchWires";
    
    public static final String SHAPE_SHAPED = "shaped";
    public static final String SHAPE_RECTANGULAR = "rectangular";
    public static final String SHAPE_DIN40700 = "din40700";
    
    public static final String ACCEL_DEFAULT = "default";
    public static final String ACCEL_NONE = "none";
    public static final String ACCEL_OPENGL = "opengl";
    public static final String ACCEL_D3D = "d3d";
    
    // class variables for holding individual preferences
    private static int templateType = TEMPLATE_PLAIN;
    private static File templateFile = null;
    private static boolean accentsReplace = false;
    private static String gateShape = SHAPE_SHAPED;
    private static String graphicsAccel = ACCEL_DEFAULT;
    private static boolean stretchWires = false;

    // class variables for maintaining consistency between properties,
    // internal variables, and other classes
    private static Preferences prefs = null;
    private static MyListener myListener = null;
    private static PropertyChangeWeakSupport propertySupport
        = new PropertyChangeWeakSupport(LogisimPreferences.class);

    // class variables for holding the current template
    private static LogisimFile plainTemplate = null;
    private static LogisimFile emptyTemplate = null;
    private static LogisimFile customTemplate = null;
    private static File customTemplateFile = null;
    
    //
    // methods for accessing preferences
    //
    private static class MyListener implements PreferenceChangeListener {
        public void preferenceChange(PreferenceChangeEvent event) {
            Preferences prefs = event.getNode();
            String prop = event.getKey();
            if(prop.equals(ACCENTS_REPLACE)) {
                boolean oldValue = accentsReplace;
                boolean value = prefs.getBoolean(ACCENTS_REPLACE, false);
                if(value != oldValue) {
                    accentsReplace = value;
                    LocaleManager.setReplaceAccents(accentsReplace);
                    propertySupport.firePropertyChange(ACCENTS_REPLACE, oldValue, value);
                }
            } else if(prop.equals(STRETCH_WIRES)) {
                boolean oldValue = stretchWires;
                boolean value = prefs.getBoolean(STRETCH_WIRES, false);
                if(value != oldValue) {
                    stretchWires = value;
                    propertySupport.firePropertyChange(STRETCH_WIRES, oldValue, value);
                }
            } else if(prop.equals(GATE_SHAPE)) {
                String oldValue = gateShape;
                String value = prefs.get(GATE_SHAPE, SHAPE_SHAPED).toLowerCase();
                if(!value.equals(oldValue)) {
                    if(value.equals(SHAPE_RECTANGULAR)) gateShape = SHAPE_RECTANGULAR;
                    else if(value.equals(SHAPE_DIN40700)) gateShape = SHAPE_DIN40700;
                    else gateShape = SHAPE_SHAPED;
                    propertySupport.firePropertyChange(GATE_SHAPE, oldValue, value);
                }
            } else if(prop.equals(TEMPLATE_TYPE)) {
                int oldValue = templateType;
                int value = prefs.getInt(TEMPLATE_TYPE, TEMPLATE_UNKNOWN);
                if(value != oldValue) {
                    templateType = value;
                    propertySupport.firePropertyChange(TEMPLATE, oldValue, value);
                    propertySupport.firePropertyChange(TEMPLATE_TYPE, oldValue, value);
                }
            } else if(prop.equals(TEMPLATE_FILE)) {
                File oldValue = templateFile;
                File value = convertFile(prefs.get(TEMPLATE_FILE, null));
                if(value == null ? oldValue != null : !value.equals(oldValue)) {
                    templateFile = value;
                    if(templateType == TEMPLATE_CUSTOM) {
                        customTemplate = null;
                        propertySupport.firePropertyChange(TEMPLATE, oldValue, value);
                    }
                    propertySupport.firePropertyChange(TEMPLATE_FILE, oldValue, value);
                }
            } else if(prop.equals(GRAPHICS_ACCELERATION)) {
                String oldValue = graphicsAccel;
                String value = prefs.get(GRAPHICS_ACCELERATION, ACCEL_DEFAULT).toLowerCase();
                if(!value.equals(oldValue)) {
                    if(value.equals(ACCEL_NONE)) graphicsAccel = ACCEL_NONE;
                    else if(value.equals(ACCEL_OPENGL)) graphicsAccel = ACCEL_OPENGL;
                    else if(value.equals(ACCEL_D3D)) graphicsAccel = ACCEL_D3D;
                    else graphicsAccel = ACCEL_DEFAULT;
                    /* I'm not supposed to change this after the application starts,
                     * so we wouldn't handle graphics acceleration here. */
                    propertySupport.firePropertyChange(GRAPHICS_ACCELERATION, oldValue, value);
                }
            }
        }
    }
    
    private static Preferences getPrefs() {
        if(prefs == null) {
            synchronized(LogisimPreferences.class) {
                if(prefs == null) {
                    Preferences p = Preferences.userNodeForPackage(Main.class);
                    myListener = new MyListener();
                    p.addPreferenceChangeListener(myListener);
                    prefs = p;

                    setGraphicsAcceleration(p.get(GRAPHICS_ACCELERATION, ACCEL_DEFAULT));
                    setStretchWires(p.getBoolean(STRETCH_WIRES, false));
                    setAccentsReplace(p.getBoolean(ACCENTS_REPLACE, false));
                    setGateShape(p.get(GATE_SHAPE, SHAPE_SHAPED));
                    setTemplateFile(convertFile(p.get(TEMPLATE_FILE, null)));
                    setTemplateType(p.getInt(TEMPLATE_TYPE, TEMPLATE_PLAIN));
                }
            }
        }
        return prefs;
    }
    
    private static File convertFile(String fileName) {
        if(fileName == null || fileName.equals("")) {
            return null;
        } else {
            File file = new File(fileName);
            return file.canRead() ? file : null;
        }
    }
    
    //
    // PropertyChangeSource methods
    //
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    public static void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }
    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    public static void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }

    //
    // accessor methods
    //
    public static int getTemplateType() {
        getPrefs();
        return templateType;
    }
    
    public static void setTemplateType(int value) {
        getPrefs();
        if(value != TEMPLATE_PLAIN && value != TEMPLATE_EMPTY && value != TEMPLATE_CUSTOM) {
            value = TEMPLATE_UNKNOWN;
        }
        if(value == TEMPLATE_CUSTOM && templateFile == null) {
            value = TEMPLATE_UNKNOWN;
        }
        if(value != TEMPLATE_UNKNOWN && templateType != value) {
            getPrefs().putInt(TEMPLATE_TYPE, value);
        }
    }
    
    public static File getTemplateFile() {
        getPrefs();
        return templateFile;
    }
    
    private static void setTemplateFile(File value) {
        getPrefs();
        setTemplateFile(value, null);
    }
    
    public static void setTemplateFile(File value, LogisimFile template) {
        getPrefs();
        if(value != null && !value.canRead()) value = null;
        if(value == null ? templateFile != null : !value.equals(templateFile)) {
            try {
                customTemplateFile = template == null ? null : value;
                customTemplate = template;
                getPrefs().put(TEMPLATE_FILE, value == null ? "" : value.getCanonicalPath());
            } catch(IOException ex) { }
        }
    }
    
    public static String getGraphicsAcceleration() {
        getPrefs();
        return graphicsAccel;
    }

    public static void setGraphicsAcceleration(String value) {
        getPrefs();
        if(graphicsAccel != value) {
            getPrefs().put(GRAPHICS_ACCELERATION, value.toLowerCase());
        }
    }
    
    public static void handleGraphicsAcceleration() {
        String accel = getGraphicsAcceleration();
        try {
            if(accel == ACCEL_NONE) {
                System.setProperty("sun.java2d.opengl", "False");
                System.setProperty("sun.java2d.d3d", "False");
            } else if(accel == ACCEL_OPENGL) {
                System.setProperty("sun.java2d.opengl", "True");
                System.setProperty("sun.java2d.d3d", "False");
            } else if(accel == ACCEL_D3D) {
                System.setProperty("sun.java2d.opengl", "False");
                System.setProperty("sun.java2d.d3d", "True");
            }
        } catch(Throwable t) { }
    }
    
    public static boolean getStretchWires() {
        getPrefs();
        return stretchWires;
    }
    
    public static void setStretchWires(boolean value) {
        getPrefs();
        if(stretchWires != value) {
            getPrefs().putBoolean(STRETCH_WIRES, value);
        }
    }
    
    public static boolean getAccentsReplace() {
        getPrefs();
        return accentsReplace;
    }
    
    public static void setAccentsReplace(boolean value) {
        getPrefs();
        if(accentsReplace != value) {
            getPrefs().putBoolean(ACCENTS_REPLACE, value);
        }
    }
    
    public static String getGateShape() {
        getPrefs();
        return gateShape;
    }
    
    public static void setGateShape(String value) {
        getPrefs();
        if(!gateShape.equals(value)) {
            getPrefs().put(GATE_SHAPE, value.toLowerCase());
        }
    }
    
    //
    // template methods
    //
    public static LogisimFile getTemplate(Loader loader) {
        getPrefs();
        switch(templateType) {
        case TEMPLATE_PLAIN: return getPlainTemplate(loader);
        case TEMPLATE_EMPTY: return getEmptyTemplate(loader);
        case TEMPLATE_CUSTOM: return getCustomTemplate(loader);
        default: return getPlainTemplate(loader);
        }
    }
    
    private static LogisimFile getPlainTemplate(Loader loader) {
        if(plainTemplate == null) {
            ClassLoader ld = Startup.class.getClassLoader();
            InputStream in = ld.getResourceAsStream("com/cburch/logisim/resources/default.templ");
            if(in == null) {
                plainTemplate = getEmptyTemplate(loader); 
            } else {
                try {
                    Reader templReader = new InputStreamReader(in);
                    plainTemplate = loader.openLogisimFile(templReader);
                    templReader.close();
                } catch(Throwable e) {
                    plainTemplate = getEmptyTemplate(loader);
                }
            }
        }
        Circuit circ = plainTemplate.getCircuit("main");
        if(circ != null) circ.setName(Strings.get("newCircuitName"));
        return plainTemplate;
    }
    
    private static LogisimFile getEmptyTemplate(Loader loader) {
        if(emptyTemplate == null) {
            emptyTemplate = LogisimFile.createNew(loader);
        }
        Circuit circ = emptyTemplate.getCircuit("main");
        if(circ != null) circ.setName(Strings.get("newCircuitName"));
        return emptyTemplate;
    }
    
    private static LogisimFile getCustomTemplate(Loader loader) {
        if(customTemplateFile == null || !(customTemplateFile.equals(templateFile))) {
            if(templateFile == null) {
                customTemplate = null;
                customTemplateFile = null;
            } else {
                try {
                    customTemplate = loader.openLogisimFile(templateFile);
                    customTemplateFile = templateFile;
                } catch(Throwable t) {
                    setTemplateFile(null);
                    customTemplate = null;
                    customTemplateFile = null;
                }
            }
        }
        return customTemplate == null ? getPlainTemplate(loader) : customTemplate;
    }
}
