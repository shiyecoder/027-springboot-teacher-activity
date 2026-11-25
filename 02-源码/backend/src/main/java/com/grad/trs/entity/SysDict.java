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
 * 字典（表 sys_dict）
 */
@Data
@TableName("sys_dict")
public class SysDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型 */
    private String dictType;

    /** 标签 */
    private String dictLabel;

    /** 值 */
    private String dictValue;

    /** 排序 */
    private Integer sort;

    /** 状态 */
    private Integer status;

}
