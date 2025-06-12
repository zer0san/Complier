package com.Parser.Quadruple;

import lombok.Getter;

import java.util.*;
import static java.lang.String.format;

/**
 * AssemblyGenerator 类用于将四元式中间代码转换为 x86 汇编代码（16位）。
 * 支持变量声明、算术运算、条件跳转、函数调用、数组声明等。
 */
public class AssemblyGenerator {
    // 汇编代码字符串构建器
    private final StringBuilder assemblyCode = new StringBuilder();
    // 已声明的变量集合，避免重复声明
    private final Set<String> declaredVariables = new HashSet<>();
    // 数据段声明列表
    private final List<String> dataSegmentDeclarations = new ArrayList<>();
    // 控制标签生成的临时计数器
    private int tempCounter = 0;
    // 当前处理的函数名
    private String currentFunction = "MAIN";
    // 所有函数名集合（用于区分变量和函数）
    private final Set<String> functionNames = new HashSet<>();
    // 符号表，用于记录变量、函数等信息
    @Getter
    private SymbolTable symbolTable = new SymbolTable();

    /**
     * 构造函数，初始化汇编文件的头部（模型、堆栈、数据段）。
     */
    public AssemblyGenerator() {
        // 初始化模型和段
        assemblyCode.append(".MODEL SMALL\n");
        assemblyCode.append(".STACK 100h\n");
        assemblyCode.append(".DATA\n");
        // .CODE 段稍后插入
    }

    /**
     * 主方法：根据四元式列表生成汇编代码。
     * @param quadruples 四元式列表
     */
    public void generateAssembly(List<Quadruple> quadruples) {
        // 先构建符号表
        symbolTable.buildFromQuadruples(quadruples);

        // 输出符号表内容（调试用）
        System.out.println(symbolTable.printSymbolTable());

        // 收集函数名（用于避免函数名当作变量）
        for (Quadruple q : quadruples) {
            if ("FuncStart".equals(q.op) && q.result != null) {
                functionNames.add(q.result);
            }
        }

        // 检查是否存在main函数
        if(!functionNames.contains("main")){
            throw new RuntimeException("Error: required main function");
        }

        // 收集变量声明（包括参数、数组等）
        for (Quadruple q : quadruples) {
            collectVariable(q.arg1);
            collectVariable(q.arg2);
            collectVariable(q.result);
        }

        // 添加变量声明到数据段
        for (String var : declaredVariables) {
            dataSegmentDeclarations.add(format("    %s DW ?\n", var));
        }
        for (String decl : dataSegmentDeclarations) {
            assemblyCode.append(decl);
        }

        // 代码段开始，使用 _start 作为入口
        assemblyCode.append(".CODE\n");
        assemblyCode.append("_start:\n");
        assemblyCode.append("    MOV AX, @DATA\n");
        assemblyCode.append("    MOV DS, AX\n");
        assemblyCode.append("    CALL main\n");
        assemblyCode.append("    MOV AX, 4C00H\n");
        assemblyCode.append("    INT 21H\n\n");

        // 遍历四元式，生成对应的汇编代码
        for (Quadruple q : quadruples) {
            switch (q.op) {
                case "=" -> generateAssignment(q); // 赋值
                case "+", "-", "*", "/" -> generateArithmetic(q); // 算术运算
                case "if" -> generateConditional(q); // 条件跳转
                case "goto" -> generateGoto(q); // 无条件跳转
                case "label" -> generateLabel(q); // 标签
                case "el", "ie", "we", "wh" -> generateControlLabel(q); // 控制流标签
                case "FuncStart" -> generateFunctionStart(q); // 函数开始
                case "FuncEnd" -> generateFunctionEnd(q); // 函数结束
                case "ARRAY_DECL" -> generateArrayDeclaration(q); // 数组声明
                case "param_decl" -> generateParamDeclaration(q); // 参数声明
                case "param" -> generateParamPassing(q); // 参数传递
                case "FuncDef" -> generateFunctionDefinition(q); // 函数定义
                case "call" -> generateFunctionCall(q); // 函数调用
                case "return" -> generateReturnStatement(q); // 返回语句
                case "var_decl" -> generateVariableDeclaration(q); // 变量声明
                case "==", "!=", "<", "<=", ">", ">=" -> {
                    // 处理比较操作，生成CMP和条件跳转
                    String op1 = toOperand(q.arg1);
                    String op2 = toOperand(q.arg2);
                    assemblyCode.append(format("    MOV AX, %s\n", op1));
                    assemblyCode.append(format("    CMP AX, %s\n", op2));
                    switch (q.op) {
                        case "==" -> assemblyCode.append(format("    JE %s\n", q.result));
                        case "!=" -> assemblyCode.append(format("    JNE %s\n", q.result));
                        case "<" -> assemblyCode.append(format("    JL %s\n", q.result));
                        case "<=" -> assemblyCode.append(format("    JLE %s\n", q.result));
                        case ">" -> assemblyCode.append(format("    JG %s\n", q.result));
                        case ">=" -> assemblyCode.append(format("    JGE %s\n", q.result));
                    }
                }
                default -> throw new RuntimeException("Unsupported operation: " + q.op);
            }
        }

        // 程序结束
        assemblyCode.append("END _start\n");
    }

