package Quadruple;

import java.util.*;

public class QuadrupleGenerator {
    private int tempId = 0;
    private final Map<String, String> cseCache = new HashMap<>(); // 公共子表达式消除
    List<Quadruple> quds = new ArrayList<>();

    boolean isNumber(String s) {
        return s.matches("-?\\d+");
    }

    // 创建临时变量
    String newTemp() {
        return "t" + (tempId++);
    }

    String generateExpr(Expr expr) {
        if (expr instanceof NumberExpr n) {
            return Integer.toString(n.value);
        } else if (expr instanceof VarExpr v) {
            return v.name;
        } else if (expr instanceof BinaryExpr b) {
            String arg1 = generateExpr(b.left);
            String arg2 = generateExpr(b.right);
            String key = b.op + "," + arg1 + "," + arg2;
            if (cseCache.containsKey(key)) {
                return cseCache.get(key);
            }
            if (isNumber(arg1) && isNumber(arg2)) {
                int folded = switch (b.op) {
                    case "+" -> Integer.parseInt(arg1) + Integer.parseInt(arg2);
                    case "-" -> Integer.parseInt(arg1) - Integer.parseInt(arg2);
                    case "*" -> Integer.parseInt(arg1) * Integer.parseInt(arg2);
                    case "/" -> Integer.parseInt(arg1) / Integer.parseInt(arg2);
                    default -> throw new RuntimeException("Unexpected operator: " + b.op);
                };
                return Integer.toString(folded);
            }
            String result = newTemp();
            quds.add(new Quadruple(b.op, arg1, arg2, result));
            cseCache.put(key, result);
            return result;
        }
        return null;
    }

    public void assign(String var, Expr expr) {
        String value = generateExpr(expr);
        quds.add(new Quadruple("=", value, "_", var));
    }

    public void show() {
        for (var q : quds) {
            System.out.println(q);
        }
    }
}
