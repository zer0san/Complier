package com.Rest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result {
    String res;
    String msg;

    boolean success;

    public static Result fail(String msg) {
        return Result.builder().success(false).msg(msg).build();
    }
    public static  Result ok(String res) {
        return Result.builder().success(true).res(res).build();
    }

}
