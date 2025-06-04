package com.Parser.Quadruple;

public class StringExpr extends Expr {
    public final String value;

    public StringExpr(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}