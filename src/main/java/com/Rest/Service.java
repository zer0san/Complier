package com.Rest;

import cn.hutool.core.util.StrUtil;
import com.Main;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin()
@Controller()
public class Service {
    Result res;

    @PostMapping("/parse")
    @ResponseBody
    public Result parse(@RequestBody Map<String, String> payload) {
        try {
            String sourceCode = payload.get("sourceCode");
            if (sourceCode == null || sourceCode.isEmpty()) {
                return Result.fail("sourceCode is empty retry again");
            }
            sourceCode = sourceCode.replace('\r', ' ').replace('\n', ' ');
            System.out.printf("%s", sourceCode);
            System.out.println();
            res = Main.Solve(sourceCode);
            return res;
        } catch (Exception e) {
            return Result.fail("error" + e.getMessage());
        }
    }

    @GetMapping("/")
    public String doGet() {
        return "forward:/index.html";
    }
}
