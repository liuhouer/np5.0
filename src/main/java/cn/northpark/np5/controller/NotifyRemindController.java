package cn.northpark.np5.controller;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.model.Result;
import cn.northpark.np5.model.User;
import cn.northpark.np5.service.NotifyRemindService;
import cn.northpark.np5.utils.ResultGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息通知控制器
 * 
 * @author bruce
 */
@Controller
@RequestMapping("")
@Slf4j
public class NotifyRemindController {

    @Autowired
    private NotifyRemindService notifyRemindService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int PAGE_SIZE = 15;

    private User getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }

    /**
     * 拉取未读消息数量
     */
    @RequestMapping("/notify/count")
    @ResponseBody
    public Result<Integer> notifyNum(HttpServletRequest request) {
        try {
            User userInfo = getLoginUser(request);
            if (userInfo == null) {
                return ResultGenerator.genSuccessResult(0);
            }

            QueryWrapper<NotifyRemind> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("recipient_id", userInfo.getId().toString())
                        .eq("status", "0");
            int i = Math.toIntExact(notifyRemindService.count(queryWrapper));

            return ResultGenerator.genSuccessResult(i);
        } catch (Exception e) {
            log.error("notify/count error", e);
        }
        return ResultGenerator.genSuccessResult(0);
    }

    /**
     * 当前页的消息设置为已读
     */
    @RequestMapping("/notify/readNotify")
    @ResponseBody
    public Result<Boolean> readNotify(HttpServletRequest request) {
        try {
            String ids = request.getParameter("id");
            if (StringUtils.isNotEmpty(ids)) {
                User userInfo = getLoginUser(request);
                if (userInfo == null) {
                    return ResultGenerator.genSuccessResult(false);
                }

                String readNotifySql = "update bc_notify_remind_b set status = ? where recipient_id = ? and id in (" + ids + ")";
                jdbcTemplate.update(readNotifySql, "1", userInfo.getId().toString());
            }
        } catch (Exception e) {
            log.error("notify/readNotify error", e);
            return ResultGenerator.genSuccessResult(false);
        }
        return ResultGenerator.genSuccessResult(true);
    }

    /**
     * 查看通知列表
     */
    @RequestMapping(value = "/notifications")
    public String list1(Model model, HttpServletRequest request) throws IOException {
        request.getSession().removeAttribute("tabs");
        return listPage(model, 1, request);
    }

    /**
     * 消息分页列表
     */
    @RequestMapping(value = "/notifications/page/{page}")
    public String listPage(Model model, @PathVariable Integer page, HttpServletRequest request) throws IOException {
        User userInfo = getLoginUser(request);
        if (userInfo == null) {
            return "redirect:/login?redirectURI=/notifications";
        }

        QueryWrapper<NotifyRemind> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("recipient_id", userInfo.getId().toString());

        // 标签分类
        String remindID = request.getParameter("remindID");
        if (StringUtils.isNotEmpty(remindID)) {
            try {
                Preconditions.checkArgument(Integer.parseInt(remindID) > 0, "编号错误");
            } catch (Exception e) {
                throw new IllegalArgumentException("u r shit");
            }
            queryWrapper.eq("remind_id", remindID);
            model.addAttribute("remindID", remindID);
        }

        queryWrapper.orderByAsc("status").orderByDesc("created_at");

        IPage<NotifyRemind> iPage = new Page<>(page, PAGE_SIZE);
        notifyRemindService.page(iPage, queryWrapper);

        List<NotifyRemind> resultList = iPage.getRecords();
        // 格式化时间
        resultList.forEach(item -> {
            if (item.getCreatedAt() != null) {
                item.setCreateTime(formatTime(item.getCreatedAt()));
            }
        });

        model.addAttribute("list", resultList);
        model.addAttribute("totalPages", iPage.getPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("actionUrl", "/notifications");

        return "notify";
    }

    private String formatTime(Date date) {
        long ago = (System.currentTimeMillis() - date.getTime()) / 1000;
        long oneMinute = TimeUnit.MINUTES.toSeconds(1);
        long oneHour = TimeUnit.HOURS.toSeconds(1);
        long oneDay = TimeUnit.DAYS.toSeconds(1);
        long oneMonth = TimeUnit.DAYS.toSeconds(30);
        long oneYear = TimeUnit.DAYS.toSeconds(365);

        if (ago <= oneMinute) return ago + "秒前";
        if (ago <= oneHour) return ago / oneMinute + "分钟前";
        if (ago <= oneDay) return ago / oneHour + "小时前";
        if (ago <= oneDay * 2) return "昨天";
        if (ago <= oneDay * 3) return "前天";
        if (ago <= oneMonth) return ago / oneDay + "天前";
        if (ago <= oneYear) return ago / oneMonth + "月前";
        return ago / oneYear + "年前";
    }
}