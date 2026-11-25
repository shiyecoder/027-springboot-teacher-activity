package com.grad.trs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.trs.entity.*;
import com.grad.trs.mapper.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 教研组与成员：用于活动发布时的组织选择与组内完成情况统计。
 */
@Service
public class GroupService {

    private final TeachingGroupMapper groupMapper;
    private final TeachingMemberMapper memberMapper;
    private final SysUserMapper userMapper;
    private final ResearchActivityMapper activityMapper;
    private final CreditLedgerMapper ledgerMapper;

    public GroupService(TeachingGroupMapper groupMapper, TeachingMemberMapper memberMapper, SysUserMapper userMapper,
                        ResearchActivityMapper activityMapper, CreditLedgerMapper ledgerMapper) {
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.activityMapper = activityMapper;
        this.ledgerMapper = ledgerMapper;
    }

    public List<Map<String, Object>> list() {
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        List<ResearchActivity> acts = activityMapper.selectList(null);
        List<CreditLedger> ledgers = ledgerMapper.selectList(null);
        return groupMapper.selectList(null).stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("groupName", g.getGroupName());
            m.put("subject", g.getSubject());
            m.put("grade", g.getGrade());
            m.put("leaderName", names.getOrDefault(g.getLeaderId(), "-"));
            m.put("memberCount", g.getMemberCount());
            m.put("targetCredit", g.getTargetCredit());
            m.put("remark", g.getRemark());
            long total = acts.stream().filter(a -> Objects.equals(a.getGroupId(), g.getId())).count();
            long archived = acts.stream().filter(a -> Objects.equals(a.getGroupId(), g.getId()) && "已归档".equals(a.getStatus())).count();
            m.put("activityTotal", total);
            m.put("activityArchived", archived);
            List<Long> memberIds = memberMapper.selectList(new LambdaQueryWrapper<TeachingMember>()
                            .eq(TeachingMember::getGroupId, g.getId()))
                    .stream().map(TeachingMember::getTeacherId).collect(Collectors.toList());
            double credit = ledgers.stream().filter(l -> memberIds.contains(l.getTeacherId()))
                    .map(l -> l.getCredit() == null ? 0.0 : l.getCredit().doubleValue())
                    .mapToDouble(Double::doubleValue).sum();
            m.put("creditSum", Math.round(credit * 10) / 10.0);
            m.put("members", memberIds.stream().map(id -> Map.of("id", id, "name", names.getOrDefault(id, "-")))
                    .collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
    }
}
