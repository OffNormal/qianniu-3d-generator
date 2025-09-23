package org.example.trigger.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.generation.service.IHunyuanService;
import org.example.types.common.Response;
import org.example.types.model.HunyuanRequest;
import org.example.types.model.HunyuanResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 腾讯混元3D API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/hunyuan")
@Tag(name = "腾讯混元3D", description = "腾讯混元3D模型生成相关接口")
public class HunyuanController {
    
    @Resource
    private IHunyuanService hunyuanService;
    
    @PostMapping("/text-to-3d")
    @Operation(summary = "文本生成3D模型", description = "基于文本描述生成3D模型")
    public Response<HunyuanResponse> textTo3D(
            @Parameter(description = "文本描述", required = true)
            @RequestParam String prompt,
            @Parameter(description = "生成格式", example = "OBJ")
            @RequestParam(defaultValue = "OBJ") String format,
            @Parameter(description = "是否启用PBR材质")
            @RequestParam(defaultValue = "false") Boolean enablePBR) {
        
        try {
            HunyuanRequest request = HunyuanRequest.builder()
                    .prompt(prompt)
                    .resultFormat(format)
                    .enablePBR(enablePBR)
                    .build();
            
            HunyuanResponse response = hunyuanService.submitHunyuanTo3DJob(request);
            
            return Response.success(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("文本生成3D参数验证失败: {}", e.getMessage());
            return Response.fail("参数验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文本生成3D任务提交失败", e);
            return Response.fail("任务提交失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/image-to-3d")
    @Operation(summary = "图片生成3D模型", description = "基于图片生成3D模型")
    public Response<HunyuanResponse> imageTo3D(
            @Parameter(description = "图片URL", required = true)
            @RequestParam String imageUrl,
            @Parameter(description = "生成格式", example = "OBJ")
            @RequestParam(defaultValue = "OBJ") String format,
            @Parameter(description = "是否启用PBR材质")
            @RequestParam(defaultValue = "false") Boolean enablePBR) {
        
        try {
            HunyuanRequest request = HunyuanRequest.builder()
                    .imageUrl(imageUrl)
                    .resultFormat(format)
                    .enablePBR(enablePBR)
                    .build();
            
            HunyuanResponse response = hunyuanService.submitHunyuanTo3DJob(request);
            
            return Response.success(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("图片生成3D参数验证失败: {}", e.getMessage());
            return Response.fail("参数验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("图片生成3D任务提交失败", e);
            return Response.fail("任务提交失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/submit-job")
    @Operation(summary = "提交混元生3D任务", description = "提交腾讯混元3D生成任务")
    public Response<HunyuanResponse> submitJob(@RequestBody HunyuanRequest request) {
        
        try {
            hunyuanService.validateRequest(request);
            HunyuanResponse response = hunyuanService.submitHunyuanTo3DJob(request);
            
            return Response.success(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("任务提交参数验证失败: {}", e.getMessage());
            return Response.fail("参数验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("任务提交失败", e);
            return Response.fail("任务提交失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/job-status/{jobId}")
    @Operation(summary = "查询任务状态", description = "根据任务ID查询3D生成任务的状态")
    public Response<Object> queryJobStatus(
            @Parameter(description = "任务ID", required = true)
            @PathVariable String jobId) {
        
        try {
            Object status = hunyuanService.queryJobStatus(jobId);
            
            return Response.success(status);
            
        } catch (Exception e) {
            log.error("查询任务状态失败，任务ID: {}", jobId, e);
            return Response.fail("查询任务状态失败: " + e.getMessage());
        }
    }
}