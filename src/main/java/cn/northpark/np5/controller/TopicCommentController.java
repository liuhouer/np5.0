package cn.northpark.np5.controller;

import cn.northpark.np5.entity.TopicComment;
import cn.northpark.np5.result.Result;
import cn.northpark.np5.entity.User;
import cn.northpark.np5.service.TopicCommentService;
import cn.northpark.np5.utils.ResultGenerator;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;

/**
 * 主题评论控制器
 * 
 * @author bruce
 */
@Controller
@RequestMapping("/topicComment")
@Slf4j
public class TopicCommentController {

    @Autowired
    private TopicCommentService topicCommentService;

    /**
     * 添加评论
     * 
     * @param topicId 主题ID
     * @param topicType 主题类型
     * @param content 评论内容
     * @param toUid 被回复者ID(可选)
     * @param toUname 被回复者用户名(可选)
     * @return 结果
     */
    @PostMapping("/add")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public Result<?> addComment(@RequestParam("topicId") Integer topicId,
                                @RequestParam("topicType") String topicType,
                                @RequestParam("content") String content,
                                @RequestParam(value = "toUid", required = false) Integer toUid,
                                @RequestParam(value = "toUname", required = false) String toUname,
                                HttpServletRequest request
                                ) {
        try {
            // 参数校验
            if (topicId == null || StringUtils.isBlank(topicType) || StringUtils.isBlank(content)) {
                return ResultGenerator.genErrorResult(400, "参数不完整");
            }

            // 内容长度校验
            if (content.length() > 500) {
                return ResultGenerator.genErrorResult(400, "评论内容不能超过500字");
            }

            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResultGenerator.genErrorResult(401, "请先登录");
            }

            // 获取用户名和用户ID（从Principal获取）
            String username = auth.getName();
            User user = null;
            HttpSession session = request.getSession();
            if(Objects.nonNull(session)){
                user =  (User) session.getAttribute("user");
            }

            // 从Session或Security Context中获取用户ID
            Integer userId = user.getId();
            
            // 构建评论对象
            TopicComment topicComment = TopicComment.builder()
                    .topicId(topicId)
                    .topicType(topicType)
                    .content(escapeHtml(content))
                    .fromUid(userId)
                    .fromUname(username)
                    .toUid(toUid)
                    .toUname(toUname)
                    .build();

            // 保存评论
            boolean success = topicCommentService.addComment(topicComment);

            if (success) {
                return ResultGenerator.genSuccessResult();
            } else {
                return ResultGenerator.genErrorResult(500, "评论失败，请稍后重试");
            }

        } catch (Exception e) {
            log.error("添加评论失败", e);
            return ResultGenerator.genErrorResult(500, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 获取评论列表
     * 
     * @param topicId 主题ID
     * @param topicType 主题类型
     * @return 评论列表
     */
    @GetMapping("/list")
    @ResponseBody
    public Result<List<TopicComment>> getComments(@RequestParam("topicId") Integer topicId,
                                                   @RequestParam("topicType") String topicType) {
        try {
            if (topicId == null || StringUtils.isBlank(topicType)) {
                return ResultGenerator.genErrorResult(400, "参数不完整");
            }

            List<TopicComment> comments = topicCommentService.getCommentsByTopicAndType(topicId, topicType);
            return ResultGenerator.genSuccessResult(comments);

        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return ResultGenerator.genErrorResult(500, "系统错误: " + e.getMessage());
        }
    }

    /**
     * HTML转义(防止XSS攻击)
     */
    private String escapeHtml(String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }
}