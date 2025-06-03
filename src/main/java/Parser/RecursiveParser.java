package Parser;

import java.util.*;

import Parser.Quadruple.*;
import Lexer.*;

public class RecursiveParser {
    private final List<Token> tokens;
    private int pos = 0;
    private int labelId = 0;
    private final QuadrupleGenerator gen = new QuadrupleGenerator();

    public RecursiveParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token lookahead() {
        if (pos < tokens.size())
            return tokens.get(pos);
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


    // 程序入口
    public void parseProgram() {
        while(lookahead().type != Token.Type.EOF) {
            if(isFuncDeclStart()) {
                parseFuncDecl();
            }
            else {
                parseStmt();
            }
        }

        parseStmtList();
        System.out.println("Parse Successful!");
    }

    // 判断是否为函数声明起始
    // 即int后面跟标识符，再跟(
    private boolean isFuncDeclStart() {
        if(!lookahead().value.equals("int")) return false;
        // 尝试看下一个和下下个
        if(pos + 1 < tokens.size() && tokens.get(pos + 1).type == Token.Type.IDENTIFIER) {
            if(pos + 2 < tokens.size() && tokens.get(pos + 2).value.equals("(")) {
                return true;
            }
        }
        return false;
    }

    // 处理函数
    private void parseFuncDecl() {

    }


    // 处理语句
    // 不断调用parseStmt，直到遇到右大括号或文件结尾
    private void parseStmtList() {
        while (!lookahead().value.equals("}") && lookahead().type != Token.Type.EOF) {
            parseStmt();
        }
    }


    // 识别语句是哪种类型
    private void parseStmt() {
        Token t = lookahead();
        // 声明语句
        if (t.value.equals("int")) {
            parseDeclStmt();
        }
        // 赋值语句
        else if (t.type == Token.Type.IDENTIFIER) {
            parseAssignStmt();
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
        match("int");
        match(Token.Type.IDENTIFIER);
        match(";");
    }

    private void parseAssignStmt() {
        String var = match(Token.Type.IDENTIFIER).value;
        match("=");
        Expr expr = parseExpr();
        gen.assign(var, expr);
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

    // 处理变量、数字、括号表达式
    private Expr parseFactor() {
        Token t = lookahead();
        if (t.type == Token.Type.IDENTIFIER) {
            return new VarExpr(match(Token.Type.IDENTIFIER).value);
        } else if (t.type == Token.Type.NUMBER) {
            return new NumberExpr(Integer.parseInt(match(Token.Type.NUMBER).value));
        } else if (t.value.equals("(")) {
            match("(");
            Expr expr = parseExpr();
            match(")");
            return expr;
        } else {
            // 当源代码存在语法错误，报错
            throw new RuntimeException("Expected Identifier or Number, but found " + t.value);
        }
    }

    // 输出四元式
    public void show() {
        System.out.println("生成的四元式");
        gen.show();
    }
}
