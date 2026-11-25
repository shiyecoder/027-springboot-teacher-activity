package com.grad.trs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.trs.common.BizException;
import com.grad.trs.common.UserContext;
import com.grad.trs.entity.*;
import com.grad.trs.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 创新点「教研活动与成果关联图谱」：
 * 1) 权重可配置，合计必须 100%，每次修改生成新版本并记录生效时间；
 * 2) 每次计算保存输入快照、规则版本、各项得分与结论，结果可追溯；
 * 3) 结果页给出推荐结论、主要影响因素、未满足条件与可改进项；
 * 4) 负责人可人工调整等级但必须填写原因，原始快照不被覆盖。
 */
@Service
public class GraphService {

    private final GraphRuleMapper ruleMapper;
    private final GraphScoreMapper scoreMapper;
    private final GraphAdjustMapper adjustMapper;
    private final ResearchAchievementMapper achievementMapper;
    private final ActivityMemberMapper memberMapper;
    private final ResearchActivityMapper activityMapper;
    private final TeachingGroupMapper groupMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper json = new ObjectMapper();

    public GraphService(GraphRuleMapper ruleMapper, GraphScoreMapper scoreMapper, GraphAdjustMapper adjustMapper,
                        ResearchAchievementMapper achievementMapper, ActivityMemberMapper memberMapper,
                        ResearchActivityMapper activityMapper, TeachingGroupMapper groupMapper, SysUserMapper userMapper) {
        this.ruleMapper = ruleMapper;
        this.scoreMapper = scoreMapper;
        this.adjustMapper = adjustMapper;
        this.achievementMapper = achievementMapper;
        this.memberMapper = memberMapper;
        this.activityMapper = activityMapper;
        this.groupMapper = groupMapper;
        this.userMapper = userMapper;
    }

    public GraphRule activeRule() {
        GraphRule r = ruleMapper.selectOne(new LambdaQueryWrapper<GraphRule>()
                .eq(GraphRule::getStatus, 1).orderByDesc(GraphRule::getVersion).last("limit 1"));
        if (r == null) {
            throw new BizException("尚未配置图谱规则，请先在规则配置页设置权重");
        }
        return r;
    }

