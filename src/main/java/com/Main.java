package com;

import cn.hutool.core.util.StrUtil;
import com.Lexer.*;

import com.Parser.*;
import com.Parser.Quadruple.Quadruple;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;

// Main.java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Main {
    static public String Solve( String s) {
        // Scanner scan = new Scanner(System.in);
        // 词法分析
        try {

            Lexer lexer = new Lexer(s);
            List<Token> tokens = lexer.analyze();
            System.out.println("词法分析结果:");
            lexer.show();
            // 语法分析
            RecursiveParser parser = new RecursiveParser(tokens);
            parser.parseProgram();
            List<Quadruple> qds = parser.show();
            List<String> rTokens = tokens.stream().map(Token::toString).collect(Collectors.toList());
            List<String> collect = qds.stream().map(Quadruple::toString).collect(Collectors.toList());
            rTokens.addAll(collect);
            return StrUtil.join("\n", rTokens);
        } catch (Exception e) {
            return "syntx error " + e.toString();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
