package com.grad.trs.controller;

import com.grad.trs.common.PageResult;
import com.grad.trs.common.R;
import com.grad.trs.service.AchievementService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 教研成果接口：申报、提交、三级审核、详情与评审记录 */
@RestController
@RequestMapping("/api/achievement")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String achType,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String level,
                                                   @RequestParam(required = false) Long teacherId) {
        return R.ok(achievementService.page(page, size, keyword, achType, status, level, teacherId));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(achievementService.detail(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(achievementService.save(body));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(achievementService.save(body));
    }

    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        achievementService.submit(id);
        return R.ok();
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = body.get("pass") != null && Boolean.parseBoolean(String.valueOf(body.get("pass")));
        BigDecimal score = body.get("score") == null || "".equals(String.valueOf(body.get("score")))
                ? null : new BigDecimal(String.valueOf(body.get("score")));
        achievementService.audit(id, pass, body.get("opinion") == null ? null : String.valueOf(body.get("opinion")), score);
        return R.ok();
    }
}
