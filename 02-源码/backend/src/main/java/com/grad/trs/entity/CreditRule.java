package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学时规则（表 credit_rule）
 */
@Data
@TableName("credit_rule")
public class CreditRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 活动类型 */
    private String actType;

    /** 参与角色 */
    private String roleType;

    /** 基础学时 */
    private BigDecimal baseCredit;

    /** 材料加分 */
    private BigDecimal materialBonus;

    /** 缺签扣分 */
    private BigDecimal signPenalty;

    /** 成果系数 */
    private BigDecimal achievementFactor;

    /** 版本 */
    private Integer version;

    /** 状态 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectTime;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
