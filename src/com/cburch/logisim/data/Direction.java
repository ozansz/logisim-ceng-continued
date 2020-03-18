/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.data;

import com.cburch.logisim.util.StringGetter;

public class Direction implements AttributeOptionInterface {
    public static final Direction EAST
        = new Direction("east", Strings.getter("directionEastOption"), 0);
    public static final Direction WEST
        = new Direction("west", Strings.getter("directionWestOption"), 1);
    public static final Direction NORTH
        = new Direction("north", Strings.getter("directionNorthOption"), 2);
    public static final Direction SOUTH
        = new Direction("south", Strings.getter("directionSouthOption"), 3);
    public static final Direction[] cardinals
        = { NORTH, EAST, SOUTH, WEST };

    public static Direction parse(String str) {
        if(str.equals(EAST.name))  return EAST;
        if(str.equals(WEST.name))  return WEST;
        if(str.equals(NORTH.name)) return NORTH;
        if(str.equals(SOUTH.name)) return SOUTH;
        throw new NumberFormatException("illegal direction '" + str + "'");
    }

    private String name;
    private StringGetter disp;
    private int id;

    private Direction(String name, StringGetter disp, int id) {
        this.name = name;
        this.disp = disp;
        this.id = id;
    }

    public String toString() {
        return name;
    }

    public String toDisplayString() {
        return disp.get();
    }

    public int hashCode() {
        return id;
    }
    
    public double toRadians() {
        if(this == Direction.EAST) return 0.0;
        if(this == Direction.WEST) return Math.PI;
        if(this == Direction.NORTH) return Math.PI / 2.0;
        if(this == Direction.SOUTH) return -Math.PI / 2.0;
        return 0.0;
    }
    
    public int toDegrees() {
        if(this == Direction.EAST) return 0;
        if(this == Direction.WEST) return 180;
        if(this == Direction.NORTH) return 90;
        if(this == Direction.SOUTH) return 270;
        return 0;
    }
    
    public Direction reverse() {
        if(this == Direction.EAST) return Direction.WEST;
        if(this == Direction.WEST) return Direction.EAST;
        if(this == Direction.NORTH) return Direction.SOUTH;
        if(this == Direction.SOUTH) return Direction.NORTH;
        return Direction.WEST;
    }

    // for AttributeOptionInterface
    public Object getValue() {
        return this;
    }
}
