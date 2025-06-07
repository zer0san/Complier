package com.Lexer;

/**
 * Token类表示词法分析过程中识别出的词法单元
 * 在编译器前端，词法分析器将源代码字符流转换为Token序列
 * 每个Token都有类型和值两个基本属性
 */
public class Token {

    /**
     * Token类型枚举
     * KEYWORD - 关键字，如if, else, while等语言保留字
     * IDENTIFIER - 标识符，如变量名、函数名等用户定义的名称
     * NUMBER - 数字常量，如整数、浮点数等
     * CHAR_LITERAL - 字符字面量，单引号括起来的单个字符，如'a'
     * OPERATOR - 运算符，如+, -, *, /等操作符号
     * SEPARATOR - 分隔符，如括号、分号、逗号等
     * STRING_LITERAL - 字符串字面量，双引号括起来的字符序列
     * EOF - 文件结束标记
     */
    public enum Type {
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        CHAR_LITERAL,
        OPERATOR,
        SEPARATOR,
        STRING_LITERAL,
        EOF
    }

    /**
     * token的类型，使用Type枚举表示
     * 表明这个词法单元属于哪一类（关键字、标识符、常量等）
     */
    public final Type type;  // token 类型
    //final 代表初始化后不能修改

    /**
     * token的值，存储实际的字符串内容
     * 例如：标识符"count"，数字"42"，关键字"if"等
     */
    public final String value;  // token 值

    /**
     * 构造函数，创建一个新的Token实例
     * @param type Token的类型，来自Type枚举
     * @param value Token的字符串值
     */
    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * 重写toString方法，提供Token的字符串表示形式
     * 格式为"类型:值"，方便调试和日志输出
     * @return Token的字符串表示
     */
    @Override
    public String toString() {
        return type + ":" + value;
    }
}
