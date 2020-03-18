/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.ComboBoxEditor;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

public class FontUtil {
    private static class IntegerComboBoxEditor
            implements ComboBoxEditor {
        private ComboBoxEditor parent;
        private Object oldval;

        private IntegerComboBoxEditor(ComboBoxEditor parent) {
            this.parent = parent;
            this.oldval = parent.getItem();
        }

        public void addActionListener(ActionListener l) {
            parent.addActionListener(l);
        }

        public void removeActionListener(ActionListener l) {
            parent.removeActionListener(l);
        }

        public Component getEditorComponent() {
            return parent.getEditorComponent();
        }

        public void selectAll() {
            parent.selectAll();
        }

        public Object getItem() {
            Object ret = parent.getItem();
            if(ret instanceof Integer) return ret;
            String str = ret.toString();
            try {
                return IntegerFactory.create(str);
            } catch(NumberFormatException e) {
                return oldval;
            }
        }

        public void setItem(Object anObject)  {
            parent.setItem(anObject);
            oldval = anObject;
        }
    }

    private static class FontChooser extends JPanel
            implements JInputComponent {
        JList font = new JList(new String[] {
            "Monospaced",
            "Serif",
            "SansSerif",
        });
        JComboBox size = new JComboBox(new Integer[] {
            IntegerFactory.create(10),
            IntegerFactory.create(12),
            IntegerFactory.create(14),
            IntegerFactory.create(16),
            IntegerFactory.create(18),
            IntegerFactory.create(24),
        });
        JList style = new JList(new StyleItem[] {
            new StyleItem(Font.PLAIN),
            new StyleItem(Font.ITALIC),
            new StyleItem(Font.BOLD),
            new StyleItem(Font.BOLD | Font.ITALIC),
        });

        public FontChooser(Font dflt) {
            Border border = BorderFactory.createEtchedBorder();

            font.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            font.setBorder(border);

            size.setEditable(true);
            size.setEditor(new IntegerComboBoxEditor(size.getEditor()));

            style.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            style.setBorder(border);

            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            setLayout(gridbag);
            JLabel lab;

            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.NONE;
            c.ipadx = 10;
            c.ipady = 5;
            c.weighty = 1.0;

            c.gridx = 0;

            lab = new JLabel(Strings.get("fontDlogFontLabel"));
            c.gridx = 0;
            gridbag.setConstraints(lab, c);
            add(lab);

            lab = new JLabel(Strings.get("fontDlogStyleLabel"));
            c.gridx = 1;
            gridbag.setConstraints(lab, c);
            add(lab);

            lab = new JLabel(Strings.get("fontDlogSizeLabel"));
            c.gridx = 2;
            gridbag.setConstraints(lab, c);
            add(lab);

            c.gridy = 1;

            c.gridx = 0;
            gridbag.setConstraints(font, c);
            add(font);

            c.gridx = 1;
            gridbag.setConstraints(style, c);
            add(style);

            c.gridx = 2;
            gridbag.setConstraints(size, c);
            add(size);

            setValue(dflt);
        }

        public void setValue(Object raw_val) {
            Font val;
            if(raw_val instanceof String) {
                val = Font.decode((String) raw_val);
            } else if(raw_val instanceof Font) {
                val = (Font) raw_val;
            } else {
                return;
            }

            font.setSelectedValue(val.getName(), true);
            size.setSelectedItem(IntegerFactory.create(val.getSize()));
            ListModel model = style.getModel();
            for(int i = 0; i < model.getSize(); i++) {
                StyleItem s = (StyleItem) model.getElementAt(i);
                if(s.style == val.getStyle()) {
                    style.setSelectedValue(s, true);
                }
            }
        }

        public Object getValue() {
            String fname;
            int fsize;
            int fstyle;

            Object name_val = font.getSelectedValue();
            fname = name_val == null ? null : name_val.toString();

            fsize = ((Integer) size.getSelectedItem()).intValue();
            fstyle = ((StyleItem) style.getSelectedValue()).style;

            Font ret = new Font(fname, fstyle, fsize);
            return ret;
        }
    }

    private static class StyleItem {
        int style;

        StyleItem(int style) {
            this.style = style;
        }

        public String toString() {
            return toStyleDisplayString(style);
        }
    }

    public static JPanel createFontChooser(Font dflt) {
        return new FontChooser(dflt);
    }

    public static String toStyleStandardString(int style) {
        switch(style) {
        case Font.PLAIN:
            return "plain";
        case Font.ITALIC:
            return "italic";
        case Font.BOLD:
            return "bold";
        case Font.BOLD | Font.ITALIC:
            return "bolditalic";
        default:
            return "??";
        }
    }

    public static String toStyleDisplayString(int style) {
        switch(style) {
        case Font.PLAIN:
            return Strings.get("fontPlainStyle");
        case Font.ITALIC:
            return Strings.get("fontItalicStyle");
        case Font.BOLD:
            return Strings.get("fontBoldStyle");
        case Font.BOLD | Font.ITALIC:
            return Strings.get("fontBoldItalicStyle");
        default:
            return "??";
        }
    }

}
