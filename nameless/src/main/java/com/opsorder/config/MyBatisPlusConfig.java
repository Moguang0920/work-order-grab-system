package com.opsorder.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

@Configuration
@MapperScan(basePackages = "com.opsorder.mapper")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器：分页插件。
     *
     * 不加这个，this.page(...) 会查出全表再在内存里截取（甚至不截取），
     * 抢单池、我的工单这类列表接口必须靠它做真正的 LIMIT 分页。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
