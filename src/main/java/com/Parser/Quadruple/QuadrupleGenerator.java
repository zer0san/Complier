package com.Parser.Quadruple;// 表示该类属于com.Parser.Quadruple包

import java.util.*;

import static java.lang.String.format;

/**
 * 四元式中间代码生成器
 * 负责将语法分析过程中的表达式和语句转换为四元式形式的中间代码
 * 四元式格式：(操作符, 操作数1, 操作数2, 结果)
 */
public class QuadrupleGenerator {
    // 临时变量计数器，用于生成唯一的临时变量名(t0, t1, t2...)
    private int tempId = 0;
    // 用于公共子表达式消除的缓存，键为"操作符,操作数1,操作数2"，值为对应的临时变量
    private final Map<String, String> cseCache = new HashMap<>(); // 公共子表达式消除
    // 存储生成的四元式列表
    List<Quadruple> quds = new ArrayList<>();
    // 存储已声明的变量名，用于避免重复声明
    private final Set<String> declaredVariables = new HashSet<>();
    // 存储函数是否有返回值的映射，键为函数名，值为是否有返回值
    private final Map<String, Boolean> functionHasReturn = new HashMap<>();
    // 新增：按作用域（函数名或 "global"）区分的声明变量集合
    private final Map<String, Set<String>> scopedDeclaredVariables = new HashMap<>();



    /**
     * 函数签名内部类，用于存储函数的返回类型、参数类型和参数名称
     * 用于函数声明和调用时的类型检查
     */
    private static class FunctionSignature {
        String returnType;              // 函数返回类型
        List<String> paramTypes;        // 函数参数类型列表
        List<String> paramNames;        // 函数参数名称列表

