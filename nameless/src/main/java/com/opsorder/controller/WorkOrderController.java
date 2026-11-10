package com.opsorder.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsorder.common.Result;
import com.opsorder.service.WorkOrderService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;

/**
 * 工单抢单接口。
 * 角色：engineer（拥有 order:grab / order:complete 权限）
 */
@RestController
@RequestMapping("/order")
public class WorkOrderController {

    @Resource
    private WorkOrderService workOrderService;

    /** 抢单池 */
    @SaCheckPermission("order:grab")
    @GetMapping("/pool")
    public Result pool(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size) {
        return workOrderService.pagePool(page, size);
    }

    /** 抢单：成功 / 手慢了 / 你已经抢过 */
    @SaCheckPermission("order:grab")
    @PostMapping("/{id}/grab")
    public Result grab(@PathVariable Long id) {
        return workOrderService.grab(id, StpUtil.getLoginIdAsLong());
    }

    /** 我的工单 */
    @SaCheckPermission("order:grab")
    @GetMapping("/my")
    public Result my(@RequestParam(required = false) String status) {
        return workOrderService.pageMy(StpUtil.getLoginIdAsLong(), status);
    }

    /** 完成工单 */
    @SaCheckPermission("order:complete")
    @PostMapping("/{id}/complete")
    public Result complete(@PathVariable Long id) {
        return workOrderService.complete(id, StpUtil.getLoginIdAsLong());
    }

    /** 放弃工单，回到池子 */
    @SaCheckPermission("order:grab")
    @PostMapping("/{id}/release")
    public Result release(@PathVariable Long id) {
        return workOrderService.release(id, StpUtil.getLoginIdAsLong());
    }
}
