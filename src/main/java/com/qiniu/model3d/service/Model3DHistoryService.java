package com.qiniu.model3d.service;

import com.qiniu.model3d.dto.Model3DHistoryDTO;
import com.qiniu.model3d.entity.Model3DHistory;
import com.qiniu.model3d.entity.ModelTask;
import com.qiniu.model3d.repository.Model3DHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 3D模型历史记录服务类
 * 处理模型文件和预览图的保存、查询、下载等功能
 * 
 * @author Qiniu Team
 * @version 1.0.0
 */
@Service
@Transactional
public class Model3DHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(Model3DHistoryService.class);

    // 文件大小阈值：5MB，超过此大小的文件存储到文件系统
    private static final long FILE_SIZE_THRESHOLD = 5 * 1024 * 1024;
    
    // 预览图尺寸
    private static final int PREVIEW_WIDTH = 200;
    private static final int PREVIEW_HEIGHT = 150;

    @Autowired
    private Model3DHistoryRepository historyRepository;

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.model-dir:./models}")
    private String modelDir;

    @Value("${app.file.preview-dir:./previews}")
    private String previewDir;

    /**
     * 从ModelTask创建历史记录
     */
    public Model3DHistory createFromModelTask(ModelTask modelTask, String clientIp) {
        try {
            Model3DHistory history = new Model3DHistory();
            
            // 基本信息
            history.setModelName(generateModelName(modelTask));
            history.setDescription(modelTask.getInputText());
            history.setInputText(modelTask.getInputText());
            history.setInputImagePath(modelTask.getInputImagePath());
            history.setOriginalTaskId(modelTask.getTaskId());
            history.setClientIp(clientIp);
            
            // 模型信息
            history.setModelFormat(modelTask.getOutputFormat() != null ? 
                modelTask.getOutputFormat().toString() : "GLB");
            history.setVerticesCount(modelTask.getVerticesCount());
            history.setFacesCount(modelTask.getFacesCount());
            history.setFileSize(modelTask.getFileSize());
            history.setComplexity(modelTask.getComplexity() != null ? 
                modelTask.getComplexity().toString() : null);
            
            // 计算生成时间
            if (modelTask.getCreatedAt() != null && modelTask.getCompletedAt() != null) {
                long seconds = java.time.Duration.between(
                    modelTask.getCreatedAt(), modelTask.getCompletedAt()).getSeconds();
                history.setGenerationTimeSeconds((int) seconds);
            }
            
            // 处理模型文件
            if (StringUtils.hasText(modelTask.getModelFilePath())) {
                saveModelFile(history, modelTask.getModelFilePath());
            }
            
            // 处理预览图
            if (StringUtils.hasText(modelTask.getPreviewImagePath())) {
                savePreviewImage(history, modelTask.getPreviewImagePath());
            }
            
            return historyRepository.save(history);
            
        } catch (Exception e) {
            logger.error("创建历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建历史记录失败", e);
        }
    }

    /**
     * 保存模型文件
     */
    private void saveModelFile(Model3DHistory history, String sourceFilePath) throws IOException {
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            logger.warn("源模型文件不存在: {}", sourceFilePath);
            return;
        }

        long fileSize = sourceFile.length();
        history.setFileSize(fileSize);

        if (fileSize <= FILE_SIZE_THRESHOLD) {
            // 小文件：存储到数据库
            byte[] fileData = Files.readAllBytes(sourceFile.toPath());
            history.setModelFileData(fileData);
            logger.info("模型文件存储到数据库，大小: {} bytes", fileSize);
        } else {
            // 大文件：存储到文件系统
            String fileName = generateUniqueFileName(history.getModelName(), getFileExtension(sourceFile.getName()));
            Path targetPath = Paths.get(modelDir, fileName);
            
            // 确保目录存在
            Files.createDirectories(targetPath.getParent());
            
            // 复制文件
            Files.copy(sourceFile.toPath(), targetPath);
            history.setModelFilePath(targetPath.toString());
            logger.info("模型文件存储到文件系统: {}", targetPath);
        }
    }

    /**
     * 保存预览图
     */
    private void savePreviewImage(Model3DHistory history, String sourceImagePath) throws IOException {
        File sourceFile = new File(sourceImagePath);
        if (!sourceFile.exists()) {
            logger.warn("源预览图文件不存在: {}", sourceImagePath);
            return;
        }

        // 生成缩略图
        BufferedImage thumbnail = createThumbnail(sourceFile);
        
        // 将缩略图转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "JPEG", baos);
        byte[] imageData = baos.toByteArray();

        if (imageData.length <= FILE_SIZE_THRESHOLD) {
            // 预览图通常较小，直接存储到数据库
            history.setPreviewImageData(imageData);
            logger.info("预览图存储到数据库，大小: {} bytes", imageData.length);
        } else {
            // 如果预览图仍然很大，存储到文件系统
            String fileName = generateUniqueFileName(history.getModelName() + "_preview", "jpg");
            Path targetPath = Paths.get(previewDir, fileName);
            
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, imageData);
            history.setPreviewImagePath(targetPath.toString());
            logger.info("预览图存储到文件系统: {}", targetPath);
        }
    }

    /**
     * 创建缩略图
     */
    private BufferedImage createThumbnail(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        
        // 计算缩放比例
        double scaleX = (double) PREVIEW_WIDTH / originalImage.getWidth();
        double scaleY = (double) PREVIEW_HEIGHT / originalImage.getHeight();
        double scale = Math.min(scaleX, scaleY);
        
        int newWidth = (int) (originalImage.getWidth() * scale);
        int newHeight = (int) (originalImage.getHeight() * scale);
        
        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(PREVIEW_WIDTH, PREVIEW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        
        // 设置高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 填充白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        
        // 居中绘制图像
        int x = (PREVIEW_WIDTH - newWidth) / 2;
        int y = (PREVIEW_HEIGHT - newHeight) / 2;
        g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
        
        g2d.dispose();
        return thumbnail;
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String baseName, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String cleanName = baseName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        return cleanName + "_" + timestamp + "." + extension;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "bin";
    }

    /**
     * 生成模型名称
     */
    private String generateModelName(ModelTask modelTask) {
        if (StringUtils.hasText(modelTask.getInputText())) {
            String text = modelTask.getInputText();
            if (text.length() > 50) {
                text = text.substring(0, 50) + "...";
            }
            return text;
        }
        return "3D模型_" + System.currentTimeMillis();
    }

    /**
     * 分页查询历史记录
     */
    @Transactional(readOnly = true)
    public Page<Model3DHistoryDTO> getHistoryPage(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Model3DHistory> historyPage = historyRepository.findAll(pageable);
        
        return historyPage.map(this::convertToDTO);
    }

    /**
     * 搜索历史记录
     */
    @Transactional(readOnly = true)
    public Page<Model3DHistoryDTO> searchHistory(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Model3DHistory> historyPage = historyRepository.searchByKeyword(keyword, pageable);
        
        return historyPage.map(this::convertToDTO);
    }

    /**
     * 获取历史记录列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<Model3DHistoryDTO> getHistoryList(int page, int size, String sortBy, String sortDir, 
                                                  String search, String status, String clientIp) {
        try {
            // 创建分页和排序对象
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Model3DHistory> historyPage;
            
            // 根据搜索条件查询
            if (StringUtils.hasText(search)) {
                historyPage = historyRepository.findByModelNameContainingIgnoreCase(search, pageable);
            } else {
                historyPage = historyRepository.findAll(pageable);
            }
            
            // 转换为DTO
            return historyPage.map(this::convertToDTO);
            
        } catch (Exception e) {
            logger.error("获取历史记录列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取历史记录列表失败", e);
        }
    }

    /**
     * 根据ID获取历史记录
     */
    @Transactional(readOnly = true)
    public Optional<Model3DHistory> getHistoryById(Long id) {
        return historyRepository.findById(id);
    }
    
    /**
     * 根据ID获取历史记录DTO
     */
    @Transactional(readOnly = true)
    public Model3DHistoryDTO getHistoryDTOById(Long id) {
        Optional<Model3DHistory> historyOpt = historyRepository.findById(id);
        if (historyOpt.isPresent()) {
            return convertToDTO(historyOpt.get());
        }
        return null;
    }

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount(Long id) {
        historyRepository.incrementDownloadCount(id);
    }

    /**
     * 获取模型文件数据
     */
    @Transactional(readOnly = true)
    public byte[] getModelFileData(Long id) throws IOException {
        Optional<Model3DHistory> historyOpt = historyRepository.findById(id);
        if (!historyOpt.isPresent()) {
            throw new RuntimeException("历史记录不存在");
        }

        Model3DHistory history = historyOpt.get();
        
        if (history.hasModelFileData()) {
            return history.getModelFileData();
        } else if (StringUtils.hasText(history.getModelFilePath())) {
            Path filePath = Paths.get(history.getModelFilePath());
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
        }
        
        throw new RuntimeException("模型文件不存在");
    }

    /**
     * 下载模型文件
     */
    @Transactional(readOnly = true)
    public Resource downloadModel(Long id) throws IOException {
        Optional<Model3DHistory> historyOpt = historyRepository.findById(id);
        if (!historyOpt.isPresent()) {
            throw new RuntimeException("历史记录不存在");
        }

        Model3DHistory history = historyOpt.get();
        
        if (history.hasModelFileData()) {
            // 从数据库获取文件数据
            byte[] fileData = history.getModelFileData();
            return new ByteArrayResource(fileData);
        } else if (StringUtils.hasText(history.getModelFilePath())) {
            // 从文件系统获取文件
            Path filePath = Paths.get(history.getModelFilePath());
            if (Files.exists(filePath)) {
                return new FileSystemResource(filePath);
            }
        }
        
        throw new RuntimeException("模型文件不存在");
    }

    /**
     * 获取预览图数据
     */
    @Transactional(readOnly = true)
    public byte[] getPreviewImageData(Long id) throws IOException {
        Optional<Model3DHistory> historyOpt = historyRepository.findById(id);
        if (!historyOpt.isPresent()) {
            throw new RuntimeException("历史记录不存在");
        }

        Model3DHistory history = historyOpt.get();
        
        if (history.hasPreviewImageData()) {
            return history.getPreviewImageData();
        } else if (StringUtils.hasText(history.getPreviewImagePath())) {
            Path filePath = Paths.get(history.getPreviewImagePath());
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
        }
        
        throw new RuntimeException("预览图不存在");
    }

    /**
     * 获取预览图资源
     */
    @Transactional(readOnly = true)
    public Resource getPreviewImage(Long id) throws IOException {
        Optional<Model3DHistory> historyOpt = historyRepository.findById(id);
        if (!historyOpt.isPresent()) {
            throw new RuntimeException("历史记录不存在");
        }

        Model3DHistory history = historyOpt.get();
        
        if (history.hasPreviewImageData()) {
            // 从数据库获取图片数据
            byte[] imageData = history.getPreviewImageData();
            return new ByteArrayResource(imageData);
        } else if (StringUtils.hasText(history.getPreviewImagePath())) {
            // 从文件系统获取图片
            Path filePath = Paths.get(history.getPreviewImagePath());
            if (Files.exists(filePath)) {
                return new FileSystemResource(filePath);
            }
        }
        
        throw new RuntimeException("预览图不存在");
    }

    /**
     * 转换为DTO
     */
    private Model3DHistoryDTO convertToDTO(Model3DHistory history) {
        Model3DHistoryDTO dto = new Model3DHistoryDTO();
        
        dto.setId(history.getId());
        dto.setModelName(history.getModelName());
        dto.setDescription(history.getDescription());
        dto.setInputText(history.getInputText());
        dto.setFileSize(history.getFileSize());
        dto.setFormattedFileSize(history.getFormattedFileSize());
        dto.setCreateTime(history.getCreateTime());
        dto.setModelFormat(history.getModelFormat());
        dto.setVerticesCount(history.getVerticesCount());
        dto.setFacesCount(history.getFacesCount());
        dto.setDownloadCount(history.getDownloadCount());
        dto.setGenerationTimeSeconds(history.getGenerationTimeSeconds());
        dto.setComplexity(history.getComplexity());
        dto.setStatus(history.getStatus());
        
        // 设置文件存在标志
        dto.setHasModelFile(history.hasModelFileData() || 
            StringUtils.hasText(history.getModelFilePath()));
        dto.setHasPreviewImage(history.hasPreviewImageData() || 
            StringUtils.hasText(history.getPreviewImagePath()));
        
        // 设置URL
        if (dto.isHasModelFile()) {
            dto.setDownloadUrl("/api/history/" + history.getId() + "/download");
        }
        if (dto.isHasPreviewImage()) {
            dto.setPreviewUrl("/api/history/" + history.getId() + "/preview");
        }
        
        return dto;
    }

    /**
     * 保存模型任务到历史记录
     */
    public void saveModelToHistory(ModelTask modelTask) {
        try {
            if (modelTask.getStatus() != ModelTask.TaskStatus.COMPLETED) {
                logger.warn("任务未完成，跳过保存到历史记录: {}", modelTask.getId());
                return;
            }

            Model3DHistory history = new Model3DHistory();
            
            // 基本信息
            history.setModelName(generateModelName(modelTask));
            history.setDescription(modelTask.getInputText());
            history.setInputText(modelTask.getInputText());
            history.setCreateTime(LocalDateTime.now());
            history.setStatus("completed");
            
            // 处理模型文件
            if (StringUtils.hasText(modelTask.getModelFilePath())) {
                Path modelPath = Paths.get(modelTask.getModelFilePath());
                if (Files.exists(modelPath)) {
                    long fileSize = Files.size(modelPath);
                    history.setFileSize(fileSize);
                    history.setModelFormat(getFileExtension(modelTask.getModelFilePath()));
                    
                    if (fileSize <= FILE_SIZE_THRESHOLD) {
                        // 小文件存储到数据库
                        byte[] fileData = Files.readAllBytes(modelPath);
                        history.setModelFileData(fileData);
                    } else {
                        // 大文件存储路径
                        history.setModelFilePath(modelTask.getModelFilePath());
                    }
                }
            }
            
            // 处理预览图
            if (StringUtils.hasText(modelTask.getPreviewImagePath())) {
                Path previewPath = Paths.get(modelTask.getPreviewImagePath());
                if (Files.exists(previewPath)) {
                    long imageSize = Files.size(previewPath);
                    
                    if (imageSize <= FILE_SIZE_THRESHOLD) {
                        // 小文件存储到数据库
                        byte[] imageData = Files.readAllBytes(previewPath);
                        history.setPreviewImageData(imageData);
                    } else {
                        // 大文件存储路径
                        history.setPreviewImagePath(modelTask.getPreviewImagePath());
                    }
                }
            }
            
            // 设置其他属性
            history.setDownloadCount(0);
            history.setGenerationTimeSeconds(calculateGenerationTime(modelTask));
            
            historyRepository.save(history);
            logger.info("成功保存模型到历史记录: {}", history.getId());
            
        } catch (Exception e) {
            logger.error("保存模型到历史记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 计算生成时间（秒）
     */
    private Integer calculateGenerationTime(ModelTask modelTask) {
        if (modelTask.getCreatedAt() != null && modelTask.getUpdatedAt() != null) {
            long seconds = Duration.between(modelTask.getCreatedAt(), modelTask.getUpdatedAt()).getSeconds();
            return (int) seconds;
        }
        return null;
    }

    /**
     * 批量删除历史记录
     */
    public int batchDeleteHistory(List<Long> ids) {
        int deletedCount = 0;
        try {
            for (Long id : ids) {
                if (deleteHistory(id)) {
                    deletedCount++;
                }
            }
            return deletedCount;
        } catch (Exception e) {
            logger.error("批量删除历史记录失败: {}", e.getMessage(), e);
            return deletedCount;
        }
    }

    /**
     * 删除历史记录
     */
    public boolean deleteHistory(Long id) {
        Optional<Model3DHistory> historyOpt = historyRepository.findById(id);
        if (historyOpt.isPresent()) {
            Model3DHistory history = historyOpt.get();
            
            // 删除文件系统中的文件
            if (StringUtils.hasText(history.getModelFilePath())) {
                try {
                    Files.deleteIfExists(Paths.get(history.getModelFilePath()));
                } catch (IOException e) {
                    logger.warn("删除模型文件失败: {}", e.getMessage());
                }
            }
            
            if (StringUtils.hasText(history.getPreviewImagePath())) {
                try {
                    Files.deleteIfExists(Paths.get(history.getPreviewImagePath()));
                } catch (IOException e) {
                    logger.warn("删除预览图文件失败: {}", e.getMessage());
                }
            }
            
            historyRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 获取统计信息
     */
    @Transactional(readOnly = true)
    public Model3DHistoryStats getStats() {
        long totalRecords = historyRepository.countAllRecords();
        long totalFileSize = historyRepository.getTotalFileSize();
        long totalDownloads = historyRepository.getTotalDownloadCount();
        
        return new Model3DHistoryStats(totalRecords, totalFileSize, totalDownloads);
    }

    /**
     * 获取统计信息（带用户过滤）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(String userFilter) {
        // 目前不支持用户过滤，返回全局统计
        Model3DHistoryStats stats = getStats();
        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", stats.getTotalRecords());
        result.put("totalDownloads", stats.getTotalDownloads());
        result.put("totalFileSize", stats.getTotalFileSize());
        result.put("formattedFileSize", stats.getFormattedFileSize());
        return result;
    }

    /**
     * 统计信息类
     */
    public static class Model3DHistoryStats {
        private final long totalRecords;
        private final long totalFileSize;
        private final long totalDownloads;

        public Model3DHistoryStats(long totalRecords, long totalFileSize, long totalDownloads) {
            this.totalRecords = totalRecords;
            this.totalFileSize = totalFileSize;
            this.totalDownloads = totalDownloads;
        }

        public long getTotalRecords() { return totalRecords; }
        public long getTotalFileSize() { return totalFileSize; }
        public long getTotalDownloads() { return totalDownloads; }
        
        public String getFormattedFileSize() {
            if (totalFileSize < 1024) {
                return totalFileSize + " B";
            } else if (totalFileSize < 1024 * 1024) {
                return String.format("%.1f KB", totalFileSize / 1024.0);
            } else if (totalFileSize < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", totalFileSize / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", totalFileSize / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}