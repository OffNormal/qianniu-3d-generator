package com.qiniu.model3d.controller;

import com.qiniu.model3d.dto.Model3DHistoryDTO;
import com.qiniu.model3d.entity.Model3DHistory;
import com.qiniu.model3d.service.Model3DHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 3D模型历史记录控制器
 * 提供历史记录的查询、下载、删除等功能
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class Model3DHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(Model3DHistoryController.class);

    @Autowired
    private Model3DHistoryService model3DHistoryService;

    /**
     * 获取历史记录列表（分页）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getHistoryList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIp(request);
            Page<Model3DHistoryDTO> historyPage = model3DHistoryService.getHistoryList(
                page, size, sortBy, sortDir, search, status, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", historyPage.getContent());
            response.put("totalElements", historyPage.getTotalElements());
            response.put("totalPages", historyPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取历史记录列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取历史记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取历史记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getHistoryDetail(@PathVariable Long id) {
        try {
            Model3DHistoryDTO history = model3DHistoryService.getHistoryDTOById(id);
            if (history == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "历史记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取历史记录详情失败: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取历史记录详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 下载3D模型文件
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadModel(@PathVariable Long id, HttpServletRequest request) {
        try {
            Resource resource = model3DHistoryService.downloadModel(id);
            if (resource == null || !resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 获取文件信息
            Model3DHistoryDTO history = model3DHistoryService.getHistoryDTOById(id);
            String filename = history.getModelName() + getFileExtension(history.getModelFormat());
            
            // 增加下载次数
            model3DHistoryService.incrementDownloadCount(id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("下载模型文件失败: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取预览图
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> getPreviewImage(@PathVariable Long id) {
        try {
            Resource resource = model3DHistoryService.getPreviewImage(id);
            if (resource == null || !resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("获取预览图失败: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除历史记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistory(@PathVariable Long id) {
        try {
            boolean deleted = model3DHistoryService.deleteHistory(id);
            Map<String, Object> response = new HashMap<>();
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "删除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "历史记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            logger.error("删除历史记录失败: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 批量删除历史记录
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteHistory(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Long> ids = (java.util.List<Long>) request.get("ids");
            
            if (ids == null || ids.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请选择要删除的记录");
                return ResponseEntity.badRequest().body(response);
            }
            
            int deletedCount = model3DHistoryService.batchDeleteHistory(ids);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "成功删除 " + deletedCount + " 条记录");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量删除历史记录失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批量删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics(HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            Map<String, Object> stats = model3DHistoryService.getStatistics(clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 根据模型格式获取文件扩展名
     */
    private String getFileExtension(String format) {
        if (format == null) {
            return ".obj";
        }
        
        switch (format.toUpperCase()) {
            case "OBJ":
                return ".obj";
            case "PLY":
                return ".ply";
            case "STL":
                return ".stl";
            case "GLTF":
                return ".gltf";
            case "GLB":
                return ".glb";
            default:
                return ".obj";
        }
    }
}