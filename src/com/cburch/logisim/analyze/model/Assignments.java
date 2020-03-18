/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.analyze.model;

import java.util.HashMap;
import java.util.Map;

class Assignments {
    private Map map = new HashMap();
    
    public Assignments() { }
    
    public boolean get(String variable) {
        Boolean value = (Boolean) map.get(variable);
        return value != null ? value.booleanValue() : false;
    }
    
    public void put(String variable, boolean value) {
        map.put(variable, Boolean.valueOf(value));
    }
}
