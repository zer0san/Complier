package com.Rest;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.Main;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin()
@RestController("/")
public class Controller {
    @PostMapping("/parse")
    public String parse(@RequestBody MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String s = JSONUtil.toJsonStr(bytes);
            List<String> solve = Main.Solve(s);
            return StrUtil.join("\n", solve);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
