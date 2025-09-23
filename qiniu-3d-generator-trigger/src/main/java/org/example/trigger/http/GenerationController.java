package org.example.trigger.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.generation.model.entity.GenerationTaskEntity;
import org.example.service.GenerationApplicationService;
import org.example.types.common.Response;
import org.example.types.enums.GenerationType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3D模型生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/generation")
@Tag(name = "3D模型生成", description = "3D模型生成相关接口")
public class GenerationController {
    
    @Resource
    private GenerationApplicationService generationApplicationService;
    
    /**
     * 文本生成3D模型
     */
    @PostMapping("/text-to-3d")
    @Operation(summary = "文本生成3D模型", description = "根据文本描述生成3D模型")
    public Response<String> textTo3D(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "文本描述") @RequestParam String text,
            @Parameter(description = "质量等级") @RequestParam(defaultValue = "medium") String quality,
            @Parameter(description = "艺术风格") @RequestParam(defaultValue = "realistic") String style,
            @Parameter(description = "API提供商") @RequestParam(required = false) String apiProvider) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("quality", quality);
        params.put("style", style);
        
        return generationApplicationService.createGenerationTask(userId, text, GenerationType.TEXT_TO_3D, params, apiProvider);
    }
    
    /**
     * 图片生成3D模型
     */
    @PostMapping("/image-to-3d")
    @Operation(summary = "图片生成3D模型", description = "根据图片生成3D模型")
    public Response<String> imageTo3D(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "图片URL") @RequestParam String imageUrl,
            @Parameter(description = "质量等级") @RequestParam(defaultValue = "medium") String quality,
            @Parameter(description = "生成纹理") @RequestParam(defaultValue = "true") boolean generateTexture,
            @Parameter(description = "API提供商") @RequestParam(required = false) String apiProvider) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("quality", quality);
        params.put("generateTexture", generateTexture);
        
        return generationApplicationService.createGenerationTask(userId, imageUrl, GenerationType.IMAGE_TO_3D, params, apiProvider);
    }
    
    /**
     * 查询任务状态
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询任务状态", description = "根据任务ID查询生成任务的状态")
    public Response<GenerationTaskEntity> queryTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        
        return generationApplicationService.queryTask(taskId);
    }
    
    /**
     * 获取用户任务列表
     */
    @GetMapping("/tasks")
    @Operation(summary = "获取用户任务列表", description = "获取指定用户的任务列表")
    public Response<List<GenerationTaskEntity>> getUserTasks(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") int limit) {
        
        return generationApplicationService.getUserTasks(userId, limit);
    }
    
    /**
     * 取消任务
     */
    @DeleteMapping("/task/{taskId}")
    @Operation(summary = "取消任务", description = "取消指定的生成任务")
    public Response<Void> cancelTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        
        return generationApplicationService.cancelTask(taskId);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查生成服务的健康状态")
    public Response<String> health() {
        return Response.success("服务正常");
    }
    
    /**
     * 数据库初始化
     */
    @PostMapping("/init-db")
    @Operation(summary = "数据库初始化", description = "初始化数据库表结构")
    public Response<String> initDatabase() {
        return generationApplicationService.initDatabase();
    }
    
    /**
     * 获取可用的API提供商列表
     */
    @GetMapping("/providers")
    @Operation(summary = "获取API提供商", description = "获取可用的API提供商列表")
    public Response<Map<String, Object>> getApiProviders() {
        return generationApplicationService.getApiProviders();
    }
}