package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教研活动（表 research_activity）
 */
@Data
@TableName("research_activity")
public class ResearchActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动编号 */
    private String actNo;

    /** 活动主题 */
    private String title;

    /** 教研组 */
    private Long groupId;

    /** 教研类型 */
    private String actType;

    /** 学科 */
    private String subject;

    /** 年级 */
    private String grade;

    /** 关联计划 */
    private Long planId;

    /** 主持人 */
    private Long hostId;

    /** 主讲人 */
    private Long speakerId;

    /** 记录人 */
    private Long recorderId;

    /** 活动时间 */
    private LocalDateTime activityTime;

    /** 时长分钟 */
    private Integer duration;

    /** 活动地点 */
    private String location;

    /** 签到码 */
    private String signCode;

    /** 已签到 */
    private Integer signCount;

    /** 应参与 */
    private Integer memberCount;

    /** 活动照片 */
    private String cover;

    /** 活动小结 */
    private String summary;

    /** 状态 */
    private String status;

    /** 材料校验 */
    private Integer archivePass;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
