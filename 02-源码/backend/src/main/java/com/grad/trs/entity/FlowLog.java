package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 业务流转留痕（表 flow_log）
 */
@Data
@TableName("flow_log")
public class FlowLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 原状态 */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 动作 */
    private String action;

    /** 原因 */
    private String reason;

    /** 操作人 */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
