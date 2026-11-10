package com.opsorder.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.opsorder.common.Result;
import com.opsorder.entity.GrabRecord;
import com.opsorder.entity.WorkOrder;
import com.opsorder.enums.OrderStatus;
import com.opsorder.mapper.GrabRecordMapper;
import com.opsorder.mapper.WorkOrderMapper;
import com.opsorder.service.WorkOrderService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    /** 抢单后多少分钟未处理自动释放 */
    private static final int GRAB_TIMEOUT_MINUTES = 15;

    /** Redis 延时队列的 key，score 存到期时间戳 */
    private static final String TIMEOUT_KEY = "work:order:timeout";

    @Resource
    private GrabRecordMapper grabRecordMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result pagePool(int page, int size) {
        Page<WorkOrder> result = this.page(
                new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
                new QueryWrapper<WorkOrder>()
                        .eq("status", OrderStatus.PENDING.name())
                        .orderByDesc("create_time"));
        return Result.success(result);
    }

    @Override
    public Result pageMy(Long userId, String status) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>()
                .eq("assignee_id", userId)
                .orderByDesc("grab_time");
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.toUpperCase());
        }
        return Result.success(this.page(new Page<>(1, 50), wrapper));
    }

    /**
     * 抢单核心流程。
     *
     * 顺序有讲究：
     * ① 幂等判断必须最先做，否则用户双击时第二个请求会收到「手慢了」
     * ② 先查一次只为话术友好，不具备并发安全性
     * ③ 条件更新的影响行数才是唯一判定依据
     * ④ 流水只在成功时写
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result grab(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return Result.error(400, "参数不完整");
        }
        long start = System.currentTimeMillis();

        // ① 幂等：已经抢到过，直接返回成功
        GrabRecord exist = grabRecordMapper.selectOne(new QueryWrapper<GrabRecord>()
                .eq("order_id", orderId)
                .eq("user_id", userId)
                .last("LIMIT 1"));
        if (exist != null && Integer.valueOf(1).equals(exist.getSuccess())) {
            return Result.success("你已经抢到这一单了");
        }

        // ② 友好提示（非并发安全）
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            return Result.error(400, "工单不存在");
        }
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            return Result.error(409, "手慢了，这单已经被抢走");
        }

        // ③ 并发控制点：条件更新
        int affected = this.baseMapper.grabOrder(orderId, userId, GRAB_TIMEOUT_MINUTES);
        if (affected == 0) {
            return Result.error(409, "手慢了，这单已经被抢走");
        }

        // ④ 落抢单流水（唯一索引 uk_order_user 兜底）
        try {
            GrabRecord record = new GrabRecord();
            record.setOrderId(orderId);
            record.setUserId(userId);
            record.setSuccess(1);
            record.setCostMs((int) (System.currentTimeMillis() - start));
            grabRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 极端并发下同一用户两个请求同时通过前面的检查，唯一索引拦下第二个
            log.warn("重复抢单被唯一索引拦截 orderId={} userId={}", orderId, userId);
            return Result.success("你已经抢到这一单了");
        }

        // ⑤ 投递超时任务到 Redis ZSet
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(GRAB_TIMEOUT_MINUTES);
        stringRedisTemplate.opsForZSet().add(TIMEOUT_KEY, String.valueOf(orderId), expireAt);

        return Result.success("抢单成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result complete(Long orderId, Long userId) {
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            return Result.error(400, "工单不存在");
        }
        OrderStatus current = OrderStatus.of(order.getStatus());
        // 先校验归属再校验状态：越权操作优先返回 403，不暴露单据当前状态
        if (!userId.equals(order.getAssigneeId())) {
            return Result.error(403, "只能完成自己抢到的工单");
        }
        if (current == null || !current.canTransferTo(OrderStatus.COMPLETED)) {
            return Result.error(409, "当前状态不允许完成：" + order.getStatus());
        }

        int affected = this.baseMapper.completeOrder(orderId, userId);
        if (affected == 0) {
            return Result.error(409, "工单状态已变化，请刷新后重试");
        }
        stringRedisTemplate.opsForZSet().remove(TIMEOUT_KEY, String.valueOf(orderId));
        return Result.success("工单已完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result release(Long orderId, Long userId) {
        WorkOrder order = this.getById(orderId);
        if (order == null) {
            return Result.error(400, "工单不存在");
        }
        if (!userId.equals(order.getAssigneeId())) {
            return Result.error(403, "只能放弃自己抢到的工单");
        }

        int affected = this.baseMapper.releaseOrder(orderId);
        if (affected == 0) {
            return Result.error(409, "工单状态已变化，请刷新后重试");
        }
        stringRedisTemplate.opsForZSet().remove(TIMEOUT_KEY, String.valueOf(orderId));
        log.info("工单 {} 被用户 {} 主动放弃", orderId, userId);
        return Result.success("已放弃，工单回到抢单池");
    }

    /**
     * 超时释放。
     *
     * 先按 DB 的 deadline 兜底扫描，再清掉对应的 ZSet 成员。
     * 之所以不直接依赖 ZSet：Redis 数据可能丢，DB 才是最终事实来源。
     */
    @Override
    public int releaseTimeoutOrders() {
        List<Long> timeoutIds = this.baseMapper.selectTimeoutOrderIds(100);
        if (timeoutIds == null || timeoutIds.isEmpty()) {
            return 0;
        }
        int released = 0;
        for (Long id : timeoutIds) {
            // releaseOrder 里的 status='PROCESSING' 保证重复执行也只释放一次
            if (this.baseMapper.releaseOrder(id) > 0) {
                released++;
                log.info("工单 {} 超时未处理，已自动释放回池", id);
            }
            stringRedisTemplate.opsForZSet().remove(TIMEOUT_KEY, String.valueOf(id));
        }
        return released;
    }
}
