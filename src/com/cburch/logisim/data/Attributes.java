/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.data;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.cburch.logisim.util.FontUtil;
import com.cburch.logisim.util.IntegerFactory;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.StringGetter;

public class Attributes {
    private Attributes() { }
    
    private static class ConstantGetter implements StringGetter {
        private String str;
        public ConstantGetter(String str) { this.str = str; }
        public String get() { return str; }
        public String toString() { return get(); }
    }
    private static StringGetter getter(String s) { return new ConstantGetter(s); } 
    
    //
    // methods with display name == standard name
    //
    public static Attribute forString(String name) {
        return forString(name, getter(name));
    }

    public static Attribute forOption(String name,
            Object[] vals) {
        return forOption(name, getter(name), vals);
    }

    public static Attribute forInteger(String name) {
        return forInteger(name, getter(name));
    }

    public static Attribute forHexInteger(String name) {
        return forHexInteger(name, getter(name));
    }

    public static Attribute forIntegerRange(String name,
            int start, int end) {
        return forIntegerRange(name, getter(name), start, end);
    }

    public static Attribute forDouble(String name) {
        return forDouble(name, getter(name));
    }

    public static Attribute forBoolean(String name) {
        return forBoolean(name, getter(name));
    }

    public static Attribute forDirection(String name) {
        return forDirection(name, getter(name));
    }

    public static Attribute forBitWidth(String name) {
        return forBitWidth(name, getter(name));
    }

    public static Attribute forBitWidth(String name, int min, int max) {
        return forBitWidth(name, getter(name), min, max);
    }

    public static Attribute forFont(String name) {
        return forFont(name, getter(name));
    }

    public static Attribute forLocation(String name) {
        return forLocation(name, getter(name));
    }
    
    public static Attribute forColor(String name) {
        return forColor(name, getter(name));
    }
    
    //
    // methods with internationalization support
    //
    public static Attribute forString(String name, StringGetter disp) {
        return new StringAttribute(name, disp);
    }

    public static Attribute forOption(String name, StringGetter disp,
            Object[] vals) {
        return new OptionAttribute(name, disp, vals);
    }

    public static Attribute forInteger(String name, StringGetter disp) {
        return new IntegerAttribute(name, disp);
    }

    public static Attribute forHexInteger(String name, StringGetter disp) {
        return new HexIntegerAttribute(name, disp);
    }

    public static Attribute forIntegerRange(String name, StringGetter disp,
            int start, int end) {
        return new IntegerRangeAttribute(name, disp, start, end);
    }

    public static Attribute forDouble(String name, StringGetter disp) {
        return new DoubleAttribute(name, disp);
    }

    public static Attribute forBoolean(String name, StringGetter disp) {
        return new BooleanAttribute(name, disp);
    }

    public static Attribute forDirection(String name, StringGetter disp) {
        return new DirectionAttribute(name, disp);
    }

    public static Attribute forBitWidth(String name, StringGetter disp) {
        return new BitWidth.Attribute(name, disp);
    }

    public static Attribute forBitWidth(String name, StringGetter disp, int min, int max) {
        return new BitWidth.Attribute(name, disp, min, max);
    }

    public static Attribute forFont(String name, StringGetter disp) {
        return new FontAttribute(name, disp);
    }

    public static Attribute forLocation(String name, StringGetter disp) {
        return new LocationAttribute(name, disp);
    }
    
    public static Attribute forColor(String name, StringGetter disp) {
        return new ColorAttribute(name, disp);
    }

    private static class StringAttribute extends Attribute {
        private StringAttribute(String name, StringGetter disp) {
            super(name, disp);
        }

        public Object parse(String value) {
            return value;
        }
    }

