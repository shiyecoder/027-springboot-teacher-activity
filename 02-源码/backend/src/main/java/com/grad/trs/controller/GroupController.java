package com.grad.trs.controller;

import com.grad.trs.common.R;
import com.grad.trs.service.GroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 教研组接口：组织列表与组内完成情况 */
@RestController
@RequestMapping("/api/group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(groupService.list());
    }
}
