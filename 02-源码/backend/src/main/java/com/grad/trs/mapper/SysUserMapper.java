package com.grad.trs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.trs.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 用户 Mapper */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
