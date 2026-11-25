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
 * 用户（表 sys_user）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 组织 */
    private Long orgId;

    /** 角色 */
    private Long roleId;

    /** 状态 */
    private Integer status;

    /** 失败次数 */
    private Integer loginFail;

    /** 锁定截止 */
    private LocalDateTime lockUntil;

    /** 最后登录 */
    private LocalDateTime lastLogin;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
