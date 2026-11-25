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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教研活动服务：活动主档、分工与签到、听评课、材料归档、课例改进。
 * 状态机：草稿 → 已发布 → 报名中 → 进行中 → 待归档 → 已归档，
 * 只允许前进一步或退回一步（退回必须填写原因），越级流转一律拒绝。
 */
@Service
public class ActivityService {

    public static final List<String> FLOW = List.of("草稿", "已发布", "报名中", "进行中", "待归档", "已归档");

    private final ResearchActivityMapper activityMapper;
    private final ActivityMemberMapper memberMapper;
    private final ActivityMaterialMapper materialMapper;
    private final LessonEvaluationMapper evaluationMapper;
    private final LessonReflectionMapper reflectionMapper;
    private final TeachingGroupMapper groupMapper;
    private final SysUserMapper userMapper;
    private final FlowLogMapper flowLogMapper;
    private final CreditService creditService;
    private final LogService logService;

    public ActivityService(ResearchActivityMapper activityMapper, ActivityMemberMapper memberMapper,
                           ActivityMaterialMapper materialMapper, LessonEvaluationMapper evaluationMapper,
                           LessonReflectionMapper reflectionMapper, TeachingGroupMapper groupMapper,
                           SysUserMapper userMapper, FlowLogMapper flowLogMapper,
                           CreditService creditService, LogService logService) {
        this.activityMapper = activityMapper;
        this.memberMapper = memberMapper;
        this.materialMapper = materialMapper;
        this.evaluationMapper = evaluationMapper;
        this.reflectionMapper = reflectionMapper;
        this.groupMapper = groupMapper;
        this.userMapper = userMapper;
        this.flowLogMapper = flowLogMapper;
        this.creditService = creditService;
        this.logService = logService;
    }

    public Map<Long, String> userNames() {
        Map<Long, String> m = new HashMap<>();
        userMapper.selectList(null).forEach(u -> m.put(u.getId(), u.getRealName()));
        return m;
    }

    public Map<Long, String> groupNames() {
        Map<Long, String> m = new HashMap<>();
        groupMapper.selectList(null).forEach(g -> m.put(g.getId(), g.getGroupName()));
        return m;
    }

