package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成果审核记录（表 achievement_review）
 */
@Data
@TableName("achievement_review")
public class AchievementReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成果 */
    private Long achievementId;

    /** 审核人 */
    private Long reviewerId;

    /** 审核阶段 */
    private String stage;

    /** 结论 */
    private String result;

    /** 评分 */
    private BigDecimal score;

    /** 意见 */
    private String opinion;

    /** 创建时间 */
    private LocalDateTime createTime;
}
