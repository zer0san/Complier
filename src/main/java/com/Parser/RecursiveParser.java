package com.Parser;

import java.util.*;

import com.Parser.*;
import com.Lexer.*;
import com.Lexer.Token;
import com.Parser.Quadruple.*;
import lombok.Data;
import lombok.Setter;

@Data
public class RecursiveParser {
    private final List<Token> tokens;
    private int pos = 0;
    private int labelId = 0;
    private final QuadrupleGenerator gen = new QuadrupleGenerator();
    @Setter
    String sourceCode = "";

    @Setter // lombok annotation, 生成setter方法
    Map<Token, Integer> bugFinder = new HashMap<>();

    public RecursiveParser(List<Token> tokens) {
        this.tokens = tokens;
    }


    private Token lookahead() {
        if (pos < tokens.size()) {
            return tokens.get(pos);
        }
        return new Token(Token.Type.EOF, "");
    }

    // 检查当前token是否匹配预期的词法单位，并前进到下一个token
    private Token match(String value) {
        Token t = lookahead();
        if (t.value.equals(value)) {
            pos++;
            return t;
        } else {
            // 当源代码存在语法错误，报错
            Integer i = bugFinder.get(t);
            // System.out.println();
            throw new RuntimeException("Expected " + value + ", but found " + t.value);
        }
    }

    private Token match(Token.Type type) {
        Token t = lookahead();
        if (t.type == type) {
            pos++;
            return t;
        } else {
            // 当源代码存在语法错误，报错
            throw new RuntimeException("Expected " + type + ", but found " + t.type);
        }
    }

    private String newLabel() {
        return "L" + (labelId++);
    }
    //return
    private void parseReturnStmt() {
        match("return");

        // 检查是否有返回值，如果分号前有表达式则解析
        Expr returnExpr = null;
        if (!lookahead().value.equals(";")) {
            returnExpr = parseExpr();
        }

        match(";");

        // 使用QuadrupleGenerator中已有的returnStmt方法
        gen.returnStmt(returnExpr);
    }

    // 程序入口
    public void parseProgram() {
        while (lookahead().type != Token.Type.EOF) {
            if (isFuncDeclStart()) {
                parseFuncDecl();
            } else {
                parseStmt();
            }
        }

        parseStmtList();
        System.out.println("Parse Successful!");
    }

    // 判断是否为函数声明起始
    // 即int后面跟标识符，再跟(
    private boolean isFuncDeclStart() {
        String value = lookahead().value;
        if(!value.equals("int") && !value.equals("char") && !value.equals("string")) return false;

        // 尝试看下一个和下下个
        if (pos + 1 < tokens.size() && tokens.get(pos + 1).type == Token.Type.IDENTIFIER) {
            if (pos + 2 < tokens.size() && tokens.get(pos + 2).value.equals("(")) {
                return true;
            }
        }
        return false;
    }

    // 处理函数
    private void parseFuncDecl() {
        String returnType = match(lookahead().value).value;
        String funcName = match(Token.Type.IDENTIFIER).value;
        match("(");
        List<String[]> paramsWithTypes = parseParamList();
        match(")");

        List<String> paramNames = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        for (String[] param : paramsWithTypes) {
            paramTypes.add(param[0]);
            paramNames.add(param[1]);
        }

        gen.emitFuncLabel(funcName);
        gen.emitFuncParam(returnType, funcName, paramNames);
        parseBlock();
        gen.emitFuncEnd(funcName);
    }

    // 参数列表解析，支持int a或者空
    private List<String[]> parseParamList() {
        List<String[]> paramsWithTypes = new ArrayList<>();

        if (lookahead().value.equals(")")) {
            return paramsWithTypes;
        }

        do {
            String type = match(lookahead().value).value; // 获取参数类型
            String paramName = match(Token.Type.IDENTIFIER).value;
            paramsWithTypes.add(new String[]{type, paramName});

            if(lookahead().value.equals(",")) {
                match(",");
            } else {
                break;
            }
        } while(true);

        return paramsWithTypes;
    }


    // 处理语句
    // 不断调用parseStmt，直到遇到右大括号或文件结尾
    private void parseStmtList() {
        while (!lookahead().value.equals("}") && lookahead().type != Token.Type.EOF) {
            parseStmt();
        }
    }

    // 在 RecursiveParser 类中添加这个方法
    private Expr parseFunctionCall(String funcName) {
        List<Expr> arguments = new ArrayList<>();
        match("(");

        // 解析参数列表
        if (!lookahead().value.equals(")")) {
            do {
                Expr arg = parseExpr();  // 解析参数表达式
                arguments.add(arg);      // 添加到参数列表

                // 检查是否还有更多参数
                if (lookahead().value.equals(",")) {
                    match(",");
                } else {
                    break;
                }
            } while (true);
        }

        match(")");
        return new FunctionCallExpr(funcName, arguments);
    }
    // 识别语句是哪种类型
    private void parseStmt() {
        Token t = lookahead();
        // 声明语句
        if (t.value.equals("int") || t.value.equals("char") || t.value.equals("string")) {
            parseDeclStmt();
        }
        // return语句
        else if (t.value.equals("return")) {
            parseReturnStmt();
        }
        // 赋值语句或函数调用
        else if (t.type == Token.Type.IDENTIFIER) {
            String id = match(Token.Type.IDENTIFIER).value;
            if (lookahead().value.equals("(")) {
                // 函数调用
                Expr funcCall = parseFunctionCall(id);
                match(";");
                gen.generateFunctionCall((FunctionCallExpr)funcCall);
            } else if (lookahead().value.equals("=")) {
                // 赋值语句
                match("=");
                Expr expr = parseExpr();
                gen.assign(id, expr);
                match(";");
            }
        }
        // 代码块
        else if (t.value.equals("{")) {
            parseBlock();
        } else if (t.value.equals("if")) {
            parseIfStmt();
        } else if (t.value.equals("while")) {
            parseWhileStmt();
        } else {
            // 当源代码存在语法错误，报错
            throw new RuntimeException("Expected DeclStmt, AssignStmt or Block, but found " + t.value);
        }
    }


