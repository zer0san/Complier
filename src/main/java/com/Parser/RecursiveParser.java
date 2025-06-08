package com.Parser;

import java.util.*;

import com.Parser.*;
import com.Lexer.*;
import com.Lexer.Token;
import com.Parser.Quadruple.*;
import lombok.Data;
import lombok.Setter;

/**
 * 递归下降语法分析器
 * 负责将词法分析器生成的Token序列解析为语法结构，并生成四元式中间代码
 */
@Data// lombok annotation, 生成getter和setter方法
public class RecursiveParser {
    // 词法分析器生成的Token序列
    private final List<Token> tokens;
    // 当前解析位置的指针
    private int pos = 0;
    // 标签计数器，用于生成唯一的标签（如跳转标签）
    private int labelId = 0;
    // 四元式生成器，用于生成中间代码
    private final QuadrupleGenerator gen = new QuadrupleGenerator();
    
    // 原始源代码，用于错误报告
    @Setter
    String sourceCode = "";

    // 用于记录token位置的映射，便于错误定位
    @Setter
    Map<Token, Integer> bugFinder = new HashMap<>();

    /**
     * 构造函数
     * @param tokens 词法分析器生成的Token序列
     */
    public RecursiveParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * 查看当前token，但不消耗它
     * @return 当前位置的Token，如果已到末尾则返回EOF
     */
    private Token lookahead() {
        if (pos < tokens.size()) {
            return tokens.get(pos);
        }
        return new Token(Token.Type.EOF, "");
    }

    /**
     * 匹配并消耗指定值的token
     * @param value 期望的token值
     * @return 匹配的token
     * @throws RuntimeException 如果当前token与期望值不匹配
     */
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

    /**
     * 匹配并消耗指定类型的token
     * @param type 期望的token类型
     * @return 匹配的token
     * @throws RuntimeException 如果当前token与期望类型不匹配
     */
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

    /**
     * 生成新的唯一标签名
     * @return 格式为"L数字"的标签字符串
     */
    private String newLabel() {
        return "L" + (labelId++);
    }
    
    /**
     * 解析return语句
     * 语法形式: return [expr];
     */
    private void parseReturnStmt() {
        match("return");

        // 检查是否有返回值，如果分号前有表达式则解析
        Expr returnExpr = null;
        if (!lookahead().value.equals(";")) {
            returnExpr = parseExpr();
        }

        match(";");

        // 使用QuadrupleGenerator中已有的returnStmt方法生成返回四元式
        gen.returnStmt(returnExpr);
    }

    /**
     * 程序解析入口
     * 按顺序解析函数声明和语句，直到文件结束
     */
    public void parseProgram() {
        while (lookahead().type != Token.Type.EOF) {
            if (isFuncDeclStart()) {// 判断当前位置是否为函数声明的开始
                parseFuncDecl();// 解析函数声明
            } else {
                parseStmt();// 解析语句
            }
        }

        parseStmtList();
        System.out.println("Parse Successful!");
    }