    /**
     * 生成 return 语句的汇编代码
     * @param q 四元式
     */
    private void generateReturnStatement(Quadruple q) {
        if (!q.arg1.equals("_")) {
            // 有返回值，存入AX
            assemblyCode.append(format("    MOV AX, %s\n", toOperand(q.arg1)));
        }
        // 恢复栈帧并返回
        assemblyCode.append("    MOV SP, BP\n");
        assemblyCode.append("    POP BP\n");
        assemblyCode.append("    RET\n");
    }

    /**
     * 收集变量名，避免重复声明和保留字、函数名冲突
     * @param name 变量名
     */
    private void collectVariable(String name) {
        if (name == null || name.isEmpty()) return;

        if (Character.isLetter(name.charAt(0))
                && !declaredVariables.contains(name)
                && !isReserved(name)
                && !functionNames.contains(name)) {
            declaredVariables.add(name);
        }
    }

    /**
     * 判断是否为汇编保留寄存器名
     * @param name 名称
     * @return 是否为保留名
     */
    private boolean isReserved(String name) {
        return switch (name.toUpperCase()) {
            case "AX", "BX", "CX", "DX" -> true;
            default -> false;
        };
    }

    /**
     * 生成赋值语句的汇编代码
     * @param q 四元式
     */
    private void generateAssignment(Quadruple q) {
        assemblyCode.append(format("    MOV AX, %s\n", toOperand(q.arg1)));
        assemblyCode.append(format("    MOV %s, AX\n", q.result));
    }

    /**
     * 生成算术运算的汇编代码
     * @param q 四元式
     */
    private void generateArithmetic(Quadruple q) {
        String op1 = toOperand(q.arg1);
        String op2 = toOperand(q.arg2);

        assemblyCode.append(format("    MOV AX, %s\n", op1));
        switch (q.op) {
            case "+" -> assemblyCode.append(format("    ADD AX, %s\n", op2));
            case "-" -> assemblyCode.append(format("    SUB AX, %s\n", op2));
            case "*" -> {
                assemblyCode.append(format("    MOV BX, %s\n", op2));
                assemblyCode.append("    MUL BX\n");
            }
            case "/" -> {
                assemblyCode.append("    CWD\n"); // 扩展符号位
                assemblyCode.append(format("    MOV BX, %s\n", op2));
                assemblyCode.append("    DIV BX\n");
            }
        }
        assemblyCode.append(format("    MOV %s, AX\n", q.result));
    }

    /**
     * 生成条件跳转的汇编代码（if）
     * @param q 四元式
     */
    private void generateConditional(Quadruple q) {
        String op1 = toOperand(q.arg1);
        String op2 = toOperand(q.arg2);
        assemblyCode.append(format("    MOV AX, %s\n", op1));
        assemblyCode.append(format("    CMP AX, %s\n", op2));
        assemblyCode.append(format("    JNE %s\n", q.result));
    }

    /**
     * 生成无条件跳转的汇编代码（goto）
     * @param q 四元式
     */
    private void generateGoto(Quadruple q) {
        assemblyCode.append(format("    JMP %s\n", q.result));
    }

    /**
     * 生成标签的汇编代码
     * @param q 四元式
     */
    private void generateLabel(Quadruple q) {
        assemblyCode.append(format("%s:\n", q.result));
    }

    /**
     * 生成控制流标签（如 if-else, while 等）
     * @param q 四元式
     */
    private void generateControlLabel(Quadruple q) {
        assemblyCode.append(format("%s_%d:\n", q.op.toUpperCase(), tempCounter++));
    }

    /**
     * 生成函数开始的汇编代码
     * @param q 四元式
     */
    private void generateFunctionStart(Quadruple q) {
        currentFunction = q.result.toLowerCase();
        functionNames.add(currentFunction);
        assemblyCode.append(format("%s PROC\n", currentFunction));

        // 保存基指针并建立新栈帧
        assemblyCode.append("    PUSH BP\n");
        assemblyCode.append("    MOV BP, SP\n");

        // 为局部变量预留栈空间（此处固定为32字节，可根据需要调整）
        assemblyCode.append("    SUB SP, 20h\n");
    }

