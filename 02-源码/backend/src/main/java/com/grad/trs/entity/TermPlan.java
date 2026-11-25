package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期教研计划（表 term_plan）
 */
@Data
@TableName("term_plan")
public class TermPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划编号 */
    private String planNo;

    /** 教研组 */
    private Long groupId;

    /** 学期 */
    private String term;

    /** 教研目标 */
    private String goal;

    /** 活动类型安排 */
    private String activityTypes;

    /** 计划次数 */
    private Integer planCount;

    /** 开始 */
    private LocalDate startDate;

    /** 结束 */
    private LocalDate endDate;

    /** 预期成果 */
    private String expectedResult;

    /** 状态 */
    private String status;

    /** 提交人 */
    private Long submitBy;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审批人 */
    private Long auditBy;

    /** 审批时间 */
    private LocalDateTime auditTime;

    /** 审批意见 */
    private String auditOpinion;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
