import Lexer.*;
import Parser.*;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
//        Scanner scan = new Scanner(System.in);
//        String s = scan.nextLine();
        String s = """
                int a;
                int b;
                a = 2 + 3 * 4;
                b = a + 5;
                a = a + 1;
                """;
        // 词法分析
        Lexer lexer = new Lexer(s);
        List<Token> tokens = lexer.analyze();
        System.out.println("词法分析结果:");
        lexer.show();

        // 语法分析
        RecursiveParser parser = new RecursiveParser(tokens);
        parser.parseProgram();
        parser.show();
    }
}
