package Lexer;

import java.util.*;

public class Lexer {
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "if", "else", "while", "for", "int"
    ));
    private static final Set<Character> operators = new HashSet<>(Arrays.asList(
            '+', '-', '*', '/', '=', '<', '>', '!'
    ));
    private static final Set<String> two_operators = new HashSet<>(Arrays.asList(
            "++", "--", "==", "!=", "<=", ">="
    ));
    private static final Set<Character> separators = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', ';', ','
    ));

    private String input;
    private int pos;

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    private List<Token> tokens = new ArrayList<>();

    public List<Token> analyze() {
        while (pos < input.length()) {
            char current = input.charAt(pos);
            if (Character.isWhitespace(current)) {
                pos++;
            } else if (Character.isLetter(current)) {
                readIdentifierOrKeyword();
            } else if (Character.isDigit(current)) {
                readNumber();
            } else if (operators.contains(current)) {
                readP();
            } else if (separators.contains(current)) {
                tokens.add(new Token(Token.Type.SEPARATOR, String.valueOf(current)));
//                System.out.println("SEPARATOR: " + current);
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
            tokens.add(new Token(Token.Type.KEYWORD, token));
//            System.out.println("KEYWORD: " + token);
        } else {
            tokens.add(new Token(Token.Type.IDENTIFIER, token));
//            System.out.println("IDENTIFIER: " + token);
        }
    }

    private void readNumber() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        String token = input.substring(start, pos);
        tokens.add(new Token(Token.Type.NUMBER, token));
//        System.out.println("NUMBER: " + token);
    }

    private void readP() {
        char current = input.charAt(pos);
        String twoChar = "";
        if (pos + 1 < input.length()) {
            twoChar = "" + current + input.charAt(pos + 1);
        }

        // 优先匹配双字符运算符
        if (two_operators.contains(twoChar)) {
            tokens.add(new Token(Token.Type.OPERATOR, twoChar));
//            System.out.println("OPERATOR: " + twoChar);
            pos += 2;
        } else {
            tokens.add(new Token(Token.Type.OPERATOR, String.valueOf(current)));
//            System.out.println("OPERATOR: " + current);
            pos++;
        }
    }

    public void show(){
        for(var t : tokens){
            System.out.println(t);
        }
    }
}
