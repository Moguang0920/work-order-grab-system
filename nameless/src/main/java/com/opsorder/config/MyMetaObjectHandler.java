package com.opsorder.config;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

/**
 * MyBatis-Plus 字段自动填充处理器。
 *
 * 背景：User 实体上标注了 @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)，
 * 但项目里缺少 MetaObjectHandler 实现，这些注解不会生效，
 * create_time / update_time 只能靠数据库列的 DEFAULT CURRENT_TIMESTAMP 兜底。
 * 换库、改表或字段为 null 时就填不上了，这里补上处理器让注解真正生效。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 属性名对应实体里的 createTime / updateTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
