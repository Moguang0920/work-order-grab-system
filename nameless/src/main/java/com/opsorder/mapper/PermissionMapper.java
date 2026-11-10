package com.opsorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsorder.entity.Permission;

/**
 * 权限 Mapper：除基础 CRUD 外，提供角色-权限、用户-权限的查询与维护
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 查询某个用户拥有的全部权限标识（经 用户->角色->权限 三级关联，供 Sa-Token 鉴权使用） */
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p "
            + "JOIN sys_role_permission rp ON rp.perm_id = p.id "
            + "JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND p.status = 1 AND p.deleted = 0")
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /** 查询某个用户拥有的权限对象 */
    @Select("SELECT DISTINCT p.* FROM sys_permission p "
            + "JOIN sys_role_permission rp ON rp.perm_id = p.id "
            + "JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND p.deleted = 0 ORDER BY p.sort")
    List<Permission> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询某个角色拥有的权限 */
    @Select("SELECT p.* FROM sys_permission p "
            + "JOIN sys_role_permission rp ON rp.perm_id = p.id "
            + "WHERE rp.role_id = #{roleId} AND p.deleted = 0 ORDER BY p.sort")
    List<Permission> selectPermsByRoleId(@Param("roleId") Long roleId);

    /** 按权限标识查权限ID */
    @Select("SELECT id FROM sys_permission WHERE perm_code = #{permCode} AND deleted = 0 LIMIT 1")
    Long selectIdByPermCode(@Param("permCode") String permCode);

    /** 给角色绑定权限（已存在则忽略） */
    @Insert("INSERT IGNORE INTO sys_role_permission (role_id, perm_id) VALUES (#{roleId}, #{permId})")
    int insertRolePermission(@Param("roleId") Long roleId, @Param("permId") Long permId);

    /** 清空某个角色的全部权限 */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissionsByRoleId(@Param("roleId") Long roleId);
}
