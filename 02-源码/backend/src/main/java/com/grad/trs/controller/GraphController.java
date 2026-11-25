package com.grad.trs.controller;

import com.grad.trs.common.R;
import com.grad.trs.entity.GraphRule;
import com.grad.trs.service.GraphService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 创新点接口：图谱规则配置、计算、结果解释与人工修正 */
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/rules")
    public R<List<GraphRule>> rules() {
        return R.ok(graphService.rules());
    }

    @PostMapping("/rule")
    public R<Long> saveRule(@RequestBody Map<String, Object> body) {
        return R.ok(graphService.saveRule(body));
    }

    /** 执行计算并返回明细：推荐结论、主要因素、未满足条件、可改进项 */
    @PostMapping("/evaluate/{achievementId}")
    public R<Map<String, Object>> evaluate(@PathVariable Long achievementId) {
        return R.ok(graphService.evaluate(achievementId));
    }

    @GetMapping("/{achievementId}")
    public R<Map<String, Object>> detail(@PathVariable Long achievementId) {
        return R.ok(graphService.detail(achievementId));
    }

    @PostMapping("/adjust")
    public R<Void> adjust(@RequestBody Map<String, Object> body) {
        graphService.adjust(Long.valueOf(String.valueOf(body.get("scoreId"))),
                String.valueOf(body.get("afterLevel")),
                body.get("reason") == null ? null : String.valueOf(body.get("reason")));
        return R.ok();
    }
}
