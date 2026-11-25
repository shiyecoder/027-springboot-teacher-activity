package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动分工与签到（表 activity_member）
 */
@Data
@TableName("activity_member")
public class ActivityMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动 */
    private Long activityId;

    /** 教师 */
    private Long teacherId;

    /** 参与角色 */
    private String roleType;

    /** 签到状态 */
    private Integer signStatus;

    /** 签到时间 */
    private LocalDateTime signTime;

    /** 材料完成 */
    private Integer materialDone;

    /** 学时 */
    private BigDecimal credit;

    /** 时间冲突 */
    private Integer conflictFlag;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
