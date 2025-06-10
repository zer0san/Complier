package com.Parser.Quadruple;

import lombok.Getter;

import java.util.*;
import static java.lang.String.format;

public class AssemblyGenerator {
    private final StringBuilder assemblyCode = new StringBuilder();
    private final Set<String> declaredVariables = new HashSet<>();
    private final List<String> dataSegmentDeclarations = new ArrayList<>();
    private int tempCounter = 0;
    private String currentFunction = "MAIN";
    private final Set<String> functionNames = new HashSet<>();
    @Getter
    private SymbolTable symbolTable = new SymbolTable();
    public AssemblyGenerator() {
        // 初始化模型和段
        assemblyCode.append(".MODEL SMALL\n");
        assemblyCode.append(".STACK 100h\n");
        assemblyCode.append(".DATA\n");
        // .CODE 段稍后插入
    }

    public void generateAssembly(List<Quadruple> quadruples) {
        // 先构建符号表
        symbolTable.buildFromQuadruples(quadruples);

        // 输出符号表内容
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

        // 收集变量声明
        for (Quadruple q : quadruples) {
            collectVariable(q.arg1);
            collectVariable(q.arg2);
            collectVariable(q.result);
        }

        // 添加变量声明
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
        // 生成中间代码对应汇编
        for (Quadruple q : quadruples) {
            switch (q.op) {
                case "=" -> generateAssignment(q);
                case "+", "-", "*", "/" -> generateArithmetic(q);
                case "if" -> generateConditional(q);
                case "goto" -> generateGoto(q);
                case "label" -> generateLabel(q);
                case "el", "ie", "we", "wh" -> generateControlLabel(q);
                case "FuncStart" -> generateFunctionStart(q);
                case "FuncEnd" -> generateFunctionEnd(q);
                case "ARRAY_DECL" -> generateArrayDeclaration(q);
                case "param_decl" -> generateParamDeclaration(q);
                case "param" -> generateParamPassing(q);
                case "FuncDef" -> generateFunctionDefinition(q);
                case "call" -> generateFunctionCall(q);
                case "return" -> generateReturnStatement(q);
                case "var_decl" -> generateVariableDeclaration(q);
                case "==", "!=", "<", "<=", ">", ">=" -> {
                    // 处理比较操作
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

    private void collectVariable(String name) {
        if (name == null || name.isEmpty()) return;

        if (Character.isLetter(name.charAt(0))
                && !declaredVariables.contains(name)
                && !isReserved(name)
                && !functionNames.contains(name)) {
            declaredVariables.add(name);
        }
    }

    private boolean isReserved(String name) {
        return switch (name.toUpperCase()) {
            case "AX", "BX", "CX", "DX" -> true;
            default -> false;
        };
    }

    private void generateAssignment(Quadruple q) {
        assemblyCode.append(format("    MOV AX, %s\n", toOperand(q.arg1)));
        assemblyCode.append(format("    MOV %s, AX\n", q.result));
    }

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
                assemblyCode.append("    CWD\n");
                assemblyCode.append(format("    MOV BX, %s\n", op2));
                assemblyCode.append("    DIV BX\n");
            }
        }
        assemblyCode.append(format("    MOV %s, AX\n", q.result));
    }

    private void generateConditional(Quadruple q) {
        String op1 = toOperand(q.arg1);
        String op2 = toOperand(q.arg2);
        assemblyCode.append(format("    MOV AX, %s\n", op1));
        assemblyCode.append(format("    CMP AX, %s\n", op2));
        assemblyCode.append(format("    JNE %s\n", q.result));
    }

    private void generateGoto(Quadruple q) {
        assemblyCode.append(format("    JMP %s\n", q.result));
    }

    private void generateLabel(Quadruple q) {
        assemblyCode.append(format("%s:\n", q.result));
    }

    private void generateControlLabel(Quadruple q) {
        assemblyCode.append(format("%s_%d:\n", q.op.toUpperCase(), tempCounter++));
    }

    private void generateFunctionStart(Quadruple q) {
        currentFunction = q.result.toLowerCase();
        functionNames.add(currentFunction);
        assemblyCode.append(format("%s PROC\n", currentFunction));

        // 保存基指针并建立新栈帧
        assemblyCode.append("    PUSH BP\n");
        assemblyCode.append("    MOV BP, SP\n");

        // 为局部变量预留栈空间
        assemblyCode.append("    SUB SP, 20h\n");  // 可根据函数内局部变量数量调整
    }

    private void generateFunctionEnd(Quadruple q) {
        // 恢复栈指针和基指针
        assemblyCode.append("    MOV SP, BP\n");
        assemblyCode.append("    POP BP\n");
        assemblyCode.append("    RET\n");
        assemblyCode.append(format("%s ENDP\n\n", currentFunction));
    }

    private void generateArrayDeclaration(Quadruple q) {
        int size = Integer.parseInt(q.arg2);
        if (!declaredVariables.contains(q.arg1)) {
            declaredVariables.add(q.arg1);
            dataSegmentDeclarations.add(format("    %s DW %d DUP(?)\n", q.arg1, size));
        }
    }

    private void generateParamDeclaration(Quadruple q) {
        // 参数声明在汇编中可以作为局部变量处理
        // 形式为: param_decl, type, _, paramName
        if (!declaredVariables.contains(q.result)) {
            declaredVariables.add(q.result);
        }
    }

    private void generateParamPassing(Quadruple q) {
        // 从右向左传递参数（符合cdecl约定）
        assemblyCode.append(format("    MOV AX, %s\n", toOperand(q.arg1)));
        assemblyCode.append("    PUSH AX\n");
    }

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

    private void generateFunctionCall(Quadruple q) {
        // 保存现场
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

        // 处理返回值
        if (!q.result.equals("_")) {
            assemblyCode.append(format("    MOV %s, AX\n", q.result));
        }
    }

    private void generateVariableDeclaration(Quadruple q) {
        // 处理变量声明：var_decl, type, _, varName
        if (!declaredVariables.contains(q.result)) {
            declaredVariables.add(q.result);
        }
    }

    private String toOperand(String value) {
        if (value == null) return "0";
        if (value.matches("-?\\d+")) return value;
        return value;
    }

    public String getAssemblyCode() {
        return assemblyCode.toString();
    }

    public String show() {
        for (String line : assemblyCode.toString().split("\n")) {
            System.out.println(line);
        }
        return assemblyCode.toString();
    }
}
