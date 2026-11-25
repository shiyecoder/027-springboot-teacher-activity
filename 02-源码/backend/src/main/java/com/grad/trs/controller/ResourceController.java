package com.grad.trs.controller;

import com.grad.trs.common.PageResult;
import com.grad.trs.common.R;
import com.grad.trs.entity.ResourceVersion;
import com.grad.trs.service.ResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 教研资源库接口：检索、新增、版本记录 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String subject,
                                                   @RequestParam(required = false) String grade,
                                                   @RequestParam(required = false) String resType,
                                                   @RequestParam(required = false) String auditStatus) {
        return R.ok(resourceService.page(page, size, keyword, subject, grade, resType, auditStatus));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(resourceService.save(body));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(resourceService.save(body));
    }

    @GetMapping("/{id}/versions")
    public R<List<ResourceVersion>> versions(@PathVariable Long id) {
        return R.ok(resourceService.versions(id));
    }

    @PostMapping("/{id}/version")
    public R<Void> addVersion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        resourceService.addVersion(id, body);
        return R.ok();
    }
}
