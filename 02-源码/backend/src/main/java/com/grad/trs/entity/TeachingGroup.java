package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教研组（表 teaching_group）
 */
@Data
@TableName("teaching_group")
public class TeachingGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String groupName;

    /** 学科 */
    private String subject;

    /** 年级 */
    private String grade;

    /** 所属院系 */
    private Long orgId;

    /** 组长 */
    private Long leaderId;

    /** 成员数 */
    private Integer memberCount;

    /** 学期 */
    private String term;

    /** 学期学时任务 */
    private BigDecimal targetCredit;

    /** 状态 */
    private Integer status;

    /** 说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
