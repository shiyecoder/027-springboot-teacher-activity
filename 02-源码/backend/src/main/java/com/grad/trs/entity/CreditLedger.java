package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学时台账（表 credit_ledger）
 */
@Data
@TableName("credit_ledger")
public class CreditLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 台账编号 */
    private String ledgerNo;

    /** 教师 */
    private Long teacherId;

    /** 学时来源 */
    private String sourceType;

    /** 来源主键 */
    private Long sourceId;

    /** 来源编号 */
    private String sourceNo;

    /** 关联活动 */
    private Long activityId;

    /** 学时 */
    private BigDecimal credit;

    /** 学期 */
    private String term;

    /** 状态 */
    private String status;

    /** 计算依据 */
    private String calcBasis;

    /** 确认人 */
    private Long confirmBy;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
