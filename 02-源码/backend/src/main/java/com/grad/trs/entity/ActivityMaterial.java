package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动材料（表 activity_material）
 */
@Data
@TableName("activity_material")
public class ActivityMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动 */
    private Long activityId;

    /** 材料类型 */
    private String matType;

    /** 文件路径 */
    private String filePath;

    /** 文件名 */
    private String fileName;

    /** 版本 */
    private Integer version;

    /** 大小 */
    private Integer fileSize;

    /** 上传人 */
    private Long uploaderId;

    /** 审核状态 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
