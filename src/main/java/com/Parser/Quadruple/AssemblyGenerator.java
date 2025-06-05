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
        // 数据段变量稍后插入
        // .CODE 段先占位
    }

    public void generateAssembly(List<Quadruple> quadruples) {
        // 先收集变量声明
        for (Quadruple q : quadruples) {
            if ("FuncStart".equals(q.op)) {
                functionNames.add(q.result); // 提前识别函数名
                continue;
            }
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

        // 代码段开始
        assemblyCode.append(".CODE\n");
        assemblyCode.append("MAIN PROC\n");
        assemblyCode.append("    MOV AX, @DATA\n");
        assemblyCode.append("    MOV DS, AX\n\n");

        // 生成指令
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
                default -> throw new RuntimeException("Unsupported operation: " + q.op);
            }
        }

        // 程序结束
        assemblyCode.append("\n    MOV AX, 4C00H\n");
        assemblyCode.append("    INT 21H\n");
        assemblyCode.append("MAIN ENDP\n");
        assemblyCode.append("END MAIN\n");
    }

    private void collectVariable(String name) {
        if (name == null || name.isEmpty()) return;

        if (Character.isLetter(name.charAt(0))
                && !declaredVariables.contains(name)
                && !isReserved(name)
                && !functionNames.contains(name)) // 👈 不加入函数名
        {
            declaredVariables.add(name);
        }
    }


    private boolean isReserved(String name) {
        return name.equals("AX") || name.equals("BX") || name.equals("CX") || name.equals("DX");
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
        currentFunction = q.result.toUpperCase();
        functionNames.add(q.result); // 👈 添加到函数名集合
        assemblyCode.append(format("%s PROC\n", currentFunction));
    }


    private void generateFunctionEnd(Quadruple q) {
        assemblyCode.append("    RET\n");
        assemblyCode.append(format("%s ENDP\n", currentFunction));
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

    public void show() {
        for (String line : assemblyCode.toString().split("\n")) {
            System.out.println(line);

        }
    }
}
