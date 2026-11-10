package com.opsorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsorder.entity.Role;

/**
 * 角色 Mapper：除基础 CRUD 外，提供用户-角色关联查询与维护
 */
public interface RoleMapper extends BaseMapper<Role> {

    /** 查询某个用户拥有的角色标识（供 Sa-Token 鉴权使用） */
    @Select("SELECT r.role_code FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 查询某个用户拥有的角色对象 */
    @Select("SELECT r.* FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.deleted = 0 ORDER BY r.sort")
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /** 按角色标识查角色ID */
    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} AND deleted = 0 LIMIT 1")
    Long selectIdByRoleCode(@Param("roleCode") String roleCode);

    /** 绑定用户与角色（已存在则忽略） */
    @Insert("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 清空某个用户的全部角色 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRolesByUserId(@Param("userId") Long userId);

    /** 清空某个角色下的全部用户关联 */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteUserRolesByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询某个角色下绑定的所有用户ID。
     * 用途：角色权限变更时把这些人的权限缓存全部失效
     * （缓存按用户维度存，改一个角色会波及多个用户）。
     */
    @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除角色。
     * 注意：role_code 上有唯一索引，如果只做逻辑删除，同一个 role_code 将永远无法再次创建
     * （唯一索引不认 deleted 字段），所以角色采用物理删除。
     */
    @Delete("DELETE FROM sys_role WHERE id = #{id}")
    int deleteRolePhysically(@Param("id") Long id);
}