        /**
         * 构造函数签名对象
         * @param returnType 返回类型
         * @param paramTypes 参数类型列表
         * @param paramNames 参数名称列表
         */
        public FunctionSignature(String returnType, List<String> paramTypes, List<String> paramNames) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.paramNames = paramNames;
        }
    }

    // 函数表，存储所有函数的签名信息，键为函数名
    private final Map<String, FunctionSignature> functionTable = new HashMap<>();
    // 当前正在处理的函数名，用于上下文相关的操作如返回语句检查
    private String currentFunction = null;

    /**
     * 生成变量声明的四元式
     * @param type 变量类型(int/char/string)
     * @param varName 变量名
     * @throws RuntimeException 当变量重复声明时
     */
    public void declareVariable(String type, String varName) {

        if (currentFunction == null) {
            // 全局作用域，沿用原有 declaredVariables 逻辑
            if (declaredVariables.contains(varName)) {
                throw new RuntimeException("变量 '" + varName + "' 重复声明");
            }
            if (functionTable.containsKey(varName)) {
                throw new RuntimeException("变量名 '" + varName + "' 与已声明函数冲突");
            }
            declaredVariables.add(varName);
        } else {
            // 函数作用域，使用新建的 scopedDeclaredVariables
            Set<String> vars = scopedDeclaredVariables.get(currentFunction);
            if (vars.contains(varName)) {
                throw new RuntimeException("变量 '" + varName + "' 在函数 '" + currentFunction + "' 中重复声明");
            }
            if (functionTable.containsKey(varName)) {
                throw new RuntimeException("变量名 '" + varName + "' 与已声明函数冲突");
            }
            vars.add(varName);
            declaredVariables.add(varName);
        }
        // 添加到已声明变量集合

        // 生成变量声明四元式
        quds.add(new Quadruple("var_decl", type, "_", varName));
    }
    
    /**
     * 获取表达式的类型
     * @param expr 要检查类型的表达式
     * @return 表达式的类型(int/char/string)
     * @throws RuntimeException 当表达式引用未声明的变量或函数时
     */
    private String getExprType(Expr expr) {
        if (expr instanceof NumberExpr) {
            // 数字字面量的类型是int
            return "int";
        } else if (expr instanceof CharExpr) {
            // 字符字面量的类型是char
            return "char";
        } else if (expr instanceof StringExpr) {
            // 字符串字面量的类型是string
            return "string";
        } else if (expr instanceof VarExpr) {
            VarExpr varExpr = (VarExpr) expr;
            // 使用getVarType获取变量类型，它能正确识别数组类型
            return getVarType(varExpr.name);
        } else if (expr instanceof FunctionCallExpr) {
            // 函数调用表达式的类型是函数的返回类型
            FunctionCallExpr funcCall = (FunctionCallExpr) expr;
            if (functionTable.containsKey(funcCall.funcName)) {
                return functionTable.get(funcCall.funcName).returnType;
            }
            throw new RuntimeException("未定义的函数: " + funcCall.funcName);
        } else if (expr instanceof ArrayAccessExpr) {
            // 数组访问表达式的类型是数组元素的类型
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
            // 二元表达式的类型由操作数决定
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

    /**
     * 检查两个类型是否兼容(可以相互赋值)，添加数组类型检查
     * @param expectedType 期望的类型
     * @param actualType 实际的类型
     * @return 如果类型兼容则返回true，否则返回false
     */
    private boolean isTypeCompatible(String expectedType, String actualType) {
        // 完全相同的类型总是兼容的
        if (expectedType.equals(actualType)) {
            return true;
        }

        // 数组类型检查
        boolean expectedIsArray = expectedType.endsWith("[]");
        boolean actualIsArray = actualType.endsWith("[]");

        // 如果一个是数组，另一个不是数组，则绝对不兼容
        if (expectedIsArray != actualIsArray) {
            throw new RuntimeException("类型不兼容: 数组类型不能与基本类型互相转换 (" + actualType + " → " + expectedType + ")");
        }

        // 如果都是数组，则必须是相同类型的数组
        if (expectedIsArray && actualIsArray) {
            String expectedBaseType = expectedType.substring(0, expectedType.length() - 2);
            String actualBaseType = actualType.substring(0, actualType.length() - 2);
            if (!expectedBaseType.equals(actualBaseType)) {
                throw new RuntimeException("数组类型不兼容: " + actualType + " 不能转换为 " + expectedType);
            }
            return true;
        }

        // 允许的基本类型转换规则
        if (expectedType.equals("int") && actualType.equals("char")) {
            return true; // char可以转换为int
        }

        return false; // 其他类型组合不兼容
    }

    /**
     * 生成条件判断的四元式，如果条件为false则跳转到指定标签
     * @param cond 条件表达式
     * @param label 跳转目标标签
     */
    public void ifFalse(Condition cond, String label) {
        String left = generateExpr(cond.left);
        String right = generateExpr(cond.right);
        String temp = newTemp();
        quds.add(new Quadruple(cond.op, left, right, temp));
        quds.add(new Quadruple("if", temp, "_", label));
    }

    /**
     * 生成无条件跳转到指定标签的四元式
     * @param label 跳转目标标签
     */
    public void gotoLabel(String label) {
        quds.add(new Quadruple("goto", "_", "_", label));
    }

    /**
     * 生成标签定义的四元式
     * @param label 标签名
     */
    public void emitLabel(String label) {
        quds.add(new Quadruple("label", "_", "_", label));
    }

    /**
     * 生成else开始标记的四元式
     */
    public void emitElLabel() {
        quds.add(new Quadruple("el", "_", "_", "_"));
    }

    /**
     * 生成if-else结束标记的四元式
     */
    public void emitIeLabel() {
        quds.add(new Quadruple("ie", "_", "_", "_"));
    }

    /**
     * 生成while循环结束标记的四元式
     */
    public void emitWeLabel() {
        quds.add(new Quadruple("we", "_", "_", "_"));
    }

    /**
     * 生成while循环开始标记的四元式
     */
    public void emitWhLabel() {
        quds.add(new Quadruple("wh", "_", "_", "_"));
    }

    /**
     * 生成函数开始标记的四元式
     * @param label 函数名
     */
    public void emitFuncLabel(String label) {
        quds.add(new Quadruple("FuncStart", "_", "_", label));
    }

    /**
     * 生成函数结束标记的四元式
     * @param label 函数名
     */
    public void emitFuncEnd(String label) {
        // 检查非void函数是否有返回语句
        if (functionTable.containsKey(label)) {
            String returnType = functionTable.get(label).returnType;
            if (!"void".equals(returnType) &&
                    (!functionHasReturn.containsKey(label) || !functionHasReturn.get(label))) {
                throw new RuntimeException("错误：函数 '" + label + "' 声明为 '" + returnType +
                        "' 类型但没有返回语句");
            }
        }
        // 清除当前函数上下文
        currentFunction = null;
        quds.add(new Quadruple("FuncEnd", "_", "_", label));
    }

    /**
     * 生成数组声明的四元式
     * @param arrayName 数组名
     * @param size 数组大小
     */
    public void declareArray(String arrayName, int size) {
        quds.add(new Quadruple("ARRAY_DECL", arrayName, Integer.toString(size), "_"));
    }

    /**
     * 生成数组访问的四元式
     * @param arrayName 数组名
     * @param indexExpr 索引表达式
     * @return 存储数组元素值的临时变量
     */
    public String arrayAccess(String arrayName, Expr indexExpr) {
        String indexValue = generateExpr(indexExpr);
        String temp = newTemp();
        quds.add(new Quadruple("=", arrayName + "[" + indexValue + "]", "_", temp));
        return temp;
    }

    /**
     * 生成数组赋值的四元式
     * @param arrayName 数组名
     * @param indexExpr 索引表达式
     * @param valueExpr 值表达式
     */
    public void assignArray(String arrayName, Expr indexExpr, Expr valueExpr) {
        String indexValue = generateExpr(indexExpr);
        String value = generateExpr(valueExpr);
        quds.add(new Quadruple("=", value, "_", arrayName + "[" + indexValue + "]"));
    }

    /**
     * 判断字符串是否为数字
     * @param s 要检查的字符串
     * @return 如果是数字则返回true
     */
    boolean isNumber(String s) {
        return s.matches("-?\\d+");
    }

    /**
     * 生成函数调用的四元式
     * @param call 函数调用表达式
     * @return 存储函数返回值的临时变量
     * @throws RuntimeException 当函数未定义或参数不匹配时
     */
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

    /**
     * 生成参数声明的四元式
     * @param type 参数类型
     * @param paramName 参数名
     */
    public void declareParameter(String type, String paramName) {
        quds.add(new Quadruple("param_decl",/*type*/"_", "_", paramName));
    }



    /**
     * 生成返回语句的四元式(带类型检查)
     * @param returnExpr 返回表达式，可以为null表示无返回值
     * @throws RuntimeException 当返回类型与函数声明不匹配时
     */
    public void returnStmt(Expr returnExpr) {
        // 检查当前是否在函数内
        if (currentFunction != null && functionTable.containsKey(currentFunction)) {
            String declaredReturnType = functionTable.get(currentFunction).returnType;

            if (returnExpr != null) {
                String value = generateExpr(returnExpr);
                String exprType = getExprType(returnExpr);

                // 直接调用isTypeCompatible，它会在类型不兼容时抛出异常
                isTypeCompatible(declaredReturnType, exprType);

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
        // 如果当前在函数内并且有返回值，标记该函数已有返回语句
        if (currentFunction != null && returnExpr != null) {
            functionHasReturn.put(currentFunction, true);
        }
    }
    
    /**
     * 创建新的临时变量名
     * @return 格式为"t数字"的临时变量名
     */
    String newTemp() {
        return "t" + (tempId++);
    }

    /**
     * 递归生成表达式的四元式，并返回表达式的结果(变量名或常量值)
     * @param expr 要生成四元式的表达式
     * @return 表达式的结果(临时变量名、常量值或变量名)
     */
    String generateExpr(Expr expr) {
        if (expr instanceof NumberExpr n) {
            // 数字字面量直接返回其值
            return Integer.toString(n.value);
        } else if (expr instanceof CharExpr c) {
            // 字符字面量返回带引号的字符
            return "'" + Character.toString(c.value) + "'";
        } else if (expr instanceof VarExpr v) {
            // 变量表达式直接返回变量名
            return v.name;
        } else if (expr instanceof FunctionCallExpr f) {
            // 函数调用表达式生成函数调用的四元式
            return generateFunctionCall(f);
        } else if (expr instanceof ArrayAccessExpr a) {
            // 数组访问表达式生成数组访问的四元式
            return arrayAccess(a.arrayName, a.index);
        } else if (expr instanceof BinaryExpr b) {
            // 二元表达式递归处理左右两边的表达式
            String arg1 = generateExpr(b.left);
            String arg2 = generateExpr(b.right);
            String key = b.op + "," + arg1 + "," + arg2;
            
            // 公共子表达式消除：检查是否已计算过相同的表达式
            if (cseCache.containsKey(key)) {
                return cseCache.get(key);
            }
            
            // 常量折叠：如果两个操作数都是常量，直接计算结果
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
            
            // 生成二元运算的四元式
            String result = newTemp();
            quds.add(new Quadruple(b.op, arg1, arg2, result));
            // 缓存结果用于公共子表达式消除
            cseCache.put(key, result);
            return result;
        }
        return null;
    }
    
    /**
     * 获取变量的类型
     * @param varName 变量名
     * @return 变量的类型
     * @throws RuntimeException 当变量未声明时
     */
    private String getVarType(String varName) {
        // 查找变量声明获取类型
        for (int j = quds.size() - 1; j >= 0; j--) {
            Quadruple q = quds.get(j);
            // 查找参数声明
            if ("param_decl".equals(q.op) && varName.equals(q.result)) {
                return q.arg1;
            }
            // 查找局部变量声明
            if ("var_decl".equals(q.op) && varName.equals(q.result)) {
                return q.arg1.equals("_") ? "int" : q.arg1;
            }
            // 查找数组声明
            if ("ARRAY_DECL".equals(q.op) && varName.equals(q.arg1)) {
                // 向上查找数组类型
                for (int k = j; k >= 0; k--) {
                    Quadruple decl = quds.get(k);
                    if ("var_decl".equals(decl.op) && varName.equals(decl.result)) {
                        return decl.arg1 + "[]"; // 添加数组标记
                    }
                }
            }
        }
        throw new RuntimeException("未声明的变量: " + varName);
    }

    /**
     * 生成变量赋值的四元式(带类型检查)
     * @param var 变量名
     * @param expr 赋值表达式
     * @throws RuntimeException 当赋值类型不匹配时
     */
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
            // 数字字面量赋值
            quds.add(new Quadruple("=", ((NumberExpr) expr).value + "", "_", var));
        } else if (expr instanceof CharExpr) {
            // 字符字面量赋值
            quds.add(new Quadruple("=", "'" + ((CharExpr) expr).value + "'", "_", var));
        } else if (expr instanceof StringExpr) {
            // 字符串字面量赋值
            quds.add(new Quadruple("=", "\"" + ((StringExpr) expr).value + "\"", "_", var));
        } else if (expr instanceof VarExpr) {
            // 变量赋值
            quds.add(new Quadruple("=", ((VarExpr) expr).name, "_", var));
        } else if (expr instanceof FunctionCallExpr) {
            // 函数调用结果赋值
            String result = generateFunctionCall((FunctionCallExpr) expr);
            quds.add(new Quadruple("=", result, "_", var));
        } else if (expr instanceof BinaryExpr) {
            // 二元表达式结果赋值
            String result = generateExpr((BinaryExpr) expr);
            quds.add(new Quadruple("=", result, "_", var));
        }
    }

    /**
     * 显示并返回所有生成的四元式
     * @return 四元式列表
     */
    public List<Quadruple> show() {
        for (var q : quds) {
            System.out.println(q);
        }
        return quds;
    }

    /**
     * 获取所有生成的四元式
     * @return 四元式列表
     */
    public List<Quadruple> getQuadruples() {
        return quds;
    }

    /**
     * 生成函数声明和参数的四元式
     * @param returnType 函数返回类型
     * @param funcName 函数名
     * @param paramNames 参数名称列表
     * @param paramTypes 参数类型列表
     * @throws RuntimeException 当函数名冲突或参数数量不匹配时
     */
    public void emitFuncParam(String returnType, String funcName,
                              List<String> paramNames, List<String> paramTypes) {

        // 初始化函数作用域的局部变量集合
        scopedDeclaredVariables.put(funcName, new HashSet<>());

        // 检查函数名是否与变量名冲突
        if (declaredVariables.contains(funcName)) {
            throw new RuntimeException("函数名 '" + funcName + "' 与已声明变量冲突");
        }

        // 检查函数是否已经声明
        if (functionTable.containsKey(funcName)) {
            throw new RuntimeException("函数 '" + funcName + "' 重复声明");
        }

        // 参数数量检查
        if (paramTypes.size() != paramNames.size()) {
            throw new RuntimeException("函数 '" + funcName + "' 的参数类型数量与参数名数量不匹配");
        }

        // 检查参数名重复
        Set<String> paramSet = new HashSet<>();
        for (String param : paramNames) {
            if (!paramSet.add(param)) {
                throw new RuntimeException("函数 '" + funcName + "' 中参数名 '" + param + "' 重复");
            }
        }

        // 初始化返回状态跟踪
        if (!"void".equals(returnType)) {
            functionHasReturn.put(funcName, false);
        }

        // 设置当前函数上下文
        currentFunction = funcName;

        // 存储函数签名
        functionTable.put(funcName, new FunctionSignature(returnType, paramTypes, paramNames));

        // 生成函数定义四元式
        quds.add(new Quadruple("FuncDef", returnType, String.valueOf(paramNames.size()), funcName));

        // 为每个参数生成带正确类型的声明四元式
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramType = paramTypes.get(i);
            quds.add(new Quadruple("param_decl", paramType, "_", paramName));
            scopedDeclaredVariables.get(funcName).add(paramNames.get(i));
        }
    }
}
