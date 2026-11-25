package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课例改进（表 lesson_reflection）
 */
@Data
@TableName("lesson_reflection")
public class LessonReflection {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动 */
    private Long activityId;

    /** 授课教师 */
    private Long teacherId;

    /** 版本 */
    private Integer version;

    /** 反思内容 */
    private String content;

    /** 改进要点 */
    private String improvePoints;

    /** 版本对比 */
    private String compareNote;

    /** 创建时间 */
    private LocalDateTime createTime;
}