    /**
     * 生成函数结束的汇编代码
     * @param q 四元式
     */
    private void generateFunctionEnd(Quadruple q) {
        // 恢复栈指针和基指针
        assemblyCode.append("    MOV SP, BP\n");
        assemblyCode.append("    POP BP\n");
        assemblyCode.append("    RET\n");
        assemblyCode.append(format("%s ENDP\n\n", currentFunction));
    }

    /**
     * 生成数组声明的汇编代码
     * @param q 四元式
     */
    private void generateArrayDeclaration(Quadruple q) {
        int size = Integer.parseInt(q.arg2);
        if (!declaredVariables.contains(q.arg1)) {
            declaredVariables.add(q.arg1);
            dataSegmentDeclarations.add(format("    %s DW %d DUP(?)\n", q.arg1, size));
        }
    }

    /**
     * 生成参数声明的汇编代码
     * @param q 四元式
     */
    private void generateParamDeclaration(Quadruple q) {
        // 参数声明在汇编中可以作为局部变量处理
        // 形式为: param_decl, type, _, paramName
        if (!declaredVariables.contains(q.result)) {
            declaredVariables.add(q.result);
        }
    }

    /**
     * 生成参数传递的汇编代码（调用前将参数压栈）
     * @param q 四元式
     */
    private void generateParamPassing(Quadruple q) {
        // 从右向左传递参数（符合cdecl约定）
        assemblyCode.append(format("    MOV AX, %s\n", toOperand(q.arg1)));
        assemblyCode.append("    PUSH AX\n");
    }

    /**
     * 处理函数定义（记录参数数量等信息，实际汇编中不输出）
     * @param q 四元式
     */
    private void generateFunctionDefinition(Quadruple q) {
        // 函数定义 FuncDef, returnType, paramCount, funcName
        // 在汇编中，这通常只需记录函数名和参数数量
        int paramCount = 0;
        if (!q.arg2.equals("_")) {
            try {
                paramCount = Integer.parseInt(q.arg2);
            } catch (NumberFormatException e) {
                // 处理无法解析为数字的情况
            }
        }
        // 可以存储函数信息以便后续使用
    }

    /**
     * 生成函数调用的汇编代码
     * @param q 四元式
     */
    private void generateFunctionCall(Quadruple q) {
        // 保存现场（保护寄存器）
        assemblyCode.append("    PUSH AX\n");
        assemblyCode.append("    PUSH BX\n");
        assemblyCode.append("    PUSH CX\n");
        assemblyCode.append("    PUSH DX\n");

        // 调用函数
        assemblyCode.append(format("    CALL %s\n", q.arg1.toLowerCase()));

        // 清理参数栈
        int paramCount = 0;
        if (!q.arg2.equals("_")) {
            try {
                paramCount = Integer.parseInt(q.arg2);
                if (paramCount > 0) {
                    assemblyCode.append(format("    ADD SP, %d\n", paramCount * 2));
                }
            } catch (NumberFormatException e) {
                // 处理解析错误
            }
        }

        // 恢复现场
        assemblyCode.append("    POP DX\n");
        assemblyCode.append("    POP CX\n");
        assemblyCode.append("    POP BX\n");
        assemblyCode.append("    POP AX\n");

        // 处理返回值（函数返回值存入AX，赋值给目标变量）
        if (!q.result.equals("_")) {
            assemblyCode.append(format("    MOV %s, AX\n", q.result));
        }
    }

    /**
     * 生成变量声明的汇编代码
     * @param q 四元式
     */
    private void generateVariableDeclaration(Quadruple q) {
        // 处理变量声明：var_decl, type, _, varName
        if (!declaredVariables.contains(q.result)) {
            declaredVariables.add(q.result);
        }
    }

    /**
     * 将四元式参数转换为汇编操作数（区分立即数和变量）
     * @param value 参数
     * @return 汇编操作数
     */
    private String toOperand(String value) {
        if (value == null) return "0";
        if (value.matches("-?\\d+")) return value; // 立即数
        return value; // 变量名
    }

    /**
     * 获取生成的汇编代码字符串
     * @return 汇编代码
     */
    public String getAssemblyCode() {
        return assemblyCode.toString();
    }

    /**
     * 打印并返回汇编代码
     * @return 汇编代码
     */
    public String show() {
        for (String line : assemblyCode.toString().split("\n")) {
            System.out.println(line);
        }
        return assemblyCode.toString();
    }
}
