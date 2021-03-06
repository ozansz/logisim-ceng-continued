/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package tr.edu.metu.ceng.ceng232.gates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Pin;
import com.cburch.logisim.circuit.PinFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.IntegerFactory;

public class CircuitBuilder {
    private CircuitBuilder() { }
    
    public static void build(Circuit dest, AnalyzerModel model,
            boolean twoInputs, boolean useNands) {
        Layout[] layouts = new Layout[model.getOutputs().size()];
        int maxWidth = 0;
        for(int i = 0; i < layouts.length; i++) {
            String output = model.getOutputs().get(i);
            Expression expr = model.getOutputExpressions().getExpression(output);
            CircuitDetermination det = CircuitDetermination.create(expr);
            if(det != null) {
                if(twoInputs) det.convertToTwoInputs();
                if(useNands) det.convertToNands();
                det.repair();
                layouts[i] = layoutGates(det);
                maxWidth = Math.max(maxWidth, layouts[i].width);
            } else {
                layouts[i] = null;
            }
        }
        
        InputData inputData = computeInputData(model);
        int x = inputData.getStartX();
        int y = 10;
        int outputX = x + maxWidth + 20;
        for(int i = 0; i < layouts.length; i++) {
            String outputName = model.getOutputs().get(i);
            Layout layout = layouts[i];
            Location output;
            int height;
            if(layout == null) {
                output = Location.create(outputX, y + 20);
                height = 40;
            } else {
                int dy = 0;
                if(layout.outputY < 20) dy = 20 - layout.outputY;
                height = Math.max(dy + layout.height, 40);
                output = Location.create(outputX, y + dy + layout.outputY);
                placeComponents(dest, layouts[i], x, y + dy, inputData, output);
            }
            placeOutput(dest, output, outputName);
            y += height + 10;
        }
        placeInputs(dest, inputData);
    }

    //
    // layoutGates
    //
    private static Layout layoutGates(CircuitDetermination det) {
        return layoutGatesSub(det);
    }
    
    private static class Layout {
        // initialized by parent
        int y; // top edge relative to parent's top edge
           // (or edge corresponding to input)
        
        // initialized by self
        int width;
        int height;
        ComponentFactory factory;
        AttributeSet attrs;
        int outputY; // where output is relative to my top edge
        int subX; // where right edge of sublayouts should be relative to my left edge
        Layout[] subLayouts;
        String inputName; // for references directly to inputs

        Layout(int width, int height, int outputY,
                ComponentFactory factory, AttributeSet attrs,
                Layout[] subLayouts, int subX) {
            this.width = width;
            this.height = roundUp(height);
            this.outputY = outputY;
            this.factory = factory;
            this.attrs = attrs;
            this.subLayouts = subLayouts;
            this.subX = subX;
            this.inputName = null;
        }
        
        Layout(String inputName) {
            this(0, 0, 0, null, null, null, 0);
            this.inputName = inputName;
        }
    }
    
