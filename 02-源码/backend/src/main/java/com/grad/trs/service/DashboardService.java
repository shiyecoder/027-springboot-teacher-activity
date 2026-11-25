package com.grad.trs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.trs.entity.*;
import com.grad.trs.mapper.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 院系教研看板：6 个核心指标 + 6 组图表数据。
 * 统计口径全部在后端按数据库聚合完成，前端不做二次全表聚合，保证图表与明细总数一致。
 */
@Service
public class DashboardService {

    private final ResearchActivityMapper activityMapper;
    private final ResearchAchievementMapper achievementMapper;
    private final ActivityMemberMapper memberMapper;
    private final CreditLedgerMapper ledgerMapper;
    private final ResourceItemMapper resourceMapper;
    private final LessonEvaluationMapper evaluationMapper;
    private final TeachingGroupMapper groupMapper;
    private final SysUserMapper userMapper;

    public DashboardService(ResearchActivityMapper activityMapper, ResearchAchievementMapper achievementMapper,
                            ActivityMemberMapper memberMapper, CreditLedgerMapper ledgerMapper,
                            ResourceItemMapper resourceMapper, LessonEvaluationMapper evaluationMapper,
                            TeachingGroupMapper groupMapper, SysUserMapper userMapper) {
        this.activityMapper = activityMapper;
        this.achievementMapper = achievementMapper;
        this.memberMapper = memberMapper;
        this.ledgerMapper = ledgerMapper;
        this.resourceMapper = resourceMapper;
        this.evaluationMapper = evaluationMapper;
        this.groupMapper = groupMapper;
        this.userMapper = userMapper;
    }

    public Map<String, Object> overview() {
        List<ResearchActivity> acts = activityMapper.selectList(null);
        List<ResearchAchievement> achs = achievementMapper.selectList(null);
        List<ActivityMember> members = memberMapper.selectList(null);
        long teacherCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        long joined = members.stream().map(ActivityMember::getTeacherId).distinct().count();
        long certified = achs.stream().filter(a -> "已认定".equals(a.getStatus())).count();
        int courseApps = achs.stream().mapToInt(a -> a.getCourseCount() == null ? 0 : a.getCourseCount()).sum();
        long resourceCount = resourceMapper.selectCount(null);

        List<Map<String, Object>> kpis = new ArrayList<>();
        kpis.add(kpi("活动数量", String.valueOf(acts.size()), "本学期累计教研活动"));
        kpis.add(kpi("教师参与率", pct(joined, teacherCount), "覆盖 " + groupMapper.selectCount(null) + " 个教研组"));
        kpis.add(kpi("成果数量", String.valueOf(achs.size()), "已认定 " + certified + " 项"));
        kpis.add(kpi("成果认定率", pct(certified, achs.size()), "含退回与评审中"));
        kpis.add(kpi("课程应用数", String.valueOf(courseApps), "成果关联课程合计"));
        kpis.add(kpi("资源总数", String.valueOf(resourceCount), "教案/课件/试题/反思"));

        // 趋势：按活动时间所在月份统计
        Map<String, Long> trend = new TreeMap<>();
        for (ResearchActivity a : acts) {
            String key = a.getActivityTime() == null ? "-" : String.format("%02d 月", a.getActivityTime().getMonthValue());
            trend.merge(key, 1L, Long::sum);
        }
        List<Map<String, Object>> trendData = trend.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList());

        Map<String, Long> byStatus = acts.stream().collect(Collectors.groupingBy(ResearchActivity::getStatus, Collectors.counting()));
        Map<String, Long> byType = acts.stream().collect(Collectors.groupingBy(ResearchActivity::getActType, Collectors.counting()));
        Map<String, Long> byLevel = achs.stream().filter(a -> a.getGraphLevel() != null)
                .collect(Collectors.groupingBy(ResearchAchievement::getGraphLevel, Collectors.counting()));

        // 教研组完成率：已归档活动 / 计划次数
        List<Map<String, Object>> groupRank = new ArrayList<>();
        for (TeachingGroup g : groupMapper.selectList(null)) {
            long total = acts.stream().filter(a -> Objects.equals(a.getGroupId(), g.getId())).count();
            long archived = acts.stream().filter(a -> Objects.equals(a.getGroupId(), g.getId()) && "已归档".equals(a.getStatus())).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", g.getGroupName());
            m.put("value", total == 0 ? 0 : Math.round(archived * 100.0 / Math.max(total, 1)));
            m.put("archived", archived);
            m.put("total", total);
            groupRank.add(m);
        }
        groupRank.sort((x, y) -> Long.compare(((Number) y.get("value")).longValue(), ((Number) x.get("value")).longValue()));

        // 评课得分分布：按 60 以下 / 60-70 / 70-80 / 80-90 / 90 以上分桶
        int[] buckets = new int[5];
        for (LessonEvaluation e : evaluationMapper.selectList(null)) {
            int s = e.getTotalScore() == null ? 0 : e.getTotalScore();
            buckets[s < 60 ? 0 : (s < 70 ? 1 : (s < 80 ? 2 : (s < 90 ? 3 : 4)))]++;
        }
        String[] labels = {"<60", "60-70", "70-80", "80-90", "≥90"};
        List<Map<String, Object>> scoreDist = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", labels[i]);
            m.put("value", buckets[i]);
            scoreDist.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kpis", kpis);
        out.put("trend", trendData);
        out.put("statusDist", kv(byStatus));
        out.put("typeDist", kv(byType));
        out.put("groupRank", groupRank);
        out.put("scoreDist", scoreDist);
        out.put("levelDist", kv(byLevel));
        out.put("creditTotal", ledgerMapper.selectList(null).stream()
                .map(l -> l.getCredit() == null ? BigDecimal.ZERO : l.getCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return out;
    }

    private List<Map<String, Object>> kv(Map<String, Long> src) {
        return src.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> kpi(String label, String value, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("desc", desc);
        return m;
    }

    private String pct(long a, long b) {
        if (b == 0) {
            return "0%";
        }
        return BigDecimal.valueOf(a * 100.0 / b).setScale(1, RoundingMode.HALF_UP) + "%";
    }
}
