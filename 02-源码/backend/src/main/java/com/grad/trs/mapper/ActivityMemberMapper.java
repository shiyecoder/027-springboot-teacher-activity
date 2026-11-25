package com.grad.trs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.trs.entity.ActivityMember;
import org.apache.ibatis.annotations.Mapper;

/** 活动分工与签到 Mapper */
@Mapper
public interface ActivityMemberMapper extends BaseMapper<ActivityMember> {
}
