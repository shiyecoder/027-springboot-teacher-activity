package com.grad.trs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志（表 sys_log）
 */
@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 */
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 模块 */
    private String module;

    /** 动作 */
    private String action;

    /** 业务编号 */
    private String bizNo;

    /** 说明 */
    private String detail;

    /** IP */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createTime;

}
