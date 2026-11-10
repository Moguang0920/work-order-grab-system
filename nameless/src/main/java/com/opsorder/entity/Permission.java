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
 * 权限实体，对应表 sys_permission
 */
@Data
@TableName("sys_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 权限标识，如 user:read */
    @TableField("perm_code")
    private String permCode;

    /** 权限名称 */
    @TableField("perm_name")
    private String permName;

    /** 类型：1接口 2菜单 3按钮 */
    @TableField("perm_type")
    private Integer permType;

    /** 父权限ID，0为顶级 */
    @TableField("parent_id")
    private Long parentId;

    /** 前端路由/接口路径 */
    @TableField("path")
    private String path;

    /** 排序 */
    @TableField("sort")
    private Integer sort;

    /** 状态：1启用 0禁用 */
    @TableField("status")
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
