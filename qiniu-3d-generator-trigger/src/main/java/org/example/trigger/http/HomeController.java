package org.example.trigger.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.types.common.Response;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 */
@Slf4j
@Controller
@Tag(name = "首页", description = "首页相关接口")
public class HomeController {
    
    /**
     * 首页
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "七牛云3D模型生成应用");
        model.addAttribute("description", "基于AI的智能3D模型生成平台");
        model.addAttribute("apiDoc", "/swagger-ui.html");
        model.addAttribute("version", "1.0.0");
        return "index";
    }
    
    /**
     * 应用信息API
     */
    @GetMapping("/api/info")
    @ResponseBody
    @Operation(summary = "获取应用信息", description = "获取应用的基本信息和功能列表")
    public Response<Map<String, Object>> getAppInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "七牛云3D模型生成应用");
        info.put("version", "1.0.0");
        info.put("description", "基于AI的智能3D模型生成平台");
        info.put("features", new String[]{
            "文本生成3D模型",
            "图片生成3D模型", 
            "智能缓存优化",
            "质量评估系统"
        });
        info.put("apiDoc", "/swagger-ui.html");
        info.put("status", "running");
        
        return Response.success(info);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @ResponseBody
    @Operation(summary = "健康检查", description = "检查应用运行状态")
    public Response<String> health() {
        return Response.success("应用运行正常");
    }
}