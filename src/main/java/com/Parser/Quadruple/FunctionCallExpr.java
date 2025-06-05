package com.Parser.Quadruple;
import java.util.List;

public class FunctionCallExpr extends Expr {
    String funcName;
    List<Expr> arguments;

    public FunctionCallExpr(String funcName, List<Expr> arguments) {
        this.funcName = funcName;
        this.arguments = arguments;
    }
}