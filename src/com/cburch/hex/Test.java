/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.hex;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class Test {
    private static class Model implements HexModel {
        private ArrayList listeners = new ArrayList();
        private int[] data = new int[924];
        
        public void addHexModelListener(HexModelListener l) {
            listeners.add(l);
        }

        public void removeHexModelListener(HexModelListener l) {
            listeners.remove(l);
        }

        public long getFirstOffset() {
            return 11111;
        }

        public long getLastOffset() {
            return data.length + 11110;
        }

        public int getValueWidth() {
            return 9;
        }

        public int get(long address) {
            return data[(int) (address - 11111)];
        }

        public void set(long address, int value) {
            int[] oldValues = new int[] { data[(int) (address - 11111)] };
            data[(int) (address - 11111)] = value & 0x1FF;
            for(int i = listeners.size() - 1; i >= 0; i--) {
                HexModelListener l = (HexModelListener) listeners.get(i);
                l.bytesChanged(this, address, 1, oldValues);
            }
        }
        
        public void set(long start, int[] values) {
            int[] oldValues = new int[values.length];
            System.arraycopy(data, (int) (start - 11111), oldValues, 0, values.length);
            System.arraycopy(values, 0, data, (int) (start - 11111), values.length);
            for(int i = listeners.size() - 1; i >= 0; i--) {
                HexModelListener l = (HexModelListener) listeners.get(i);
                l.bytesChanged(this, start, values.length, oldValues);
            }
        }
        
        public void fill(long start, long len, int value) {
            int[] oldValues = new int[(int) len];
            System.arraycopy(data, (int) (start - 11111), oldValues, 0, (int) len);
            Arrays.fill(data, (int) (start - 11111), (int) len, value);
            for(int i = listeners.size() - 1; i >= 0; i--) {
                HexModelListener l = (HexModelListener) listeners.get(i);
                l.bytesChanged(this, start, len, oldValues);
            }
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        HexModel model = new Model();
        HexEditor editor = new HexEditor(model);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new JScrollPane(editor));
        frame.pack();
        frame.setVisible(true);
    }
}
