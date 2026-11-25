package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 站内消息（表 sys_message）
 */
@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务类型 */
    private String bizType;

    /** 业务编号 */
    private String bizNo;

    /** 业务主键 */
    private Long bizId;

    /** 接收人 */
    private Long receiverId;

    /** 是否已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;

}
