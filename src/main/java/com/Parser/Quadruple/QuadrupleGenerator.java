package com.Parser.Quadruple;

import java.util.*;

public class QuadrupleGenerator {
    private int tempId = 0;
    private final Map<String, String> cseCache = new HashMap<>(); // 公共子表达式消除
    List<Quadruple> quds = new ArrayList<>();

    // 判断条件
    public void ifFalse(Condition cond, String label) {
        String left = generateExpr(cond.left);
        String right = generateExpr(cond.right);
        String temp = newTemp();
        quds.add(new Quadruple(cond.op, left, right, temp));
        quds.add(new Quadruple("if", temp, "_", label));
    }

    // 跳转标签
    public void gotoLabel(String label) {
        quds.add(new Quadruple("goto", "_", "_", label));
    }

    // 生成标签
    public void emitLabel(String label) {
        quds.add(new Quadruple("label", "_", "_", label));
    }

    // el 标签
    public void emitElLabel() {
        quds.add(new Quadruple("el", "_", "_", "_"));
    }

    // ie 标签
    public void emitIeLabel() {
        quds.add(new Quadruple("ie", "_", "_", "_"));
    }

    // we 标签
    public void emitWeLabel() {
        quds.add(new Quadruple("we", "_", "_", "_"));
    }

    // wh 标签
    public void emitWhLabel(){
        quds.add(new Quadruple("wh", "_", "_", "_"));
    }


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
                // 常量折叠
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

    public List<Quadruple> show() {
        for (var q : quds) {
            System.out.println(q);
        }
        return quds;
    }

    public List<Quadruple> getQuadruples() {
        return quds;
    }
}
