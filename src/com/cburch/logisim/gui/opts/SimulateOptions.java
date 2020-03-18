/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.opts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.util.IntegerFactory;
import com.cburch.logisim.util.TableLayout;

class SimulateOptions extends OptionsPanel {
    private class MyListener implements ActionListener, AttributeListener {
        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if(source == simLimit) {
                Integer opt = (Integer) simLimit.getSelectedItem();
                if(opt != null) {
                    AttributeSet attrs = getOptions().getAttributeSet();
                    getProject().doAction(OptionsActions.setAttribute(attrs, Options.sim_limit_attr,
                        opt));
                }
            } else if(source == simRandomness) {
                AttributeSet attrs = getOptions().getAttributeSet();
                getProject().doAction(OptionsActions.setAttribute(attrs, Options.sim_rand_attr,
                        simRandomness.isSelected() ? Options.sim_rand_dflt : IntegerFactory.ZERO));
            }
        }
        
        public void attributeListChanged(AttributeEvent e) { }
        public void attributeValueChanged(AttributeEvent e) {
            Attribute attr = e.getAttribute();
            Object val = e.getValue();
            if(attr == Options.sim_limit_attr) {
                loadSimLimit((Integer) val);
            } else if(attr == Options.sim_rand_attr) {
                loadSimRandomness((Integer) val);
            }
        }

        private void loadSimLimit(Integer val) {
            int value = val.intValue();
            ComboBoxModel model = simLimit.getModel();
            for(int i = 0; i < model.getSize(); i++) {
                Integer opt = (Integer) model.getElementAt(i);
                if(opt.intValue() == value) {
                    simLimit.setSelectedItem(opt);
                }
            }
        }
        
        private void loadSimRandomness(Integer val) {
            simRandomness.setSelected(val.intValue() > 0);
        }
    }
    
    private MyListener myListener = new MyListener();

    private JLabel simLimitLabel = new JLabel();
    private JComboBox simLimit = new JComboBox(new Integer[] {
            IntegerFactory.create(200),
            IntegerFactory.create(500),
            IntegerFactory.create(1000),
            IntegerFactory.create(2000),
            IntegerFactory.create(5000),
            IntegerFactory.create(10000),
            IntegerFactory.create(20000),
            IntegerFactory.create(50000),
    });
    private JCheckBox simRandomness = new JCheckBox();

    public SimulateOptions(OptionsFrame window) {
        super(window);
        
        JPanel simLimitPanel = new JPanel();
        simLimitPanel.add(simLimitLabel);
        simLimitPanel.add(simLimit);
        simLimit.addActionListener(myListener);
        
        simRandomness.addActionListener(myListener);

        setLayout(new TableLayout(1));
        add(simRandomness);
        add(simLimitPanel);
        
        window.getOptions().getAttributeSet().addAttributeListener(myListener);
        AttributeSet attrs = getOptions().getAttributeSet();
        myListener.loadSimLimit((Integer) attrs.getValue(Options.sim_limit_attr));
        myListener.loadSimRandomness((Integer) attrs.getValue(Options.sim_rand_attr));
    }

    public String getTitle() {
        return Strings.get("simulateTitle");
    }

    public String getHelpText() {
        return Strings.get("simulateHelp");
    }
    
    public void localeChanged() {
        simRandomness.setText(Strings.get("simulateRandomness"));
        simLimitLabel.setText(Strings.get("simulateLimit"));
    }
}
