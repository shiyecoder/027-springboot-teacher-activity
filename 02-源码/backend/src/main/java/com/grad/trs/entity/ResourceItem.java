package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教研资源（表 resource_item）
 */
@Data
@TableName("resource_item")
public class ResourceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源编号 */
    private String resNo;

    /** 资源名称 */
    private String title;

    /** 类型 */
    private String resType;

    /** 学科 */
    private String subject;

    /** 年级 */
    private String grade;

    /** 主题 */
    private String topic;

    /** 封面 */
    private String cover;

    /** 教研组 */
    private Long groupId;

    /** 上传人 */
    private Long ownerId;

    /** 来源活动 */
    private Long activityId;

    /** 下载量 */
    private Integer downloadCount;

    /** 状态 */
    private Integer status;

    /** 审核状态 */
    private String auditStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
