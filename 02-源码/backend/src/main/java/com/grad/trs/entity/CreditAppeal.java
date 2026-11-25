package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学时申诉（表 credit_appeal）
 */
@Data
@TableName("credit_appeal")
public class CreditAppeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 台账 */
    private Long ledgerId;

    /** 教师 */
    private Long teacherId;

    /** 申诉理由 */
    private String reason;

    /** 期望学时 */
    private BigDecimal expectCredit;

    /** 状态 */
    private String status;

    /** 处理人 */
    private Long handleBy;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 回复 */
    private String reply;

    /** 创建时间 */
    private LocalDateTime createTime;
}
