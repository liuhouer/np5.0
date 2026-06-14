package cn.northpark.np5.utils;

import cn.northpark.np5.result.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * 接口返回通用结果生成工具类
 * 
 * @author bruce
 */
@Slf4j
public class ResultGenerator {

    public static <T> Result<T> genResult(boolean flag, T data, Integer code, String message) {
        Result<T> result = Result.newInstance();
        result.setResult(flag);
        result.setData(data);
        result.setMessage(message);
        result.setCode(code);
        if (log.isDebugEnabled()) {
            log.debug("generate rest result:{}", result);
        }
        return result;
    }

    public static <T> Result<T> genSuccessResult(T data) {
        return genResult(true, data, 200, "ok");
    }

    public static Result<?> genSuccessResult() {
        return genSuccessResult(null);
    }

    public static <T> Result<T> genErrorResult(Integer code, String message) {
        return genResult(false, null, code, message);
    }
}