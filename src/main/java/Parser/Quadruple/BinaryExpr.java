package Parser.Quadruple;

// 二元运算
public class BinaryExpr extends Expr {
    String op;
    Expr left, right;
    public BinaryExpr(String op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }
}
