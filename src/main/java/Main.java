import Lexer.*;
import Parser.*;

import java.util.*;

public class Main {
    public static void main(String[] args) {
//        Scanner scan = new Scanner(System.in);
//        String s = scan.nextLine();
        String s = """
                int a;
                int b;
                int c;
                a = b + c;
                while (a >= 1){
                    a = b + c;
                }
                if (a == 1){
                    a = b + c;
                }
                else {
                    a = a * 2;
                }
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
