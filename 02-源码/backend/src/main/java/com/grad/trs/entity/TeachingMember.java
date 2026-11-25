package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教研组成员（表 teaching_member）
 */
@Data
@TableName("teaching_member")
public class TeachingMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 教研组 */
    private Long groupId;

    /** 教师 */
    private Long teacherId;

    /** 组内角色 */
    private String memberRole;

    /** 加入时间 */
    private LocalDate joinTime;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
