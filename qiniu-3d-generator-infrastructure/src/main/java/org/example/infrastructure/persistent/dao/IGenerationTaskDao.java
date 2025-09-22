package org.example.infrastructure.persistent.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistent.po.GenerationTaskPO;

import java.util.List;

/**
 * 生成任务DAO接口
 */
@Mapper
public interface IGenerationTaskDao {
    
    /**
     * 插入任务
     */
    void insert(GenerationTaskPO taskPO);
    
    /**
     * 更新任务
     */
    void update(GenerationTaskPO taskPO);
    
    /**
     * 根据任务ID查询
     */
    GenerationTaskPO selectByTaskId(@Param("taskId") String taskId);
    
    /**
     * 根据用户ID查询任务列表
     */
    List<GenerationTaskPO> selectByUserId(@Param("userId") String userId, @Param("limit") int limit);
    
    /**
     * 根据输入哈希查询
     */
    GenerationTaskPO selectByInputHash(@Param("inputHash") String inputHash);
    
    /**
     * 查询相似的已完成任务
     */
    List<GenerationTaskPO> selectSimilarCompletedTasks(@Param("inputContent") String inputContent, 
                                                       @Param("similarity") double similarity);
    
    /**
     * 统计用户任务数量
     */
    int countByUserId(@Param("userId") String userId);
    
    /**
     * 查询质量评分统计
     */
    List<GenerationTaskPO> selectQualityStatistics(@Param("userId") String userId, @Param("days") int days);
}