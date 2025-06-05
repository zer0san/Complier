package com.Parser.Quadruple;

import java.util.*;
import java.io.*;

import static java.lang.String.format;

public class AssemblyGenerator {
    private StringBuilder assemblyCode = new StringBuilder();
    private Map<String, Integer> memoryLocations = new HashMap<>();
    private int currentMemory = 100; // Start memory allocation at 100
    private int tempCounter = 0;
    private String currentFunction = "_main";

    public AssemblyGenerator() {
        // Initialize data segment
        assemblyCode.append(".MODEL SMALL\n");
        assemblyCode.append(".DATA\n");
        assemblyCode.append("\n");

        // Initialize code segment
        assemblyCode.append(".CODE\n");
        assemblyCode.append("MAIN PROC\n");
        assemblyCode.append("    MOV AX, @DATA\n");
        assemblyCode.append("    MOV DS, AX\n");
        assemblyCode.append("\n");
    }

    public void generateAssembly(List<Quadruple> quadruples) {
        for (Quadruple q : quadruples) {
            switch (q.op) {
                case "=":
                    generateAssignment(q);
                    break;
                case "+", "-", "*", "/":
                    generateArithmetic(q);
                    break;
                case "if":
                    generateConditional(q);
                    break;
                case "goto":
                    generateGoto(q);
                    break;
                case "label":
                    generateLabel(q);
                    break;
                case "el", "ie", "we", "wh":
                    generateControlLabel(q);
                    break;
                case "FuncStart":
                    generateFunctionStart(q);
                    break;
                case "FuncEnd":
                    generateFunctionEnd(q);
                    break;
                case "ARRAY_DECL":
                    generateArrayDeclaration(q);
                    break;
                default:
                    throw new RuntimeException("Unsupported operation: " + q.op);
            }
        }

        // Add function return and program end
        assemblyCode.append("    MOV AX, 4C00H\n");
        assemblyCode.append("    INT 21H\n");
        assemblyCode.append("MAIN ENDP\n");
        assemblyCode.append("END MAIN\n");
    }

    private void generateAssignment(Quadruple q) {
        if (q.result.startsWith("t")) {
            // Temporary variable assignment
            String temp = q.result;
            if (!memoryLocations.containsKey(temp)) {
                memoryLocations.put(temp, currentMemory);
                currentMemory += 2;
            }
            assemblyCode.append(format("    MOV AX, %s\n", q.arg1));
            assemblyCode.append(format("    MOV [SI+%d], AX\n", memoryLocations.get(temp)));
        } else {
            // Variable assignment
            if (!memoryLocations.containsKey(q.result)) {
                memoryLocations.put(q.result, currentMemory);
                currentMemory += 2;
            }
            assemblyCode.append(format("    MOV AX, %s\n", q.arg1));
            assemblyCode.append(format("    MOV [SI+%d], AX\n", memoryLocations.get(q.result)));
        }
    }

    private void generateArithmetic(Quadruple q) {
        String temp = q.result;
        if (!memoryLocations.containsKey(temp)) {
            memoryLocations.put(temp, currentMemory);
            currentMemory += 2;
        }

        assemblyCode.append(format("    MOV AX, %s\n", q.arg1));
        switch (q.op) {
            case "+":
                assemblyCode.append(format("    ADD AX, %s\n", q.arg2));
                break;
            case "-":
                assemblyCode.append(format("    SUB AX, %s\n", q.arg2));
                break;
            case "*":
                assemblyCode.append(format("    MUL %s\n", q.arg2));
                break;
            case "/":
                assemblyCode.append(format("    MOV BX, %s\n", q.arg2));
                assemblyCode.append("    MOV DX, 0\n");
                assemblyCode.append("    DIV BX\n");
                break;
        }
        assemblyCode.append(format("    MOV [SI+%d], AX\n", memoryLocations.get(temp)));
    }

    private void generateConditional(Quadruple q) {
        assemblyCode.append(format("    CMP AX, %s\n", q.arg2));
        assemblyCode.append(format("    JNE %s\n", q.result));
    }

    private void generateGoto(Quadruple q) {
        assemblyCode.append(format("    JMP %s\n", q.result));
    }

    private void generateLabel(Quadruple q) {
        assemblyCode.append(format("%s:\n", q.result));
    }

    private void generateControlLabel(Quadruple q) {
        assemblyCode.append(format("%s_%d:\n", q.op, tempCounter++));
    }

    private void generateFunctionStart(Quadruple q) {
        currentFunction = q.result;
        assemblyCode.append(format("%s PROC\n", currentFunction.toUpperCase()));
    }

    private void generateFunctionEnd(Quadruple q) {
        assemblyCode.append("    RET\n");
        assemblyCode.append(format("%s ENDP\n", currentFunction.toUpperCase()));
    }

    private void generateArrayDeclaration(Quadruple q) {
        int size = Integer.parseInt(q.arg2);
        assemblyCode.append(format("%s DW %d DUP(?)\n", q.arg1, size));
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
