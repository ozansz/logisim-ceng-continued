/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;


import java.awt.Graphics;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;

class DFlipFlop extends AbstractFlipFlop {
    public static DFlipFlop instance = new DFlipFlop();

    private DFlipFlop() {
        super("D Flip-Flop", Strings.getter("dFlipFlopComponent"));
    }

    public void paintIcon(ComponentDrawContext context,
            int x, int y, AttributeSet attrs) {
        Graphics g = context.getGraphics();
        g.drawRect(x + 2, y + 2, 16, 16);
        GraphicsUtil.drawCenteredText(g, "D", x + 10, y + 8);
    }

    protected int getNumInputs() {
        return 1;
    }

    protected String getInputName(int index) {
        return "D";
    }

    protected Value computeValue(Value[] inputs,
            Value curValue) {
        return inputs[0];
    }
}
