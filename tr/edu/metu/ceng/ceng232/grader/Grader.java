/*
 * UPDATE LOG
 * ------------------------
 * Mar 18, 2020
 *   + Increased verbosity of grader
 */

package tr.edu.metu.ceng.ceng232.grader;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Clock;
import com.cburch.logisim.circuit.Pin;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import tr.edu.metu.ceng.ceng232.others.ic74195;
import tr.edu.metu.ceng.ceng232.others.ic7495;

/**
 * @author kerem
 * @author sazak
 */
public class Grader {
    private Project project;
    private MyListener myListener = new MyListener();
    private Simulator simulator;
    private boolean initialPropagationCompleted = false;
    private Object[] inputPins;
    private Object[] outputPins;
//    private int currentRun = 0;
//    private int currentState = 0;

    public Grader(Project proj) {
        if (!Settings.isEnabled())
            return;
        
        project = proj;
        simulator = project.getSimulator();
        
        simulator.setIsTicking(false);
        simulator.addSimulatorListener(myListener);
    }

    private Object fetchInputPin(String name) {
        Circuit c = project.getCurrentCircuit();
        Set<Object> nonWires = c.getNonWires();
        for (Object o : nonWires) {
            if (o instanceof com.cburch.logisim.circuit.Pin) {
                Pin p = (Pin)o;
                if (p.isInputPin()
                        && name.equals(p.getAttributeSet().getValue(Pin.label_attr))) {
                    if (1 != p.getLogValue(project.getCircuitState(), null).getBitWidth().getWidth()) {
                        System.err.println("Pin " + name + " has a wider bit-width than 1");
                        System.exit(1);
                    }
                    return p;
                }
            }
            else if (o instanceof com.cburch.logisim.circuit.Clock) {
                Clock p = (Clock)o;
                if (name.equals(p.getAttributeSet().getValue(Pin.label_attr))) {
                    if (1 != p.getLogValue(project.getCircuitState(), null).getBitWidth().getWidth()) {
                        System.err.println("Pin " + name + " has a wider bit-width than 1");
                        System.exit(1);
                    }
                    return p;
                }
                    
            }
        }
        return null;
    }
    
    private Object fetchOutputPin(String name) {
        Circuit c = project.getCurrentCircuit();
        Set<Object> nonWires = c.getNonWires();
        for (Object o : nonWires) {
            if (o instanceof com.cburch.logisim.circuit.Pin) {
                Pin p = (Pin)o;
                if (!p.isInputPin()
                        && name.equals(p.getAttributeSet().getValue(Pin.label_attr)))
                    return p;
            }
        }
        return null;
    }

    public void fetchGradingPins() {
        inputPins = new Object[Settings.inputs.length];
        outputPins = new Object[Settings.outputs.length];

        for (int i = 0; i < Settings.inputs.length; i++) {
            inputPins[i] = fetchInputPin(Settings.inputs[i]);
            if (inputPins[i] == null) {
                System.err.println("Cannot find input pin " + Settings.inputs[i]);
                System.exit(1);
            }
        }

        for (int i = 0; i < Settings.outputs.length; i++) {
            outputPins[i] = fetchOutputPin(Settings.outputs[i]);
            if (outputPins[i] == null) {
                System.err.println("Cannot find output pin " + Settings.outputs[i]);
                System.exit(1);
            }
        }
        //System.out.println("OK found pins");
    }

    public void printCircuitComponents() {
        Circuit c = project.getCurrentCircuit();
        Set<Object> nonWires = c.getNonWires();
        for (Object o : nonWires) {
            if (o instanceof com.cburch.logisim.circuit.Pin) {
                Pin p = (Pin)o;
                System.out.println(p.getAttributeSet().getValue(Pin.label_attr));
                if (p.isInputPin()) {
                    setPinValue(p, Value.TRUE);
                    //System.out.println("deneme");
                }
            }
        }

        project.repaintCanvas();
    }

