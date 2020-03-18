/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.analyze.model;

import java.util.List;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.proj.Project;

public class AnalyzerModel {
    public static final int MAX_INPUTS = 12;
    public static final int MAX_OUTPUTS = 12;
    
    private VariableList inputs = new VariableList(MAX_INPUTS);
    private VariableList outputs = new VariableList(MAX_OUTPUTS);
    private TruthTable table;
    private OutputExpressions outputExpressions;
    private Project currentProject = null;
    private Circuit currentCircuit = null;
    
    public AnalyzerModel() {
        // the order here is important, because the output expressions
        // need the truth table to exist for listening.
        table = new TruthTable(this);
        outputExpressions = new OutputExpressions(this);
    }
    
    //
    // access methods
    //
    public Project getCurrentProject() {
        return currentProject;
    }
    
    public Circuit getCurrentCircuit() {
        return currentCircuit;
    }
    
    public VariableList getInputs() {
        return inputs;
    }
    
    public VariableList getOutputs() {
        return outputs;
    }
    
    public TruthTable getTruthTable() {
        return table;
    }
    
    public OutputExpressions getOutputExpressions() {
        return outputExpressions;
    }
    
    //
    // modifier methods
    //
    public void setCurrentProject(Project value) {
        currentProject = value;
        currentCircuit = value.getCurrentCircuit();
    }
    
    public void setVariables(List inputs, List outputs) {
        this.inputs.setAll(inputs);
        this.outputs.setAll(outputs);
    }
}
