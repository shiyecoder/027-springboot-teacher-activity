package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图谱权重规则（表 graph_rule）
 */
@Data
@TableName("graph_rule")
public class GraphRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 活动参与权重 */
    private Integer activityWeight;

    /** 成果来源权重 */
    private Integer achievementWeight;

    /** 课程应用权重 */
    private Integer courseWeight;

    /** 负责人权重 */
    private Integer leaderWeight;

    /** 认定等级权重 */
    private Integer levelWeight;

    /** 时间顺序权重 */
    private Integer timeWeight;

    /** 版本 */
    private Integer version;

    /** 状态 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectTime;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
