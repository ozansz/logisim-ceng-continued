/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.comp;


import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretEvent;
import com.cburch.logisim.tools.CaretListener;

class TextFieldCaret implements Caret {
    private LinkedList listeners = new LinkedList();
    private TextField field;
    private Graphics g;
    private String old_text;
    private String cur_text;
    private int pos;

    public TextFieldCaret(TextField field, Graphics g, int pos) {
        this.field = field;
        this.g = g;
        this.old_text = field.getText();
        this.cur_text = field.getText();
        this.pos = pos;
    }
    public TextFieldCaret(TextField field, Graphics g, int x, int y) {
        this(field, g, 0);
        moveCaret(x, y);
    }

    public void addCaretListener(CaretListener l) {
        listeners.add(l);
    }

    public void removeCaretListener(CaretListener l) {
        listeners.remove(l);
    }

    public String getText() { return cur_text; }

    public void commitText(String text) {
        cur_text = text;
        pos = cur_text.length();
        field.setText(text);
    }

    public void draw(Graphics g) {
        if(field.getFont() != null) g.setFont(field.getFont());

        // draw boundary
        Bounds bds = getBounds(g);
        g.setColor(Color.white);
        g.fillRect(bds.getX(), bds.getY(),
                bds.getWidth(), bds.getHeight());
        g.setColor(Color.black);
        g.drawRect(bds.getX(), bds.getY(),
                bds.getWidth(), bds.getHeight());

        // draw text
        int x = field.getX();
        int y = field.getY();
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(cur_text);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        switch(field.getHAlign()) {
            case TextField.H_CENTER:    x -= width / 2; break;
            case TextField.H_RIGHT:     x -= width; break;
            default:                    break;
        }
        switch(field.getVAlign()) {
            case TextField.V_TOP:       y += ascent; break;
            case TextField.V_CENTER:    y += (ascent - descent) / 2; break;
            case TextField.V_BOTTOM:    y -= descent; break;
            default:                    break;
        }
        g.drawString(cur_text, x, y);

        // draw cursor
        if(pos > 0) x += fm.stringWidth(cur_text.substring(0, pos));
        g.drawLine(x, y, x, y - ascent);
    }

    public Bounds getBounds(Graphics g) {
        int x = field.getX();
        int y = field.getY();
        Font font = field.getFont();
        FontMetrics fm;
        if(font == null)    fm = g.getFontMetrics();
        else                fm = g.getFontMetrics(font);
        int width = fm.stringWidth(cur_text);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int height = ascent + descent;
        switch(field.getHAlign()) {
            case TextField.H_CENTER:    x -= width / 2; break;
            case TextField.H_RIGHT:     x -= width; break;
            default:                    break;
        }
        switch(field.getVAlign()) {
            case TextField.V_TOP:       y += ascent; break;
            case TextField.V_CENTER:    y += (ascent - descent) / 2; break;
            case TextField.V_BOTTOM:    y -= descent; break;
            default:                    break;
        }
        return Bounds.create(x, y - ascent, width, height)
                .add(field.getBounds(g))
                .expand(3);
    }

    public void cancelEditing() {
        CaretEvent e = new CaretEvent(this, old_text, old_text);
        cur_text = old_text;
        pos = cur_text.length();
        Iterator it = ((List) listeners.clone()).iterator();
        while(it.hasNext()) {
            ((CaretListener) it.next()).editingCanceled(e);
        }
    }

    public void stopEditing() {
        CaretEvent e = new CaretEvent(this, old_text, cur_text);
        field.setText(cur_text);
        Iterator it = ((List) listeners.clone()).iterator();
        while(it.hasNext()) {
            ((CaretListener) it.next()).editingStopped(e);
        }
    }

    public void mousePressed(MouseEvent e) {
        //TODO: enhance label editing
        moveCaret(e.getX(), e.getY());
    }

    public void mouseDragged(MouseEvent e) {
        //TODO: enhance label editing
    }

    public void mouseReleased(MouseEvent e) {
        //TODO: enhance label editing
        moveCaret(e.getX(), e.getY());
    }

    public void keyPressed(KeyEvent e) {
        int ign = InputEvent.ALT_MASK | InputEvent.CTRL_MASK
            | InputEvent.META_MASK;
        if((e.getModifiers() & ign) != 0) return;
        switch(e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_KP_LEFT:
            if(pos > 0) --pos;
            break;
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_KP_RIGHT:
            if(pos < cur_text.length()) ++pos;
            break;
        case KeyEvent.VK_HOME:
            pos = 0;
            break;
        case KeyEvent.VK_END:
            pos = cur_text.length();
            break;
        case KeyEvent.VK_ESCAPE:
        case KeyEvent.VK_CANCEL:
            cancelEditing();
            break;
        case KeyEvent.VK_CLEAR:
            cur_text = "";
            pos = 0;
            break;
        case KeyEvent.VK_ENTER:
            stopEditing();
            break;
        case KeyEvent.VK_BACK_SPACE:
            if(pos > 0) {
                cur_text = cur_text.substring(0, pos - 1)
                    + cur_text.substring(pos);
                --pos;
            }
            break;
        case KeyEvent.VK_DELETE:
            if(pos < cur_text.length()) {
                cur_text = cur_text.substring(0, pos)
                    + cur_text.substring(pos + 1);
            }
            break;
        case KeyEvent.VK_INSERT:
        case KeyEvent.VK_COPY:
        case KeyEvent.VK_CUT:
        case KeyEvent.VK_PASTE:
            //TODO: enhance label editing
            break;
        default:
            ; // ignore
        }
    }

    public void keyReleased(KeyEvent e) { }

    public void keyTyped(KeyEvent e) {
        int ign = InputEvent.ALT_MASK | InputEvent.CTRL_MASK
            | InputEvent.META_MASK;
        if((e.getModifiers() & ign) != 0) return;

        char c = e.getKeyChar();
        if(c == '\n') {
            stopEditing();
        } else if(c != KeyEvent.CHAR_UNDEFINED
                && !Character.isISOControl(c)) {
            if(pos < cur_text.length()) {
                cur_text = cur_text.substring(0, pos) + c
                    + cur_text.substring(pos);
            } else {
                cur_text += c;
            }
            ++pos;
        }
    }

    private void moveCaret(int x, int y) {
        Bounds bds = getBounds(g);
        FontMetrics fm = g.getFontMetrics();
        x -= bds.getX();
        int last = 0;
        for(int i = 0; i < cur_text.length(); i++) {
            int cur = fm.stringWidth(cur_text.substring(0, i + 1));
            if(x <= (last + cur) / 2) {
                pos = i;
                return;
            }
            last = cur;
        }
        pos = cur_text.length();
    }
}
