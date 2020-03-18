/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.cburch.logisim.analyze.gui.Analyzer;
import com.cburch.logisim.analyze.gui.AnalyzerManager;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.AnalyzeException;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.circuit.Pin;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.StringUtil;

public class ProjectCircuitActions {
    private ProjectCircuitActions() { }
    
    public static void doAddCircuit(Project proj) {
        String name = promptForCircuitName(proj.getFrame(), proj.getLogisimFile(), "");
        if(name != null) {
            Circuit circuit = new Circuit(name);
            proj.doAction(LogisimFileActions.addCircuit(circuit));
            proj.setCurrentCircuit(circuit);
        }
    }

    private static String promptForCircuitName(JFrame frame,
            Library lib, String initialValue) {
        JLabel label = new JLabel(Strings.get("circuitNamePrompt"));
        final JTextField field = new JTextField(15);
        field.setText(initialValue);
        JLabel error = new JLabel(" ");
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        JPanel strut = new JPanel(null);
        strut.setPreferredSize(new Dimension(3 * field.getPreferredSize().width / 2, 0));
        JPanel panel = new JPanel(gb);
        gc.gridx = 0;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_START;
        gb.setConstraints(label, gc); panel.add(label);
        gb.setConstraints(field, gc); panel.add(field);
        gb.setConstraints(error, gc); panel.add(error);
        gb.setConstraints(strut, gc); panel.add(strut);
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        pane.setInitialValue(field);
        JDialog dlog = pane.createDialog(frame, Strings.get("circuitNameDialogTitle"));
        dlog.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent arg0) {
                field.grabFocus();
            }

            public void windowLostFocus(WindowEvent arg0) { }
        });
        
        while(true) {
            field.selectAll();
            field.grabFocus();
            dlog.pack();
            dlog.setVisible(true);
            Object action = pane.getValue();
            if(action == null || !(action instanceof Integer)
                    || ((Integer) action).intValue() != JOptionPane.OK_OPTION) {
                return null;
            }

            String name = field.getText().trim();
            if(name.equals("")) {
                error.setText(Strings.get("circuitNameMissingError"));
            } else {
                if(lib.getTool(name) == null) {
                    return name;
                } else {
                    error.setText(Strings.get("circuitNameDuplicateError"));
                }
            }
        }
    }
    
    public static void doRenameCircuit(Project proj, Circuit circuit) {
        String name = promptForCircuitName(proj.getFrame(), proj.getLogisimFile(), circuit.getName());
        if(name != null) {
            proj.doAction(CircuitActions.setCircuitName(circuit, name));
        }
    }

    public static void doSetAsMainCircuit(Project proj, Circuit circuit) {
        proj.doAction(LogisimFileActions.setMainCircuit(circuit));
    }

    public static void doRemoveCircuit(Project proj, Circuit circuit) {
        if(proj.getLogisimFile().getTools().size() == 1) {
            JOptionPane.showMessageDialog(proj.getFrame(),
                    Strings.get("circuitRemoveLastError"),
                    Strings.get("circuitRemoveErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
        } else if(!proj.getDependencies().canRemove(circuit)) {
            JOptionPane.showMessageDialog(proj.getFrame(),
                Strings.get("circuitRemoveUsedError"),
                Strings.get("circuitRemoveErrorTitle"),
                JOptionPane.ERROR_MESSAGE);
        } else {
            proj.doAction(LogisimFileActions.removeCircuit(circuit));
        }
    }
    
    public static void doAnalyze(Project proj, Circuit circuit) {
        Map pinNames = Analyze.getPinLabels(circuit);
        ArrayList inputNames = new ArrayList();
        ArrayList outputNames = new ArrayList();
        for(Iterator it = pinNames.keySet().iterator(); it.hasNext(); ) {
            Pin pin = (Pin) it.next();
            EndData pinEnd = pin.getEnd(0);
            boolean isOutput = pinEnd.getType() != EndData.OUTPUT_ONLY;
            if(pin.isInputPin()) {
                inputNames.add(pinNames.get(pin));
            } else {
                outputNames.add(pinNames.get(pin));
            }
            if(pin.getEnd(0).getWidth().getWidth() > 1) {
                if(isOutput) {
                    analyzeError(proj, Strings.get("analyzeMultibitOutputError"));
                } else {
                    analyzeError(proj, Strings.get("analyzeMultibitInputError"));
                }
                return;
            }
        }
        if(inputNames.size() > AnalyzerModel.MAX_INPUTS) {
            analyzeError(proj, StringUtil.format(Strings.get("analyzeTooManyInputsError"),
                    "" + AnalyzerModel.MAX_INPUTS));
            return;
        }
        if(outputNames.size() > AnalyzerModel.MAX_OUTPUTS) {
            analyzeError(proj, StringUtil.format(Strings.get("analyzeTooManyOutputsError"),
                    "" + AnalyzerModel.MAX_OUTPUTS));
            return;
        }
        
        Analyzer analyzer = AnalyzerManager.getAnalyzer();
        analyzer.getModel().setCurrentProject(proj);
        configureAnalyzer(proj, circuit, analyzer, pinNames, inputNames, outputNames);
        analyzer.setVisible(true);
        analyzer.toFront();
    }
    
    private static void configureAnalyzer(Project proj, Circuit circuit, Analyzer analyzer,
            Map pinNames, ArrayList inputNames, ArrayList outputNames) {
        analyzer.getModel().setVariables(inputNames, outputNames);
        
        // If there are no inputs, we stop with that tab selected
        if(inputNames.size() == 0) {
            analyzer.setSelectedTab(Analyzer.INPUTS_TAB);
            return;
        }
        
        // If there are no outputs, we stop with that tab selected
        if(outputNames.size() == 0) {
            analyzer.setSelectedTab(Analyzer.OUTPUTS_TAB);
            return;
        }
        
        // Attempt to show the corresponding expression
        try {
            Analyze.computeExpression(analyzer.getModel(), circuit, pinNames);
            analyzer.setSelectedTab(Analyzer.EXPRESSION_TAB);
            return;
        } catch(AnalyzeException ex) {
            JOptionPane.showMessageDialog(proj.getFrame(), ex.getMessage(),
                    Strings.get("analyzeNoExpressionTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        
        // As a backup measure, we compute a truth table.
        Analyze.computeTable(analyzer.getModel(), proj, circuit, pinNames);
        analyzer.setSelectedTab(Analyzer.TABLE_TAB);
    }
        
    private static void analyzeError(Project proj, String message) {
        JOptionPane.showMessageDialog(proj.getFrame(), message,
            Strings.get("analyzeErrorTitle"),
            JOptionPane.ERROR_MESSAGE);
        return;
    }
}
