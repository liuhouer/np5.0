package cn.northpark.np5.model;

import lombok.Data;

/**
 * 接口返回的通用结果包装类
 * 
 * @author bruce
 */
@Data
public class Result<T> {

    private boolean result;
    
    private Integer code;

    private String message;

    private T data;

    private Result() {}

    public static <T> Result<T> newInstance() {
        return new Result<>();
    }
}