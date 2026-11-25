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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学时台账：由活动签到、材料完成与成果认定按规则计算。
 * 台账一旦确认不再直接修改，需要变更时建立调整记录，并保留原始行。
 */
@Service
public class CreditService {

    private final CreditRuleMapper ruleMapper;
    private final CreditLedgerMapper ledgerMapper;
    private final CreditAppealMapper appealMapper;
    private final ActivityMemberMapper memberMapper;
    private final ResearchActivityMapper activityMapper;
    private final SysUserMapper userMapper;
    private final LogService logService;

    public CreditService(CreditRuleMapper ruleMapper, CreditLedgerMapper ledgerMapper, CreditAppealMapper appealMapper,
                         ActivityMemberMapper memberMapper, ResearchActivityMapper activityMapper,
                         SysUserMapper userMapper, LogService logService) {
        this.ruleMapper = ruleMapper;
        this.ledgerMapper = ledgerMapper;
        this.appealMapper = appealMapper;
        this.memberMapper = memberMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
        this.logService = logService;
    }

    public List<CreditRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<CreditRule>().orderByDesc(CreditRule::getVersion));
    }

    /** 规则变更生成新版本，旧版本置为历史，保证历史台账可追溯到当时的规则 */
    @Transactional
    public Long saveRule(Map<String, Object> body) {
        CreditRule r = new CreditRule();
        r.setRuleName(String.valueOf(body.getOrDefault("ruleName", "学时规则")));
        r.setActType(body.get("actType") == null ? "*" : String.valueOf(body.get("actType")));
        r.setRoleType(body.get("roleType") == null ? "*" : String.valueOf(body.get("roleType")));
        r.setBaseCredit(dec(body.getOrDefault("baseCredit", "1.0")));
        r.setMaterialBonus(dec(body.getOrDefault("materialBonus", "0.5")));
        r.setSignPenalty(dec(body.getOrDefault("signPenalty", "0.5")));
        r.setAchievementFactor(dec(body.getOrDefault("achievementFactor", "1.0")));
        CreditRule cur = ruleMapper.selectOne(new LambdaQueryWrapper<CreditRule>()
                .eq(CreditRule::getStatus, 1).orderByDesc(CreditRule::getVersion).last("limit 1"));
        r.setVersion(cur == null ? 1 : cur.getVersion() + 1);
        r.setStatus(1);
        r.setEffectTime(LocalDateTime.now());
        r.setCreateBy(UserContext.userId());
        r.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(r);
        return r.getId();
    }

    /** 归档后生成学时：角色基础学时 + 材料齐全加分 − 缺签到扣分，同一活动同一教师不重复生成 */
    @Transactional
    public int generateForActivity(Long activityId) {
        ResearchActivity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        if (!"已归档".equals(a.getStatus())) {
            throw new BizException("只有已归档的活动才能生成学时");
        }
        List<ActivityMember> members = memberMapper.selectList(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getActivityId, activityId));
        int created = 0;
        for (ActivityMember m : members) {
            long exist = ledgerMapper.selectCount(new LambdaQueryWrapper<CreditLedger>()
                    .eq(CreditLedger::getSourceType, "活动参与").eq(CreditLedger::getSourceId, activityId)
                    .eq(CreditLedger::getTeacherId, m.getTeacherId()));
            if (exist > 0) {
                continue;
            }
            CreditRule rule = matchRule(a.getActType(), m.getRoleType());
            BigDecimal credit = rule == null ? BigDecimal.valueOf(1.0) : rule.getBaseCredit();
            if (m.getMaterialDone() != null && m.getMaterialDone() == 1 && rule != null) {
                credit = credit.add(rule.getMaterialBonus());
            }
            if (m.getSignStatus() == null || m.getSignStatus() == 0) {
                if (rule != null) {
                    credit = credit.subtract(rule.getSignPenalty());
                }
            }
            if (credit.compareTo(BigDecimal.ZERO) < 0) {
                credit = BigDecimal.ZERO;
            }
            CreditLedger l = new CreditLedger();
            l.setLedgerNo(genNo());
            l.setTeacherId(m.getTeacherId());
            l.setSourceType("活动参与");
            l.setSourceId(activityId);
            l.setSourceNo(a.getActNo());
            l.setActivityId(activityId);
            l.setCredit(credit);
            l.setTerm("2026-2027-1");
            l.setStatus("待确认");
            l.setCalcBasis("角色 " + m.getRoleType() + " 基础 " + (rule == null ? "1.0" : rule.getBaseCredit())
                    + (m.getMaterialDone() != null && m.getMaterialDone() == 1 ? " + 材料齐全 0.5" : "")
                    + (m.getSignStatus() == null || m.getSignStatus() == 0 ? " − 缺签到 0.5" : ""));
            l.setCreateTime(LocalDateTime.now());
            ledgerMapper.insert(l);
            m.setCredit(credit);
            memberMapper.updateById(m);
            created++;
        }
        logService.oper("学时台账", "生成", a.getActNo(), "新增 " + created + " 条学时记录");
        return created;
    }

    private CreditRule matchRule(String actType, String roleType) {
        List<CreditRule> all = ruleMapper.selectList(new LambdaQueryWrapper<CreditRule>().eq(CreditRule::getStatus, 1));
        return all.stream()
                .filter(r -> (r.getRoleType() == null || "*".equals(r.getRoleType()) || r.getRoleType().equals(roleType)))
                .filter(r -> (r.getActType() == null || "*".equals(r.getActType()) || r.getActType().equals(actType)))
                .max(Comparator.comparingInt(r -> (r.getActType() == null || "*".equals(r.getActType()) ? 0 : 2)
                        + (r.getRoleType() == null || "*".equals(r.getRoleType()) ? 0 : 1)))
                .orElse(null);
    }

    public PageResult<Map<String, Object>> page(int page, int size, String status, Long teacherId, String keyword) {
        LambdaQueryWrapper<CreditLedger> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            w.eq(CreditLedger::getStatus, status);
        }
        if (teacherId != null) {
            w.eq(CreditLedger::getTeacherId, teacherId);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(CreditLedger::getLedgerNo, keyword).or().like(CreditLedger::getSourceNo, keyword));
        }
        w.orderByDesc(CreditLedger::getCreateTime);
        Page<CreditLedger> p = ledgerMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        List<Map<String, Object>> list = p.getRecords().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("ledgerNo", l.getLedgerNo());
            m.put("teacherId", l.getTeacherId());
            m.put("teacherName", names.getOrDefault(l.getTeacherId(), "-"));
            m.put("sourceType", l.getSourceType());
            m.put("sourceNo", l.getSourceNo());
            m.put("credit", l.getCredit());
            m.put("term", l.getTerm());
            m.put("status", l.getStatus());
            m.put("calcBasis", l.getCalcBasis());
            m.put("createTime", l.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    /** 确认学时：待确认或已申诉的记录可确认，确认后不可直接修改 */
    @Transactional
    public int confirm(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要确认的台账");
        }
        int n = 0;
        for (Long id : ids) {
            CreditLedger l = ledgerMapper.selectById(id);
            if (l == null) {
                continue;
            }
            if (!List.of("待确认", "已申诉").contains(l.getStatus())) {
                continue;
            }
            l.setStatus("已确认");
            l.setConfirmBy(UserContext.userId());
            l.setConfirmTime(LocalDateTime.now());
            ledgerMapper.updateById(l);
            n++;
        }
        logService.oper("学时台账", "确认", null, "确认 " + n + " 条");
        return n;
    }

    @Transactional
    public void appeal(Long ledgerId, String reason, BigDecimal expect) {
        CreditLedger l = ledgerMapper.selectById(ledgerId);
        if (l == null) {
            throw new BizException("台账不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("申诉理由不少于 5 个字");
        }
        CreditAppeal ap = new CreditAppeal();
        ap.setLedgerId(ledgerId);
        ap.setTeacherId(l.getTeacherId());
        ap.setReason(reason.trim());
        ap.setExpectCredit(expect);
        ap.setStatus("待处理");
        ap.setCreateTime(LocalDateTime.now());
        appealMapper.insert(ap);
        l.setStatus("已申诉");
        ledgerMapper.updateById(l);
    }

    public PageResult<Map<String, Object>> appeals(int page, int size, String status) {
        LambdaQueryWrapper<CreditAppeal> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            w.eq(CreditAppeal::getStatus, status);
        }
        w.orderByDesc(CreditAppeal::getCreateTime);
        Page<CreditAppeal> p = appealMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        List<Map<String, Object>> list = p.getRecords().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("ledgerId", a.getLedgerId());
            m.put("teacherName", names.getOrDefault(a.getTeacherId(), "-"));
            m.put("reason", a.getReason());
            m.put("expectCredit", a.getExpectCredit());
            m.put("status", a.getStatus());
            m.put("handleName", names.getOrDefault(a.getHandleBy(), "-"));
            m.put("handleTime", a.getHandleTime());
            m.put("reply", a.getReply());
            m.put("createTime", a.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    /** 处理申诉：补记时新建调整台账，原记录保持已确认，不修改历史数据 */
    @Transactional
    public void handleAppeal(Long id, String status, String reply) {
        CreditAppeal ap = appealMapper.selectById(id);
        if (ap == null) {
            throw new BizException("申诉记录不存在");
        }
        if (!List.of("已受理", "已驳回", "已补记").contains(status)) {
            throw new BizException("处理结果不合法");
        }
        if (reply == null || reply.trim().length() < 5) {
            throw new BizException("处理意见不少于 5 个字");
        }
        CreditLedger src = ledgerMapper.selectById(ap.getLedgerId());
        ap.setStatus(status);
        ap.setReply(reply.trim());
        ap.setHandleBy(UserContext.userId());
        ap.setHandleTime(LocalDateTime.now());
        appealMapper.updateById(ap);
        if (src != null) {
            src.setStatus("已确认");
            ledgerMapper.updateById(src);
        }
        if ("已补记".equals(status) && src != null) {
            CreditLedger adj = new CreditLedger();
            adj.setLedgerNo(genNo());
            adj.setTeacherId(src.getTeacherId());
            adj.setSourceType("人工调整");
            adj.setSourceId(ap.getId());
            adj.setSourceNo(src.getSourceNo());
            adj.setActivityId(src.getActivityId());
            adj.setCredit(ap.getExpectCredit() == null ? BigDecimal.ZERO : ap.getExpectCredit().subtract(src.getCredit()));
            adj.setTerm(src.getTerm());
            adj.setStatus("已调整");
            adj.setCalcBasis("申诉补记：原 " + src.getCredit() + " → " + ap.getExpectCredit() + "，原因：" + ap.getReason());
            adj.setConfirmBy(UserContext.userId());
            adj.setConfirmTime(LocalDateTime.now());
            adj.setCreateTime(LocalDateTime.now());
            ledgerMapper.insert(adj);
        }
        logService.oper("学时台账", "申诉处理", src == null ? null : src.getSourceNo(), status + "：" + reply);
    }

    private String genNo() {
        return "XS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-"
                + String.format("%04d", (int) (System.currentTimeMillis() % 9000 + 1000));
    }

    private BigDecimal dec(Object o) {
        return new BigDecimal(String.valueOf(o));
    }
}
