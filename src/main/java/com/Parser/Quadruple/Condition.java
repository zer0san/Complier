package com.Parser.Quadruple;

public class Condition {
    public final String op;
    public final Expr left, right;

    public Condition(String op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }
}
