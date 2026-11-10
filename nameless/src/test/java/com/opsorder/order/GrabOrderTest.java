package com.opsorder.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.opsorder.common.Result;
import com.opsorder.entity.GrabRecord;
import com.opsorder.entity.WorkOrder;
import com.opsorder.mapper.GrabRecordMapper;
import com.opsorder.mapper.WorkOrderMapper;
import com.opsorder.nameless.NamelessApplication;
import com.opsorder.service.WorkOrderService;

/**
 * 抢单并发测试。
 *
 * 注意：这里不能加 @Transactional —— 并发线程各自开事务，
 * 测试方法的事务回滚对它们不起作用，加了两边都看不到对方的数据。
 * 所以测试数据用 JdbcTemplate 物理删除来清理。
 */
@SpringBootTest(classes = NamelessApplication.class)
class GrabOrderTest {

    private static final String ORDER_PREFIX = "TEST_";

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private GrabRecordMapper grabRecordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 造一张待抢工单 */
    private Long createPendingOrder(String suffix) {
        String orderNo = ORDER_PREFIX + suffix;
        jdbcTemplate.update("DELETE FROM grab_record WHERE order_id IN "
                + "(SELECT id FROM work_order WHERE order_no = ?)", orderNo);
        jdbcTemplate.update("DELETE FROM work_order WHERE order_no = ?", orderNo);
        jdbcTemplate.update("INSERT INTO work_order "
                + "(order_no, title, type, status, version, create_time, update_time, deleted) "
                + "VALUES (?, ?, '测试', 'PENDING', 0, NOW(), NOW(), 0)", orderNo, "测试工单-" + suffix);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM work_order WHERE order_no = ?", Long.class, orderNo);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM grab_record WHERE order_id IN "
                + "(SELECT id FROM work_order WHERE order_no LIKE ?)", ORDER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM work_order WHERE order_no LIKE ?", ORDER_PREFIX + "%");
    }

    @Test
    @DisplayName("100 个线程同时抢同一单，只能有一个成功")
    void concurrentGrabOnlyOneWins() throws Exception {
        Long orderId = createPendingOrder("CONCURRENT");
        int threads = 100;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger grabbed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            long userId = 9000L + i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    Result result = workOrderService.grab(orderId, userId);
                    if (Boolean.TRUE.equals(result.getSuccess())
                            && "抢单成功".equals(result.getData())) {
                        grabbed.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // 抢失败属于预期
                }
            });
        }

        assertTrue(ready.await(20, TimeUnit.SECONDS), "线程未全部就绪");
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "并发任务未在 60s 内结束");

        assertEquals(1, grabbed.get(), "只能有一个人抢到");
        WorkOrder order = workOrderMapper.selectById(orderId);
        assertEquals("PROCESSING", order.getStatus());
        assertNotNull(order.getAssigneeId(), "抢到后必须有处理人");
        assertEquals(1L, grabRecordMapper.selectCount(
                new QueryWrapper<GrabRecord>().eq("order_id", orderId)), "抢单流水只能有一条");
    }

    @Test
    @DisplayName("同一个人重复抢同一单，第二次返回已抢到")
    void grabTwiceReturnsAlreadyGrabbed() {
        Long orderId = createPendingOrder("TWICE");

        Result first = workOrderService.grab(orderId, 8001L);
        assertTrue(first.getSuccess());
        assertEquals("抢单成功", first.getData());

        Result second = workOrderService.grab(orderId, 8001L);
        assertTrue(second.getSuccess());
        assertEquals("你已经抢到这一单了", second.getData());

        assertEquals(1L, grabRecordMapper.selectCount(
                new QueryWrapper<GrabRecord>().eq("order_id", orderId)), "幂等后流水仍只有一条");
    }

    @Test
    @DisplayName("单子被抢走后，别人再抢会失败")
    void grabTakenOrderShouldFail() {
        Long orderId = createPendingOrder("TAKEN");

        assertTrue(workOrderService.grab(orderId, 8002L).getSuccess());

        Result loser = workOrderService.grab(orderId, 8003L);
        assertFalse(loser.getSuccess());
        assertEquals(409, loser.getStatusCode());
    }

    @Test
    @DisplayName("只能完成自己抢到的工单")
    void completeOnlyByAssignee() {
        Long orderId = createPendingOrder("COMPLETE");
        workOrderService.grab(orderId, 8004L);

        Result byOther = workOrderService.complete(orderId, 8005L);
        assertFalse(byOther.getSuccess(), "别人不能完成");

        Result byOwner = workOrderService.complete(orderId, 8004L);
        assertTrue(byOwner.getSuccess());
        assertEquals("COMPLETED", workOrderMapper.selectById(orderId).getStatus());
    }

    @Test
    @DisplayName("放弃后工单回到池子，别人可以再抢")
    void releaseThenOthersCanGrab() {
        Long orderId = createPendingOrder("RELEASE");
        workOrderService.grab(orderId, 8006L);

        Result released = workOrderService.release(orderId, 8006L);
        assertTrue(released.getSuccess());

        WorkOrder afterRelease = workOrderMapper.selectById(orderId);
        assertEquals("PENDING", afterRelease.getStatus());
        assertEquals(null, afterRelease.getAssigneeId());

        assertTrue(workOrderService.grab(orderId, 8007L).getSuccess(), "释放后应能被别人抢到");
    }

    @Test
    @DisplayName("超时未处理的工单会被自动释放")
    void timeoutOrdersShouldBeReleased() {
        Long orderId = createPendingOrder("TIMEOUT");
        workOrderService.grab(orderId, 8008L);

        // 把截止时间改到过去，模拟超时
        jdbcTemplate.update("UPDATE work_order SET deadline = DATE_SUB(NOW(), INTERVAL 1 MINUTE) "
                + "WHERE id = ?", orderId);

        int released = workOrderService.releaseTimeoutOrders();
        assertTrue(released >= 1, "至少应释放 1 张超时工单");

        WorkOrder order = workOrderMapper.selectById(orderId);
        assertEquals("PENDING", order.getStatus());
        assertEquals(null, order.getAssigneeId());
    }
}
