package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 听评课评分（表 lesson_evaluation）
 */
@Data
@TableName("lesson_evaluation")
public class LessonEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动 */
    private Long activityId;

    /** 被评教师 */
    private Long teacherId;

    /** 评课人 */
    private Long evaluatorId;

    /** 教学目标 */
    private Integer targetScore;

    /** 教学内容 */
    private Integer contentScore;

    /** 教学方法 */
    private Integer methodScore;

    /** 课堂互动 */
    private Integer interactScore;

    /** 教学效果 */
    private Integer effectScore;

    /** 总分 */
    private Integer totalScore;

    /** 优点 */
    private String advantage;

    /** 改进建议 */
    private String suggestion;

    /** 创建时间 */
    private LocalDateTime createTime;
}
