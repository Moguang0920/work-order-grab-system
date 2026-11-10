package com.opsorder.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 抢单流水，对应表 grab_record
 * (order_id, user_id) 上有唯一索引，作为幂等的最后一道保险
 */
@Data
@TableName("grab_record")
public class GrabRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    /** 1成功 0失败 */
    @TableField("success")
    private Integer success;

    /** 抢单耗时(毫秒) */
    @TableField("cost_ms")
    private Integer costMs;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
