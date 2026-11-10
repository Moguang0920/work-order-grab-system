package com.opsorder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务支持（超时工单释放依赖它）。
 * 单独放一个配置类，避免和 Sa-Token / MyBatis-Plus 的配置混在一起。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
