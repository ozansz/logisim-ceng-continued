/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.prefs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LoaderException;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.util.StringUtil;

class TemplateOptions extends OptionsPanel {
    private class MyListener implements ActionListener, PropertyChangeListener {
        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            if(src == templateButton) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(Strings.get("selectDialogTitle"));
                chooser.setApproveButtonText(Strings.get("selectDialogButton"));
                int action = chooser.showOpenDialog(getPreferencesFrame());
                if(action == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    FileReader reader = null;
                    try {
                        Loader loader = new Loader(getPreferencesFrame());
                        reader = new FileReader(file);
                        LogisimFile template = LogisimFile.load(reader, loader);
                        LogisimPreferences.setTemplateFile(file, template);
                        LogisimPreferences.setTemplateType(LogisimPreferences.TEMPLATE_CUSTOM);
                    } catch(LoaderException ex) {
                    } catch(IOException ex) {
                        JOptionPane.showMessageDialog(getPreferencesFrame(),
                                StringUtil.format(Strings.get("templateErrorMessage"), ex.toString()),
                                Strings.get("templateErrorTitle"),
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        try {
                            if(reader != null) reader.close();
                        } catch(IOException ex) { }
                    }
                }
            } else {
                int value = LogisimPreferences.TEMPLATE_UNKNOWN;
                if(plain.isSelected()) value = LogisimPreferences.TEMPLATE_PLAIN;
                else if(empty.isSelected()) value = LogisimPreferences.TEMPLATE_EMPTY;
                else if(custom.isSelected()) value = LogisimPreferences.TEMPLATE_CUSTOM;
                LogisimPreferences.setTemplateType(value);
            }
            computeEnabled();
        }

        public void propertyChange(PropertyChangeEvent event) {
            String prop = event.getPropertyName();
            if(prop.equals(LogisimPreferences.TEMPLATE_TYPE)) {
                int value = LogisimPreferences.getTemplateType();
                plain.setSelected(value == LogisimPreferences.TEMPLATE_PLAIN);
                empty.setSelected(value == LogisimPreferences.TEMPLATE_EMPTY);
                custom.setSelected(value == LogisimPreferences.TEMPLATE_CUSTOM);
            } else if(prop.equals(LogisimPreferences.TEMPLATE_FILE)) {
                setTemplateField((File) event.getNewValue());
            }
        }
        
        private void setTemplateField(File f) {
            try {
                templateField.setText(f == null ? "" : f.getCanonicalPath());
            } catch(IOException e) {
                templateField.setText(f.getName());
            }
            computeEnabled();
        }
        
        private void computeEnabled() {
            custom.setEnabled(!templateField.getText().equals(""));
            templateField.setEnabled(custom.isSelected());
        }
    }
    
    private MyListener myListener = new MyListener();

    private JRadioButton plain = new JRadioButton();
    private JRadioButton empty = new JRadioButton();
    private JRadioButton custom = new JRadioButton();
    private JTextField templateField = new JTextField(40);
    private JButton templateButton = new JButton();

    public TemplateOptions(PreferencesFrame window) {
        super(window);
        
        ButtonGroup bgroup = new ButtonGroup();
        bgroup.add(plain);
        bgroup.add(empty);
        bgroup.add(custom);
        
        plain.addActionListener(myListener);
        empty.addActionListener(myListener);
        custom.addActionListener(myListener);
        templateField.setEditable(false);
        templateButton.addActionListener(myListener);
        myListener.computeEnabled();
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gridbag);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.LINE_START;
        gridbag.setConstraints(plain, gbc); add(plain);
        gridbag.setConstraints(empty, gbc); add(empty);
        gridbag.setConstraints(custom, gbc); add(custom);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = GridBagConstraints.RELATIVE;
        JPanel strut = new JPanel();
        strut.setMinimumSize(new Dimension(50, 1));
        strut.setPreferredSize(new Dimension(50, 1));
        gbc.weightx = 0.0; gridbag.setConstraints(strut, gbc); add(strut);
        gbc.weightx = 1.0; gridbag.setConstraints(templateField, gbc); add(templateField);
        gbc.weightx = 0.0; gridbag.setConstraints(templateButton, gbc); add(templateButton);
        
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.TEMPLATE_TYPE, myListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.TEMPLATE_FILE, myListener);
        switch(LogisimPreferences.getTemplateType()) {
        case LogisimPreferences.TEMPLATE_PLAIN: plain.setSelected(true); break;
        case LogisimPreferences.TEMPLATE_EMPTY: empty.setSelected(true); break;
        case LogisimPreferences.TEMPLATE_CUSTOM: custom.setSelected(true); break;
        }
        myListener.setTemplateField(LogisimPreferences.getTemplateFile());
    }

    public String getTitle() {
        return Strings.get("templateTitle");
    }

    public String getHelpText() {
        return Strings.get("templateHelp");
    }
    
    public void localeChanged() {
        plain.setText(Strings.get("templatePlainOption"));
        empty.setText(Strings.get("templateEmptyOption"));
        custom.setText(Strings.get("templateCustomOption"));
        templateButton.setText(Strings.get("templateSelectButton"));
    }
}
