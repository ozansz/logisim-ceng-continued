/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.std.memory;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JLabel;

import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;

class RomFactory extends AbstractComponentFactory {
    public static ComponentFactory INSTANCE = new RomFactory();

    public static Attribute CONTENTS_ATTR = new ContentsAttribute();
    
    private static class ContentsAttribute extends Attribute {
        public ContentsAttribute() {
            super("contents", Strings.getter("romContentsAttr"));
        }

        public java.awt.Component getCellEditor(Window source, Object value) {
            if(source instanceof Frame) {
                Project proj = ((Frame) source).getProject();
                RomAttributes.register((MemContents) value, proj);
            }
            ContentsCell ret = new ContentsCell(source, (MemContents) value);
            ret.mouseClicked(null);
            return ret;
        }

        public String toDisplayString(Object value) {
            return Strings.get("romContentsValue");
        }

        public String toStandardString(Object value) {
            MemContents state = (MemContents) value;
            int addr = state.getLogLength();
            int data = state.getWidth();
            StringWriter ret = new StringWriter();
            ret.write("addr/data: " + addr + " " + data + "\n");
            try {
                HexFile.save(ret, state);
            } catch(IOException e) { }
            return ret.toString();
        }

        public Object parse(String value) {
            int lineBreak = value.indexOf('\n');
            String first = lineBreak < 0 ? value : value.substring(0, lineBreak);
            String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
            StringTokenizer toks = new StringTokenizer(first);
            try {
                String header = toks.nextToken();
                if(!header.equals("addr/data:")) return null;
                int addr = Integer.parseInt(toks.nextToken());
                int data = Integer.parseInt(toks.nextToken());
                MemContents ret = MemContents.create(addr, data);
                HexFile.open(ret, new StringReader(rest));
                return ret;
            } catch(IOException e) {
                return null;
            } catch(NumberFormatException e) {
                return null;
            } catch(NoSuchElementException e) {
                return null;
            }
        }
    }
        
    private static class ContentsCell extends JLabel
            implements MouseListener {
        Window source;
        MemContents contents;
        
        ContentsCell(Window source, MemContents contents) {
            super(Strings.get("romContentsValue"));
            this.source = source;
            this.contents = contents;
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            if(contents == null) return;
            Project proj = source instanceof Frame ? ((Frame) source).getProject() : null;
            HexFrame frame = RomAttributes.getHexFrame(contents, proj);
            frame.setVisible(true);
            frame.toFront();
        }

        public void mousePressed(MouseEvent e) { }

        public void mouseReleased(MouseEvent e) { }

        public void mouseEntered(MouseEvent e) { }

        public void mouseExited(MouseEvent e) { }
    }

    private RomFactory() {
    }

    public String getName() {
        return "ROM";
    }

    public String getDisplayName() {
        return Strings.get("romComponent");
    }

    public AttributeSet createAttributeSet() {
        return new RomAttributes();
    }

    public Component createComponent(Location loc, AttributeSet attrs) {
        return new Rom(loc, attrs);
    }

    public Bounds getOffsetBounds(AttributeSet attrs) {
        return Mem.OFFSET_BOUNDS;
    }

    //
    // user interface methods
    //
    public void paintIcon(ComponentDrawContext context, int x, int y,
            AttributeSet attrs) {
        Graphics g = context.getGraphics();
        Font old = g.getFont();
        g.setFont(old.deriveFont(9.0f));
        GraphicsUtil.drawCenteredText(g, "ROM", x + 10, y + 9);
        g.setFont(old);
        g.drawRect(x, y + 4, 19, 12);
        for(int dx = 2; dx < 20; dx += 5) {
            g.drawLine(x + dx, y + 2, x + dx, y + 4);
            g.drawLine(x + dx, y + 16, x + dx, y + 18);
        }
    }
}
