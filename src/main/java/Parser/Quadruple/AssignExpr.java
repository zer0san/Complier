package Parser.Quadruple;

// 赋值
public class AssignExpr extends Expr {
    String name;
    Expr expr;
    public AssignExpr(String name, Expr expr) {
        this.name = name;
        this.expr = expr;
    }
}