    /**
     * 判断当前位置是否为函数声明的开始
     * 函数声明格式: 类型 标识符(参数列表)
     * @return 是否为函数声明开始
     */
    private boolean isFuncDeclStart() {
        String value = lookahead().value;
        if(!value.equals("int") && !value.equals("char") && !value.equals("string")) return false;

        // 尝试看下一个和下下个token是否符合函数声明模式
        if (pos + 1 < tokens.size() && tokens.get(pos + 1).type == Token.Type.IDENTIFIER) {
            if (pos + 2 < tokens.size() && tokens.get(pos + 2).value.equals("(")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析函数声明
     * 语法形式: 返回类型 函数名(参数列表) { 函数体 }
     */
    private void parseFuncDecl() {
        String returnType = match(lookahead().value).value;// 获取返回类型（int/char/string等）
        String funcName = match(Token.Type.IDENTIFIER).value;// 获取函数名
        match("(");// 匹配左括号开始参数列表
        List<String[]> paramsWithTypes = parseParamList();// 解析参数列表，返回类型和名称的列表
        match(")");

        // 提取参数名和类型到单独的列表
        List<String> paramNames = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        for (String[] param : paramsWithTypes) {
            paramTypes.add(param[0]);
            paramNames.add(param[1]);
        }

        // 生成函数相关的四元式
        gen.emitFuncLabel(funcName);// 生成函数标签
        gen.emitFuncParam(returnType, funcName, paramNames);// 生成函数参数四元式
        parseBlock();// 解析函数体，包含语句列表
        gen.emitFuncEnd(funcName);// 生成函数结束四元式
    }

    /**
     * 解析函数参数列表
     * 语法形式: type1 param1, type2 param2, ...
     * @return 包含参数类型和名称的列表，每个参数表示为[类型,名称]的数组
     */
    private List<String[]> parseParamList() {
        List<String[]> paramsWithTypes = new ArrayList<>();

        // 空参数列表情况
        if (lookahead().value.equals(")")) {
            return paramsWithTypes;
        }

        // 解析一个或多个参数
        do {
            String type = match(lookahead().value).value; // 获取参数类型
            String paramName = match(Token.Type.IDENTIFIER).value;
            paramsWithTypes.add(new String[]{type, paramName});

            // 检查是否有更多参数
            if(lookahead().value.equals(",")) {
                match(",");
            } else {
                break;
            }
        } while(true);

        return paramsWithTypes;
    }

    /**
     * 解析语句列表
     * 不断解析语句，直到遇到右大括号或文件结尾
     */
    private void parseStmtList() {
        while (!lookahead().value.equals("}") && lookahead().type != Token.Type.EOF) {
            parseStmt();
        }
    }

    /**
     * 解析函数调用
     * 语法形式: 函数名(参数1, 参数2, ...)
     * @param funcName 函数名
     * @return 函数调用表达式对象
     */
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
    
    /**
     * 解析单个语句
     * 根据当前token判断语句类型并调用对应的解析方法
     */
    private void parseStmt() {
        Token t = lookahead();
        // 变量声明语句
        if (t.value.equals("int") || t.value.equals("char") || t.value.equals("string")) {
            parseDeclStmt();
        }
        // return语句
        else if (t.value.equals("return")) {
            parseReturnStmt();
        }
        // 赋值语句或函数调用
        else if (t.type == Token.Type.IDENTIFIER) {
            String id = match(Token.Type.IDENTIFIER).value;// 获取标识符（变量名或函数名）
            if (lookahead().value.equals("(")) {
                // 函数调用
                Expr funcCall = parseFunctionCall(id);// 解析函数调用
                match(";");
                gen.generateFunctionCall((FunctionCallExpr)funcCall);// 生成函数调用四元式
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
        } 
        // if语句
        else if (t.value.equals("if")) {
            parseIfStmt();
        } 
        // while循环
        else if (t.value.equals("while")) {
            parseWhileStmt();
        } else {
            // 语法错误处理
            throw new RuntimeException("Expected DeclStmt, AssignStmt or Block, but found " + t.value);// 报错
        }
    }

    /**
     * 解析if语句
     * 语法形式: if (condition) statement [else statement]
     */
    private void parseIfStmt() {
        match("if");
        match("(");
        Condition cond = parseCondition();
        match(")");

        // 生成用于跳转的标签
        String labelElse = newLabel();
        String labelEnd = newLabel();

        // 条件为假时跳转到else部分
        gen.ifFalse(cond, labelElse);
        parseStmt(); // 解析then部分语句

        // 处理可选的else部分
        if (lookahead().value.equals("else")) {
            gen.gotoLabel(labelEnd); // then执行完后跳过else部分
            gen.emitElLabel();
            gen.emitLabel(labelElse);
            match("else");
            parseStmt();  // 解析else部分语句
            gen.emitLabel(labelEnd);
        } else {
            gen.emitLabel(labelElse); // 没有else，直接作为结束点
        }
        gen.emitIeLabel();
    }

    /**
     * 解析while循环
     * 语法形式: while (condition) statement
     */
    private void parseWhileStmt() {
        match("while");
        // 生成循环开始和结束的标签
        String labelStart = newLabel();
        String labelEnd = newLabel();

        gen.emitWhLabel();
        gen.emitLabel(labelStart);

        match("(");
        Condition cond = parseCondition();
        match(")");

        // 条件为假时跳出循环
        gen.ifFalse(cond, labelEnd);
        parseStmt();  // 解析循环体
        gen.gotoLabel(labelStart);  // 循环回起始点
        gen.emitLabel(labelEnd);    // 循环结束标签

        gen.emitWeLabel();
    }

    /**
     * 解析条件表达式
     * 语法形式: expr 操作符 expr
     * @return 条件表达式对象
     */
    private Condition parseCondition() {
        Expr left = parseExpr();
        String op = match(lookahead().value).value;  // 比较操作符
        Expr right = parseExpr();
        return new Condition(op, left, right);
    }

    /**
     * 解析变量声明语句
     * 语法形式: 类型 变量名; 或 类型 数组名[大小];
     */
    private void parseDeclStmt() {
        String type = match(lookahead().value).value; // 获取变量类型(int/char/string)
        String varName = match(Token.Type.IDENTIFIER).value;
        
        // 记录变量类型信息
        gen.declareVariable(type, varName);
        
        // 检查是否为数组声明: int a[10];
        if (lookahead().value.equals("[")) {
            match("[");
            String size = match(Token.Type.NUMBER).value;
            match("]");
            gen.declareArray(varName, Integer.parseInt(size));
        }
        
        match(";");
    }

    /**
     * 解析赋值语句
     * 语法形式: 变量 = 表达式; 或 数组[索引] = 表达式;
     */
    private void parseAssignStmt() {
        String var = match(Token.Type.IDENTIFIER).value;

        // 检查是否为数组元素赋值: a[i] = expr;
        if (lookahead().value.equals("[")) {
            match("[");
            Expr indexExpr = parseExpr();  // 数组索引表达式
            match("]");
            match("=");
            Expr valueExpr = parseExpr();  // 赋值表达式
            gen.assignArray(var, indexExpr, valueExpr);
        } else {
            // 普通变量赋值: a = expr;
            match("=");
            Expr expr = parseExpr();
            gen.assign(var, expr);
        }

        match(";");
    }

    /**
     * 解析代码块
     * 语法形式: { 语句列表 }
     */
    private void parseBlock() {
        match("{");
        parseStmtList();
        match("}");
    }

    /**
     * 解析加减表达式
     * 语法形式: 项 (+|-) 项 ...
     * @return 表达式对象
     */
    private Expr parseExpr() {
        Expr left = parseTerm();
        while (lookahead().value.equals("+") || lookahead().value.equals("-")) {
            String op = match(lookahead().value).value;
            Expr right = parseTerm();
            // 构建二元表达式
            left = new BinaryExpr(op, left, right);
        }
        return left;
    }

    /**
     * 解析乘除表达式
     * 语法形式: 因子 (*|/) 因子 ...
     * @return 表达式对象
     */
    private Expr parseTerm() {
        Expr left = parseFactor();
        while (lookahead().value.equals("*") || lookahead().value.equals("/")) {
            String op = match(lookahead().value).value;
            Expr right = parseFactor();
            // 构建二元表达式
            left = new BinaryExpr(op, left, right);
        }
        return left;
    }

    /**
     * 解析表达式因子(基本单元)
     * 可以是:变量、数字、字符、字符串、数组元素、函数调用或括号表达式
     * @return 表达式对象
     */
    private Expr parseFactor() {
        Token t = lookahead();
        if (t.type == Token.Type.IDENTIFIER) {
            String varName = match(Token.Type.IDENTIFIER).value;
            // 检查是否是函数调用: func(...)
            if (lookahead().value.equals("(")) {
                return parseFunctionCall(varName);
            }
            // 检查是否是数组访问: arr[index]
            else if (lookahead().value.equals("[")) {
                match("[");
                Expr indexExpr = parseExpr();
                match("]");
                return new ArrayAccessExpr(varName, indexExpr);
            } else {
                // 普通变量
                return new VarExpr(varName);
            }
        } else if (t.type == Token.Type.NUMBER) {
            // 数字字面量
            return new NumberExpr(Integer.parseInt(match(Token.Type.NUMBER).value));
        } else if (t.type == Token.Type.CHAR_LITERAL) {
            // 字符字面量
            return new CharExpr(match(Token.Type.CHAR_LITERAL).value.charAt(0));
        } else if (t.type == Token.Type.STRING_LITERAL) {
            // 字符串字面量
            return new StringExpr(match(Token.Type.STRING_LITERAL).value);
        } else if (t.value.equals("(")) {
            // 括号表达式: (expr)
            match("(");
            Expr expr = parseExpr();
            match(")");
            return expr;
        } else {
            // 语法错误处理
            throw new RuntimeException("Expected Identifier, Number, Character or String, but found " + t.value);
        }
    }

    /**
     * 输出生成的四元式中间代码
     * @return 四元式列表
     */
    public List<Quadruple> show() {
        System.out.println("生成的四元式");
        gen.show();
        return gen.getQuadruples();
    }
}
