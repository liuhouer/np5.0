package cn.northpark.np5.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(HttpServletRequest request, Exception ex, Model model) {
        log.error("全局异常捕获 ===> ", ex);
        
        // 传递友好错误消息到 500 页面
        model.addAttribute("message", ex.getMessage() != null ? ex.getMessage() : "服务器内部处理发生异常。");
        return "error/500";
    }
}