    private static Layout layoutGatesSub(CircuitDetermination det) {
        if(det instanceof CircuitDetermination.Input) {
            CircuitDetermination.Input input = (CircuitDetermination.Input) det;
            return new Layout(input.getName());
        } else if(det instanceof CircuitDetermination.Value) {
            CircuitDetermination.Value value = (CircuitDetermination.Value) det;
            ComponentFactory factory = Constant.factory;
            AttributeSet attrs = factory.createAttributeSet();
            attrs.setValue(Constant.value_attr,
                    IntegerFactory.create(value.getValue()));
            Bounds bds = factory.getOffsetBounds(attrs);
            return new Layout(bds.getWidth(), bds.getHeight(),
                    -bds.getY(), factory, attrs,
                    new Layout[0], 0);
        }
        
        // We know det is a Gate. Determine sublayouts.
        CircuitDetermination.Gate gate = (CircuitDetermination.Gate) det;
        ComponentFactory factory = gate.getFactory();
        ArrayList inputs = gate.getInputs();
        
        // Handle a NOT implemented with a NAND as a special case
        if(gate.isNandNot()) {
            CircuitDetermination subDet = (CircuitDetermination) inputs.get(0);
            if(!(subDet instanceof CircuitDetermination.Input)) {
                Layout[] sub = new Layout[1];
                sub[0] = layoutGatesSub(subDet);
                sub[0].y = 0;
                
                AttributeSet attrs = factory.createAttributeSet();
                attrs.setValue(GateAttributes.size_attr, GateAttributes.SIZE_NARROW);
                attrs.setValue(GateAttributes.inputs_attr, IntegerFactory.create(3));
    
                // determine layout's width
                Bounds bds = factory.getOffsetBounds(attrs);
                int betweenWidth = 40;
                if(sub[0].width == 0) betweenWidth = 0;
                int width = sub[0].width + betweenWidth + bds.getWidth();
                
                // determine outputY and layout's height.
                int outputY = sub[0].y + sub[0].outputY; 
                int height = sub[0].height;
                int minOutputY = roundUp(-bds.getY());
                if(minOutputY > outputY) {
                    // we have to shift everything down because otherwise
                    // the component will peek over the rectangle's top.
                    int dy = minOutputY - outputY;
                    sub[0].y += dy;
                    height += dy;
                    outputY += dy;
                }
                int minHeight = outputY + bds.getY() + bds.getHeight();
                if(minHeight > height) height = minHeight;
                
                // ok; create and return the layout.
                return new Layout(width, height, outputY, factory, attrs,
                        sub, sub[0].width);
            }
        }
        
        Layout[] sub = new Layout[inputs.size()];
        int subWidth = 0; // maximum width of sublayouts
        int subHeight = 0; // total height of sublayouts
        for(int i = 0; i < sub.length; i++) {
            sub[i] = layoutGatesSub((CircuitDetermination) inputs.get(i));
            sub[i].y = subHeight;
            subWidth = Math.max(subWidth, sub[i].width);
            subHeight += sub[i].height + 10;
        }
        subHeight -= 10;
        
        AttributeSet attrs = factory.createAttributeSet();
        if(factory == NotGate.factory) {
            attrs.setValue(NotGate.size_attr, NotGate.SIZE_NARROW);
        } else {
            attrs.setValue(GateAttributes.size_attr, GateAttributes.SIZE_NARROW);
            
            Integer inputsValue;
            switch(sub.length) {
            case 8: case 9: inputsValue = IntegerFactory.create(9); break;
            case 6: case 7: inputsValue = IntegerFactory.create(7); break;
            case 4: case 5: inputsValue = IntegerFactory.create(5); break;
            case 2: inputsValue = IntegerFactory.create(2); break;
            default: inputsValue = IntegerFactory.create(3);
            }
            attrs.setValue(GateAttributes.inputs_attr, inputsValue);
        }

        // determine layout's width
        Bounds bds = factory.getOffsetBounds(attrs);
        int betweenWidth = 40 + 10 * (sub.length / 2 - 1);
        if(sub.length == 1) betweenWidth = 20;
        if(subWidth == 0) betweenWidth = 0;
        int width = subWidth + betweenWidth + bds.getWidth();
        
        // determine outputY and layout's height.
        int midIndex = sub.length / 2;
        int outputY = sub[midIndex].y + sub[midIndex].outputY; 
        if(sub.length % 2 == 0) {
            midIndex--;
            int upper = sub[midIndex].y + sub[midIndex].outputY;
            outputY = roundDown((outputY + upper) / 2);
        }
        int height = subHeight;
        int minOutputY = roundUp(-bds.getY());
        if(minOutputY > outputY) {
            // we have to shift everything down because otherwise
            // the component will peek over the rectangle's top.
            int dy = minOutputY - outputY;
            for(int i = 0; i < sub.length; i++) sub[i].y += dy;
            height += dy;
            outputY += dy;
        }
        int minHeight = outputY + bds.getY() + bds.getHeight();
        if(minHeight > height) height = minHeight;
        
        // ok; create and return the layout.
        return new Layout(width, height, outputY, factory, attrs,
                sub, subWidth);
    }
    
    private static int roundDown(int value) {
        return value / 10 * 10;
    }
    
