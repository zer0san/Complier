package Parser;

import Lexer.*;

import java.util.*;

// AST 定义

abstract class Expr {
}

class NumberExpr extends Expr {
    int value;

    NumberExpr(int v) {
        this.value = v;
    }
}

class VarExpr extends Expr{

}