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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教研资源库：按学科、年级、主题、类型检索，重复上传生成新版本，保留历史版本可对比。
 */
@Service
public class ResourceService {

    private final ResourceItemMapper itemMapper;
    private final ResourceVersionMapper versionMapper;
    private final SysUserMapper userMapper;
    private final LogService logService;

    public ResourceService(ResourceItemMapper itemMapper, ResourceVersionMapper versionMapper,
                           SysUserMapper userMapper, LogService logService) {
        this.itemMapper = itemMapper;
        this.versionMapper = versionMapper;
        this.userMapper = userMapper;
        this.logService = logService;
    }

    public PageResult<Map<String, Object>> page(int page, int size, String keyword, String subject,
                                                String grade, String resType, String auditStatus) {
        LambdaQueryWrapper<ResourceItem> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(ResourceItem::getTitle, keyword).or().like(ResourceItem::getResNo, keyword));
        }
        if (subject != null && !subject.isBlank() && !"全部".equals(subject)) {
            w.eq(ResourceItem::getSubject, subject);
        }
        if (grade != null && !grade.isBlank() && !"全部".equals(grade)) {
            w.eq(ResourceItem::getGrade, grade);
        }
        if (resType != null && !resType.isBlank() && !"全部".equals(resType)) {
            w.eq(ResourceItem::getResType, resType);
        }
        if (auditStatus != null && !auditStatus.isBlank() && !"全部".equals(auditStatus)) {
            w.eq(ResourceItem::getAuditStatus, auditStatus);
        }
        w.orderByDesc(ResourceItem::getCreateTime);
        Page<ResourceItem> p = itemMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> names = new HashMap<>();
        userMapper.selectList(null).forEach(u -> names.put(u.getId(), u.getRealName()));
        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("resNo", r.getResNo());
            m.put("title", r.getTitle());
            m.put("resType", r.getResType());
            m.put("subject", r.getSubject());
            m.put("grade", r.getGrade());
            m.put("topic", r.getTopic());
            m.put("cover", r.getCover());
            m.put("ownerName", names.getOrDefault(r.getOwnerId(), "-"));
            m.put("downloadCount", r.getDownloadCount());
            m.put("auditStatus", r.getAuditStatus());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    @Transactional
    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        ResourceItem r = id == null ? new ResourceItem() : itemMapper.selectById(id);
        if (r == null) {
            throw new BizException("资源不存在");
        }
        String title = body.get("title") == null ? null : String.valueOf(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException("资源名称不能为空");
        }
        r.setTitle(title.trim());
        r.setResType(String.valueOf(body.getOrDefault("resType", "教案")));
        r.setSubject(String.valueOf(body.getOrDefault("subject", "综合")));
        r.setGrade(String.valueOf(body.getOrDefault("grade", "高一")));
        r.setTopic(String.valueOf(body.getOrDefault("topic", "")));
        r.setCover(String.valueOf(body.getOrDefault("cover", "")));
        if (r.getCover() != null && r.getCover().startsWith("http")) {
            throw new BizException("封面必须上传本地图片，不允许外链");
        }
        r.setGroupId(body.get("groupId") == null ? null : Long.valueOf(String.valueOf(body.get("groupId"))));
        r.setActivityId(body.get("activityId") == null ? null : Long.valueOf(String.valueOf(body.get("activityId"))));
        if (id == null) {
            r.setResNo("ZY-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")));
            r.setOwnerId(UserContext.userId());
            r.setDownloadCount(0);
            r.setStatus(1);
            r.setAuditStatus("待审核");
            r.setCreateTime(LocalDateTime.now());
            itemMapper.insert(r);
        } else {
            itemMapper.updateById(r);
        }
        logService.oper("资源库", id == null ? "新建" : "编辑", r.getResNo(), r.getTitle());
        return r.getId();
    }

    public List<ResourceVersion> versions(Long resourceId) {
        return versionMapper.selectList(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resourceId).orderByDesc(ResourceVersion::getVersion));
    }

    @Transactional
    public void addVersion(Long resourceId, Map<String, Object> body) {
        ResourceItem r = itemMapper.selectById(resourceId);
        if (r == null) {
            throw new BizException("资源不存在");
        }
        String path = body.get("filePath") == null ? null : String.valueOf(body.get("filePath"));
        if (path == null || path.isBlank() || path.startsWith("http")) {
            throw new BizException("请上传本地文件，不允许填写外部链接");
        }
        long count = versionMapper.selectCount(new LambdaQueryWrapper<ResourceVersion>()
                .eq(ResourceVersion::getResourceId, resourceId));
        ResourceVersion v = new ResourceVersion();
        v.setResourceId(resourceId);
        v.setVersion((int) count + 1);
        v.setFilePath(path);
        v.setFileName(String.valueOf(body.getOrDefault("fileName", "")));
        v.setChangeNote(String.valueOf(body.getOrDefault("changeNote", "内容更新")));
        v.setUploaderId(UserContext.userId());
        v.setCreateTime(LocalDateTime.now());
        versionMapper.insert(v);
        logService.oper("资源库", "新增版本", r.getResNo(), "v" + v.getVersion());
    }
}
