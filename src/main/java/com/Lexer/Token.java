package com.Lexer;

public class Token {

    public enum Type {
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        OPERATOR,
        SEPARATOR,
        EOF
    }

    public final Type type;  // token 类型
    public final String value;  // token 值

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return type + ":" + value;
    }
}
