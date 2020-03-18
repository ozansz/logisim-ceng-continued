/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;

class AbstractGate extends ManagedComponent
        implements WireRepair, ExpressionComputer {
    private AbstractGateFactory src;
    
    public AbstractGate(Location loc, AttributeSet attrs, AbstractGateFactory src) {
        super(loc, attrs, 4);
        this.src = src;
        
        GateAttributes gateAttrs = (GateAttributes) attrs;
        gateAttrs.gate = this;
        setEnds();
    }
    
    public ComponentFactory getFactory() {
        return src;
    }

    void setEnds() {
        GateAttributes attrs = (GateAttributes) getAttributeSet();
        Bounds bounds = getBounds();
        Location pt = getLocation();
        BitWidth w = attrs.width;
        setEnd(0, pt, w, EndData.OUTPUT_ONLY);
        int wid;
        int ht;
        if(attrs.facing == Direction.NORTH || attrs.facing == Direction.SOUTH) {
            wid = bounds.getHeight();
            ht = bounds.getWidth();
        } else {
            wid = bounds.getWidth();
            ht = bounds.getHeight();
        }
        int dx = wid;
        int dy = -(ht / 2 - 5);
        int ddy = (ht - 10) / (attrs.inputs - 1);
        if(attrs.facing == Direction.NORTH) {
            for(int i = 1; i <= attrs.inputs; i++) {
                setEnd(i, pt.translate(dy,  dx), w, EndData.INPUT_ONLY);
                dy += ddy;
            }
        } else if(attrs.facing == Direction.SOUTH) {
            for(int i = 1; i <= attrs.inputs; i++) {
                setEnd(i, pt.translate(dy, -dx), w, EndData.INPUT_ONLY);
                dy += ddy;
            }
        } else if(attrs.facing == Direction.WEST) {
            for(int i = 1; i <= attrs.inputs; i++) {
                setEnd(i, pt.translate( dx, dy), w, EndData.INPUT_ONLY);
                dy += ddy;
            }
        } else {
            for(int i = 1; i <= attrs.inputs; i++) {
                setEnd(i, pt.translate(-dx, dy), w, EndData.INPUT_ONLY);
                dy += ddy;
            }
        }
    }

    public void propagate(CircuitState state) {
        GateAttributes attrs = (GateAttributes) getAttributeSet();
        Value[] inputs = new Value[attrs.inputs];
        int num_inputs = 0;
        for(int i = 1; i <= attrs.inputs; i++) {
            Value v = state.getValue(getEndLocation(i));
            if(v != Value.NIL) {
                inputs[num_inputs] = v;
                num_inputs++;
            }
        }
        Value out = src.computeOutput(inputs, num_inputs);
        state.setValue(getEndLocation(0), out, this, GateAttributes.DELAY);
    }

    //
    // user interface methods
    //

    public void draw(ComponentDrawContext context) {
        GateAttributes attrs = (GateAttributes) getAttributeSet();
        Location loc = getLocation();
        Bounds bds = getBounds();
        context.getGraphics().setColor(Color.BLACK);
        drawBase(context, src, this, attrs, loc.getX(), loc.getY(),
                bds.getWidth(), bds.getHeight());
        if(!context.isPrintView() || context.getGateShape() == LogisimPreferences.SHAPE_RECTANGULAR) {
            context.drawPins(this);
        }
    }
    
    static void drawBase(ComponentDrawContext context,
            AbstractGateFactory src, AbstractGate comp,
            GateAttributes attrs, int x, int y, int width, int height) {
        Direction facing = attrs.facing;
        Graphics oldG = context.getGraphics();
        if(facing != Direction.EAST && oldG instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) oldG.create();
            g2.rotate(-facing.toRadians(), x, y);
            context.setGraphics(g2);
            if(facing == Direction.NORTH || facing == Direction.SOUTH) {
                int t = width; width = height; height = t;
            }
        }
        
        if(context.getGateShape() == LogisimPreferences.SHAPE_RECTANGULAR) {
            src.drawRectangular(context, x, y, width, height);
        } else if(context.getGateShape() == LogisimPreferences.SHAPE_DIN40700) {
            Integer inputs = (Integer) attrs.getValue(GateAttributes.inputs_attr);
            src.drawDinShape(context, x, y, width, height,
                    inputs.intValue(), comp);
        } else { // SHAPE_SHAPED
            int don = src.has_dongle ? 10 : 0;
            if(comp != null) {
                src.drawInputLines(context, comp, attrs.inputs,
                    x - width, y - (height - 10) / 2, width - don, height);
            }
            src.drawShape(context, x - don, y, width - don, height);
            if(src.has_dongle) {
                context.drawDongle(x - 5, y);
            }
        }
        context.setGraphics(oldG);
    }
    
    public Object getFeature(Object key) {
        if(key == WireRepair.class) return this;
        if(key == ExpressionComputer.class) return this;
        return super.getFeature(key);
    }
    
    public boolean shouldRepairWire(WireRepairData data) {
        return src.shouldRepairWire(this, data);
    }

    public void computeExpression(Map expressionMap) {
        GateAttributes attrs = (GateAttributes) getAttributeSet();
        Expression[] inputs = new Expression[attrs.inputs];
        int numInputs = 0;
        for(int i = 1; i <= attrs.inputs; i++) {
            Expression e = (Expression) expressionMap.get(getEndLocation(i));
            if(e != null) {
                inputs[numInputs] = e;
                ++numInputs;
            }
        }
        if(numInputs > 0) {
            Expression out = src.computeExpression(inputs, numInputs);
            expressionMap.put(getEndLocation(0), out);
        }
    }

    void attributeValueChanged(Attribute attr, Object value) {
        if(attr == GateAttributes.width_attr) {
            setEnds();
        }
    }
}
