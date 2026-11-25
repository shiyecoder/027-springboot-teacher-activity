package com.grad.trs.controller;

import com.grad.trs.common.PageResult;
import com.grad.trs.common.R;
import com.grad.trs.service.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 教研活动接口：列表、详情、新建、状态流转、签到、分工、评课、反思、材料 */
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String actType,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long groupId,
                                                   @RequestParam(required = false) String start,
                                                   @RequestParam(required = false) String end) {
        return R.ok(activityService.page(page, size, keyword, actType, status, groupId, start, end));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(activityService.detail(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(activityService.save(body));
    }

    @PutMapping("/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(activityService.save(body));
    }

    /** 状态流转：目标状态与原因由前端传入，后端校验越级与必填原因 */
    @PostMapping("/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        activityService.transition(id, str(body.get("to")), str(body.get("reason")));
        return R.ok();
    }

    @PostMapping("/{id}/sign")
    public R<Map<String, Object>> sign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(activityService.sign(id, str(body.get("code")),
                body.get("teacherId") == null ? null : Long.valueOf(String.valueOf(body.get("teacherId")))));
    }

    @PostMapping("/{id}/member")
    public R<Void> addMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        activityService.addMember(id, Long.valueOf(String.valueOf(body.get("teacherId"))), str(body.get("roleType")));
        return R.ok();
    }

    @DeleteMapping("/member/{memberId}")
    public R<Void> removeMember(@PathVariable Long memberId) {
        activityService.removeMember(memberId);
        return R.ok();
    }

    @PostMapping("/evaluation")
    public R<Void> evaluation(@RequestBody Map<String, Object> body) {
        activityService.saveEvaluation(body);
        return R.ok();
    }

    @PostMapping("/reflection")
    public R<Void> reflection(@RequestBody Map<String, Object> body) {
        activityService.saveReflection(body);
        return R.ok();
    }

    @PostMapping("/material")
    public R<Void> material(@RequestBody Map<String, Object> body) {
        activityService.saveMaterial(body);
        return R.ok();
    }

    @GetMapping("/dicts")
    public R<Map<String, Object>> dicts() {
        return R.ok(Map.of(
                "actType", List.of("集体备课", "公开课", "听评课", "专题研讨", "课题分享"),
                "status", ActivityService.FLOW,
                "roleType", List.of("主持", "主讲", "记录", "评课人", "参与者")));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
