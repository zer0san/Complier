package com.Rest;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.Main;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin()
@RestController("/")
public class Controller {
    String res;

    @PostMapping("/parse")
    public String parse(@RequestBody MultipartFile  file) {
        try {
            byte[] bytes = file.getBytes();
            String s = JSONUtil.toJsonStr(bytes);
            List<String> solve = Main.Solve(s);
            res = StrUtil.join("\n", solve);
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/parse")
    public String doGet() {
        return res;
    }
}
