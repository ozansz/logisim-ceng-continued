/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.analyze.model;


public interface ExpressionVisitor {
    public Object visitAnd(Expression a, Expression b);
    public Object visitOr(Expression a, Expression b);
    public Object visitXor(Expression a, Expression b);
    public Object visitNot(Expression a);
    public Object visitVariable(String name);
    public Object visitConstant(int value);
}
