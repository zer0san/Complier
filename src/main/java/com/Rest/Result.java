package com.Rest;

import com.Lexer.Lexer;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class Result {
    String res;
    String msg;

    boolean success;
    @Getter
    private Map<String, Integer> keywordTable = new HashMap<>();

    @Getter
    private Map<String, Integer> identifierTable = new HashMap<>();

    @Getter
    private Map<String, Integer> constantTable = new HashMap<>();

    @Getter
    private Map<String, Integer> operatorTable = new HashMap<>();
    String tokens;

    @Getter
    private Map<String, Integer> separatorTable = new HashMap<>();
    public static Result fail(String msg) {
        return Result.builder().success(false).msg(msg).build();
    }
    public static  Result ok(String res,String tokens,Lexer lexer) {
        Result r = Result.builder().tokens(tokens).success(true).res(res).build();
        r.setMap(lexer);
        return r;
    }
    public void setMap(Lexer lexer) {
        keywordTable = lexer.getKeywordTable();
        identifierTable = lexer.getIdentifierTable();
        constantTable = lexer.getConstantTable();
        operatorTable = lexer.getOperatorTable();
        separatorTable = lexer.getSeparatorTable();
    }
//(k,k1)
}