    public void setInputs(char[] values) {
        for (int i = 0; i < values.length; i++) {
            if (inputPins[i] instanceof Pin)
                setPinValue((Pin)inputPins[i], values[i] == '0' ? Value.FALSE : Value.TRUE);
            else
                setClockValue((Clock)inputPins[i], values[i] == '0' ? Value.FALSE : Value.TRUE);
        }
        project.repaintCanvas();
    }

    public boolean checkOutputs(char[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 'X')
                continue;
            char sendingValue;
            if (outputPins[i] instanceof Pin)
                sendingValue = getPinValue((Pin)outputPins[i]);
            else
                sendingValue = getClockValue((Clock)outputPins[i]);
            
            if (sendingValue != values[i]) {
                System.out.printf("!!!INDEX: %d, expecting: %c, got: %c\n", i, values[i], sendingValue);
                return false;
            }
        }
        return true;
    }

    private void setClockValue(Clock clock, Value value) {
        if (value.getBitWidth().getWidth() != 1) {
            System.err.println("value.getBitWidth().getWidth() != 1");
            return;
        }
        if (!value.equals(Value.FALSE) && !value.equals(Value.TRUE)) {
            System.err.println("!value.equals(Value.FALSE) && !value.equals(Value.TRUE)");
            return;
        }

        clock.changeValue(project.getCircuitState(), value);
    }

    public void setPinValue(Pin pin, Value value) {
        if(!pin.isInputPin()) return;

        if (value.getBitWidth().getWidth() != 1) {
            System.err.println("value.getBitWidth().getWidth() != 1");
            return;
        }
        if (!value.equals(Value.FALSE) && !value.equals(Value.TRUE)) {
            System.err.println("!value.equals(Value.FALSE) && !value.equals(Value.TRUE)");
            return;
        }
        
        Value sendingValue = pin.getLogValue(project.getCircuitState(), null);
        if (sendingValue.getBitWidth().getWidth() != 1) {
            System.err.println("sendingValue.getBitWidth().getWidth() != 1");
            return;
        }

        pin.changeValue(project.getCircuitState(), value);
    }

    public char getClockValue(Clock clock) {
        Value sendingValue = clock.getLogValue(project.getCircuitState(), null);
        if (sendingValue.equals(Value.ERROR))
            return 'E';
        else if (sendingValue.equals(Value.FALSE))
            return '0';
        else if (sendingValue.equals(Value.TRUE))
            return '1';
        else
            return 'X';
    }

    public char getPinValue(Pin pin) {
        Value sendingValue = pin.getLogValue(project.getCircuitState(), null);
        if (sendingValue.getBitWidth().getWidth() != 1) {
            System.err.println("sendingValue.getBitWidth().getWidth() != 1");
            return 'X';
        }
        if (sendingValue.equals(Value.ERROR))
            return 'E';
        else if (sendingValue.equals(Value.FALSE))
            return '0';
        else if (sendingValue.equals(Value.TRUE))
            return '1';
        else
            return 'X';
    }

    // Aslında proje başına çalışmalı
    // Ama global bazda çalışıyor
    // TODO
    private void checkChips() {
        Set<String> allowedChips = new HashSet<String>(Arrays.asList(Settings.allowedChips));

        for (String chip : Settings.components.keySet()) {
            if (!allowedChips.contains(chip)) {
                System.out.printf("!!CHIPS FAIL: Unallowed chip: %s\n", chip);
                return;
            }
        }

        System.out.println("CHIPS PASS");
    }

    private boolean waitingNewRun = false;
    private int curRun = 0;
    private int curState;
    private int curPhase = 0;
    private Map<Integer, Integer> ifCount;

    private class MyListener implements SimulatorListener {
        public void propagationCompleted(SimulatorEvent e) {
            //System.out.println("propagationCompleted");
            if (!initialPropagationCompleted) {
                initialPropagationCompleted = true;
                fetchGradingPins();
                
                curRun = 0;
                waitingNewRun = true;
                simulator.requestReset();

                checkChips();

                //System.out.println(Settings.components);

                return;
            }

            if (waitingNewRun) {
                waitingNewRun = false;
                curState = 1;
                curPhase = 0;
                ifCount = new HashMap<Integer, Integer>();
            }

            if (curRun >= Settings.runs.size())
                return;

            Run r = Settings.runs.get(curRun);
            boolean runFailed = false;
            while (curState < r.states.size()) {
                State s = r.states.get(curState);

                if (s.type == State.TYPE.REGISTER_MODIFY) {
                    // This is only used for Lab Experiment 4 in CENG232@METU.
                    // Allows the grader to modify a register's value
                    applyRegisterModify(s);
                    curState++;
                }
                else if (s.type == State.TYPE.TRUTH_TABLE) {
                    if (curPhase == 0) {
                        setInputs(s.inputs);
                        System.out.println(new String(s.inputs));
                        curPhase = 1;
                        return;
                    } else {
                        if (!checkOutputs(s.outputs)) {
                            runFailed = true;
                            curState = r.states.size();
                            System.out.printf("!!TEST RUN FAIL: inputs: %s, outputs: %s\n", s.inputs, s.outputs);
                        }
                        else {
                            curState++;
                            curPhase = 0;
                        }
                    }
                }
                else if (s.type == State.TYPE.CONDITION) {
                    if (!checkOutputs(s.outputs)) {
                        curState++;
                        curPhase = 0;
                    }
                    else {
                        Integer a = ifCount.get(new Integer(curState));
                        if (a == null)
                            a = new Integer(0);
                        a = a+1;
                        ifCount.put(curState, a);

                        if (a.intValue() > s.gotoLimit) {
                            runFailed = true;
                            curState = r.states.size();
                        }
                        else {
                            curState = s.gotoState;
                            curPhase = 0;
                        }
                    }
                }
            }

            if (runFailed)
                System.out.println("FAIL");
            else
                System.out.println("PASS");

            curRun++;
            if (curRun < Settings.runs.size()) {
                waitingNewRun = true;
                simulator.requestReset();
            }
            else
                System.exit(0);
        }

        public void tickCompleted(SimulatorEvent e) {
            //System.out.println("tickCompleted");
        }

        public void simulatorStateChanged(SimulatorEvent e) {
            //System.out.println("simulatorStateChanged");
            
            /*
            try {
                State s = new State("modify 74x95 state from 0,0,1,1 to 1,0,0,0");
                applyRegisterModify(s);
            }
            catch(Exception ex) {
                System.out.println("Exception geldi: " + ex);
            }
            */
            
            //printCircuitComponents();
        }
    }

    void applyRegisterModify(State s)
    {
        if (s.type != State.TYPE.REGISTER_MODIFY)
            return;
        Value[] a = {
            s.stateFrom[0] == '1' ? Value.TRUE : Value.FALSE,
            s.stateFrom[1] == '1' ? Value.TRUE : Value.FALSE,
            s.stateFrom[2] == '1' ? Value.TRUE : Value.FALSE,
            s.stateFrom[3] == '1' ? Value.TRUE : Value.FALSE
        };
        Value[] b = {
            s.stateTo[0] == '1' ? Value.TRUE : Value.FALSE,
            s.stateTo[1] == '1' ? Value.TRUE : Value.FALSE,
            s.stateTo[2] == '1' ? Value.TRUE : Value.FALSE,
            s.stateTo[3] == '1' ? Value.TRUE : Value.FALSE
        };

        Value fromValue = Value.create(a);
        Value toValue = Value.create(b);

        Circuit c = project.getCurrentCircuit();
        
        Set<Object> nonWires = c.getNonWires();
        for (Object o : nonWires) {
            if (o instanceof tr.edu.metu.ceng.ceng232.others.ic7495) {
                ic7495 ic = (ic7495)o;
                Value v = ic.fetchValue(project.getCircuitState());
                if (v.equals(fromValue))
                    ic.modifyValue(project.getCircuitState(), toValue);

            }
            else if (o instanceof tr.edu.metu.ceng.ceng232.others.ic74195) {
                ic74195 ic = (ic74195)o;
                Value v = ic.fetchValue(project.getCircuitState());
                if (v.equals(fromValue))
                    ic.modifyValue(project.getCircuitState(), toValue);
            }
        }
    }

}
