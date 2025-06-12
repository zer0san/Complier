package com.Lexer;

import lombok.Getter;

import java.util.*;

/**
 * 词法分析器类
 * 负责将输入的源代码字符串分解成标记（tokens）序列
 * 能够识别关键字、标识符、常量、运算符和分隔符等词法单元
 */
public class Lexer {
    // 预定义的关键字集合，如if、else、while等编程语言保留字
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "if", "else", "while", "for", "int", "char", "string", "return"
    ));
    
    // 单字符运算符集合
    private static final Set<Character> operators = new HashSet<>(Arrays.asList(
            '+', '-', '*', '/', '=', '<', '>', '!'
    ));
    
    // 双字符运算符集合
    private static final Set<String> two_operators = new HashSet<>(Arrays.asList(
            "++", "--", "==", "!=", "<=", ">="
    ));
    
    // 分隔符集合
    private static final Set<Character> separators = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', ';', ',', '[', ']'));
    
    // 用于错误定位的映射，存储每个Token及其在源代码中的结束位置
    @Getter
    Map<Token, Integer> bugFinderMp = new HashMap<>();
    
    // 各类符号表，用于存储分析过程中识别的不同类型的词法单元
    @Getter
    private Map<String, Integer> keywordTable = new HashMap<>();  // 关键字表

    @Getter
    private Map<String, Integer> identifierTable = new HashMap<>();  // 标识符表

    @Getter
    private Map<String, Integer> constantTable = new HashMap<>();  // 常量表

    @Getter
    private Map<String, Integer> operatorTable = new HashMap<>();  // 运算符表

    @Getter
    private Map<String, Integer> separatorTable = new HashMap<>();  // 分隔符表

    // 输入的源代码字符串
    private final String input;
    
    // 当前分析位置的指针
    private int pos;

    /**
     * 构造函数，初始化词法分析器
     * @param input 需要分析的源代码字符串
     */
    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    // 存储分析产生的所有Token
    private final List<Token> tokens = new ArrayList<>();

    /**
     * 执行词法分析，将输入字符串转换为Token序列
     * @return 分析得到的Token列表
     */
    public List<Token> analyze() {
        while (pos < input.length()) {
            char current = input.charAt(pos);// 获取当前位置的字符
            if (Character.isWhitespace(current)) {
                // 跳过空白字符
                pos++;
            } else if (current == '\'') {
                // 处理字符字面量
                readCharLiteral();
            } else if (current == '"') {
                // 处理字符串字面量
                readStringLiteral();
            } else if (Character.isLetter(current)) {
                // 处理标识符或关键字
                readIdentifierOrKeyword();
            } else if (Character.isDigit(current)) {
                // 处理数字常量
                readNumber();
            } else if (operators.contains(current)) {
                // 处理运算符
                readP();
            } else if (separators.contains(current)) {
                // 处理分隔符
                Token e = new Token(Token.Type.SEPARATOR, String.valueOf(current));
                tokens.add(e);
                bugFinderMp.put(e, pos);// 记录分隔符的位置信息
                // System.out.println("SEPARATOR: " + current);
                pos++;
            } else {
                // 无法识别的字符
                System.out.println("Error! Unknown character: " + current);
                pos++;
            }
        }
        return tokens;
    }

    /**
     * 读取标识符或关键字
     * 标识符由字母、数字和下划线组成，但必须以字母开头
     */
    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }
        String token = input.substring(start, pos);
        if (keywords.contains(token)) {
            // 如果是预定义关键字
            Token e = new Token(Token.Type.KEYWORD, token);
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("KEYWORD: " + token);
        } else {
            // 否则是标识符
            Token e = new Token(Token.Type.IDENTIFIER, token);
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("IDENTIFIER: " + token);
        }
    }

    /**
     * 读取字符字面量，格式为单引号包围的单个字符
     * 例如：'a', '1', '*'
     */
    private void readCharLiteral() {
        pos++; // 跳过开始的单引号
        // System.out.println("处理字符字面量，当前位置: " + pos);
        char value;
        if (pos < input.length() && input.charAt(pos) != '\'') {// 确保不是空字符字面量
            value = input.charAt(pos);// 读取字符字面量的内容
            pos++;
            if (pos < input.length() && input.charAt(pos) == '\'') {// 确保结束的单引号存在
                tokens.add(new Token(Token.Type.CHAR_LITERAL, String.valueOf(value)));// 添加字符字面量到Token列表
                // System.out.println("添加字符字面量: " + value);
                pos++; // 跳过结束的单引号
            } else {
                throw new RuntimeException("Unclosed character literal");// 抛出异常，表示字符字面量未闭合
            }
        } else {
            throw new RuntimeException("Empty character literal");// 抛出异常，表示字符字面量为空
        }
    }

    /**
     * 生成标准化的Token序列，并构建各类符号表
     * @return 格式化的Token序列字符串
     */
    public String getStandardTokenSequence() {// 生成标准化的Token序列
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
        Map<String, Integer> typeCounter = new HashMap<>();  // 记录各类型Token的计数
        Map<String, Map<String, String>> valueMap = new HashMap<>();  // 记录Token值到编码的映射
        StringBuilder result = new StringBuilder();  // 构建结果字符串
        // StringBuilder 是一个可变的字符序列，适合用于频繁修改字符串内容

        // 构建符号表和token序列
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);// 获取当前Token
            String typeCode = getTypeCode(token.type);  // 获取Token类型的编码，如 "K"、"I"、"C"、"P"

            // 初始化该类型的计数器和映射.仅用于初始化
            typeCounter.putIfAbsent(typeCode, 0);// 如果不存在则初始化为0.
            //typeCounter 里面存储每种类型Token的计数，比如K1，C2
            //putIfAbsent 方法用于在Map中添加键值对，如果键不存在则添加，否则不做任何操作
            valueMap.putIfAbsent(typeCode, new HashMap<>());// 如果不存在则初始化为一个新的空Map
            // valueMap 里面存储每种类型Token的值到编码的映射，比如K -> (if,K1), K -> (else,K2),I -> (count,I1)

            // 将token添加到对应的符号表
            switch (token.type) {
                case KEYWORD:
                    keywordTable.putIfAbsent(token.value, keywordTable.size() + 1);// 不存在则添加，避免重复
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

            // 生成token序列的编码表示
            String valueCode;
            if (valueMap.get(typeCode).containsKey(token.value)) {
                // 如果该值已经有对应编码，则直接使用
                valueCode = valueMap.get(typeCode).get(token.value);
                // valueCode 如 "K1", "C2", "I3" 等
            } else {
                // 否则生成新的编码
                int count = typeCounter.get(typeCode) + 1;
                typeCounter.put(typeCode, count);
                valueCode = typeCode + count;
                valueMap.get(typeCode).put(token.value, valueCode);
            }

            // 构建token序列字符串
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

    /**
     * 打印符号表内容
     * @param table 要打印的符号表
     */
    private void printTable(Map<String, Integer> table) {// 这个代码的功能是:
        table.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())  // 按值排序
                .forEach(entry -> System.out.println(entry.getValue() + ": " + entry.getKey()));
    }

    /**
     * 获取Token类型的简写编码
     * @param type Token类型
     * @return 类型的编码：I-标识符, K-关键字, C-常量, P-运算符和分隔符
     */
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

    /**
     * 读取字符串字面量，格式为双引号包围的字符序列
     * 支持转义字符的处理
     */
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
        //System.out.println("添加字符串字面量: " + value);
        pos++; // 跳过结束的双引号
    }

    /**
     * 读取数字常量
     * 目前仅支持整数，不支持小数和科学计数法
     */
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

    /**
     * 读取运算符
     * 支持单字符运算符和双字符运算符
     */
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
            // 单字符运算符
            Token e = new Token(Token.Type.OPERATOR, String.valueOf(current));
            tokens.add(e);
            bugFinderMp.put(e, pos);
            // System.out.println("OPERATOR: " + current);
            pos++;
        }
    }

    /**
     * 显示分析结果，输出所有Token
     * @return Token列表
     */
    public List<Token> show() {
        for (var t : tokens) {
            System.out.println(t);
        }
        return tokens;
    }
}
