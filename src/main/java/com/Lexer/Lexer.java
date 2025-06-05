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
    // 添加Getter方法以访问符号表
    @Getter
    private Map<String, Integer> keywordTable = new HashMap<>();

    @Getter
    private Map<String, Integer> identifierTable = new HashMap<>();

    @Getter
    private Map<String, Integer> constantTable = new HashMap<>();

    @Getter
    private Map<String, Integer> operatorTable = new HashMap<>();

    @Getter
    private Map<String, Integer> separatorTable = new HashMap<>();

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

    public String getStandardTokenSequence() {

        // 清空旧数据
        keywordTable.clear();
        identifierTable.clear();
        constantTable.clear();
        operatorTable.clear();
        separatorTable.clear();

        // 创建符号表
        this.keywordTable = new HashMap<>();
        this.identifierTable = new HashMap<>();
        this.constantTable = new HashMap<>();
        this.operatorTable = new HashMap<>();
        this.separatorTable = new HashMap<>();

        // 初始化计数器和映射
        Map<String, Integer> typeCounter = new HashMap<>();
        Map<String, Map<String, String>> valueMap = new HashMap<>();
        StringBuilder result = new StringBuilder();

        // 构建符号表和token序列
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            String typeCode = getTypeCode(token.type);

            // 初始化该类型的计数器和映射
            typeCounter.putIfAbsent(typeCode, 0);
            valueMap.putIfAbsent(typeCode, new HashMap<>());

            // 将token添加到对应的符号表
            switch (token.type) {
                case KEYWORD:
                    keywordTable.putIfAbsent(token.value, keywordTable.size() + 1);
                    break;
                case IDENTIFIER:
                    identifierTable.putIfAbsent(token.value, identifierTable.size() + 1);
                    break;
                case NUMBER:
                case CHAR_LITERAL:
                case STRING_LITERAL:
                    constantTable.putIfAbsent(token.value, constantTable.size() + 1);
                    break;
                case OPERATOR:
                    operatorTable.putIfAbsent(token.value, operatorTable.size() + 1);
                    break;
                case SEPARATOR:
                    separatorTable.putIfAbsent(token.value, separatorTable.size() + 1);
                    break;
            }

            // 生成token序列
            String valueCode;
            if (valueMap.get(typeCode).containsKey(token.value)) {
                valueCode = valueMap.get(typeCode).get(token.value);
            } else {
                int count = typeCounter.get(typeCode) + 1;
                typeCounter.put(typeCode, count);
                valueCode = typeCode + count;
                valueMap.get(typeCode).put(token.value, valueCode);
            }

            if (i > 0) result.append(",");
            result.append("(").append(typeCode).append(",").append(valueCode).append(")");
        }

        // 打印符号表
        System.out.println("关键字表(k):");
        printTable(keywordTable);

        System.out.println("\n标识符表(i):");
        printTable(identifierTable);

        System.out.println("\n常数表(c):");
        printTable(constantTable);

        System.out.println("\n运算符表(op):");
        printTable(operatorTable);

        System.out.println("\n分隔符表(p):");
        printTable(separatorTable);

        System.out.println("\nToken序列:");
        System.out.println(result);

        return result.toString();
    }

    private void printTable(Map<String, Integer> table) {
        table.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> System.out.println(entry.getValue() + ": " + entry.getKey()));
    }

    private String getTypeCode(Token.Type type) {
        return switch (type) {
            case IDENTIFIER -> "I";
            case KEYWORD -> "K";
            case NUMBER -> "C";
            case CHAR_LITERAL -> "C";
            case STRING_LITERAL -> "C";
            case OPERATOR -> "P";
            case SEPARATOR -> "P";
            default -> "x"; // 其他类型
        };
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