    /** 列表分页：支持关键词、类型、状态、教研组与日期区间筛选，按活动时间倒序 */
    public PageResult<Map<String, Object>> page(int page, int size, String keyword, String actType,
                                                String status, Long groupId, String start, String end) {
        LambdaQueryWrapper<ResearchActivity> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(ResearchActivity::getTitle, keyword).or().like(ResearchActivity::getActNo, keyword));
        }
        if (actType != null && !actType.isBlank() && !"全部".equals(actType)) {
            w.eq(ResearchActivity::getActType, actType);
        }
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            w.eq(ResearchActivity::getStatus, status);
        }
        if (groupId != null) {
            w.eq(ResearchActivity::getGroupId, groupId);
        }
        if (start != null && !start.isBlank()) {
            w.ge(ResearchActivity::getActivityTime, start + " 00:00:00");
        }
        if (end != null && !end.isBlank()) {
            w.le(ResearchActivity::getActivityTime, end + " 23:59:59");
        }
        w.orderByDesc(ResearchActivity::getActivityTime);
        Page<ResearchActivity> p = activityMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> names = userNames();
        Map<Long, String> groups = groupNames();
        List<Map<String, Object>> list = p.getRecords().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("actNo", a.getActNo());
            m.put("title", a.getTitle());
            m.put("actType", a.getActType());
            m.put("subject", a.getSubject());
            m.put("grade", a.getGrade());
            m.put("groupId", a.getGroupId());
            m.put("groupName", groups.getOrDefault(a.getGroupId(), "-"));
            m.put("hostName", names.getOrDefault(a.getHostId(), "-"));
            m.put("speakerName", names.getOrDefault(a.getSpeakerId(), "-"));
            m.put("activityTime", a.getActivityTime());
            m.put("location", a.getLocation());
            m.put("status", a.getStatus());
            m.put("signCount", a.getSignCount());
            m.put("memberCount", a.getMemberCount());
            m.put("cover", a.getCover());
            m.put("archivePass", a.getArchivePass());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    /** 详情：基础信息 + 分工签到 + 材料 + 评课 + 课例版本 + 时间轴，供详情页一次性渲染 */
    public Map<String, Object> detail(Long id) {
        ResearchActivity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在或已删除");
        }
        Map<Long, String> names = userNames();
        Map<Long, String> groups = groupNames();
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("id", a.getId());
        base.put("actNo", a.getActNo());
        base.put("title", a.getTitle());
        base.put("actType", a.getActType());
        base.put("subject", a.getSubject());
        base.put("grade", a.getGrade());
        base.put("groupId", a.getGroupId());
        base.put("groupName", groups.getOrDefault(a.getGroupId(), "-"));
        base.put("hostName", names.getOrDefault(a.getHostId(), "-"));
        base.put("speakerName", names.getOrDefault(a.getSpeakerId(), "-"));
        base.put("recorderName", names.getOrDefault(a.getRecorderId(), "-"));
        base.put("activityTime", a.getActivityTime());
        base.put("duration", a.getDuration());
        base.put("location", a.getLocation());
        base.put("signCode", a.getSignCode());
        base.put("signCount", a.getSignCount());
        base.put("memberCount", a.getMemberCount());
        base.put("cover", a.getCover());
        base.put("summary", a.getSummary());
        base.put("status", a.getStatus());
        base.put("archivePass", a.getArchivePass());
        base.put("planId", a.getPlanId());
        out.put("activity", base);

        List<Map<String, Object>> members = memberMapper.selectList(new LambdaQueryWrapper<ActivityMember>()
                        .eq(ActivityMember::getActivityId, id).orderByAsc(ActivityMember::getId))
                .stream().map(m -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("id", m.getId());
                    x.put("teacherId", m.getTeacherId());
                    x.put("teacherName", names.getOrDefault(m.getTeacherId(), "-"));
                    x.put("roleType", m.getRoleType());
                    x.put("signStatus", m.getSignStatus());
                    x.put("signTime", m.getSignTime());
                    x.put("materialDone", m.getMaterialDone());
                    x.put("conflictFlag", m.getConflictFlag());
                    x.put("remark", m.getRemark());
                    return x;
                }).collect(Collectors.toList());
        out.put("members", members);
        out.put("materials", materialMapper.selectList(new LambdaQueryWrapper<ActivityMaterial>()
                .eq(ActivityMaterial::getActivityId, id).orderByDesc(ActivityMaterial::getCreateTime)));
        out.put("evaluations", evaluationMapper.selectList(new LambdaQueryWrapper<LessonEvaluation>()
                .eq(LessonEvaluation::getActivityId, id).orderByDesc(LessonEvaluation::getCreateTime))
                .stream().map(e -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("id", e.getId());
                    x.put("evaluatorName", names.getOrDefault(e.getEvaluatorId(), "-"));
                    x.put("teacherName", names.getOrDefault(e.getTeacherId(), "-"));
                    x.put("targetScore", e.getTargetScore());
                    x.put("contentScore", e.getContentScore());
                    x.put("methodScore", e.getMethodScore());
                    x.put("interactScore", e.getInteractScore());
                    x.put("effectScore", e.getEffectScore());
                    x.put("totalScore", e.getTotalScore());
                    x.put("advantage", e.getAdvantage());
                    x.put("suggestion", e.getSuggestion());
                    x.put("createTime", e.getCreateTime());
                    return x;
                }).collect(Collectors.toList()));
        out.put("reflections", reflectionMapper.selectList(new LambdaQueryWrapper<LessonReflection>()
                .eq(LessonReflection::getActivityId, id).orderByDesc(LessonReflection::getVersion)));
        out.put("logs", flowLogMapper.selectList(new LambdaQueryWrapper<FlowLog>()
                .eq(FlowLog::getBizType, "ACTIVITY").eq(FlowLog::getBizId, id)
                .orderByAsc(FlowLog::getCreateTime)));
        // 归档校验结果：前端可直接展示缺什么材料
        out.put("archiveCheck", archiveCheck(a));
        return out;
    }

    /** 归档前置校验：签到、教案、课件、照片、记录，公开课/听评课另需评课表与反思 */
    public Map<String, Object> archiveCheck(ResearchActivity a) {
        List<ActivityMaterial> mats = materialMapper.selectList(new LambdaQueryWrapper<ActivityMaterial>()
                .eq(ActivityMaterial::getActivityId, a.getId()));
        Set<String> types = mats.stream().map(ActivityMaterial::getMatType).collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        if (a.getSignCount() == null || a.getSignCount() <= 0) {
            missing.add("签到记录");
        }
        for (String t : List.of("教案", "课件", "照片", "记录")) {
            if (!types.contains(t)) {
                missing.add(t);
            }
        }
        if ("公开课".equals(a.getActType()) || "听评课".equals(a.getActType())) {
            if (!types.contains("评课表")) {
                missing.add("评课表");
            }
            if (!types.contains("反思")) {
                missing.add("反思");
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("passed", missing.isEmpty());
        m.put("missing", missing);
        m.put("signCount", a.getSignCount());
        m.put("memberCount", a.getMemberCount());
        return m;
    }

    /** 新建/编辑活动：编号由后端生成，必填与唯一性在此校验 */
    @Transactional
    public Long save(Map<String, Object> body) {
        Long id = lng(body.get("id"));
        ResearchActivity a = id == null ? new ResearchActivity() : activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        String title = str(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException("活动主题不能为空");
        }
        if (title.trim().length() > 150) {
            throw new BizException("活动主题不能超过 150 个字符");
        }
        String actType = str(body.get("actType"));
        if (actType == null || !List.of("集体备课", "公开课", "听评课", "专题研讨", "课题分享").contains(actType)) {
            throw new BizException("教研类型不在字典范围内");
        }
        Long groupId = lng(body.get("groupId"));
        if (groupId == null) {
            throw new BizException("请选择所属教研组");
        }
        String time = str(body.get("activityTime"));
        if (time == null || time.isBlank()) {
            throw new BizException("请选择活动时间");
        }
        a.setTitle(title.trim());
        a.setActType(actType);
        a.setGroupId(groupId);
        a.setSubject(str(body.get("subject")));
        a.setGrade(str(body.get("grade")));
        a.setPlanId(lng(body.get("planId")));
        a.setHostId(lng(body.get("hostId")));
        a.setSpeakerId(lng(body.get("speakerId")));
        a.setRecorderId(lng(body.get("recorderId")));
        a.setActivityTime(LocalDateTime.parse(time.length() > 16 ? time : time + ":00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        a.setDuration(body.get("duration") == null ? 90 : Integer.valueOf(String.valueOf(body.get("duration"))));
        a.setLocation(str(body.get("location")));
        a.setSummary(str(body.get("summary")));
        a.setCover(str(body.get("cover")));
        if (id == null) {
            a.setActNo("HD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%03d", (activityMapper.selectCount(null) % 999) + 1));
            a.setStatus("草稿");
            a.setSignCode("S" + String.format("%05d", (int) (Math.random() * 99999)));
            a.setSignCount(0);
            a.setMemberCount(0);
            a.setArchivePass(0);
            a.setCreateBy(UserContext.userId());
            a.setCreateTime(LocalDateTime.now());
            activityMapper.insert(a);
            logService.record("ACTIVITY", a.getId(), a.getActNo(), null, "草稿", "新建", null);
            logService.oper("教研活动", "新建", a.getActNo(), title);
        } else {
            activityMapper.updateById(a);
            logService.oper("教研活动", "编辑", a.getActNo(), title);
        }
        return a.getId();
    }

    /**
     * 状态流转：只允许前进一步或退回一步，退回原因不少于 5 个字；
     * 归档前必须通过材料校验，归档成功后按规则生成学时台账。
     */
    @Transactional
    public void transition(Long id, String to, String reason) {
        ResearchActivity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        int cur = FLOW.indexOf(a.getStatus());
        int target = FLOW.indexOf(to);
        if (target < 0) {
            throw new BizException("目标状态不合法");
        }
        if (target != cur + 1 && target != cur - 1) {
            throw new BizException("不允许从「" + a.getStatus() + "」越级流转到「" + to + "」");
        }
        if (target < cur) {
            if (reason == null || reason.trim().length() < 5) {
                throw new BizException("退回必须填写不少于 5 个字的原因");
            }
        }
        if ("已归档".equals(to)) {
            Map<String, Object> check = archiveCheck(a);
            if (!Boolean.TRUE.equals(check.get("passed"))) {
                throw new BizException("材料不齐全，暂不能归档：缺 " + String.join("、", (List<String>) check.get("missing")));
            }
            a.setArchivePass(1);
        }
        String from = a.getStatus();
        a.setStatus(to);
        activityMapper.updateById(a);
        logService.record("ACTIVITY", id, a.getActNo(), from, to, "流转", reason);
        logService.oper("教研活动", to, a.getActNo(), "由「" + from + "」流转至「" + to + "」");
        if ("已归档".equals(to)) {
            // 归档即触发学时生成，保证台账与活动一致
            creditService.generateForActivity(id);
        }
    }

    /** 现场签到：校验验证码与参与关系，重复签到直接返回已签到 */
    @Transactional
    public Map<String, Object> sign(Long id, String code, Long teacherId) {
        ResearchActivity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        if (!"进行中".equals(a.getStatus()) && !"报名中".equals(a.getStatus())) {
            throw new BizException("当前状态不能签到");
        }
        if (code == null || !code.trim().equalsIgnoreCase(a.getSignCode())) {
            throw new BizException("签到验证码不正确");
        }
        Long uid = teacherId == null ? UserContext.userId() : teacherId;
        ActivityMember m = memberMapper.selectOne(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getActivityId, id).eq(ActivityMember::getTeacherId, uid).last("limit 1"));
        if (m == null) {
            throw new BizException("你不在本次活动的参与名单中");
        }
        if (m.getSignStatus() != null && m.getSignStatus() == 1) {
            throw new BizException("已签到，无需重复操作");
        }
        m.setSignStatus(1);
        m.setSignTime(LocalDateTime.now());
        memberMapper.updateById(m);
        long signed = memberMapper.selectCount(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getActivityId, id).eq(ActivityMember::getSignStatus, 1));
        a.setSignCount((int) signed);
        activityMapper.updateById(a);
        logService.oper("教研活动", "签到", a.getActNo(), "教师 " + uid + " 签到成功");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("signCount", signed);
        out.put("message", "签到成功");
        return out;
    }

    /** 添加分工：同一教师同一时段已有活动时标记时间冲突，由前端提示调整 */
    @Transactional
    public void addMember(Long id, Long teacherId, String roleType) {
        ResearchActivity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        if (teacherId == null) {
            throw new BizException("请选择教师");
        }
        long dup = memberMapper.selectCount(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getActivityId, id).eq(ActivityMember::getTeacherId, teacherId));
        if (dup > 0) {
            throw new BizException("该教师已在参与名单中");
        }
        int conflict = 0;
        List<ActivityMember> mine = memberMapper.selectList(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getTeacherId, teacherId));
        for (ActivityMember m : mine) {
            ResearchActivity other = activityMapper.selectById(m.getActivityId());
            if (other == null || other.getId().equals(id)) {
                continue;
            }
            long gap = Math.abs(java.time.Duration.between(other.getActivityTime(), a.getActivityTime()).toMinutes());
            if (gap < Math.max(other.getDuration() == null ? 90 : other.getDuration(), a.getDuration() == null ? 90 : a.getDuration())) {
                conflict = 1;
                break;
            }
        }
        ActivityMember m = new ActivityMember();
        m.setActivityId(id);
        m.setTeacherId(teacherId);
        m.setRoleType(roleType == null || roleType.isBlank() ? "参与者" : roleType);
        m.setSignStatus(0);
        m.setMaterialDone(0);
        m.setCredit(BigDecimal.ZERO);
        m.setConflictFlag(conflict);
        m.setRemark(conflict == 1 ? "该时段已有其他教研活动" : null);
        m.setCreateTime(LocalDateTime.now());
        memberMapper.insert(m);
        long total = memberMapper.selectCount(new LambdaQueryWrapper<ActivityMember>().eq(ActivityMember::getActivityId, id));
        a.setMemberCount((int) total);
        activityMapper.updateById(a);
    }

    @Transactional
    public void removeMember(Long memberId) {
        ActivityMember m = memberMapper.selectById(memberId);
        if (m == null) {
            throw new BizException("分工记录不存在");
        }
        memberMapper.deleteById(memberId);
        long total = memberMapper.selectCount(new LambdaQueryWrapper<ActivityMember>()
                .eq(ActivityMember::getActivityId, m.getActivityId()));
        ResearchActivity a = activityMapper.selectById(m.getActivityId());
        if (a != null) {
            a.setMemberCount((int) total);
            activityMapper.updateById(a);
        }
    }

    /** 保存听评课：五个维度各 0–20 分，总分由后端计算，禁止前端直接提交总分 */
    @Transactional
    public void saveEvaluation(Map<String, Object> body) {
        Long activityId = lng(body.get("activityId"));
        if (activityId == null) {
            throw new BizException("请选择听评课活动");
        }
        ResearchActivity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        Long teacherId = lng(body.get("teacherId"));
        if (teacherId == null) {
            teacherId = a.getSpeakerId();
        }
        Long evaluatorId = lng(body.get("evaluatorId"));
        if (evaluatorId == null) {
            evaluatorId = UserContext.userId();
        }
        if (Objects.equals(evaluatorId, teacherId)) {
            throw new BizException("不能给自己的课打分");
        }
        int[] sc = new int[5];
        String[] keys = {"targetScore", "contentScore", "methodScore", "interactScore", "effectScore"};
        int total = 0;
        for (int i = 0; i < keys.length; i++) {
            Integer v = body.get(keys[i]) == null ? null : Integer.valueOf(String.valueOf(body.get(keys[i])).split("\\.")[0]);
            if (v == null || v < 0 || v > 20) {
                throw new BizException("每个评分维度必须在 0–20 之间");
            }
            sc[i] = v;
            total += v;
        }
        LessonEvaluation e = new LessonEvaluation();
        e.setActivityId(activityId);
        e.setTeacherId(teacherId);
        e.setEvaluatorId(evaluatorId);
        e.setTargetScore(sc[0]);
        e.setContentScore(sc[1]);
        e.setMethodScore(sc[2]);
        e.setInteractScore(sc[3]);
        e.setEffectScore(sc[4]);
        e.setTotalScore(total);
        e.setAdvantage(str(body.get("advantage")));
        e.setSuggestion(str(body.get("suggestion")));
        e.setCreateTime(LocalDateTime.now());
        evaluationMapper.insert(e);
        logService.oper("听评课", "提交评课", a.getActNo(), "总分 " + total);
    }

    /** 课例改进：同一活动版本号自增，形成可对比的版本链 */
    @Transactional
    public void saveReflection(Map<String, Object> body) {
        Long activityId = lng(body.get("activityId"));
        ResearchActivity a = activityId == null ? null : activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        String content = str(body.get("content"));
        if (content == null || content.trim().length() < 5) {
            throw new BizException("反思内容不少于 5 个字");
        }
        long count = reflectionMapper.selectCount(new LambdaQueryWrapper<LessonReflection>()
                .eq(LessonReflection::getActivityId, activityId));
        LessonReflection r = new LessonReflection();
        r.setActivityId(activityId);
        r.setTeacherId(a.getSpeakerId());
        r.setVersion((int) count + 1);
        r.setContent(content.trim());
        r.setImprovePoints(str(body.get("improvePoints")));
        r.setCompareNote(str(body.get("compareNote")));
        r.setCreateTime(LocalDateTime.now());
        reflectionMapper.insert(r);
        logService.oper("课例改进", "提交反思", a.getActNo(), "版本 v" + r.getVersion());
    }

    /** 材料上传：只保存相对路径，类型限定在业务允许范围内 */
    @Transactional
    public void saveMaterial(Map<String, Object> body) {
        Long activityId = lng(body.get("activityId"));
        ResearchActivity a = activityId == null ? null : activityMapper.selectById(activityId);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        String type = str(body.get("matType"));
        if (type == null || !List.of("教案", "课件", "评课表", "照片", "记录", "反思").contains(type)) {
            throw new BizException("材料类型不合法");
        }
        String path = str(body.get("filePath"));
        if (path == null || path.isBlank() || path.startsWith("http")) {
            throw new BizException("材料必须上传本地文件，不允许填写外部链接");
        }
        long count = materialMapper.selectCount(new LambdaQueryWrapper<ActivityMaterial>()
                .eq(ActivityMaterial::getActivityId, activityId).eq(ActivityMaterial::getMatType, type));
        ActivityMaterial m = new ActivityMaterial();
        m.setActivityId(activityId);
        m.setMatType(type);
        m.setFilePath(path);
        m.setFileName(str(body.get("fileName")));
        m.setVersion((int) count + 1);
        m.setFileSize(body.get("fileSize") == null ? 0 : Integer.valueOf(String.valueOf(body.get("fileSize"))));
        m.setUploaderId(UserContext.userId());
        m.setStatus("待审核");
        m.setCreateTime(LocalDateTime.now());
        materialMapper.insert(m);
        logService.oper("材料归档", "上传", a.getActNo(), type + " v" + m.getVersion());
    }

    static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    static Long lng(Object o) {
        return o == null || "".equals(String.valueOf(o)) ? null : Long.valueOf(String.valueOf(o).split("\\.")[0]);
    }
}
