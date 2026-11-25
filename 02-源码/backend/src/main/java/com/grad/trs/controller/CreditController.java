package com.grad.trs.controller;

import com.grad.trs.common.PageResult;
import com.grad.trs.common.R;
import com.grad.trs.entity.CreditRule;
import com.grad.trs.service.CreditService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 学时接口：规则、台账、确认、申诉与处理 */
@RestController
@RequestMapping("/api/credit")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/rules")
    public R<List<CreditRule>> rules() {
        return R.ok(creditService.rules());
    }

    @PostMapping("/rule")
    public R<Long> saveRule(@RequestBody Map<String, Object> body) {
        return R.ok(creditService.saveRule(body));
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long teacherId,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(creditService.page(page, size, status, teacherId, keyword));
    }

    @PostMapping("/confirm")
    public R<Integer> confirm(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        List<Long> ids = raw == null ? List.of() : raw.stream().map(x -> Long.valueOf(String.valueOf(x))).toList();
        return R.ok(creditService.confirm(ids));
    }

    @GetMapping("/appeal/page")
    public R<PageResult<Map<String, Object>>> appeals(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(required = false) String status) {
        return R.ok(creditService.appeals(page, size, status));
    }

    @PostMapping("/appeal")
    public R<Void> appeal(@RequestBody Map<String, Object> body) {
        BigDecimal expect = body.get("expectCredit") == null ? null : new BigDecimal(String.valueOf(body.get("expectCredit")));
        creditService.appeal(Long.valueOf(String.valueOf(body.get("ledgerId"))),
                body.get("reason") == null ? null : String.valueOf(body.get("reason")), expect);
        return R.ok();
    }

    @PostMapping("/appeal/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        creditService.handleAppeal(id, String.valueOf(body.get("status")),
                body.get("reply") == null ? null : String.valueOf(body.get("reply")));
        return R.ok();
    }

    /** 手动为指定活动补生成学时（归档自动生成失败时的补救入口） */
    @PostMapping("/generate/{activityId}")
    public R<Integer> generate(@PathVariable Long activityId) {
        return R.ok(creditService.generateForActivity(activityId));
    }
}
