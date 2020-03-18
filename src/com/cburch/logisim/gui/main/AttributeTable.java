/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

public class AttributeTable extends JTable implements LocaleListener {
    static final Color HALO_COLOR = new Color(192, 255, 255);

    public static interface Listener {
        public void valueChangeRequested(Attribute attr, Object value);
    }

    private class DefaultListener implements Listener {
        public void valueChangeRequested(Attribute attr, Object value) {
            if(attrs == null) {
                return;
            }
            if(!attrs.containsAttribute(attr)) {
                return;
            }
            Object oldValue = attrs.getValue(attr);
            proj.doAction(new ChangeAttributeAction(attrs, attr, oldValue, value));
        }
    }

    private static class ChangeAttributeAction extends Action {
        AttributeSet attrs;
        Attribute attr;
        Object oldValue;
        Object newValue;

        ChangeAttributeAction(AttributeSet as, Attribute a, Object o, Object n) {
            attrs = as;
            attr = a;
            oldValue = o;
            newValue = n;
        }
        public String getName() {
            return Strings.get("changeAttributeAction");
        }
        public void doIt(Project proj) {
            attrs.setValue(attr, newValue);
        }
        public void undo(Project proj) {
            attrs.setValue(attr, oldValue);
        }
    }

    private class MyListener implements AttributeListener {
        public void attributeListChanged(AttributeEvent e) {
            if(e.getSource() != attrs) {
                e.getSource().removeAttributeListener(this);
                return;
            }
            model.fireTableChanged();
        }
        public void attributeValueChanged(AttributeEvent e) {
            if(e.getSource() != attrs) {
                e.getSource().removeAttributeListener(this);
                return;
            }
            model.fireTableChanged();
        }
    }

    private static class AttributeData {
        AttributeSet attrs;
        Attribute attr;
        java.awt.Component comp;

        AttributeData() { }
    }

    private static class MyDialog extends JDialogOk {
        JInputComponent input;
        Object value;

        public MyDialog(Dialog parent, JInputComponent input) {
            super(parent, Strings.get("attributeDialogTitle"), true);
            configure(input);
        }

        public MyDialog(Frame parent, JInputComponent input) {
            super(parent, Strings.get("attributeDialogTitle"), true);
            configure(input);
        }

        private void configure(JInputComponent input) {
            this.input = input;
            this.value = input.getValue();
            
            // Thanks to Christophe Jacquet, who contributed a fix to this
            // so that when the dialog is resized, the component within it
            // is resized as well. (Tracker #2024479)
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            p.add((JComponent) input, BorderLayout.CENTER);
            getContentPane().add(p, BorderLayout.CENTER);

            pack();
        }

        public void okClicked() {
            value = input.getValue();
        }

        public Object getValue() {
            return value;
        }
    }

    private class Model implements TableModel {
        LinkedList listeners = new LinkedList();
        private Object lastValue = null; // to prevent two messages being shown
        private long lastUpdate;         // due to duplicate calls to setValueAt

        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }

        void fireTableChanged() {
            TableModelEvent e = new TableModelEvent(this);
            Iterator it = ((List) listeners.clone()).iterator();
            while(it.hasNext()) {
                TableModelListener l = (TableModelListener) it.next();
                l.tableChanged(e);
            }
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int columnIndex) {
            if(columnIndex == 0) return "Attribute";
            else                 return "Value";
        }

        public Class getColumnClass(int columnIndex) {
            if(columnIndex == 0) return String.class;
            else                 return Object.class;
        }

