package cn.northpark.np5.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        String message = messageObj != null ? messageObj.toString() : "";

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        } else {
            // 如果是通过 Security 的 accessDeniedPage("/error?status=403") 过来，可以从参数获取
            String statusParam = request.getParameter("status");
            if (statusParam != null) {
                try {
                    statusCode = Integer.parseInt(statusParam);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        log.error("系统发生错误, 状态码: {}, 异常信息: {}, 错误消息: {}", statusCode, exception, message);

        if (statusCode == 404) {
            return "error/404";
        } else if (statusCode == 403) {
            model.addAttribute("message", "您没有执行此操作或访问此资源的权限。");
            return "error/403";
        } else if (statusCode == 500) {
            model.addAttribute("message", message.isEmpty() ? "服务器开小差了，请稍后再试。" : message);
            return "error/500";
        }
        
        model.addAttribute("message", "未知系统错误");
        return "error/500";
    }
}