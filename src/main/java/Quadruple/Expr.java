package Quadruple;

// AST 定义

abstract class Expr {
}


// 数字
class NumberExpr extends Expr {
    int value;

    NumberExpr(int v) {
        this.value = v;
    }
}

// 标识符（变量）
class VarExpr extends Expr{
    String name;
    VarExpr(String n){
        this.name = n;
    }
}

// 二元运算
class BinaryExpr extends Expr {
    String op;
    Expr left, right;
    BinaryExpr(String op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }
}

// 赋值
class AssignExpr extends Expr {
    String name;
    Expr expr;
    AssignExpr(String name, Expr expr) {
        this.name = name;
        this.expr = expr;
    }
}