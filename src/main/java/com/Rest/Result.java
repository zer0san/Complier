package com.Rest;

import com.Lexer.Lexer;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class Result {
    String res;
    String msg;

    boolean success;
    @Getter
    @Builder.Default
    private Map<String, Integer> keywordTable = new HashMap<>();

    @Getter

    @Builder.Default
    private Map<String, Integer> identifierTable = new HashMap<>();

    @Getter
    @Builder.Default
    private Map<String, Integer> constantTable = new HashMap<>();

    @Getter
    @Builder.Default//
    private Map<String, Integer> operatorTable = new HashMap<>();
    String tokens;

    @Getter
    @Builder.Default
    private Map<String, Integer> separatorTable = new HashMap<>();

    public static Result fail(String msg) {
        return Result.builder().success(false).msg(msg).build();
    }

    @Setter
    public String asmCode;
    @Setter
    public String symbolTable;


    public static Result ok(String res, String tokens, String asmCode, Lexer lexer) {
        Result r = Result.builder().tokens(tokens).success(true).res(res).asmCode(asmCode).build();
        r.setMap(lexer);
        return r;
    }

    public Result() {

    }

    public void setMap(Lexer lexer) {
        keywordTable = lexer.getKeywordTable();
        identifierTable = lexer.getIdentifierTable();
        constantTable = lexer.getConstantTable();
        operatorTable = lexer.getOperatorTable();
        separatorTable = lexer.getSeparatorTable();
    }
    // (k,k1)
}
