package com;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.Lexer.Lexer;
import com.Lexer.Token;
import com.Parser.Quadruple.AssemblyGenerator;
import com.Parser.Quadruple.Quadruple;
import com.Parser.RecursiveParser;
import com.Rest.Result;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Main.java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class Main {
    static public Result Solve(String s) {
        // Scanner scan = new Scanner(System.in);
        // 词法分析
        try {

            Lexer lexer = new Lexer(s);
            List<Token> tokens = lexer.analyze();

            String standardTokens = lexer.getStandardTokenSequence();
            // System.out.println(standardTokens);
            // lexer.generateStandardTokens();
            System.out.println("词法分析结果:");
            lexer.show();
            // 语法分析
            RecursiveParser parser = new RecursiveParser(tokens);
            Map<Token, Integer> bugFinderMp = lexer.getBugFinderMp();
            parser.setBugFinder(bugFinderMp);
            parser.setSourceCode(s);
            try {
                parser.parseProgram();
                // assemblyGenerator.generateAssembly();
            } catch (RuntimeException e) {
                String message = e.getMessage();
                System.out.println("语法分析错误: " + message);
                Token errorToken = tokens.get(parser.getPos() == tokens.size() ? tokens.size() - 1 : parser.getPos());
                Integer srcErrPos = bugFinderMp.get(errorToken);
                System.out.println("错误位置: ");
                // System.out.printf("");
                Console.log(s.substring(0, srcErrPos));
                Console.error("^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                Console.error(s.substring(srcErrPos));
                String errorMessage = String.format(
                        s.substring(0, srcErrPos) + "\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n" + s.substring(srcErrPos));
                return Result.fail("syntax error" + e.getMessage() + "\n" + errorMessage);
            }
            List<Quadruple> qds = parser.show();

            // Generate symbol table
            com.Parser.Quadruple.SymbolTable symbolTable = new com.Parser.Quadruple.SymbolTable();
            symbolTable.buildFromQuadruples(qds);
            String symbolTableString = symbolTable.printSymbolTable();

            // 目标代码生成
            AssemblyGenerator asmGen = new AssemblyGenerator();
            asmGen.generateAssembly(qds);
            String asmCode = asmGen.show();

            List<String> rTokens = tokens.stream().map(Token::toString).collect(Collectors.toList());
            List<String> qdsList = qds.stream().map(Quadruple::toString).collect(Collectors.toList());
            // rTokens.addAll(collect);
            Result successResult = new Result();
            successResult.setAsmCode(asmCode);
            successResult.setSuccess(true);
            successResult.setRes(StrUtil.join("\n", qdsList));
            successResult.setTokens(standardTokens);
            successResult.setSymbolTable(symbolTableString);
            successResult.setMap(lexer);
            successResult.setMsg("Analysis successful");
            return successResult;

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
