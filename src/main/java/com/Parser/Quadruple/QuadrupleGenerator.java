package com.Parser.Quadruple;

import java.util.*;

import static java.lang.String.format;

public class QuadrupleGenerator {
    private int tempId = 0;
    private final Map<String, String> cseCache = new HashMap<>(); // 公共子表达式消除
    List<Quadruple> quds = new ArrayList<>();
    // 函数签名类
    private static class FunctionSignature {
        String returnType;
        List<String> paramTypes;
        List<String> paramNames;

        public FunctionSignature(String returnType, List<String> paramTypes, List<String> paramNames) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.paramNames = paramNames;
        }
    }
    // 函数表
    private final Map<String, FunctionSignature> functionTable = new HashMap<>();
    private String currentFunction = null;
    // 在QuadrupleGenerator类中添加此方法
    public void declareVariable(String type, String varName) {
        quds.add(new Quadruple("var_decl", type, "_", varName));
    }
    // 当前正在处理的函数名
    private String getExprType(Expr expr) {
        if (expr instanceof NumberExpr) {
            return "int";
        } else if (expr instanceof CharExpr) {
            return "char";
        } else if (expr instanceof StringExpr) {
            return "string";
        } else if (expr instanceof VarExpr) {
            VarExpr varExpr = (VarExpr) expr;
            // 查找变量声明获取类型 - 需要查找所有类型的声明
            for (int j = quds.size() - 1; j >= 0; j--) {
                Quadruple q = quds.get(j);
                // 查找参数声明
                if ("param_decl".equals(q.op) && varExpr.name.equals(q.result)) {
                    return q.arg1.equals("_") ? "int" : q.arg1;
                }
                // 查找局部变量声明 - 添加一个新的四元式类型
                if ("var_decl".equals(q.op) && varExpr.name.equals(q.result)) {
                    return q.arg1.equals("_") ? "int" : q.arg1;
                }
            }
            // 不再默认返回int，而是抛出异常
            throw new RuntimeException("未声明的变量: " + varExpr.name);

        } else if (expr instanceof FunctionCallExpr) {
            FunctionCallExpr funcCall = (FunctionCallExpr) expr;
            if (functionTable.containsKey(funcCall.funcName)) {
                return functionTable.get(funcCall.funcName).returnType;
            }
            throw new RuntimeException("未定义的函数: " + funcCall.funcName);
        } else if (expr instanceof ArrayAccessExpr) {
            ArrayAccessExpr arrayExpr = (ArrayAccessExpr)expr;
            // 查找数组声明，获取数组元素类型
            for (int j = quds.size() - 1; j >= 0; j--) {
                Quadruple q = quds.get(j);
                if ("var_decl".equals(q.op) && arrayExpr.arrayName.equals(q.result)) {
                    return q.arg1; // 返回数组声明的类型
                }
            }
            throw new RuntimeException("未声明的数组: " + arrayExpr.arrayName);
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr)expr;
            String leftType = getExprType(binExpr.left);
            String rightType = getExprType(binExpr.right);

            // 类型一致时才能进行二元运算
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("二元表达式两侧类型不匹配: " + leftType + " 和 " + rightType);
            }
            return leftType; // 返回表达式类型
        }
        return "unknown";
    }


    // 检查类型是否兼容
    private boolean isTypeCompatible(String expectedType, String actualType) {
        if (expectedType.equals(actualType)) {
            return true;
        }

        // 允许的类型转换规则
        if (expectedType.equals("int") && actualType.equals("char")) {
            return true; // char可以转换为int
        }

        return false; // 其他类型组合不兼容
    }

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
        // 清除当前函数上下文
        currentFunction = null;
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

    // 处理函数调用
    public String generateFunctionCall(FunctionCallExpr call) {
        // 检查函数是否已定义
        if (!functionTable.containsKey(call.funcName)) {
            throw new RuntimeException("未定义的函数: " + call.funcName);
        }

        // 获取函数签名
        FunctionSignature signature = functionTable.get(call.funcName);

        // 检查参数数量
        if (call.arguments.size() != signature.paramTypes.size()) {
            throw new RuntimeException("函数 '" + call.funcName +
                    "' 需要 " + signature.paramTypes.size() +
                    " 个参数，但提供了 " + call.arguments.size() + " 个");
        }
        // 类型检查
        for (int i = 0; i < call.arguments.size(); i++) {
            String expectedType = signature.paramTypes.get(i);
            String actualType = getExprType(call.arguments.get(i));

            if (!isTypeCompatible(expectedType, actualType)) {
                throw new RuntimeException("函数 '" + call.funcName +
                        "' 的第 " + (i+1) + " 个参数类型不匹配: 期望 " +
                        expectedType + "，实际为 " + actualType);
            }
        }
        List<String> evaluatedArgs = new ArrayList<>();

        // 先计算所有参数
        for (Expr arg : call.arguments) {
            String argValue = generateExpr(arg);
            evaluatedArgs.add(argValue);
        }

        // 生成参数传递的四元式
        for (String arg : evaluatedArgs) {
            quds.add(new Quadruple("param", arg, "_", "_"));
        }

        // 生成函数调用四元式
        String temp = newTemp();
        quds.add(new Quadruple("call", call.funcName, String.valueOf(evaluatedArgs.size()), temp));

        return temp;
    }
    // 添加函数参数声明
    public void declareParameter(String paramName) {
        quds.add(new Quadruple("param_decl", "_", "_", paramName));
    }

    // 添加函数返回语句
    // 添加函数返回语句（带类型检查）
    public void returnStmt(Expr returnExpr) {
        // 检查当前是否在函数内
        if (currentFunction != null && functionTable.containsKey(currentFunction)) {
            String declaredReturnType = functionTable.get(currentFunction).returnType;

            if (returnExpr != null) {
                String value = generateExpr(returnExpr);
                String exprType = getExprType(returnExpr);

                // 类型检查
                if (!isTypeCompatible(declaredReturnType, exprType)) {
                    throw new RuntimeException("函数 '" + currentFunction +
                            "' 返回类型不匹配: 期望 " + declaredReturnType +
                            "，实际为 " + exprType);
                }

                quds.add(new Quadruple("return", value, "_", "_"));
            } else {
                // 无返回值，检查函数是否声明为void
                if (!"void".equals(declaredReturnType)) {
                    throw new RuntimeException("函数 '" + currentFunction +
                            "' 需要返回 " + declaredReturnType + " 类型的值");
                }
                quds.add(new Quadruple("return", "_", "_", "_"));
            }
        } else {
            // 不在函数内或函数未声明
            if (returnExpr != null) {
                String value = generateExpr(returnExpr);
                quds.add(new Quadruple("return", value, "_", "_"));
            } else {
                quds.add(new Quadruple("return", "_", "_", "_"));
            }
        }
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
        } else if (expr instanceof FunctionCallExpr f) {
            return generateFunctionCall(f);
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
    // 添加获取变量类型的辅助方法
    private String getVarType(String varName) {
        // 查找变量声明获取类型
        for (int j = quds.size() - 1; j >= 0; j--) {
            Quadruple q = quds.get(j);
            // 查找参数声明
            if ("param_decl".equals(q.op) && varName.equals(q.result)) {
                return q.arg1.equals("_") ? "int" : q.arg1;
            }
            // 查找局部变量声明
            if ("var_decl".equals(q.op) && varName.equals(q.result)) {
                return q.arg1.equals("_") ? "int" : q.arg1;
            }
        }
        // 不再默认返回int，而是抛出异常
        throw new RuntimeException("未声明的变量: " + varName);
    }

    public void assign(String var, Expr expr) {
        // 获取左侧变量类型
        String varType = getVarType(var);
        // 获取右侧表达式类型
        String exprType = getExprType(expr);

        // 类型检查
        if (!isTypeCompatible(varType, exprType)) {
            throw new RuntimeException("变量 '" + var + "' 赋值类型不匹配: 变量类型 " +
                    varType + "，表达式类型 " + exprType);
        }

        if (expr instanceof NumberExpr) {
            quds.add(new Quadruple("=", ((NumberExpr) expr).value + "", "_", var));
        } else if (expr instanceof CharExpr) {
            quds.add(new Quadruple("=", "'" + ((CharExpr) expr).value + "'", "_", var));
        } else if (expr instanceof StringExpr) {
            quds.add(new Quadruple("=", "\"" + ((StringExpr) expr).value + "\"", "_", var));
        } else if (expr instanceof VarExpr) {
            quds.add(new Quadruple("=", ((VarExpr) expr).name, "_", var));
        } else if (expr instanceof FunctionCallExpr) {
            // 处理函数调用表达式
            String result = generateFunctionCall((FunctionCallExpr) expr);
            quds.add(new Quadruple("=", result, "_", var));
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
    // 新增方法，接收参数类型
    public void emitFuncParam(String returnType, String funcName, List<String> argList) {

        // 设置当前函数上下文
        currentFunction = funcName;
        // 解析RecursiveParser中参数名称列表，获取类型信息
        List<String> paramTypes = new ArrayList<>();
        // 获取参数类型信息 (从第一个参数的声明中提取)
        for (int i = 0; i < argList.size(); i++) {
            String paramName = argList.get(i);
            // 查找参数类型的四元式
            for (int j = quds.size() - 1; j >= 0; j--) {
                Quadruple q = quds.get(j);
                if ("param_decl".equals(q.op) && paramName.equals(q.result)) {
                    paramTypes.add(q.arg1.equals("_") ? "int" : q.arg1); // 如果类型未指定，默认为int
                    break;
                }
            }
            // 如果找不到类型信息，抛出异常
            if (paramTypes.size() <= i) {
                throw new RuntimeException("无法确定函数 '" + funcName + "' 参数 '" + paramName + "' 的类型");
            }
        }

        // 存储函数签名
        functionTable.put(funcName, new FunctionSignature(returnType, paramTypes, argList));

        // 生成四元式，同时添加类型信息
        quds.add(new Quadruple("FuncDef", returnType, String.valueOf(argList.size()), funcName));
        for (int i = 0; i < argList.size(); i++) {
            String paramName = argList.get(i);
            String paramType = paramTypes.get(i);
            quds.add(new Quadruple("param_decl", paramType, "_", paramName));
        }
    }
}
