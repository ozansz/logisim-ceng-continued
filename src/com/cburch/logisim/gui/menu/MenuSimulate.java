/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringUtil;

import java.util.ArrayList;

class MenuSimulate extends JMenu {
    private class TickFrequencyChoice extends JRadioButtonMenuItem
            implements ActionListener {
        private String hertz;
        private int wavelength;

        public TickFrequencyChoice(double value) {
            if(Math.abs(value - Math.round(value)) < 0.0001) {
                hertz = "" + (int) Math.round(value);
            } else {
                hertz = "" + value;
            }
            wavelength = (int) Math.round(1000.0 / value);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            if(currentSim != null) currentSim.setTickFrequency(wavelength);
        }

        public void localeChanged() {
            setText(StringUtil.format(Strings.get("simulateTickFreqItem"), hertz));
        }
    }

    private class CircuitStateMenuItem extends JMenuItem
            implements CircuitListener, ActionListener {
        private CircuitState circuitState;

        public CircuitStateMenuItem(CircuitState circuitState) {
            this.circuitState = circuitState;

            Circuit circuit = circuitState.getCircuit();
            circuit.addCircuitListener(this);
            this.setText(circuit.getName());
            addActionListener(this);
        }
        
        void unregister() {
            Circuit circuit = circuitState.getCircuit();
            circuit.removeCircuitListener(this);
        }

        public void circuitChanged(CircuitEvent event) {
            if(event.getAction() == CircuitEvent.ACTION_SET_NAME) {
                this.setText(circuitState.getCircuit().getName());
            }
        }

        public void actionPerformed(ActionEvent e) {
            menubar.fireStateChanged(currentSim, circuitState);
        }
    }

