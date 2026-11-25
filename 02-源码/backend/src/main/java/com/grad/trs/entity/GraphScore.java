package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图谱计算快照（表 graph_score）
 */
@Data
@TableName("graph_score")
public class GraphScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成果 */
    private Long achievementId;

    /** 输入快照 */
    private String inputs;

    /** 分明细 */
    private String details;

    /** 总分 */
    private BigDecimal totalScore;

    /** 结果等级 */
    private String resultLevel;

    /** 结论解释 */
    private String conclusion;

    /** 规则版本 */
    private Integer ruleVersion;

    /** 创建时间 */
    private LocalDateTime createTime;
}
