package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教研成果（表 research_achievement）
 */
@Data
@TableName("research_achievement")
public class ResearchAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成果编号 */
    private String achNo;

    /** 成果名称 */
    private String title;

    /** 成果类型 */
    private String achType;

    /** 申报人 */
    private Long teacherId;

    /** 教研组 */
    private Long groupId;

    /** 来源活动 */
    private Long activityId;

    /** 认定等级 */
    private String level;

    /** 封面 */
    private String cover;

    /** 成果文件 */
    private String filePath;

    /** 应用课程数 */
    private Integer courseCount;

    /** 审核状态 */
    private String status;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 初审人 */
    private Long firstAuditBy;

    /** 初审时间 */
    private LocalDateTime firstAuditTime;

    /** 初审意见 */
    private String firstOpinion;

    /** 复审人 */
    private Long finalAuditBy;

    /** 复审时间 */
    private LocalDateTime finalAuditTime;

    /** 复审意见 */
    private String finalOpinion;

    /** 专家评分 */
    private BigDecimal expertScore;

    /** 图谱得分 */
    private BigDecimal graphScore;

    /** 图谱等级 */
    private String graphLevel;

    /** 认定学时 */
    private BigDecimal credit;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
