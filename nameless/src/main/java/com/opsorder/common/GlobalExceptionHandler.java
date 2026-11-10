package com.opsorder.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;

/**
 * 全局异常处理：把 Sa-Token 的鉴权异常转换成项目统一的 Result 结构，
 * 避免前端拿到一坨 Spring 默认错误页或 500 堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未登录 / token 失效 */
    @ExceptionHandler(NotLoginException.class)
    public Result handleNotLogin(NotLoginException e) {
        // type 细分：未提供 token / token 无效 / token 已过期 / 已被顶下线 / 已被踢下线
        return Result.error(401, "未登录或登录已过期：" + e.getType());
    }

    /** 缺少权限 */
    @ExceptionHandler(NotPermissionException.class)
    public Result handleNotPermission(NotPermissionException e) {
        return Result.error(403, "无此权限：" + e.getPermission());
    }

    /** 缺少角色 */
    @ExceptionHandler(NotRoleException.class)
    public Result handleNotRole(NotRoleException e) {
        return Result.error(403, "无此角色：" + e.getRole());
    }

    /** 其它 Sa-Token 异常（如账号被封禁、token 被冻结等） */
    @ExceptionHandler(SaTokenException.class)
    public Result handleSaToken(SaTokenException e) {
        return Result.error(401, e.getMessage());
    }
}
