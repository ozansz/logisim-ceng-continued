/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.prefs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

class IntlOptions extends OptionsPanel {
    private static class LocaleOption {
        private Locale locale;
        
        LocaleOption(Locale locale) {
            this.locale = locale;
        }
        
        public String toString() {
            return locale.getDisplayName();
        }
    }
    
    private static class ShapeOption {
        private String value;
        private StringGetter getter;
        
        ShapeOption(String value, StringGetter getter) {
            this.value = value;
            this.getter = getter;
        }
        
        public String toString() {
            return getter.get();
        }
    }
    
    private class MyListener implements ActionListener, PropertyChangeListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if(src == gateShape) {
                ShapeOption x = (ShapeOption) gateShape.getSelectedItem();
                LogisimPreferences.setGateShape(x.value);
            } else if(src == locale) {
                LocaleOption opt = (LocaleOption) locale.getSelectedItem();
                if(opt != null) {
                    LocaleManager.setLocale(opt.locale);
                }
            } else if(src == replaceAccents) {
                LogisimPreferences.setAccentsReplace(replaceAccents.isSelected());
            }
        }

        public void propertyChange(PropertyChangeEvent event) {
            String prop = event.getPropertyName();
            if(prop.equals(LogisimPreferences.ACCENTS_REPLACE)) {
                replaceAccents.setSelected(LogisimPreferences.getAccentsReplace());
            } else if(prop.equals(LogisimPreferences.GATE_SHAPE)) {
                setGateSelection(LogisimPreferences.getGateShape());
            }
        }
    }
    
    private MyListener myListener = new MyListener();

    private JLabel localeLabel = new JLabel();
    private JComboBox locale = new JComboBox();
    private JCheckBox replaceAccents = new JCheckBox();
    private JLabel gateShapeLabel = new JLabel();
    private JComboBox gateShape = new JComboBox();

    public IntlOptions(PreferencesFrame window) {
        super(window);
        
        JPanel localePanel = new JPanel();
        localePanel.add(localeLabel);
        localePanel.add(locale);
        
        JPanel shapePanel = new JPanel();
        shapePanel.add(gateShapeLabel);
        shapePanel.add(gateShape);
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(Box.createGlue());
        add(shapePanel);
        add(localePanel);
        add(replaceAccents);
        add(Box.createGlue());
        
        Locale[] opts = Strings.getLocaleOptions();
        Locale dfltLocale = LocaleManager.getLocale();
        LocaleOption dfltOpt = null;
        for(int i = 0; i < opts.length; i++) {
            LocaleOption opt = new LocaleOption(opts[i]);
            if(opts[i].equals(dfltLocale)) dfltOpt = opt;
            locale.addItem(opt);
        }
        if(dfltOpt != null) locale.setSelectedItem(dfltOpt);
        locale.addActionListener(myListener);
        
        replaceAccents.addActionListener(myListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.ACCENTS_REPLACE,
                myListener);
        replaceAccents.setSelected(LogisimPreferences.getAccentsReplace());
        
        gateShape.addItem(new ShapeOption(LogisimPreferences.SHAPE_SHAPED, Strings.getter("shapeShaped")));
        gateShape.addItem(new ShapeOption(LogisimPreferences.SHAPE_RECTANGULAR, Strings.getter("shapeRectangular")));
        gateShape.addItem(new ShapeOption(LogisimPreferences.SHAPE_DIN40700, Strings.getter("shapeDIN40700")));
        gateShape.addActionListener(myListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.GATE_SHAPE,
                myListener);
        setGateSelection(LogisimPreferences.getGateShape());
    }

    public String getTitle() {
        return Strings.get("intlTitle");
    }

    public String getHelpText() {
        return Strings.get("intlHelp");
    }
    
    public void localeChanged() {
        localeLabel.setText(Strings.get("intlLocale"));
        replaceAccents.setText(Strings.get("intlReplaceAccents"));
        replaceAccents.setEnabled(LocaleManager.canReplaceAccents());
        gateShapeLabel.setText(Strings.get("intlGateShape"));
        
        Locale selectedLocale = LocaleManager.getLocale();
        ComboBoxModel model = locale.getModel();
        for(int n = model.getSize() - 1; n >= 0; n--) {
            LocaleOption opt = (LocaleOption) model.getElementAt(n);
            if(opt.locale == selectedLocale) {
                locale.setSelectedItem(opt);
            }
        }
    }
    
    private void setGateSelection(String value) {
        for(int i = gateShape.getItemCount() - 1; i >= 0; i--) {
            ShapeOption opt = (ShapeOption) gateShape.getItemAt(i);
            if(opt.value.equals(value)) {
                gateShape.setSelectedItem(opt);
                return;
            }
        }
        gateShape.setSelectedItem(gateShape.getItemAt(0));
    }
}