    private static int roundUp(int value) {
        return (value + 9) / 10 * 10;
    }
    
    //
    // computeInputData
    //
    private static InputData computeInputData(AnalyzerModel model) {
        InputData ret = new InputData();
        VariableList inputs = model.getInputs();
        int spineX = 60;
        ret.names = new String[inputs.size()];
        for(int i = 0; i < inputs.size(); i++) {
            String name = inputs.get(i);
            ret.names[i] = name;
            ret.inputs.put(name, new SingleInput(spineX));
            spineX += 20;
        }
        ret.startX = spineX;
        return ret;
    }
    
    private static class InputData {
        int startX;
        String[] names;
        HashMap inputs = new HashMap();
        
        InputData() { }
        
        int getStartX() {
            return startX;
        }
        
        int getSpineX(String input) {
            SingleInput data = (SingleInput) inputs.get(input);
            return data.spineX;
        }
        
        void registerConnection(String input, Location loc) {
            SingleInput data = (SingleInput) inputs.get(input);
            data.ys.add(loc);
        }
    }
        
    private static class SingleInput {
        int spineX;
        ArrayList ys = new ArrayList();
        
        SingleInput(int spineX) { this.spineX = spineX; }
    }
    
    //
    // placeComponents
    //
    /**
     * @param circuit  the circuit where to place the components.
     * @param layout   the layout specifying the gates to place there.
     * @param x        the left edge of where the layout should be placed.
     * @param y        the top edge of where the layout should be placed.
     * @param inputData  information about how to reach inputs.
     * @param output   a point to which the output should be connected.
     */
    private static void placeComponents(Circuit circuit,
            Layout layout, int x, int y, InputData inputData,
            Location output) {
        if(layout.inputName != null) {
            int inputX = inputData.getSpineX(layout.inputName);
            Location input = Location.create(inputX, output.getY());
            inputData.registerConnection(layout.inputName, input);
            circuit.add(Wire.create(input, output));
            return;
        }

        Location compOutput = Location.create(x + layout.width, output.getY());
        Component parent = layout.factory.createComponent(compOutput,
                layout.attrs);
        circuit.add(parent);
        if(!compOutput.equals(output)) circuit.add(Wire.create(compOutput, output));
        
        // handle a NOT gate pattern implemented with NAND as a special case
        if(layout.factory == NandGate.instance && layout.subLayouts.length == 1
                && layout.subLayouts[0].inputName == null) {
            Layout sub = layout.subLayouts[0];
            
            Location input0 = parent.getEnd(1).getLocation();
            Location input1 = parent.getEnd(3).getLocation();
            
            int midX = input0.getX() - 20;
            Location subOutput = Location.create(midX, output.getY());
            Location midInput0 = Location.create(midX, input0.getY());
            Location midInput1 = Location.create(midX, input1.getY());
            circuit.add(Wire.create(subOutput, midInput0));
            circuit.add(Wire.create(midInput0, input0));
            circuit.add(Wire.create(subOutput, midInput1));
            circuit.add(Wire.create(midInput1, input1));
            
            int subX = x + layout.subX - sub.width;  
            placeComponents(circuit, sub, subX, y + sub.y, inputData, subOutput);
            return;
        }

        for(int i = 0; i < layout.subLayouts.length; i++) {
            Layout sub = layout.subLayouts[i];
            
            int inputIndex = i + 1;
            if(layout.subLayouts.length % 2 == 0
                    && i >= layout.subLayouts.length / 2
                    && parent.getEnds().size() % 2 == 0) {
                inputIndex++;
            }
            Location subDest = parent.getEnd(inputIndex).getLocation();
            
            int subOutputY = y + sub.y + sub.outputY;
            if(sub.inputName != null) {
                int destY = subDest.getY();
                if(i == 0 && destY < subOutputY
                        || i == layout.subLayouts.length - 1 && destY > subOutputY) {
                    subOutputY = destY;
                }
            }
            
            Location subOutput;
            if(subOutputY == subDest.getY()) {
                subOutput = subDest;
            } else {
                int back;
                if(i < layout.subLayouts.length / 2) {
                    back = i;
                } else {
                    back = layout.subLayouts.length - 1 - i;
                }
                int subOutputX = subDest.getX() - 20 - 10 * back;
                subOutput = Location.create(subOutputX, subOutputY);
                Location mid = Location.create(subOutputX, subDest.getY());
                circuit.add(Wire.create(subOutput, mid));
                circuit.add(Wire.create(mid, subDest));
            }
                
            int subX = x + layout.subX - sub.width;
            int subY = y + sub.y;
            placeComponents(circuit, sub, subX, subY, inputData, subOutput);
        }
    }
    
