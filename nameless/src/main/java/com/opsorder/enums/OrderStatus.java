package com.opsorder.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机。
 *
 * 所有状态变更都必须先经过 canTransferTo 校验，
 * 避免出现「已完成的单又被抢」「已取消的单被完成」这类脏数据。
 */
public enum OrderStatus {

    /** 待抢 */
    PENDING,
    /** 已抢到，处理中 */
    PROCESSING,
    /** 已完成 */
    COMPLETED,
    /** 已取消 */
    CANCELLED;

    /** 合法的状态流转表 */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(COMPLETED, PENDING),   // 回到 PENDING 表示放弃或超时释放
            COMPLETED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransferTo(OrderStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    /** 字符串转枚举，非法值返回 null */
    public static OrderStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (OrderStatus s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
