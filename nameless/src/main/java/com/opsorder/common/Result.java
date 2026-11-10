package com.opsorder.common;

import java.io.Serializable;



public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int statusCode; // 状态码
    
    private T data; // 数据
    
    private Boolean success; // 是否成功
    
    private String message; // 消息
    
   

      public Result() {
   
    }
    
    public Result(int statusCode, T data, Boolean success, String message) {
        this.statusCode = statusCode;
        this.data = data;
        this.success = success;
        this.message = message;
    }
    
   public static <T> Result<T> success(T data) {
        return new Result<>(200, data, true, "操作成功");
    }
    
    public static <T> Result<T> error(int statusCode, String message) {
        return new Result<>(statusCode, null, false, message);
    }
    
    public static <T> Result<T> error(int statusCode, T data, String message) {
        return new Result<>(statusCode, data, false, message);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, null, false, message);
    }
    
    public static <T> Result<T> error(int statusCode, String message, T data) {
        return new Result<>(statusCode, data, false, message);
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}