    private void parseIfStmt() {
        match("if");
        match("(");
        Condition cond = parseCondition();
        match(")");

        String labelElse = newLabel();
        String labelEnd = newLabel();

        gen.ifFalse(cond, labelElse);
        parseStmt(); // then 语句

        if (lookahead().value.equals("else")) {
            gen.gotoLabel(labelEnd); // then 后跳过else
            gen.emitElLabel();
            gen.emitLabel(labelElse);
            match("else");
            parseStmt();  // 处理else内容
            gen.emitLabel(labelEnd);
        } else {
            gen.emitLabel(labelElse); // 没有else，else标签直接作为结束
        }
        gen.emitIeLabel();
    }

    private void parseWhileStmt() {
        match("while");
        String labelStart = newLabel();
        String labelEnd = newLabel();

        gen.emitWhLabel();

        gen.emitLabel(labelStart);

        match("(");
        Condition cond = parseCondition();
        match(")");

        gen.ifFalse(cond, labelEnd);
        parseStmt();
        gen.gotoLabel(labelStart);
        gen.emitLabel(labelEnd);

        gen.emitWeLabel();
    }

    private Condition parseCondition() {
        Expr left = parseExpr();
        String op = match(lookahead().value).value;
        Expr right = parseExpr();
        return new Condition(op, left, right);
    }

    private void parseDeclStmt() {
//        match("int");
        String type = match(lookahead().value).value; // 获取int或char
        String varName = match(Token.Type.IDENTIFIER).value;
        // 使用QuadrupleGenerator的方法记录变量类型信息
        gen.declareVariable(type, varName);
        // Check if it's an array declaration: int a[10];
        if (lookahead().value.equals("[")) {
            match("[");
            String size = match(Token.Type.NUMBER).value;
            match("]");
            gen.declareArray(varName, Integer.parseInt(size));
        }
        // Regular variable declaration: int a;

        match(";");
    }

    private void parseAssignStmt() {
        String var = match(Token.Type.IDENTIFIER).value;

        // Check if it's array assignment: a[i] = expr;
        if (lookahead().value.equals("[")) {
            match("[");
            Expr indexExpr = parseExpr();
            match("]");
            match("=");
            Expr valueExpr = parseExpr();
            gen.assignArray(var, indexExpr, valueExpr);
        } else {
            // Regular variable assignment: a = expr;
            match("=");
            Expr expr = parseExpr();
            gen.assign(var, expr);
        }

        match(";");
    }

    private void parseBlock() {
        match("{");
        parseStmtList();
        match("}");
    }

    // 处理加减表达式
    private Expr parseExpr() {
        Expr left = parseTerm();
        while (lookahead().value.equals("+") || lookahead().value.equals("-")) {
            String op = match(lookahead().value).value;
            Expr right = parseTerm();
            left = new BinaryExpr(op, left, right);
        }
        return left;
    }



    // 处理乘除表达式
    private Expr parseTerm() {
        Expr left = parseFactor();
        while (lookahead().value.equals("*") || lookahead().value.equals("/")) {
            String op = match(lookahead().value).value;
            Expr right = parseFactor();
            left = new BinaryExpr(op, left, right);
        }
        return left;
    }

    // 处理变量、数字、括号表达式、数组访问
    private Expr parseFactor() {
        Token t = lookahead();
        if (t.type == Token.Type.IDENTIFIER) {
            String varName = match(Token.Type.IDENTIFIER).value;
            // 检查是否是函数调用
            if (lookahead().value.equals("(")) {
                return parseFunctionCall(varName);
            }
            // Check if it's array access: a[i]
            else if (lookahead().value.equals("[")) {
                match("[");
                Expr indexExpr = parseExpr();
                match("]");
                return new ArrayAccessExpr(varName, indexExpr);
            } else {
                // Regular variable
                return new VarExpr(varName);
            }
        } else if (t.type == Token.Type.NUMBER) {
            return new NumberExpr(Integer.parseInt(match(Token.Type.NUMBER).value));
        } else if (t.type == Token.Type.CHAR_LITERAL) {
            return new CharExpr(match(Token.Type.CHAR_LITERAL).value.charAt(0));
        } else if (t.type == Token.Type.STRING_LITERAL) {
            return new StringExpr(match(Token.Type.STRING_LITERAL).value);
        } else if (t.value.equals("(")) {
            match("(");
            Expr expr = parseExpr();
            match(")");
            return expr;
        } else {
            // 当源代码存在语法错误，报错
            throw new RuntimeException("Expected Identifier, Number, Character or String, but found " + t.value);
        }
    }

    // 输出四元式
    public List<Quadruple> show() {
        System.out.println("生成的四元式");
        gen.show();
        return gen.getQuadruples();
    }
}
