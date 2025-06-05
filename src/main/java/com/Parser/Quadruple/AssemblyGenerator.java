package com.Parser.Quadruple;

import java.util.*;
import static java.lang.String.format;

public class AssemblyGenerator {
    private final StringBuilder assemblyCode = new StringBuilder();
    private final Set<String> declaredVariables = new HashSet<>();
    private final List<String> dataSegmentDeclarations = new ArrayList<>();
    private int tempCounter = 0;
    private String currentFunction = "MAIN";
    private final Set<String> functionNames = new HashSet<>();

    public AssemblyGenerator() {
        // 初始化模型和段
        assemblyCode.append(".MODEL SMALL\n");
        assemblyCode.append(".STACK 100h\n");
        assemblyCode.append(".DATA\n");
        // .CODE 段稍后插入
    }

    public void generateAssembly(List<Quadruple> quadruples) {
        // 收集函数名（用于避免函数名当作变量）
        for (Quadruple q : quadruples) {
            if ("FuncStart".equals(q.op) && q.result != null) {
                functionNames.add(q.result);
            }
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
                case "call" -> {
                    // 保存现场
                    assemblyCode.append("    PUSH AX\n");
                    assemblyCode.append("    PUSH BX\n");
                    assemblyCode.append("    PUSH CX\n");
                    assemblyCode.append("    PUSH DX\n");

                    // 调用函数
                    assemblyCode.append(format("    CALL %s\n", q.arg1.toUpperCase()));

                    // 恢复现场
                    assemblyCode.append("    POP DX\n");
                    assemblyCode.append("    POP CX\n");
                    assemblyCode.append("    POP BX\n");
                    assemblyCode.append("    POP AX\n");

                    // 如果有返回值，保存到目标变量
                    if (!q.result.equals("_")) {
                        assemblyCode.append(format("    MOV %s, AX\n", q.result));
                    }
                }
                case "return" -> generateReturnStatement(q);
                default -> throw new RuntimeException("Unsupported operation: " + q.op);
            }
        }

        // 程序结束
        assemblyCode.append("END _start\n");
    }
    public void generateReturnStatement(Quadruple q) {
        if (!q.arg1.equals("_")) {
            // 有返回值
            assemblyCode.append(format("    MOV AX, %s\n", toOperand(q.arg1)));
        }
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
    }

    private void generateFunctionEnd(Quadruple q) {
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
