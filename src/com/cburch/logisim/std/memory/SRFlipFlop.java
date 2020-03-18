/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.Graphics;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;

class SRFlipFlop extends AbstractFlipFlop {
    public static SRFlipFlop instance = new SRFlipFlop();

    private SRFlipFlop() {
        super("S-R Flip-Flop", Strings.getter("srFlipFlopComponent"));
    }

    public void paintIcon(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        g.drawRect(x + 2, y + 2, 16, 16);
        GraphicsUtil.drawCenteredText(g, "SR", x + 10, y + 8);
    }

    protected int getNumInputs() {
        return 2;
    }

    protected String getInputName(int index) {
        return index == 0 ? "S" : "R";
    }

    protected Value computeValue(Value[] inputs,
            Value curValue) {
        if(inputs[0] == Value.FALSE) {
            if(inputs[1] == Value.FALSE) {
                return curValue;
            } else if(inputs[1] == Value.TRUE) {
                return Value.FALSE;
            }
        } else if(inputs[0] == Value.TRUE) {
            if(inputs[1] == Value.FALSE) {
                return Value.TRUE;
            } else if(inputs[1] == Value.TRUE) {
                return Value.ERROR;
            }
        }
        return Value.UNKNOWN;
    }
}
