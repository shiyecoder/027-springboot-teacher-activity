package com.grad.trs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.trs.entity.FlowLog;
import org.apache.ibatis.annotations.Mapper;

/** 业务流转留痕 Mapper */
@Mapper
public interface FlowLogMapper extends BaseMapper<FlowLog> {
}
