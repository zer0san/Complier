package com.Parser.Quadruple;

// 数组访问表达式，如 a[i]
public class ArrayAccessExpr extends Expr {
    public final String arrayName;
    public final Expr index;

    public ArrayAccessExpr(String arrayName, Expr index) {
        this.arrayName = arrayName;
        this.index = index;
    }
}
