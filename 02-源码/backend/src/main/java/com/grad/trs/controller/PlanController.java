package com.grad.trs.controller;

import com.grad.trs.common.PageResult;
import com.grad.trs.common.R;
import com.grad.trs.service.PlanService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 学期计划接口：编制、提交、审批 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long groupId,
                                                   @RequestParam(required = false) String term) {
        return R.ok(planService.page(page, size, status, groupId, term));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(planService.save(body));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(planService.save(body));
    }

    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        planService.submit(id);
        return R.ok();
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = body.get("pass") != null && Boolean.parseBoolean(String.valueOf(body.get("pass")));
        planService.audit(id, pass, body.get("opinion") == null ? null : String.valueOf(body.get("opinion")));
        return R.ok();
    }
}
