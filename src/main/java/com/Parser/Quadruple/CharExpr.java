package com.Parser.Quadruple;

public class CharExpr extends Expr {
    public final char value;

    public CharExpr(char value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}