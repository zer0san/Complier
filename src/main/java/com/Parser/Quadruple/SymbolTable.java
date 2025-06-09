package com.Parser.Quadruple;

import java.util.*;

/**
 * 符号表类
 * 用于存储和管理程序中的符号信息（变量、数组、函数等）
 */
public class SymbolTable {
    // 符号表主体，存储所有符号的基本信息
    private final Map<String, SymbolInfo> symbols = new HashMap<>();

    // 数组表，存储数组信息：数组名 -> [类型, 大小]
    private final Map<String, String[]> arrayTable = new HashMap<>();

    // 长度表，存储各种符号的长度：符号名 -> 长度
    private final Map<String, Integer> lengthTable = new HashMap<>();

    // 作用域表，存储符号的作用域信息：符号名 -> 作用域
    private final Map<String, String> scopeTable = new HashMap<>();

    // 当前处理的函数名（作用域）
    private String currentScope = "global";

    // 存储变量的类型信息：变量名 -> 类型
    private final Map<String, String> typeTable = new HashMap<>();

    /**
     * 符号信息内部类，存储单个符号的所有相关信息
     */
    private static class SymbolInfo {
        String name;          // 符号名称
        String type;          // 符号类型（int, char, string等）
        String kind;          // 符号种类（变量、数组、函数等）
        String scope;         // 作用域
        int size;             // 大小/长度（对数组有效）

        public SymbolInfo(String name, String type, String kind, String scope) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.scope = scope;
            this.size = 0;    // 默认大小为0
        }

        public SymbolInfo(String name, String type, String kind, String scope, int size) {
            this(name, type, kind, scope);
            this.size = size;
        }

