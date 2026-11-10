package com.opsorder.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsorder.mapper.PermissionMapper;
import com.opsorder.mapper.RoleMapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户角色/权限缓存。
 *
 * 为什么需要它：Sa-Token 每次做 @SaCheckRole / @SaCheckPermission 校验时都会回调
 * StpInterface，而那里是「用户->角色->权限」三表 JOIN 实时查库。压测时同一个用户
 * 每秒 6 个请求，权限就被重复查 6 次，成了抢单池接口的瓶颈。
 *
 * 设计要点：
 * 1. 角色和权限存在同一个 key 里（一次 GET 拿全），因为 /role/me 这类接口会两个都取；
 * 2. 用 StringRedisTemplate + Jackson 存可读 JSON，不用 RedisTemplate 的默认类型信息；
 * 3. TTL 30 分钟只是兜底——正常靠写操作主动删，TTL 负责兜住「漏删」；
 * 4. Redis 读失败时降级查库，缓存问题不能把鉴权打成 500。
 */
@Slf4j
@Component
public class UserAuthCache {

    private static final String KEY_PREFIX = "auth:user:";

    /**
     * 自己持有一个 ObjectMapper，不走注入。
     * Spring Boot 4 默认自动配置的是 Jackson 3（tools.jackson.databind.ObjectMapper），
     * 容器里没有 com.fasterxml.jackson.databind.ObjectMapper 这个 Bean。
     * 这里只需要最简单的序列化能力，直接用 Jackson 2 的实例最省事（线程安全，可复用）。
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 兜底过期时间 */
    private static final Duration TTL = Duration.ofMinutes(30);

    /** 在 TTL 上叠加 0~5 分钟随机抖动，避免大批 key 同时过期一起回源砸库 */
    private static final int TTL_JITTER_SECONDS = 300;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    /** 缓存内容：一个用户拥有的角色标识与权限标识 */
    public record UserAuth(List<String> roles, List<String> perms) {

        public UserAuth {
            roles = roles == null ? Collections.emptyList() : roles;
            perms = perms == null ? Collections.emptyList() : perms;
        }

        public static UserAuth empty() {
            return new UserAuth(Collections.emptyList(), Collections.emptyList());
        }
    }

    /** 读缓存，未命中则回源查库并回填 */
    public UserAuth load(Long userId) {
        String key = KEY_PREFIX + userId;

        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            // 注意用 null 判断命中，不能判空集合：没有权限的用户也是有效缓存（空数组）
            if (json != null) {
                return MAPPER.readValue(json, UserAuth.class);
            }
        } catch (Exception e) {
            // 缓存读失败不能影响鉴权，直接降级查库
            log.warn("读取权限缓存失败，降级查库 userId={}", userId, e);
            return queryFromDb(userId);
        }

        UserAuth auth = queryFromDb(userId);
        try {
            int ttl = (int) TTL.toSeconds() + ThreadLocalRandom.current().nextInt(TTL_JITTER_SECONDS);
            stringRedisTemplate.opsForValue()
                    .set(key, MAPPER.writeValueAsString(auth), Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("写入权限缓存失败 userId={}", userId, e);
        }
        return auth;
    }

    private UserAuth queryFromDb(Long userId) {
        return new UserAuth(
                roleMapper.selectRoleCodesByUserId(userId),
                permissionMapper.selectPermCodesByUserId(userId));
    }

    /** 直接删除某个用户的缓存 */
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            // 删不掉就只能等 TTL 兜底，不影响业务
            log.warn("删除权限缓存失败 userId={}", userId, e);
        }
    }

    /** 批量删除（角色变更时，该角色下所有用户都要失效） */
    public void evictByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        try {
            List<String> keys = userIds.stream()
                    .filter(Objects::nonNull)
                    .map(id -> KEY_PREFIX + id)
                    .toList();
            stringRedisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("批量删除权限缓存失败 userIds={}", userIds, e);
        }
    }

    /** 角色权限变更：先查出该角色下的用户，再批量失效 */
    public void evictByRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        evictByIds(roleMapper.selectUserIdsByRoleId(roleId));
    }

    /**
     * 事务提交后再删缓存。
     *
     * 为什么不能在事务里直接删：事务没提交时删了 key，另一个请求立刻打进来会回源
     * 查库，此时它读不到未提交的数据（隔离级别），于是把旧权限又写回缓存——脏数据
     * 要等到 TTL 过期才消失。Redis 不参与数据库事务，必须等 afterCommit。
     */
    public void evictAfterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evict(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict(userId);
            }
        });
    }

    /** 同上，批量版本 */
    public void evictByIdsAfterCommit(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictByIds(userIds);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictByIds(userIds);
            }
        });
    }
}
