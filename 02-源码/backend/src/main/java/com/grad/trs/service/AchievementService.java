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
 * 教研成果：教师提交 → 组长初审 → 教务复审 → 专家评审 → 认定。
 * 提交人不得作为任何环节的审核人；退回必须填写不少于 5 字的原因。
 */
@Service
public class AchievementService {

    public static final List<String> FLOW = List.of("草稿", "组长初审", "教务复审", "专家评审", "已认定");

    private final ResearchAchievementMapper achMapper;
    private final AchievementReviewMapper reviewMapper;
    private final FlowLogMapper flowLogMapper;
    private final ResearchActivityMapper activityMapper;
    private final TeachingGroupMapper groupMapper;
    private final SysUserMapper userMapper;
    private final LogService logService;

    public AchievementService(ResearchAchievementMapper achMapper, AchievementReviewMapper reviewMapper,
                              FlowLogMapper flowLogMapper, ResearchActivityMapper activityMapper,
                              TeachingGroupMapper groupMapper, SysUserMapper userMapper, LogService logService) {
        this.achMapper = achMapper;
        this.reviewMapper = reviewMapper;
        this.flowLogMapper = flowLogMapper;
        this.activityMapper = activityMapper;
        this.groupMapper = groupMapper;
        this.userMapper = userMapper;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String keyword, String achType,
                                                String status, String level, Long teacherId) {
        LambdaQueryWrapper<ResearchAchievement> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(ResearchAchievement::getTitle, keyword).or().like(ResearchAchievement::getAchNo, keyword));
        }
        if (achType != null && !achType.isBlank() && !"全部".equals(achType)) {
            w.eq(ResearchAchievement::getAchType, achType);
        }
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            w.eq(ResearchAchievement::getStatus, status);
        }
        if (level != null && !level.isBlank() && !"全部".equals(level)) {
            w.eq(ResearchAchievement::getLevel, level);
        }
        if (teacherId != null) {
            w.eq(ResearchAchievement::getTeacherId, teacherId);
        }
        w.orderByDesc(ResearchAchievement::getCreateTime);
        Page<ResearchAchievement> p = achMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        List<Map<String, Object>> list = p.getRecords().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("achNo", a.getAchNo());
            m.put("title", a.getTitle());
            m.put("achType", a.getAchType());
            m.put("teacherId", a.getTeacherId());
            m.put("teacherName", names.getOrDefault(a.getTeacherId(), "-"));
            m.put("level", a.getLevel());
            m.put("status", a.getStatus());
            m.put("graphScore", a.getGraphScore());
            m.put("graphLevel", a.getGraphLevel());
            m.put("cover", a.getCover());
            m.put("courseCount", a.getCourseCount());
            m.put("submitTime", a.getSubmitTime());
            m.put("credit", a.getCredit());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    public Map<String, Object> detail(Long id) {
        ResearchAchievement a = achMapper.selectById(id);
        if (a == null) {
            throw new BizException("成果不存在");
        }
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("id", a.getId());
        base.put("achNo", a.getAchNo());
        base.put("title", a.getTitle());
        base.put("achType", a.getAchType());
        base.put("teacherName", names.getOrDefault(a.getTeacherId(), "-"));
        base.put("groupId", a.getGroupId());
        base.put("activityId", a.getActivityId());
        ResearchActivity act = a.getActivityId() == null ? null : activityMapper.selectById(a.getActivityId());
        base.put("activityTitle", act == null ? "-" : act.getTitle());
        base.put("activityNo", act == null ? "-" : act.getActNo());
        base.put("level", a.getLevel());
        base.put("cover", a.getCover());
        base.put("filePath", a.getFilePath());
        base.put("courseCount", a.getCourseCount());
        base.put("status", a.getStatus());
        base.put("submitTime", a.getSubmitTime());
        base.put("firstOpinion", a.getFirstOpinion());
        base.put("finalOpinion", a.getFinalOpinion());
        base.put("expertScore", a.getExpertScore());
        base.put("graphScore", a.getGraphScore());
        base.put("graphLevel", a.getGraphLevel());
        base.put("credit", a.getCredit());
        out.put("achievement", base);
        out.put("reviews", reviewMapper.selectList(new LambdaQueryWrapper<AchievementReview>()
                .eq(AchievementReview::getAchievementId, id).orderByAsc(AchievementReview::getCreateTime))
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("stage", r.getStage());
                    m.put("reviewerName", names.getOrDefault(r.getReviewerId(), "-"));
                    m.put("result", r.getResult());
                    m.put("score", r.getScore());
                    m.put("opinion", r.getOpinion());
                    m.put("createTime", r.getCreateTime());
                    return m;
                }).collect(Collectors.toList()));
        out.put("logs", flowLogMapper.selectList(new LambdaQueryWrapper<FlowLog>()
                .eq(FlowLog::getBizType, "ACHIEVEMENT").eq(FlowLog::getBizId, id)
                .orderByAsc(FlowLog::getCreateTime)));
        return out;
    }

    @Transactional
    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        ResearchAchievement a = id == null ? new ResearchAchievement() : achMapper.selectById(id);
        if (a == null) {
            throw new BizException("成果不存在");
        }
        if (id != null && !List.of("草稿", "已退回").contains(a.getStatus())) {
            throw new BizException("审核中的成果不允许编辑");
        }
        String title = body.get("title") == null ? null : String.valueOf(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException("成果名称不能为空");
        }
        a.setTitle(title.trim());
        a.setAchType(String.valueOf(body.getOrDefault("achType", "课例")));
        a.setGroupId(body.get("groupId") == null ? null : Long.valueOf(String.valueOf(body.get("groupId"))));
        a.setActivityId(body.get("activityId") == null ? null : Long.valueOf(String.valueOf(body.get("activityId"))));
        a.setLevel(String.valueOf(body.getOrDefault("level", "校级")));
        a.setCover(String.valueOf(body.getOrDefault("cover", "")));
        a.setFilePath(String.valueOf(body.getOrDefault("filePath", "")));
        if ((a.getCover() != null && a.getCover().startsWith("http")) || (a.getFilePath() != null && a.getFilePath().startsWith("http"))) {
            throw new BizException("成果材料必须上传本地文件，不允许外部链接");
        }
        a.setCourseCount(body.get("courseCount") == null ? 0 : Integer.valueOf(String.valueOf(body.get("courseCount"))));
        if (id == null) {
            a.setAchNo("CG-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-"
                    + String.format("%04d", (achMapper.selectCount(null) % 9999) + 1));
            a.setTeacherId(UserContext.userId());
            a.setStatus("草稿");
            a.setCredit(BigDecimal.ZERO);
            a.setCreateTime(LocalDateTime.now());
            achMapper.insert(a);
        } else {
            achMapper.updateById(a);
        }
        logService.oper("成果申报", id == null ? "新建" : "编辑", a.getAchNo(), a.getTitle());
        return a.getId();
    }

    /** 提交：草稿/已退回 → 组长初审 */
    @Transactional
    public void submit(Long id) {
        ResearchAchievement a = achMapper.selectById(id);
        if (a == null) {
            throw new BizException("成果不存在");
        }
        if (!List.of("草稿", "已退回").contains(a.getStatus())) {
            throw new BizException("当前状态不能重复提交");
        }
        if (a.getActivityId() == null) {
            throw new BizException("请先选择成果来源教研活动");
        }
        String from = a.getStatus();
        a.setStatus("组长初审");
        a.setSubmitTime(LocalDateTime.now());
        achMapper.updateById(a);
        logService.record("ACHIEVEMENT", id, a.getAchNo(), from, "组长初审", "提交", null);
        logService.oper("成果申报", "提交", a.getAchNo(), a.getTitle());
    }

    /**
     * 审核：按当前阶段推进，通过则进入下一阶段，退回则回到已退回；
     * 提交人自审会被直接拒绝。
     */
    @Transactional
    public void audit(Long id, boolean pass, String opinion, BigDecimal score) {
        ResearchAchievement a = achMapper.selectById(id);
        if (a == null) {
            throw new BizException("成果不存在");
        }
        int idx = FLOW.indexOf(a.getStatus());
        if (idx <= 0) {
            throw new BizException("当前状态不在审核流程中");
        }
        if (Objects.equals(a.getTeacherId(), UserContext.userId())) {
            throw new BizException("不能审核本人提交的成果");
        }
        if (!pass && (opinion == null || opinion.trim().length() < 5)) {
            throw new BizException("退回必须填写不少于 5 个字的原因");
        }
        String stage = a.getStatus();
        String from = a.getStatus();
        String to;
        if (pass) {
            to = idx + 1 < FLOW.size() ? FLOW.get(idx + 1) : "已认定";
        } else {
            to = "已退回";
        }
        if ("组长初审".equals(stage)) {
            a.setFirstAuditBy(UserContext.userId());
            a.setFirstAuditTime(LocalDateTime.now());
            a.setFirstOpinion(opinion);
        } else {
            a.setFinalAuditBy(UserContext.userId());
            a.setFinalAuditTime(LocalDateTime.now());
            a.setFinalOpinion(opinion);
        }
        if (score != null) {
            a.setExpertScore(score);
        }
        if ("已认定".equals(to)) {
            // 认定学时按等级系数折算，作为台账来源之一
            BigDecimal base = switch (a.getLevel() == null ? "校级" : a.getLevel()) {
                case "省级" -> BigDecimal.valueOf(4.0);
                case "市级" -> BigDecimal.valueOf(3.0);
                case "区级" -> BigDecimal.valueOf(2.0);
                default -> BigDecimal.valueOf(1.5);
            };
            a.setCredit(base);
        }
        a.setStatus(to);
        achMapper.updateById(a);

        AchievementReview r = new AchievementReview();
        r.setAchievementId(id);
        r.setReviewerId(UserContext.userId());
        r.setStage(stage);
        r.setResult(pass ? "通过" : "退回");
        r.setScore(score);
        r.setOpinion(opinion);
        r.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(r);
        logService.record("ACHIEVEMENT", id, a.getAchNo(), from, to, pass ? "审核通过" : "退回", opinion);
        logService.oper("成果申报", stage + (pass ? "通过" : "退回"), a.getAchNo(), opinion);
    }
}