        @Override
        public String toString() {
            if ("array".equals(kind)) {
                return String.format("%-15s | %-10s | %-10s | %-10s | %d", name, type, kind, scope, size);
            }
            return String.format("%-15s | %-10s | %-10s | %-10s", name, type, kind, scope);
        }
    }

    /**
     * 从四元式列表构建符号表
     * @param quadruples 四元式列表
     */
    public void buildFromQuadruples(List<Quadruple> quadruples) {
        // 存储函数参数信息：函数名 -> [参数名列表]
        Map<String, List<String>> functionParams = new HashMap<>();

        // 第一遍：收集所有类型信息和函数定义
        for (Quadruple q : quadruples) {
            if ("var_decl".equals(q.op) && !q.arg1.equals("_")) {
                typeTable.put(q.result, q.arg1);
            } else if ("param_decl".equals(q.op) && !q.arg1.equals("_")) {
                typeTable.put(q.result, q.arg1);
                // 记录参数归属的函数
                functionParams.computeIfAbsent(currentScope, k -> new ArrayList<>())
                        .add(q.result);
            } else if ("FuncDef".equals(q.op)) {
                typeTable.put(q.result, q.arg1);  // 存储函数返回类型
            } else if ("FuncStart".equals(q.op)) {
                currentScope = q.result;
            } else if ("FuncEnd".equals(q.op)) {
                currentScope = "global";
            }
        }

        // 重置当前作用域，准备第二遍处理
        currentScope = "global";

        // 第二遍：构建符号表
        for (Quadruple q : quadruples) {
            switch (q.op) {
                case "FuncStart":
                    // 函数开始
                    currentScope = q.result;
                    break;

                case "FuncEnd":
                    // 函数结束，回到全局作用域
                    currentScope = "global";
                    break;

                case "FuncDef":
                    // 函数定义：FuncDef, returnType, paramCount, funcName
                    addSymbol(q.result, q.arg1, "function", "global");
                    break;

                case "param_decl":
                    // 增强参数类型处理
                    String paramType;
                    if (!q.arg1.equals("_")) {
                        paramType = q.arg1;
                    } else if (typeTable.containsKey(q.result)) {
                        paramType = typeTable.get(q.result);
                    } else {
                        paramType = inferParameterType(q.result, quadruples, currentScope);
                    }
                    addSymbol(q.result, paramType, "parameter", currentScope);
                    break;

                case "var_decl":
                    // 变量声明：var_decl, type, _, varName
                    String varType = !q.arg1.equals("_") ? q.arg1 : inferType(q.result, quadruples);
                    addSymbol(q.result, varType, "variable", currentScope);
                    break;

                case "ARRAY_DECL":
                    // 数组声明：ARRAY_DECL, arrayName, size, _
                    try {
                        int size = Integer.parseInt(q.arg2);
                        // 查找数组类型
                        String arrayType = inferType(q.arg1, quadruples);
                        addArray(q.arg1, arrayType, size, currentScope);
                    } catch (NumberFormatException e) {
                        // 处理大小不是整数的情况
                        String arrayType = inferType(q.arg1, quadruples);
                        addArray(q.arg1, arrayType, 0, currentScope);
                    }
                    break;
            }
        }
    }

    /**
     * 专门用于推断函数参数类型的方法
     */
    private String inferParameterType(String paramName, List<Quadruple> quadruples, String funcName) {
        // 首先检查是否在类型表中有记录
        if (typeTable.containsKey(paramName)) {
            return typeTable.get(paramName);
        }

        // 查找参数在函数中的使用情况来推断类型
        for (Quadruple q : quadruples) {
            // 查找该参数在函数内的赋值和使用情况
            if (currentScope.equals(funcName)) {
                // 如果参数作为左值出现在赋值中
                if ("=".equals(q.op) && paramName.equals(q.result)) {
                    // 推断类型逻辑...与inferType类似
                }

                // 检查参数是否参与了特定类型的运算
                if (Arrays.asList("+", "-", "*", "/").contains(q.op)) {
                    if (paramName.equals(q.arg1) || paramName.equals(q.arg2)) {
                        return "int"; // 算术运算通常表示数值类型
                    }
                }
            }
        }

        // 如果无法推断，默认为int
        return "int";
    }

    /**
     * 从上下文推断符号类型
     * @param name 符号名
     * @param quadruples 四元式列表
     * @return 推断出的类型，无法推断时返回默认类型 "int"
     */
    private String inferType(String name, List<Quadruple> quadruples) {
        // 首先检查typeTable中是否已有该符号的类型信息
        if (typeTable.containsKey(name)) {
            return typeTable.get(name);
        }

        // 尝试从赋值语句推断类型
        for (Quadruple q : quadruples) {
            if ("=".equals(q.op) && name.equals(q.result)) {
                // 如果右侧是变量或临时变量，获取其类型
                if (typeTable.containsKey(q.arg1)) {
                    return typeTable.get(q.arg1);
                }
                // 如果右侧是字符字面量
                if (q.arg1 != null && q.arg1.startsWith("'") && q.arg1.endsWith("'")) {
                    return "char";
                }
                // 如果右侧是字符串字面量
                if (q.arg1 != null && q.arg1.startsWith("\"") && q.arg1.endsWith("\"")) {
                    return "string";
                }
                // 如果右侧是数字
                if (q.arg1 != null && q.arg1.matches("-?\\d+")) {
                    return "int";
                }
            }
        }

        // 如果还是无法推断，返回默认类型
        return "int";
    }

    /**
     * 添加普通符号到符号表
     */
    private void addSymbol(String name, String type, String kind, String scope) {
        if (name == null || name.isEmpty() || name.equals("_"))
            return;

        SymbolInfo info = new SymbolInfo(name, type, kind, scope);
        symbols.put(name, info);
        scopeTable.put(name, scope);
        typeTable.put(name, type);
    }

    /**
     * 添加数组到符号表和数组表
     */
    private void addArray(String name, String type, int size, String scope) {
        if (name == null || name.isEmpty())
            return;

        SymbolInfo info = new SymbolInfo(name, type, "array", scope, size);
        symbols.put(name, info);
        arrayTable.put(name, new String[]{type, String.valueOf(size)});
        lengthTable.put(name, size);
        scopeTable.put(name, scope);
        typeTable.put(name, type);
    }

    /**
     * 获取符号的类型
     */
    public String getType(String name) {
        return symbols.containsKey(name) ? symbols.get(name).type : null;
    }

    /**
     * 获取数组的大小
     */
    public int getArraySize(String name) {
        return lengthTable.getOrDefault(name, 0);
    }

    /**
     * 获取符号的作用域
     */
    public String getScope(String name) {
        return scopeTable.getOrDefault(name, "unknown");
    }

    /**
     * 判断符号是否为数组
     */
    public boolean isArray(String name) {
        return arrayTable.containsKey(name);
    }

    /**
     * 生成符号表的字符串表示
     */
    public String printSymbolTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("符号表:\n");
        sb.append(String.format("%-15s | %-10s | %-10s | %-10s | %s\n",
                "名称", "类型", "种类", "作用域", "大小(数组)"));
        sb.append("--------------------------------------------------------------------\n");

        // 按照作用域分组排序
        Map<String, List<SymbolInfo>> scopeGroups = new HashMap<>();

        for (SymbolInfo info : symbols.values()) {
            scopeGroups.computeIfAbsent(info.scope, k -> new ArrayList<>()).add(info);
        }

        // 先输出全局符号
        if (scopeGroups.containsKey("global")) {
            sb.append("全局符号:\n");
            for (SymbolInfo info : scopeGroups.get("global")) {
                sb.append(info).append("\n");
            }
            sb.append("\n");
        }

        // 然后输出各个函数作用域的符号
        for (String scope : scopeGroups.keySet()) {
            if (!"global".equals(scope)) {
                sb.append("函数 '").append(scope).append("' 的符号:\n");
                for (SymbolInfo info : scopeGroups.get(scope)) {
                    sb.append(info).append("\n");
                }
                sb.append("\n");
            }
        }

        // 特别输出数组表
        if (!arrayTable.isEmpty()) {
            sb.append("数组表:\n");
            sb.append(String.format("%-15s | %-10s | %-10s\n", "数组名", "元素类型", "大小"));
            sb.append("------------------------------------------\n");
            for (Map.Entry<String, String[]> entry : arrayTable.entrySet()) {
                sb.append(String.format("%-15s | %-10s | %-10s\n",
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
            }
        }

        return sb.toString();
    }
}