        public int getRowCount() {
            if(attrs == null) return 0;
            else              return attrs.getAttributes().size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if(attrs == null) return null;
            Attribute attr = (Attribute) attrs.getAttributes().get(rowIndex);
            if(attr == null)            return null;
            else if(columnIndex == 0)   return attr.getDisplayName();
            else                        return attr.toDisplayString(attrs.getValue(attr));
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex == 0) {
                return false;
            } else {
                if(attrs == null) return false;
                Attribute attr = (Attribute) attrs.getAttributes().get(rowIndex);
                return !attrs.isReadOnly(attr);
            }
        }

        public void setValueAt(Object aValue, int rowIndex,
                int columnIndex) {
            Attribute attr = findCurrentAttribute();
            if(attr == null) {
                if(attrs == null) return;
                List attrList = attrs.getAttributes();
                if(rowIndex >= attrList.size()) return;
                attr = (Attribute) attrList.get(rowIndex);
            }
            if(attr == null || aValue == null) return;
            
            try {
                if(aValue instanceof String) {
                    aValue = attr.parse((String) aValue);
                }
                listener.valueChangeRequested(attr, aValue);
            } catch(ClassCastException e) {
                String msg = Strings.get("attributeChangeInvalidError")
                    + ": " + e;
                JOptionPane.showMessageDialog(proj.getFrame(), msg,
                        Strings.get("attributeChangeInvalidTitle"),
                        JOptionPane.WARNING_MESSAGE);
            } catch(NumberFormatException e) {
                long now = System.currentTimeMillis();
                if(aValue.equals(lastValue) && now < lastUpdate + 500) {
                    return;
                }
                lastValue = aValue;
                lastUpdate = System.currentTimeMillis();

                String msg = Strings.get("attributeChangeInvalidError");
                String emsg = e.getMessage();
                if(emsg != null && emsg.length() > 0) msg += ": " + emsg;
                msg += ".";
                JOptionPane.showMessageDialog(proj.getFrame(), msg,
                        Strings.get("attributeChangeInvalidTitle"),
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        private Attribute findCurrentAttribute() {
            for(int i = 0; i < history.length; i++) {
                if(history[i] != null && history[i].comp == editorComp) {
                    return history[i].attr;
                }
            }
            return null;
        }

    }

    private class CellEditor
            implements TableCellEditor, FocusListener, ActionListener {
        LinkedList listeners = new LinkedList();

        //
        // TableCellListener management
        //
        public void addCellEditorListener(CellEditorListener l) {
            // Adds a listener to the list that's notified when the
            // editor stops, or cancels editing. 
            listeners.add(l);
        }

        public void removeCellEditorListener(CellEditorListener l) {
            // Removes a listener from the list that's notified 
            listeners.remove(l);
        }

        public void fireEditingCanceled() {
            ChangeEvent e = new ChangeEvent(AttributeTable.this);
            Iterator it = ((List) listeners.clone()).iterator();
            while(it.hasNext()) {
                CellEditorListener l = (CellEditorListener) it.next();
                l.editingCanceled(e);
            }
        }

        public void fireEditingStopped() {
            ChangeEvent e = new ChangeEvent(AttributeTable.this);
            Iterator it = ((LinkedList) listeners.clone()).iterator();
            while(it.hasNext()) {
                CellEditorListener l = (CellEditorListener) it.next();
                l.editingStopped(e);
            }
        }

        //
        // other TableCellEditor methods
        //
        public void cancelCellEditing() {
            // Tells the editor to cancel editing and not accept any
            // partially edited value. 
            fireEditingCanceled();
        }

        public boolean stopCellEditing() {
            // Tells the editor to stop editing and accept any partially
            // edited value as the value of the editor. 
            fireEditingStopped();
            return true;
        }

        public Object getCellEditorValue() {
            // Returns the value contained in the editor. 
            java.awt.Component comp = editorComp;
            if(comp instanceof JTextField) {
                return ((JTextField) comp).getText();
            } else if(comp instanceof JComboBox) {
                Object val = ((JComboBox) comp).getSelectedItem();
                return val;
            } else {
                return null;
            }
        }

        public boolean isCellEditable(EventObject anEvent) {
            // Asks the editor if it can start editing using anEvent. 
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            // Returns true if the editing cell should be selected,
            // false otherwise. 
            return true;
        }

        public java.awt.Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {
            if(column == 0 || attrs == null) {
                return new JLabel(value.toString());
            } else {
                if(editorComp != null) editorComp.transferFocus();

                Attribute attr = (Attribute) attrs.getAttributes().get(row);
                java.awt.Component ret = attr.getCellEditor(parent, attrs.getValue(attr));
                if(ret instanceof JComboBox) {
                    ((JComboBox) ret).addActionListener(this);
                } else if(ret instanceof JInputComponent) {
                    JInputComponent input = (JInputComponent) ret;
                    MyDialog dlog;
                    Window parent = AttributeTable.this.parent;
                    if(parent instanceof Frame) {
                        dlog = new MyDialog((Frame) parent, input);
                    } else {
                        dlog = new MyDialog((Dialog) parent, input);
                    }
                    dlog.setVisible(true);
                    Object retval = dlog.getValue();
                    listener.valueChangeRequested(attr, retval);
                    return new JLabel(attr.toDisplayString(retval));
                } else {
                    ret.addFocusListener(this);
                }

                AttributeData n = history[history.length - 1];
                if(n == null) n = new AttributeData();
                for(int i = history.length - 1; i > 0; i--) {
                    history[i] = history[i - 1];
                }
                n.attrs = attrs;
                n.attr = attr;
                n.comp = ret;

                return ret;
            }
        }

        //
        // FocusListener methods
        //
        public void focusLost(FocusEvent e) {
            Object dst = e.getOppositeComponent();
            if(dst instanceof java.awt.Component) {
                java.awt.Component p = (java.awt.Component) dst;
                while(p != null && !(p instanceof java.awt.Window)) {
                    if(p == AttributeTable.this) {
                        // switch to another place in this table,
                        // no problem
                        return;
                    }
                    p = p.getParent();
                }
                // focus transferred outside table; stop editing
                editor.stopCellEditing();
            }
        }

        public void focusGained(FocusEvent e) { }

        //
        // ActionListener methods
        //
        public void actionPerformed(ActionEvent e) {
            stopCellEditing();
        }

    }

    private Window parent;
    private Project proj;
    private Model model = new Model();
    private CellEditor editor = new CellEditor();
    private AttributeSet attrs = null;
    private Listener listener = new DefaultListener();
    private MyListener attrsListener = new MyListener();
    private AttributeData[] history = new AttributeData[5];

    public AttributeTable(Window parent, Project proj) {
        this.parent = parent;
        this.proj = proj;
        setModel(model);
        setDefaultEditor(Object.class, editor);
        setTableHeader(null);
        setRowHeight(20);
        LocaleManager.addLocaleListener(this);
    }

    public AttributeSet getAttributeSet() {
        return attrs;
    }

    public void setAttributeSet(AttributeSet attrs) {
        setAttributeSet(attrs, null);
    }

    public void setAttributeSet(AttributeSet attrs, Listener l) {
        if(attrs != this.attrs) {
            removeEditor();
            if(this.attrs != null) {
                this.attrs.removeAttributeListener(attrsListener);
            }
            this.attrs = attrs;
            if(this.attrs != null) {
                this.attrs.addAttributeListener(attrsListener);
            }
            this.listener = (l == null ? new DefaultListener() : l);
            model.fireTableChanged();
        }
    }

    public void localeChanged() {
        model.fireTableChanged();
    }

}
