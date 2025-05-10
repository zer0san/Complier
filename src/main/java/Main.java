import Lexer.*;
import Parser.*;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        String s = scan.nextLine();
        Lexer lexer = new Lexer(s);
        List<Token> tokens = lexer.analyze();
        RecursiveParser parser = new RecursiveParser(tokens);
        parser.parseProgram();
    }
}
