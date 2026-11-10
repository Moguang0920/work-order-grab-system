package com.opsorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsorder.entity.WorkOrder;

/**
 * 工单 Mapper。
 *
 * 这里的 @Update 方法就是抢单的并发控制点：
 * 「靠 WHERE 条件 + 影响行数」保证一单只能被一个人抢到，
 * 不需要分布式锁。
 */
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /**
     * 抢单：条件更新。
     *
     * WHERE 里的 status='PENDING' 和 assignee_id IS NULL 是关键，
     * 数据库行锁会把并发请求串行化，只有一个能命中并返回 1。
     *
     * @return 1 抢到；0 已被别人抢走或状态不对
     */
    @Update("UPDATE work_order SET assignee_id = #{userId}, status = 'PROCESSING', "
            + "grab_time = NOW(), deadline = DATE_ADD(NOW(), INTERVAL #{minutes} MINUTE), "
            + "version = version + 1 "
            + "WHERE id = #{orderId} AND status = 'PENDING' AND assignee_id IS NULL AND deleted = 0")
    int grabOrder(@Param("orderId") Long orderId,
                  @Param("userId") Long userId,
                  @Param("minutes") int minutes);

    /**
     * 释放工单：回到待抢池。
     * 只处理 PROCESSING 状态的单，重复调用返回 0，天然幂等。
     */
    @Update("UPDATE work_order SET assignee_id = NULL, status = 'PENDING', "
            + "grab_time = NULL, deadline = NULL, version = version + 1 "
            + "WHERE id = #{orderId} AND status = 'PROCESSING' AND deleted = 0")
    int releaseOrder(@Param("orderId") Long orderId);

    /**
     * 完成工单：必须是当前处理人本人。
     */
    @Update("UPDATE work_order SET status = 'COMPLETED', version = version + 1 "
            + "WHERE id = #{orderId} AND assignee_id = #{userId} AND status = 'PROCESSING' AND deleted = 0")
    int completeOrder(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 超时未处理的工单（定时任务用）。加上 limit，避免一次捞太多。
     */
    @Select("SELECT id FROM work_order WHERE status = 'PROCESSING' "
            + "AND deadline IS NOT NULL AND deadline < NOW() AND deleted = 0 "
            + "ORDER BY deadline ASC LIMIT #{limit}")
    List<Long> selectTimeoutOrderIds(@Param("limit") int limit);
}
