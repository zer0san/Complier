package Parser;

import java.util.*;

import Lexer.*;

public class RecursiveParser {
    private final List<Token> tokens;
    private int pos = 0;

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

    // 程序入口
    public void parseProgram()
    {
        parseStmtList();
        System.out.println("Parse Successful!");
    }

    // 处理语句
    // 不断调用parseStmt，直到遇到右大括号或文件结尾
    private void parseStmtList() {
        while(!lookahead().value.equals("}") && lookahead().type != Token.Type.EOF) {
            parseStmt();
        }
    }

    // 识别语句是哪种类型
    private void parseStmt(){
        Token t = lookahead();
        // 声明语句
        if(t.value.equals("int")){
            parseDeclStmt();
        }
        // 赋值语句
        else if(t.type == Token.Type.IDENTIFIER){
            parseAssignStmt();
        }
        // 代码块
        else if(t.value.equals("{")){
            parseBlock();
        }
        else{
            // 当源代码存在语法错误，报错
            throw new RuntimeException("Expected DeclStmt, AssignStmt or Block, but found " + t.value);
        }
    }

    private void parseDeclStmt(){
        match("int");
        match(Token.Type.IDENTIFIER);
        match(";");
    }

    private void parseAssignStmt(){
        match(Token.Type.IDENTIFIER);
        match("=");
        parseExpr();
        match(";");
    }

    private void parseBlock(){
        match("{");
        parseStmtList();
        match("}");
    }

    // 处理加减表达式
    private void parseExpr(){
        parseTerm();
        parseExprPrime();
    }

    // 处理多个+或-连接的项
    private void parseExprPrime(){
        Token t = lookahead();
        if(t.value.equals("+") || t.value.equals("-")){
            match(t.value);
            parseTerm();
            parseExprPrime();
        }
    }

    // 处理乘除表达式
    private void parseTerm(){
        parseFactor();
        parseTermPrime();
    }

    // 处理多个*或/连接的项
    private void parseTermPrime(){
        Token t = lookahead();
        if(t.value.equals("*") || t.value.equals("/")){
            match(t.value);
            parseFactor();
            parseTermPrime();
        }
    }

    // 处理变量、数字、括号表达式
    private void parseFactor(){
        Token t = lookahead();
        if(t.type == Token.Type.IDENTIFIER || t.type == Token.Type.NUMBER){
            match(t.type);
        }
        else if(t.value.equals("(")){
            match("(");
            parseExpr();
            match(")");
        }
        else{
            // 当源代码存在语法错误，报错
            throw new RuntimeException("Expected Identifier or Number, but found " + t.value);
        }
    }
}