    private static class OptionComboRenderer
            extends BasicComboBoxRenderer {
        Attribute attr;

        OptionComboRenderer(Attribute attr) {
            this.attr = attr;
        }

        public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Component ret = super.getListCellRendererComponent(list,
                value, index, isSelected, cellHasFocus);
            if(ret instanceof JLabel) {
                ((JLabel) ret).setText(attr.toDisplayString(value));
            }
            return ret;
        }
    }

    private static class OptionAttribute extends Attribute {
        private Object[] vals;

        private OptionAttribute(String name, StringGetter disp,
                Object[] vals) {
            super(name, disp);
            this.vals = vals;
        }

        public String toDisplayString(Object value) {
            if(value instanceof AttributeOptionInterface) {
                return ((AttributeOptionInterface) value).toDisplayString();
            } else {
                return value.toString();
            }
        }

        public Object parse(String value) {
            for(int i = 0; i < vals.length; i++) {
                if(value.equals(vals[i].toString())) return vals[i];
            }
            throw new NumberFormatException("value not among choices");
        }

        public java.awt.Component getCellEditor(Object value) {
            javax.swing.JComboBox combo = new javax.swing.JComboBox(vals);
            combo.setRenderer(new OptionComboRenderer(this));
            combo.setSelectedItem(value);
            return combo;
        }
    }

    private static class IntegerAttribute extends Attribute {
        private IntegerAttribute(String name, StringGetter disp) {
            super(name, disp);
        }

        public Object parse(String value) {
            return IntegerFactory.create(value);
        }
    }

    private static class HexIntegerAttribute extends Attribute {
        private HexIntegerAttribute(String name, StringGetter disp) {
            super(name, disp);
        }

        public String toDisplayString(Object value) {
            int val = ((Integer) value).intValue();
            return "0x" + Integer.toHexString(val);
        }

        public String toStandardString(Object value) {
            return toDisplayString(value);
        }

        public Object parse(String value) {
            value = value.toLowerCase();
            if(value.startsWith("0x")) value = value.substring(2);
            return IntegerFactory.create((int) Long.parseLong(value, 16));
        }
    }

    private static class DoubleAttribute extends Attribute {
        private DoubleAttribute(String name, StringGetter disp) {
            super(name, disp);
        }

        public Object parse(String value) {
            return Double.valueOf(value);
        }
    }

    private static class BooleanAttribute extends OptionAttribute {
        private static Object[] vals = { Boolean.TRUE, Boolean.FALSE };

        private BooleanAttribute(String name, StringGetter disp) {
            super(name, disp, vals);
        }

        public String toDisplayString(Object value) {
            if(value == vals[0]) return Strings.get("booleanTrueOption");
            else return Strings.get("booleanFalseOption");
        }

        public Object parse(String value) {
            Boolean b = Boolean.valueOf(value);
            return vals[b.booleanValue() ? 0 : 1];
        }
    }

    private static class IntegerRangeAttribute extends Attribute {
        Integer[] options = null;
        int start;
        int end;
        private IntegerRangeAttribute(String name, StringGetter disp, int start, int end) {
            super(name, disp);
            this.start = start;
            this.end = end;
        }
        public Object parse(String value) {
            int v = (int) Long.parseLong(value);
            if(v < start) throw new NumberFormatException("integer too small");
            if(v > end) throw new NumberFormatException("integer too large");
            return IntegerFactory.create(v);
        }
        public java.awt.Component getCellEditor(Object value) {
            if(options == null) {
                options = new Integer[end - start + 1];
                for(int i = start; i <= end; i++) {
                    options[i - start] = IntegerFactory.create(i);
                }
            }
            JComboBox combo = new JComboBox(options);
            combo.setSelectedItem(value);
            return combo;
        }
    }

    private static class DirectionAttribute extends OptionAttribute {
        private static Direction[] vals = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
        };

        public DirectionAttribute(String name, StringGetter disp) {
            super(name, disp, vals);
        }

        public String toDisplayString(Object value) {
            return ((Direction) value).toDisplayString();
        }

        public Object parse(String value) {
            return Direction.parse(value);
        }
    }

    private static class FontAttribute extends Attribute {
        private FontAttribute(String name, StringGetter disp) {
            super(name, disp);
        }

        public String toDisplayString(Object value) {
            Font f = (Font) value;
            return f.getFamily()
                + " " + FontUtil.toStyleDisplayString(f.getStyle())
                + " " + f.getSize();
        }

        public String toStandardString(Object value) {
            Font f = (Font) value;
            return f.getFamily()
                + " " + FontUtil.toStyleStandardString(f.getStyle())
                + " " + f.getSize();
        }

        public Object parse(String value) {
            return Font.decode(value);
        }

        public java.awt.Component getCellEditor(Object value) {
            return FontUtil.createFontChooser((Font) value);
        }
    }
    
    private static class LocationAttribute extends Attribute {
        public LocationAttribute(String name, StringGetter desc) {
            super(name, desc);
        }
        public Object parse(String value) {
            return Location.parse(value);
        }
    }

    private static class ColorAttribute extends Attribute {
        public ColorAttribute(String name, StringGetter desc) {
            super(name, desc);
        }

        public String toDisplayString(Object value) {
            return toStandardString(value);
        }
        public String toStandardString(Object value) {
            Color c = (Color) value;
            return "#" + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
        }
        private String hex(int value) {
            if(value >= 16) return Integer.toHexString(value);
            else return "0" + Integer.toHexString(value);
        }
        public Object parse(String value) {
            return Color.decode(value);
        }
        public java.awt.Component getCellEditor(Object value) {
            return new ColorChooser((Color) value);
        }
    }
    
    private static class ColorChooser extends JColorChooser
            implements JInputComponent {
        ColorChooser(Color initial) {
            super(initial);
        }

        public Object getValue() {
            return getColor();
        }

        public void setValue(Object value) {
            setColor((Color) value);
        }
    }
}
