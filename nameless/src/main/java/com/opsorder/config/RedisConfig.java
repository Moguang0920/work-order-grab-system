package com.opsorder.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. key 使用 String 序列化（存进去就是可读的字符串）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 2. value 使用 JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper();

        // 支持 Java 8 时间类型（LocalDateTime 等）
        objectMapper.registerModule(new JavaTimeModule());

        // 允许访问所有字段（包括 private）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 在 JSON 中带上类型信息，反序列化时能还原成原来的对象
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // 3. 设置序列化规则
        template.setKeySerializer(stringSerializer);          // key -> String
        template.setHashKeySerializer(stringSerializer);      // hash key -> String
        template.setValueSerializer(jsonSerializer);          // value -> JSON
        template.setHashValueSerializer(jsonSerializer);      // hash value -> JSON

        template.afterPropertiesSet();
        return template;
    }
}