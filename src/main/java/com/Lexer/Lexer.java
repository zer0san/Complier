package com.Lexer;

import lombok.Getter;

import java.util.*;

public class Lexer {
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "if", "else", "while", "for", "int", "char", "string", "return"
    ));
    private static final Set<Character> operators = new HashSet<>(Arrays.asList(
            '+', '-', '*', '/', '=', '<', '>', '!'
    ));
    private static final Set<String> two_operators = new HashSet<>(Arrays.asList(
            "++", "--", "==", "!=", "<=", ">="
    ));
    private static final Set<Character> separators = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', ';', ',', '[', ']'));
    @Getter
    Map<Token, Integer> bugFinderMp = new HashMap<>();

    private final String input;
    private int pos;

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    private final List<Token> tokens = new ArrayList<>();

    public List<Token> analyze() {
        while (pos < input.length()) {
            char current = input.charAt(pos);
            if (Character.isWhitespace(current)) {
                pos++;
            } else if (current == '\'') {
                readCharLiteral();
            } else if (current == '"') {  // 添加对双引号的检测
                readStringLiteral();
            } else if (Character.isLetter(current)) {
                readIdentifierOrKeyword();
            } else if (Character.isDigit(current)) {
                readNumber();
            } else if (operators.contains(current)) {
                readP();
            } else if (separators.contains(current)) {
                Token e = new Token(Token.Type.SEPARATOR, String.valueOf(current));
                tokens.add(e);
                bugFinderMp.put(e, pos);
                // System.out.println("SEPARATOR: " + current);
                pos++;
            } else {
                System.out.println("Error! Unknown character: " + current);
                pos++;
            }
        }
        return tokens;
    }

    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }
        String token = input.substring(start, pos);
        if (keywords.contains(token)) {
            Token e = new Token(Token.Type.KEYWORD, token);
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("KEYWORD: " + token);
        } else {
            Token e = new Token(Token.Type.IDENTIFIER, token);
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("IDENTIFIER: " + token);
        }
    }


    private void readCharLiteral() {
        pos++; // 跳过开始的单引号
        System.out.println("处理字符字面量，当前位置: " + pos);
        char value;
        if (pos < input.length() && input.charAt(pos) != '\'') {
            value = input.charAt(pos);
            pos++;
            if (pos < input.length() && input.charAt(pos) == '\'') {
                tokens.add(new Token(Token.Type.CHAR_LITERAL, String.valueOf(value)));
                System.out.println("添加字符字面量: " + value);
                pos++; // 跳过结束的单引号
            } else {
                throw new RuntimeException("Unclosed character literal");
            }
        } else {
            throw new RuntimeException("Empty character literal");
        }
    }

    private void readStringLiteral() {
        pos++; // 跳过开始的双引号
        int start = pos;
        System.out.println("处理字符串字面量，当前位置: " + pos);

        while (pos < input.length() && input.charAt(pos) != '"') {
            // 处理转义字符
            if (input.charAt(pos) == '\\' && pos + 1 < input.length()) {
                pos += 2; // 跳过转义字符和被转义字符
            } else {
                pos++;
            }
        }

        if (pos >= input.length()) {
            throw new RuntimeException("未闭合的字符串字面量");
        }

        String value = input.substring(start, pos);
        tokens.add(new Token(Token.Type.STRING_LITERAL, value));
        System.out.println("添加字符串字面量: " + value);
        pos++; // 跳过结束的双引号
    }

    private void readNumber() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        String token = input.substring(start, pos);
        Token e = new Token(Token.Type.NUMBER, token);
        tokens.add(e);
        bugFinderMp.put(e, pos);
        // System.out.println("NUMBER: " + token);
    }

    private void readP() {
        char current = input.charAt(pos);
        String twoChar = "";
        if (pos + 1 < input.length()) {
            twoChar = "" + current + input.charAt(pos + 1);
        }

        // 优先匹配双字符运算符
        if (two_operators.contains(twoChar)) {
            Token e = new Token(Token.Type.OPERATOR, twoChar);
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("OPERATOR: " + twoChar);
            pos += 2;
        } else {
            Token e = new Token(Token.Type.OPERATOR, String.valueOf(current));
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("OPERATOR: " + current);
            pos++;
        }
    }

    public List<Token> show() {
        for (var t : tokens) {
            System.out.println(t);
        }
        return tokens;
    }
}
