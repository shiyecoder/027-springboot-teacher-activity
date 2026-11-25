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
 * 菜单（表 sys_menu）
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级 */
    private Long parentId;

    /** 名称 */
    private String menuName;

    /** 类型 */
    private String menuType;

    /** 路由 */
    private String path;

    /** 权限标识 */
    private String perm;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 可见 */
    private Integer visible;

    /** 创建时间 */
    private LocalDateTime createTime;

}
