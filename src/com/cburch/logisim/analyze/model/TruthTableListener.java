/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.analyze.model;

public interface TruthTableListener {
    public void cellsChanged(TruthTableEvent event);
    public void structureChanged(TruthTableEvent event);
}