    public List<GraphRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<GraphRule>().orderByDesc(GraphRule::getVersion));
    }

    /** 保存规则：六项权重合计必须为 100%，校验通过后旧版本自动置为历史 */
    @Transactional
    public Long saveRule(Map<String, Object> body) {
        int[] w = new int[6];
        String[] keys = {"activityWeight", "achievementWeight", "courseWeight", "leaderWeight", "levelWeight", "timeWeight"};
        int sum = 0;
        for (int i = 0; i < keys.length; i++) {
            if (body.get(keys[i]) == null) {
                throw new BizException("六项权重都必须填写");
            }
            int v = Integer.parseInt(String.valueOf(body.get(keys[i])).split("\\.")[0]);
            if (v < 0 || v > 100) {
                throw new BizException("权重必须在 0–100 之间");
            }
            w[i] = v;
            sum += v;
        }
        if (sum != 100) {
            throw new BizException("六项权重合计必须等于 100%，当前为 " + sum + "%");
        }
        GraphRule cur = ruleMapper.selectOne(new LambdaQueryWrapper<GraphRule>()
                .eq(GraphRule::getStatus, 1).orderByDesc(GraphRule::getVersion).last("limit 1"));
        int nextVer = (cur == null ? 1 : cur.getVersion() + 1);
        if (cur != null) {
            cur.setStatus(0);
            ruleMapper.updateById(cur);
        }
        GraphRule r = new GraphRule();
        r.setRuleName(String.valueOf(body.getOrDefault("ruleName", "成果关联图谱权重 v" + nextVer)));
        r.setActivityWeight(w[0]);
        r.setAchievementWeight(w[1]);
        r.setCourseWeight(w[2]);
        r.setLeaderWeight(w[3]);
        r.setLevelWeight(w[4]);
        r.setTimeWeight(w[5]);
        r.setVersion(nextVer);
        r.setStatus(1);
        r.setEffectTime(LocalDateTime.now());
        r.setCreateBy(UserContext.userId());
        r.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(r);
        return r.getId();
    }

    /**
     * 执行计算：
     * 六项因素各自归一化为 0–1，再按权重加权得到 0–100 分；
     * 缺失数据不参与计算并在结论中提示补录，相同输入与规则版本结果恒定。
     */
    @Transactional
    public Map<String, Object> evaluate(Long achievementId) {
        ResearchAchievement a = achievementMapper.selectById(achievementId);
        if (a == null) {
            throw new BizException("成果不存在");
        }
        GraphRule rule = activeRule();
        List<String> missing = new ArrayList<>();

        // 1 活动参与关系：本人参与已归档活动的次数，5 次封顶
        long actCount = 0;
        List<ActivityMember> ms = memberMapper.selectList(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getTeacherId, a.getTeacherId()));
        for (ActivityMember m : ms) {
            ResearchActivity ra = activityMapper.selectById(m.getActivityId());
            if (ra != null && "已归档".equals(ra.getStatus())) {
                actCount++;
            }
        }
        if (actCount == 0) {
            missing.add("活动参与关系");
        }
        double f1 = Math.min(actCount, 5) / 5.0;

        // 2 成果来源：是否关联来源活动
        boolean hasSource = a.getActivityId() != null;
        if (!hasSource) {
            missing.add("成果来源活动");
        }
        double f2 = hasSource ? 1.0 : 0.0;

        // 3 课程应用：应用课程数，3 门封顶
        int courses = a.getCourseCount() == null ? 0 : a.getCourseCount();
        if (courses < 3) {
            missing.add("课程应用不足 3 门");
        }
        double f3 = Math.min(courses, 3) / 3.0;

        // 4 负责人：组长/骨干加分
        TeachingGroup g = a.getGroupId() == null ? null : groupMapper.selectById(a.getGroupId());
        boolean leader = g != null && Objects.equals(g.getLeaderId(), a.getTeacherId());
        double f4 = leader ? 1.0 : 0.6;

        // 5 认定等级：校级 0.5 / 区级 0.65 / 市级 0.85 / 省级 1.0
        double f5 = switch (a.getLevel() == null ? "校级" : a.getLevel()) {
            case "省级" -> 1.0;
            case "市级" -> 0.85;
            case "区级" -> 0.65;
            default -> 0.5;
        };

        // 6 时间顺序：材料提交与活动时间的间隔，越晚提交折损越大
        int gapDays = 0;
        if (a.getActivityId() != null) {
            ResearchActivity src = activityMapper.selectById(a.getActivityId());
            if (src != null && a.getSubmitTime() != null) {
                gapDays = (int) Duration.between(src.getActivityTime(), a.getSubmitTime()).toDays();
            }
        }
        double f6 = gapDays <= 0 ? 1.0 : (gapDays <= 7 ? 0.85 : (gapDays <= 30 ? 0.6 : 0.3));
        if (gapDays > 30) {
            missing.add("材料提交晚于活动 " + gapDays + " 天");
        }

        double[] f = {f1, f2, f3, f4, f5, f6};
        int[] w = {rule.getActivityWeight(), rule.getAchievementWeight(), rule.getCourseWeight(),
                rule.getLeaderWeight(), rule.getLevelWeight(), rule.getTimeWeight()};
        String[] keys = {"activity", "source", "course", "leader", "level", "time"};
        String[] labels = {"活动参与关系", "成果来源", "课程应用", "负责人", "认定等级", "时间顺序"};
        Map<String, Object> detail = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < 6; i++) {
            double score = w[i] * f[i];
            total += score;
            detail.put(keys[i], round(score));
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("key", keys[i]);
            it.put("label", labels[i]);
            it.put("weight", w[i]);
            it.put("factor", round(f[i]));
            it.put("score", round(score));
            it.put("hint", i == 0 ? actCount + " 次已归档活动" : i == 1 ? (hasSource ? "已关联来源活动" : "未关联来源活动")
                    : i == 2 ? courses + " 门课程（建议 ≥3）" : i == 3 ? (leader ? "教研组长" : "组员")
                    : i == 4 ? (a.getLevel() == null ? "校级" : a.getLevel())
                    : (gapDays <= 0 ? "提交及时" : "晚于活动 " + gapDays + " 天"));
            items.add(it);
        }
        total = round(total);
        String level = total >= 85 ? "A" : (total >= 70 ? "B" : (total >= 55 ? "C" : "D"));
        String conclusion;
        if ("A".equals(level)) {
            conclusion = "推荐认定：材料完整、活动与课程关联充分";
        } else if ("B".equals(level)) {
            conclusion = "推荐认定：来源清晰，课程应用或时效性仍有提升空间";
        } else if ("C".equals(level)) {
            conclusion = "有条件推荐：需补充课程应用与材料时效证明";
        } else {
            conclusion = "暂不推荐：关键依据缺失，请先补录活动与课程关联";
        }

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("activityCount", actCount);
        inputs.put("hasSource", hasSource);
        inputs.put("courseCount", courses);
        inputs.put("leader", leader);
        inputs.put("level", a.getLevel());
        inputs.put("timeGap", gapDays);

        GraphScore s = new GraphScore();
        s.setAchievementId(achievementId);
        s.setInputs(toJson(inputs));
        s.setDetails(toJson(detail));
        s.setTotalScore(BigDecimal.valueOf(total));
        s.setResultLevel(level);
        s.setConclusion(conclusion);
        s.setRuleVersion(rule.getVersion());
        s.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(s);

        a.setGraphScore(BigDecimal.valueOf(total));
        a.setGraphLevel(level);
        achievementMapper.updateById(a);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scoreId", s.getId());
        out.put("totalScore", total);
        out.put("level", level);
        out.put("conclusion", conclusion);
        out.put("items", items);
        out.put("missing", missing);
        out.put("ruleVersion", rule.getVersion());
        out.put("weightSum", rule.getActivityWeight() + rule.getAchievementWeight() + rule.getCourseWeight()
                + rule.getLeaderWeight() + rule.getLevelWeight() + rule.getTimeWeight());
        out.put("improvements", buildImprovements(missing));
        out.put("inputSnapshot", inputs);
        return out;
    }

    private List<String> buildImprovements(List<String> missing) {
        List<String> tips = new ArrayList<>();
        if (missing.isEmpty()) {
            tips.add("各项依据齐全，保持材料归档时效即可");
        }
        for (String m : missing) {
            if (m.contains("活动参与")) {
                tips.add("补录至少 1 次已归档教研活动的参与记录");
            } else if (m.contains("来源")) {
                tips.add("在成果申报中选择对应的来源教研活动");
            } else if (m.contains("课程")) {
                tips.add("补充课程应用记录，建议达到 3 门及以上");
            } else if (m.contains("时间") || m.contains("晚于")) {
                tips.add("在活动归档前提交成果材料，可提升时间顺序得分");
            }
        }
        return tips;
    }

    public Map<String, Object> detail(Long achievementId) {
        ResearchAchievement a = achievementMapper.selectById(achievementId);
        if (a == null) {
            throw new BizException("成果不存在");
        }
        GraphScore latest = scoreMapper.selectOne(new LambdaQueryWrapper<GraphScore>()
                .eq(GraphScore::getAchievementId, achievementId).orderByDesc(GraphScore::getId).last("limit 1"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("achievement", a);
        out.put("score", latest);
        if (latest != null) {
            out.put("details", parse(latest.getDetails()));
            out.put("inputs", parse(latest.getInputs()));
        }
        out.put("adjusts", adjustMapper.selectList(new LambdaQueryWrapper<GraphAdjust>()
                .eq(GraphAdjust::getAchievementId, achievementId).orderByDesc(GraphAdjust::getCreateTime)));
        out.put("rule", activeRule());
        return out;
    }

    /** 人工调整：必须填写不少于 5 字原因，只新增修正记录，不覆盖原始计算结果 */
    @Transactional
    public void adjust(Long scoreId, String afterLevel, String reason) {
        GraphScore s = scoreMapper.selectById(scoreId);
        if (s == null) {
            throw new BizException("计算记录不存在");
        }
        if (afterLevel == null || !List.of("A", "B", "C", "D").contains(afterLevel)) {
            throw new BizException("调整等级只能是 A/B/C/D");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写不少于 5 个字的原因");
        }
        GraphAdjust adj = new GraphAdjust();
        adj.setScoreId(scoreId);
        adj.setAchievementId(s.getAchievementId());
        adj.setBeforeLevel(s.getResultLevel());
        adj.setAfterLevel(afterLevel);
        adj.setReason(reason.trim());
        adj.setOperatorId(UserContext.userId());
        adj.setCreateTime(LocalDateTime.now());
        adjustMapper.insert(adj);
        ResearchAchievement a = achievementMapper.selectById(s.getAchievementId());
        if (a != null) {
            // 成果上的等级跟随人工结论，但 graph_score 原始快照保持不变
            a.setGraphLevel(afterLevel);
            achievementMapper.updateById(a);
        }
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String s) {
        try {
            return json.readValue(s, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
