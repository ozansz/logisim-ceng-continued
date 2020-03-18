/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.util;

import java.util.NoSuchElementException;

public class PQueue {
    private static final int INIT_SIZE = 64;
    
    private Comparable[] data;
    private int size;
    
    public PQueue() {
        data = new Comparable[INIT_SIZE];
        size = 0;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public void clear() {
        size = 0;
        if(data.length != INIT_SIZE) data = new Comparable[64];
    }

    public void add(Comparable value) {
        if(value == null) {
            throw new IllegalArgumentException("Cannot add null");
        }
        
        if(size == data.length) {
            Comparable[] newData = new Comparable[2 * data.length];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
        
        int index = size;
        while(index > 0) {
            int parent = (index - 1) / 2;
            Comparable c = data[parent];
            if(c.compareTo(value) >= 0) break;
            data[index] = c;
            index = parent;
        }
        data[index] = value;
        size++;
    }
    
    public Object peek() {
        return size > 0 ? data[0] : null;
    }

    public Object remove() {
        int newSize = size - 1;
        if(newSize < 0) {
            throw new NoSuchElementException("priority queue is empty");
        }
        
        Object ret = data[0];
        Comparable value = data[newSize];
        if(value == null) return null;
        size = newSize;
        data[newSize] = null;
        int index = 0;
        while(true) {
            int childIndex = 2 * index + 1;
            Comparable child;
            if(childIndex + 1 < newSize) {
                child = data[childIndex];
                Comparable other = data[childIndex + 1];
                if(other.compareTo(child) > 0) {
                    child = other;
                    childIndex++;
                }
            } else if(childIndex < newSize) {
                child = data[childIndex];
            } else {
                break;
            }
            if(value.compareTo(child) >= 0) break;
            data[index] = child;
            index = childIndex;
        }
        data[index] = value;
        return ret;
    }

    public static void main(String[] args) throws java.io.IOException {
        java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(System.in));
        PQueue pq = new PQueue();
        while(true) {
            System.out.print("data:"); //OK
            if(!pq.isEmpty()) {
                for(int i = 0; i < pq.size; i++) {
                    System.out.print(" " + pq.data[i]); //OK
                }
                System.out.println(); //OK
            } else {
                System.out.println(" (empty)"); //OK
            }
            System.out.print("? "); //OK
            String line = in.readLine();
            java.util.StringTokenizer toks = new java.util.StringTokenizer(line);
            if(!toks.hasMoreTokens()) continue;
            String cmd = toks.nextToken();
            if(cmd.equals("+")) {
                String data = toks.hasMoreTokens() ? toks.nextToken() : "";
                pq.add(data);
            } else if(cmd.equals("-")) {
                System.out.println("removed " + pq.remove()); //OK
            } else {
                System.out.println("unknown command " + cmd); //OK
            }
        }
    }
}
