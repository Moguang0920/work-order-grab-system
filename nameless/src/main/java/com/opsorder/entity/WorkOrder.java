package com.opsorder.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 工单实体，对应表 work_order
 */
@Data
@TableName("work_order")
public class WorkOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 工单号 */
    @TableField("order_no")
    private String orderNo;

    /** 标题 */
    @TableField("title")
    private String title;

    /** 类型：空调/水电/网络 */
    @TableField("type")
    private String type;

    /** 描述 */
    @TableField("content")
    private String content;

    /** 状态：PENDING / PROCESSING / COMPLETED / CANCELLED */
    @TableField("status")
    private String status;

    /** 处理人ID，NULL 表示未分配 */
    @TableField("assignee_id")
    private Long assigneeId;

    /** 抢单时间 */
    @TableField("grab_time")
    private LocalDateTime grabTime;

    /** 处理截止时间 */
    @TableField("deadline")
    private LocalDateTime deadline;

    /** 版本号（每次状态变更 +1） */
    @TableField("version")
    private Integer version;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
