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
    String res;

    @PostMapping("/parse")
    @ResponseBody
    public String parse(@RequestBody Map<String,String> payload ) {
        try {
            String sourceCode = payload.get("sourceCode");
            if (sourceCode == null || sourceCode.isEmpty()) {
                return "error: sourceCode is empty";
            }
            // For debugging purposes, you can print the source code
//            byte[] bytes = file.getBytes();

//            List.of(bytes).stream().filter(bt -> bt != '\n'&&bt!='\r').collect(Collectors.toList());

//            Arrays.stream(bytes).forEach(b -> System.out.printf("%02x ", b));
            // String s =StrUtil.toString(bytes);
//            String s = new String(bytes);
            sourceCode = sourceCode.replace('\r', ' ').replace('\n', ' ');
            System.out.printf("%s", sourceCode);
            System.out.println();
            res = Main.Solve(sourceCode);
            return res;
        } catch (Exception e) {
            return "error" + e.getMessage();
        }
    }

    @GetMapping("/")
    public String doGet() {
        return "forward:/index.html";
    }
}
