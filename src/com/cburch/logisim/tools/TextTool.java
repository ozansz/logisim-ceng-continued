/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.tools;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitActions;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.Text;
import com.cburch.logisim.std.TextClass;

import java.util.Iterator;

public class TextTool extends Tool {
    private static class TextChangedAction extends Action {
        private Circuit circ;
        private Component comp;
        private Caret caret;
        private String oldstr;
        private String newstr;

        TextChangedAction(Circuit circ, Component comp, Caret caret,
                String oldstr, String newstr) {
            this.circ = circ;
            this.caret = caret;
            this.comp = comp;
            this.oldstr = oldstr;
            this.newstr = newstr;
        }

        public String getName() {
            return Strings.get("changeTextAction");
        }

        public void doIt(Project proj) {
            caret.commitText(newstr);
            circ.componentChanged(comp);
        }

        public void undo(Project proj) {
            caret.commitText(oldstr);
            circ.componentChanged(comp);
        }
    }

    private class MyListener
            implements CaretListener, CircuitListener {
        public void editingCanceled(CaretEvent e) {
            if(e.getCaret() != caret) {
                e.getCaret().removeCaretListener(this);
                return;
            }
            caret.removeCaretListener(this);
            caret_circ.removeCircuitListener(this);

            caret_circ = null;
            caret_comp = null;
            caret_comp_created = false;
            caret = null;
        }
        public void editingStopped(CaretEvent e) {
            if(e.getCaret() != caret) {
                e.getCaret().removeCaretListener(this);
                return;
            }
            caret.removeCaretListener(this);
            caret_circ.removeCircuitListener(this);

            String val = caret.getText();
            boolean is_null = (val == null || val.equals(""));
            Action a;
            if(caret_comp_created) {
                if(!is_null) {
                    a = CircuitActions.addComponent(caret_circ, caret_comp, false);
                } else {
                    a = null; // don't add the blank text field
                }
            } else {
                if(is_null && caret_comp instanceof Text) {
                    a = CircuitActions.removeComponent(caret_circ, caret_comp);
                } else {
                    a = new TextChangedAction(caret_circ, caret_comp,
                        caret, e.getOldText(), e.getText());
                }
            }

            caret_circ = null;
            caret_comp = null;
            caret_comp_created = false;
            caret = null;
            
            if(a != null) caret_canvas.getProject().doAction(a);
        }

        public void circuitChanged(CircuitEvent event) {
            if(event.getCircuit() != caret_circ) {
                event.getCircuit().removeCircuitListener(this);
                return;
            }
            int action = event.getAction();
            if(action == CircuitEvent.ACTION_REMOVE) {
                if(event.getData() == caret_comp) {
                    caret.cancelEditing();
                }
            } else if(action == CircuitEvent.ACTION_CLEAR) {
                if(caret_comp != null) {
                    caret.cancelEditing();
                }
            }
        }
    }

    private static Cursor cursor
        = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

    private MyListener listener = new MyListener();
    private AttributeSet attrs;
    private Canvas caret_canvas = null;
    private Circuit caret_circ = null;
    private Component caret_comp = null;
    private Caret caret = null;
    private boolean caret_comp_created;

    public TextTool() {
        attrs = TextClass.instance.createAttributeSet();
    }
    
    public boolean equals(Object other) {
        return other instanceof TextTool;
    }
    
    public int hashCode() {
        return TextTool.class.hashCode();
    }

    public String getName() {
        return "Text Tool";
    }

    public String getDisplayName() {
        return Strings.get("textTool");
    }

    public String getDescription() {
        return Strings.get("textToolDesc");
    }

    public AttributeSet getAttributeSet() {
        return attrs;
    }

    public void paintIcon(ComponentDrawContext c, int x, int y) {
        TextClass.instance.paintIcon(c, x, y, null);
    }

    public void draw(Canvas canvas, ComponentDrawContext context) {
        if(caret != null) caret.draw(context.getGraphics());
    }

    public void deselect(Canvas canvas) {
        if(caret != null) {
            caret.stopEditing();
            caret = null;
        }
    }

    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        Project proj = canvas.getProject();
        Circuit circ = canvas.getCircuit();

        if(!proj.getLogisimFile().contains(circ)) {
            if(caret != null) caret.cancelEditing();
            canvas.setErrorMessage(Strings.getter("cannotModifyError"));
            return;
        }

        // Maybe user is clicking within the current caret.
        if(caret != null) {
            if(caret.getBounds(g).contains(e.getX(), e.getY())) { // Yes
                caret.mousePressed(e);
                proj.repaintCanvas();
                return;
            } else { // No. End the current caret.
                caret.stopEditing();
            }
        }
        // caret will be null at this point

        // Otherwise search for a new caret.
        int x = e.getX();
        int y = e.getY();
        Location loc = Location.create(x, y);
        ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);

        // First search in selection.
        Iterator it = proj.getSelection()
                        .getComponentsContaining(loc, g).iterator();
        while(it.hasNext()) {
            Component comp = (Component) it.next();
            TextEditable editable = (TextEditable) comp.getFeature(TextEditable.class);
            if(editable != null) {
                caret = editable.getTextCaret(event);
                if(caret != null) {
                    proj.getFrame().viewComponentAttributes(circ, comp);
                    caret_comp = comp;
                    caret_comp_created = false;
                    break;
                }
            }
        }

        // Then search in circuit
        if(caret == null) {
            it = circ.getAllContaining(loc, g).iterator();
            while(it.hasNext()) {
                Component comp = (Component) it.next();
                TextEditable editable = (TextEditable) comp.getFeature(TextEditable.class);
                if(editable != null) {
                    caret = editable.getTextCaret(event);
                    if(caret != null) {
                        proj.getFrame().viewComponentAttributes(circ, comp);
                        caret_comp = comp;
                        caret_comp_created = false;
                        break;
                    }
                }
            }
        }

        // if nothing found, create a new label
        if(caret == null) {
            if(loc.getX() < 0 || loc.getY() < 0) return;
            AttributeSet copy = (AttributeSet) attrs.clone();
            caret_comp = TextClass.instance.createComponent(loc, copy);
            caret_comp_created = true;
            TextEditable editable = (TextEditable) caret_comp.getFeature(TextEditable.class);
            if(editable != null) {
                caret = editable.getTextCaret(event);
                proj.getFrame().viewComponentAttributes(circ, caret_comp);
            }
        }

        if(caret != null) {
            caret_canvas = canvas;
            caret_circ = canvas.getCircuit();
            caret.addCaretListener(listener);
            caret_circ.addCircuitListener(listener);
        }
        proj.repaintCanvas();
    }

    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        //TODO: enhance label editing
    }

    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        //TODO: enhance label editing
    }

    public void keyPressed(Canvas canvas, KeyEvent e) {
        if(caret != null) {
            caret.keyPressed(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public void keyReleased(Canvas canvas, KeyEvent e) {
        if(caret != null) {
            caret.keyReleased(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public void keyTyped(Canvas canvas, KeyEvent e) {
        if(caret != null) {
            caret.keyTyped(e);
            canvas.getProject().repaintCanvas();
        }
    }

    public Cursor getCursor() {
        return cursor;
    }
}

