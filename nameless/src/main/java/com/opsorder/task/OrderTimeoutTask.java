package com.opsorder.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.opsorder.service.WorkOrderService;

import lombok.extern.slf4j.Slf4j;

/**
 * 超时工单释放任务。
 *
 * 每 30 秒扫一次 DB 中 deadline 已过期的 PROCESSING 工单，释放回抢单池。
 * 用 fixedDelay 而不是 fixedRate：上一轮没跑完不会叠起来。
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    private final WorkOrderService workOrderService;

    public OrderTimeoutTask(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void releaseTimeoutOrders() {
        try {
            int released = workOrderService.releaseTimeoutOrders();
            if (released > 0) {
                log.info("[超时释放] 本轮共释放 {} 张工单", released);
            }
        } catch (Exception e) {
            // 定时任务里必须兜住异常，否则整个调度线程会停掉
            log.error("[超时释放] 执行失败", e);
        }
    }
}