    private class MyListener implements ActionListener, SimulatorListener,
            ChangeListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            Project proj = menubar.getProject();
            Simulator sim = proj == null ? null : proj.getSimulator();
            if(src == run) {
                boolean value = run.isSelected();
                if(sim != null) {
                    sim.setIsRunning(value);
                    proj.repaintCanvas();
                }
            } else if(src == reset) {
                if(sim != null) sim.requestReset();
            } else if(src == step) {
                if(sim != null) sim.step();
            } else if(src == tickOnce) {
                if(sim != null) sim.tick();
            } else if(src == ticksEnabled) {
                boolean value = ticksEnabled.isSelected();
                if(sim != null) sim.setIsTicking(value);
            } else if(src == log) {
                LogFrame frame = menubar.getProject().getLogFrame(true);
                frame.setVisible(true);
            }
        }

        public void propagationCompleted(SimulatorEvent e) { }
        public void tickCompleted(SimulatorEvent e) { }
        public void simulatorStateChanged(SimulatorEvent e) {
            Simulator sim = e.getSource();
            if(sim != currentSim) return;
            run.setSelected(sim.isRunning());
            ticksEnabled.setEnabled(sim.isRunning());
            ticksEnabled.setSelected(sim.isTicking());
            int wavelength = sim.getTickFrequency();
            for(int i = 0; i < tickFreqs.length; i++) {
                TickFrequencyChoice item = tickFreqs[i];
                item.setSelected(wavelength == item.wavelength);
            }
        }

        public void stateChanged(ChangeEvent e) {
            step.setEnabled(run.isEnabled() && !run.isSelected());
        }
    }

    private LogisimMenuBar menubar;
    private MyListener myListener = new MyListener();
    private CircuitState currentState = null;
    private CircuitState bottomState = null;
    private Simulator currentSim = null;

    private JCheckBoxMenuItem run = new JCheckBoxMenuItem();
    private JMenuItem reset = new JMenuItem();
    private JMenuItem step = new JMenuItem();
    private JCheckBoxMenuItem ticksEnabled = new JCheckBoxMenuItem();
    private JMenuItem tickOnce = new JMenuItem();
    private JMenu tickFreq = new JMenu();
    private TickFrequencyChoice[] tickFreqs = {
        new TickFrequencyChoice(1024),
        new TickFrequencyChoice(512),
        new TickFrequencyChoice(256),
        new TickFrequencyChoice(64),
        new TickFrequencyChoice(32),
        new TickFrequencyChoice(16),
        new TickFrequencyChoice(8),
        new TickFrequencyChoice(4),
        new TickFrequencyChoice(2),
        new TickFrequencyChoice(1),
        new TickFrequencyChoice(0.5),
        new TickFrequencyChoice(0.25),
    };
    private JMenu downStateMenu = new JMenu();
    private ArrayList downStateItems = new ArrayList();
    private JMenu upStateMenu = new JMenu();
    private ArrayList upStateItems = new ArrayList();
    private JMenuItem log = new JMenuItem();

    public MenuSimulate(LogisimMenuBar menubar) {
        this.menubar = menubar;

        int menuMask = getToolkit().getMenuShortcutKeyMask();
        run.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_E, menuMask));
        reset.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_R, menuMask));
        step.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_I, menuMask));
        tickOnce.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_T, menuMask));

        ButtonGroup bgroup = new ButtonGroup();
        for(int i = 0; i < tickFreqs.length; i++) {
            bgroup.add(tickFreqs[i]);
            tickFreq.add(tickFreqs[i]);
        }

        add(run);
        add(reset);
        add(step);
        addSeparator();
        add(upStateMenu);
        add(downStateMenu);
        addSeparator();
        add(tickOnce);
        add(ticksEnabled);
        add(tickFreq);
        addSeparator();
        add(log);

        setEnabled(false);
        run.setEnabled(false);
        reset.setEnabled(false);
        step.setEnabled(false);
        upStateMenu.setEnabled(false);
        downStateMenu.setEnabled(false);
        tickOnce.setEnabled(false);
        ticksEnabled.setEnabled(false);
        tickFreq.setEnabled(false);
        
        run.addChangeListener(myListener);
        run.addActionListener(myListener);
        reset.addActionListener(myListener);
        step.addActionListener(myListener);
        tickOnce.addActionListener(myListener);
        ticksEnabled.addActionListener(myListener);
        log.addActionListener(myListener);
    }

    public void localeChanged() {
        this.setText(Strings.get("simulateMenu"));
        run.setText(Strings.get("simulateRunItem"));
        reset.setText(Strings.get("simulateResetItem"));
        step.setText(Strings.get("simulateStepItem"));
        tickOnce.setText(Strings.get("simulateTickOnceItem"));
        ticksEnabled.setText(Strings.get("simulateTickItem"));
        tickFreq.setText(Strings.get("simulateTickFreqMenu"));
        for(int i = 0; i < tickFreqs.length; i++) {
            tickFreqs[i].localeChanged();
        }
        downStateMenu.setText(Strings.get("simulateDownStateMenu"));
        upStateMenu.setText(Strings.get("simulateUpStateMenu"));
        log.setText(Strings.get("simulateLogItem"));
    }
    
    public void setCurrentState(Simulator sim, CircuitState value) {
        if(currentState == value) return;
        Simulator oldSim = currentSim;
        CircuitState oldState = currentState;
        currentSim = sim;
        currentState = value;
        if(bottomState == null) {
            bottomState = currentState;
        } else if(currentState == null) {
            bottomState = null;
        } else {
            CircuitState cur = bottomState;
            while(cur != null && cur != currentState) {
                cur = cur.getParentState();
            }
            if(cur == null) bottomState = currentState;
        }

        boolean oldPresent = oldState != null;
        boolean present = currentState != null;
        if(oldPresent != present) {
            setEnabled(present);
            run.setEnabled(present);
            reset.setEnabled(present);
            step.setEnabled(present && !run.isSelected());
            upStateMenu.setEnabled(present);
            downStateMenu.setEnabled(present);
            tickOnce.setEnabled(present);
            ticksEnabled.setEnabled(present);
            tickFreq.setEnabled(present);
        }

        if(currentSim != oldSim) {
            int wavelength = currentSim == null ? -1 : currentSim.getTickFrequency();
            for(int i = 0; i < tickFreqs.length; i++) {
                tickFreqs[i].setSelected(Math.abs(tickFreqs[i].wavelength - wavelength) < 0.001);
            }

            if(oldSim != null) oldSim.removeSimulatorListener(myListener);
            if(currentSim != null) currentSim.addSimulatorListener(myListener);
            myListener.simulatorStateChanged(new SimulatorEvent(sim));
        }

        clearItems(downStateItems);
        CircuitState cur = bottomState;
        while(cur != null && cur != currentState) {
            downStateItems.add(new CircuitStateMenuItem(cur));
            cur = cur.getParentState();
        }
        if(cur != null) cur = cur.getParentState();
        clearItems(upStateItems);
        while(cur != null) {
            upStateItems.add(0, new CircuitStateMenuItem(cur));
            cur = cur.getParentState();
        }
        recreateStateMenus();
    }
    
    private void clearItems(ArrayList items) {
        for(int i = 0; i < items.size(); i++) {
            CircuitStateMenuItem item = (CircuitStateMenuItem) items.get(i);
            item.unregister();
        }
        items.clear();
    }

    private void recreateStateMenus() {
        recreateStateMenu(downStateMenu, downStateItems, KeyEvent.VK_DOWN);
        recreateStateMenu(upStateMenu, upStateItems, KeyEvent.VK_UP);
    }
    
    private void recreateStateMenu(JMenu menu, ArrayList items, int code) {
        menu.removeAll();
        menu.setEnabled(items.size() > 0);
        boolean first = true;
        for(int i = items.size() - 1; i >= 0; i--) {
            JMenuItem item = (JMenuItem) items.get(i);
            menu.add(item);
            if(first) {
                int mask = getToolkit().getMenuShortcutKeyMask();
                item.setAccelerator(KeyStroke.getKeyStroke(code, mask));
                first = false;
            } else {
                item.setAccelerator(null);
            }
        }
    }
}
