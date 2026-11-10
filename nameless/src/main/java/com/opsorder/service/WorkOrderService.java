package com.opsorder.service;

import com.opsorder.common.Result;

/**
 * 工单与抢单
 */
public interface WorkOrderService {

    /** 抢单池：只看 PENDING，按创建时间倒序分页 */
    Result pagePool(int page, int size);

    /** 我的工单 */
    Result pageMy(Long userId, String status);

    /** 抢单 */
    Result grab(Long orderId, Long userId);

    /** 完成工单（只能是当前处理人） */
    Result complete(Long orderId, Long userId);

    /** 放弃工单，回到池子（只能是当前处理人） */
    Result release(Long orderId, Long userId);

    /** 释放所有超时未处理的工单，返回释放数量（定时任务调用） */
    int releaseTimeoutOrders();
}
