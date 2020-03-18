/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.prefs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.util.StringGetter;

class ExperimentalOptions extends OptionsPanel {
    private static class AccelOption {
        private String value;
        private StringGetter getter;
        
        AccelOption(String value, StringGetter getter) {
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
            if(src == accel) {
                AccelOption x = (AccelOption) accel.getSelectedItem();
                LogisimPreferences.setGraphicsAcceleration(x.value);
                /* This won't take effect until Logisim starts again, due to limitations
                 * of the rendering pipeline and Java's interaction with it. */
            } else if(src == stretchWires) {
                LogisimPreferences.setStretchWires(stretchWires.isSelected());
            }
        }

        public void propertyChange(PropertyChangeEvent event) {
            String prop = event.getPropertyName();
            if(prop.equals(LogisimPreferences.STRETCH_WIRES)) {
                stretchWires.setSelected(LogisimPreferences.getStretchWires());
            }
        }
    }
    
    private MyListener myListener = new MyListener();

    private JLabel accelLabel = new JLabel();
    private JLabel accelRestart = new JLabel();
    private JComboBox accel = new JComboBox();
    private JCheckBox stretchWires = new JCheckBox();

    public ExperimentalOptions(PreferencesFrame window) {
        super(window);
        
        JPanel accelPanel = new JPanel(new BorderLayout());
        accelPanel.add(accelLabel, BorderLayout.LINE_START);
        accelPanel.add(accel, BorderLayout.CENTER);
        accelPanel.add(accelRestart, BorderLayout.PAGE_END);
        accelRestart.setFont(accelRestart.getFont().deriveFont(Font.ITALIC));
        JPanel accelPanel2 = new JPanel();
        accelPanel2.add(accelPanel);
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(Box.createGlue());
        add(accelPanel2);
        add(Box.createGlue());
        add(stretchWires);
        add(Box.createGlue());
        
        stretchWires.addActionListener(myListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.STRETCH_WIRES,
                myListener);
        stretchWires.setSelected(LogisimPreferences.getStretchWires());
        
        accel.addItem(new AccelOption(LogisimPreferences.ACCEL_DEFAULT, Strings.getter("accelDefault")));
        accel.addItem(new AccelOption(LogisimPreferences.ACCEL_NONE, Strings.getter("accelNone")));
        accel.addItem(new AccelOption(LogisimPreferences.ACCEL_OPENGL, Strings.getter("accelOpenGL")));
        accel.addItem(new AccelOption(LogisimPreferences.ACCEL_D3D, Strings.getter("accelD3D")));
        accel.addActionListener(myListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.GRAPHICS_ACCELERATION,
                myListener);
        setAccel(LogisimPreferences.getGraphicsAcceleration());
    }

    public String getTitle() {
        return Strings.get("experimentTitle");
    }

    public String getHelpText() {
        return Strings.get("experimentHelp");
    }
    
    public void localeChanged() {
        stretchWires.setText(Strings.get("stretchWires"));
        accelLabel.setText(Strings.get("accelLabel"));
        accelRestart.setText(Strings.get("accelRestartLabel"));
    }
    
    private void setAccel(String value) {
        for(int i = accel.getItemCount() - 1; i >= 0; i--) {
            AccelOption opt = (AccelOption) accel.getItemAt(i);
            if(opt.value.equals(value)) {
                accel.setSelectedItem(opt);
                return;
            }
        }
        accel.setSelectedItem(accel.getItemAt(0));
    }
}
