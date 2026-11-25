package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源版本（表 resource_version）
 */
@Data
@TableName("resource_version")
public class ResourceVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源 */
    private Long resourceId;

    /** 版本 */
    private Integer version;

    /** 文件路径 */
    private String filePath;

    /** 文件名 */
    private String fileName;

    /** 变更说明 */
    private String changeNote;

    /** 上传人 */
    private Long uploaderId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
