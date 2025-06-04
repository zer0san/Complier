package com.Parser.Quadruple;

import java.util.*;

import static java.lang.String.format;

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
    public void emitWhLabel() {
        quds.add(new Quadruple("wh", "_", "_", "_"));
    }

    // 函数开始标签
    public void emitFuncLabel(String label) {
        quds.add(new Quadruple("FuncStart", "_", "_", label));
    }

    // 函数结束标签
    public void emitFuncEnd(String label) {
        quds.add(new Quadruple("FuncEnd", "_", "_", label));
    }

    // 数组声明
    public void declareArray(String arrayName, int size) {
        quds.add(new Quadruple("ARRAY_DECL", arrayName, Integer.toString(size), "_"));
    }

    // 数组访问
    public String arrayAccess(String arrayName, Expr indexExpr) {
        String indexValue = generateExpr(indexExpr);
        String temp = newTemp();
        quds.add(new Quadruple("=", arrayName + indexValue, "_", temp));
        return temp;
    }

    // 数组赋值
    public void assignArray(String arrayName, Expr indexExpr, Expr valueExpr) {
        String indexValue = generateExpr(indexExpr);
        String value = generateExpr(valueExpr);
        quds.add(new Quadruple("=", value, "_", arrayName + "[" + indexValue + "]"));
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
        } else if (expr instanceof CharExpr c) {
            return "'" + Character.toString(c.value) + "'";
        } else if (expr instanceof VarExpr v) {
            return v.name;
        } else if (expr instanceof ArrayAccessExpr a) {
            return arrayAccess(a.arrayName, a.index);
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
        if (expr instanceof NumberExpr) {
            quds.add(new Quadruple("=", ((NumberExpr) expr).value + "", "_", var));
        } else if (expr instanceof CharExpr) {
            // 处理字符字面量
            quds.add(new Quadruple("=", "'" + ((CharExpr) expr).value + "'", "_", var));
        } else if (expr instanceof StringExpr) {
            quds.add(new Quadruple("=", "\"" + ((StringExpr) expr).value + "\"", "_", var));
        } else if (expr instanceof VarExpr) {
            quds.add(new Quadruple("=", ((VarExpr) expr).name, "_", var));
        } else if (expr instanceof BinaryExpr) {
            String result = generateExpr((BinaryExpr) expr);
            quds.add(new Quadruple("=", result, "_", var));
        }
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
