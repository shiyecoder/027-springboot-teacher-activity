package com.grad.trs.controller;

import com.grad.trs.common.R;
import com.grad.trs.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 院系教研看板：指标与图表数据由后端聚合，前端只做渲染 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }
}
