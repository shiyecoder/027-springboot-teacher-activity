package com.grad.trs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.trs.common.BizException;
import com.grad.trs.common.PageResult;
import com.grad.trs.common.UserContext;
import com.grad.trs.entity.*;
import com.grad.trs.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学期教研计划：编制 → 提交审批 → 教务审批（通过/退回）。
 * 计划审批由教务管理员完成，编制人不得审批本人提交的计划。
 */
@Service
public class PlanService {

    private final TermPlanMapper planMapper;
    private final TeachingGroupMapper groupMapper;
    private final SysUserMapper userMapper;
    private final LogService logService;

    public PlanService(TermPlanMapper planMapper, TeachingGroupMapper groupMapper,
                       SysUserMapper userMapper, LogService logService) {
        this.planMapper = planMapper;
        this.groupMapper = groupMapper;
        this.userMapper = userMapper;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String status, Long groupId, String term) {
        LambdaQueryWrapper<TermPlan> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            w.eq(TermPlan::getStatus, status);
        }
        if (groupId != null) {
            w.eq(TermPlan::getGroupId, groupId);
        }
        if (term != null && !term.isBlank() && !"全部".equals(term)) {
            w.eq(TermPlan::getTerm, term);
        }
        w.orderByDesc(TermPlan::getCreateTime);
        Page<TermPlan> p = planMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> groups = new HashMap<>();
        groupMapper.selectList(null).forEach(g -> groups.put(g.getId(), g.getGroupName()));
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        List<Map<String, Object>> list = p.getRecords().stream().map(x -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", x.getId());
            m.put("planNo", x.getPlanNo());
            m.put("groupId", x.getGroupId());
            m.put("groupName", groups.getOrDefault(x.getGroupId(), "-"));
            m.put("term", x.getTerm());
            m.put("goal", x.getGoal());
            m.put("activityTypes", x.getActivityTypes());
            m.put("planCount", x.getPlanCount());
            m.put("startDate", x.getStartDate());
            m.put("endDate", x.getEndDate());
            m.put("expectedResult", x.getExpectedResult());
            m.put("status", x.getStatus());
            m.put("submitName", names.getOrDefault(x.getSubmitBy(), "-"));
            m.put("submitTime", x.getSubmitTime());
            m.put("auditOpinion", x.getAuditOpinion());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    @Transactional
    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        TermPlan p = id == null ? new TermPlan() : planMapper.selectById(id);
        if (p == null) {
            throw new BizException("计划不存在");
        }
        if (id != null && !List.of("草稿", "已退回").contains(p.getStatus())) {
            throw new BizException("当前状态不允许编辑");
        }
        String goal = body.get("goal") == null ? null : String.valueOf(body.get("goal"));
        if (goal == null || goal.isBlank()) {
            throw new BizException("教研目标不能为空");
        }
        Long groupId = body.get("groupId") == null ? null : Long.valueOf(String.valueOf(body.get("groupId")));
        if (groupId == null) {
            throw new BizException("请选择教研组");
        }
        p.setGroupId(groupId);
        p.setTerm(String.valueOf(body.getOrDefault("term", "2026-2027-1")));
        p.setGoal(goal.trim());
        p.setActivityTypes(String.valueOf(body.getOrDefault("activityTypes", "")));
        p.setPlanCount(body.get("planCount") == null ? 0 : Integer.valueOf(String.valueOf(body.get("planCount"))));
        String s = body.get("startDate") == null ? null : String.valueOf(body.get("startDate"));
        String e = body.get("endDate") == null ? null : String.valueOf(body.get("endDate"));
        if (s != null && e != null && s.compareTo(e) > 0) {
            throw new BizException("计划开始日期不能晚于结束日期");
        }
        p.setStartDate(s == null ? null : LocalDate.parse(s.substring(0, 10)));
        p.setEndDate(e == null ? null : LocalDate.parse(e.substring(0, 10)));
        p.setExpectedResult(String.valueOf(body.getOrDefault("expectedResult", "")));
        if (id == null) {
            p.setPlanNo("JH" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + String.format("%02d", (planMapper.selectCount(null) % 99) + 1));
            p.setStatus("草稿");
            p.setCreateTime(LocalDateTime.now());
            planMapper.insert(p);
        } else {
            planMapper.updateById(p);
        }
        logService.oper("学期计划", id == null ? "新建" : "编辑", p.getPlanNo(), p.getGoal());
        return p.getId();
    }

    /** 提交审批：草稿或被退回的计划可提交 */
    @Transactional
    public void submit(Long id) {
        TermPlan p = planMapper.selectById(id);
        if (p == null) {
            throw new BizException("计划不存在");
        }
        if (!List.of("草稿", "已退回").contains(p.getStatus())) {
            throw new BizException("只有草稿或已退回的计划可以提交");
        }
        String from = p.getStatus();
        p.setStatus("待审批");
        p.setSubmitBy(UserContext.userId());
        p.setSubmitTime(LocalDateTime.now());
        planMapper.updateById(p);
        logService.record("PLAN", id, p.getPlanNo(), from, "待审批", "提交审批", null);
        logService.oper("学期计划", "提交审批", p.getPlanNo(), "计划次数 " + p.getPlanCount());
    }

    /** 教务审批：编制人不得自审；通过后计划进入执行 */
    @Transactional
    public void audit(Long id, boolean pass, String opinion) {
        TermPlan p = planMapper.selectById(id);
        if (p == null) {
            throw new BizException("计划不存在");
        }
        if (!"待审批".equals(p.getStatus())) {
            throw new BizException("只有待审批的计划可以审批");
        }
        if (Objects.equals(p.getSubmitBy(), UserContext.userId())) {
            throw new BizException("不能审批本人提交的计划");
        }
        if (!pass && (opinion == null || opinion.trim().length() < 5)) {
            throw new BizException("退回必须填写不少于 5 个字的原因");
        }
        p.setStatus(pass ? "已通过" : "已退回");
        p.setAuditBy(UserContext.userId());
        p.setAuditTime(LocalDateTime.now());
        p.setAuditOpinion(opinion);
        planMapper.updateById(p);
        logService.record("PLAN", id, p.getPlanNo(), "待审批", p.getStatus(), pass ? "通过" : "退回", opinion);
        logService.oper("学期计划", pass ? "审批通过" : "审批退回", p.getPlanNo(), opinion);
    }
}
