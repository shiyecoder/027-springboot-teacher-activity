package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图谱人工修正（表 graph_adjust）
 */
@Data
@TableName("graph_adjust")
public class GraphAdjust {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计算快照 */
    private Long scoreId;

    /** 成果 */
    private Long achievementId;

    /** 原等级 */
    private String beforeLevel;

    /** 调整后 */
    private String afterLevel;

    /** 原因 */
    private String reason;

    /** 操作人 */
    private Long operatorId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
