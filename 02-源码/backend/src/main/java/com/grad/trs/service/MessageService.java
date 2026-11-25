package com.grad.trs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.trs.common.PageResult;
import com.grad.trs.common.UserContext;
import com.grad.trs.entity.SysMessage;
import com.grad.trs.mapper.SysMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内消息：待审批、超时、退回、待验收等事件进入消息中心。
 */
@Service
public class MessageService {

    private final SysMessageMapper messageMapper;

    public MessageService(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public PageResult<SysMessage> page(int page, int size, Boolean unreadOnly) {
        LambdaQueryWrapper<SysMessage> w = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, UserContext.userId());
        if (Boolean.TRUE.equals(unreadOnly)) {
            w.eq(SysMessage::getIsRead, 0);
        }
        w.orderByDesc(SysMessage::getId);
        Page<SysMessage> p = messageMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public long unreadCount() {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, UserContext.userId()).eq(SysMessage::getIsRead, 0));
    }

    @Transactional
    public void read(Long id) {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getId, id).eq(SysMessage::getReceiverId, UserContext.userId())
                .set(SysMessage::getIsRead, 1));
    }

    @Transactional
    public void readAll() {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, UserContext.userId()).set(SysMessage::getIsRead, 1));
    }

    public List<SysMessage> latest(int limit) {
        return messageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, UserContext.userId())
                .orderByDesc(SysMessage::getId).last("limit " + limit));
    }
}