    //
    // placeOutput
    //
    private static void placeOutput(Circuit circuit, Location loc, String name) {
        ComponentFactory factory = PinFactory.instance;
        AttributeSet attrs = factory.createAttributeSet();
        attrs.setValue(Pin.facing_attr, Direction.WEST);
        attrs.setValue(Pin.type_attr, Boolean.TRUE);
        attrs.setValue(Pin.label_attr, name);
        attrs.setValue(Pin.labelloc_attr, Direction.NORTH);
        circuit.add(factory.createComponent(loc, attrs));
    }
    
    //
    // placeInputs
    //
    private static void placeInputs(Circuit circuit, InputData inputData) {
        ArrayList forbiddenYs = new ArrayList();
        Comparator compareYs = new CompareYs();
        final int curX = 40;
        int curY = 30;
        for(int i = 0; i < inputData.names.length; i++) {
            String name = inputData.names[i];
            SingleInput singleInput = (SingleInput) inputData.inputs.get(name);
            
            // determine point where we can intersect with spine
            int spineX = singleInput.spineX;
            Location spineLoc = Location.create(spineX, curY);
            if(singleInput.ys.size() > 0) {
                // search for a Y that won't intersect with others
                // (we needn't bother if the pin doesn't connect
                // with anything anyway.)
                Collections.sort(forbiddenYs, compareYs);
                while(Collections.binarySearch(forbiddenYs, spineLoc, compareYs) >= 0) {
                    curY += 10;
                    spineLoc = Location.create(spineX, curY);
                }
                singleInput.ys.add(spineLoc);
            }
            Location loc = Location.create(curX, curY);
            
            // now create the pin
            ComponentFactory factory = PinFactory.instance;
            AttributeSet attrs = factory.createAttributeSet();
            attrs.setValue(Pin.facing_attr, Direction.EAST);
            attrs.setValue(Pin.type_attr, Boolean.FALSE);
            attrs.setValue(Pin.threeState_attr, Boolean.FALSE);
            attrs.setValue(Pin.label_attr, name);
            attrs.setValue(Pin.labelloc_attr, Direction.NORTH);
            circuit.add(factory.createComponent(loc, attrs));
            
            ArrayList spine = singleInput.ys;
            if(spine.size() > 0) {
                // create wire connecting pin to spine
                if(spine.size() == 2 && spine.get(0).equals(spine.get(1))) {
                    // a freak accident where the input is used just once,
                    // and it happens that the pin is placed where no
                    // spine is necessary
                    Iterator it = circuit.getWires(spineLoc).iterator();
                    Wire existing = (Wire) it.next();
                    circuit.remove(existing);
                    circuit.add(Wire.create(loc, existing.getEnd1()));
                } else {
                    circuit.add(Wire.create(loc, spineLoc));
                }

                // create spine
                Collections.sort(spine, compareYs);
                Location prev = (Location) spine.get(0);
                for(int k = 1, n = spine.size(); k < n; k++) {
                    Location cur = (Location) spine.get(k);
                    if(!cur.equals(prev)) {
                        circuit.add(Wire.create(prev, cur));
                        prev = cur;
                    }
                }
            }
                    
            // advance y and forbid spine intersections for next pin
            forbiddenYs.addAll(singleInput.ys);
            curY += 50;
        }
    }
    
    private static class CompareYs implements Comparator {
        public int compare(Object aObj, Object bObj) {
            Location a = (Location) aObj;
            Location b = (Location) bObj;
            return a.getY() - b.getY();
        }
    }
}
