package cn.northpark.np5.controller;

import cn.northpark.np5.entity.Donates;
import cn.northpark.np5.service.DonatesService;
import cn.northpark.np5.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private DonatesService donatesService;

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        request.getSession().setAttribute("tabs", "index");
        // 获取用户总数展示在首页中
        int userCount = Math.toIntExact(userService.count());
        model.addAttribute("userCount", userCount);
        return "index";
    }

    @GetMapping("/about")
    public String about(HttpServletRequest request) {
        request.getSession().setAttribute("tabs", "about");
        return "about";
    }

    @GetMapping("/sponsor")
    public String donate(HttpServletRequest request) {
        request.getSession().setAttribute("tabs", "sponsor");
        return "donate";
    }

    @GetMapping("/sponsor/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> donateList(
            @RequestParam(value = "typeId", defaultValue = "1") String typeId,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        
        // 1-大老板 (>=100)
        // 2-老板 (>=1 && <100)
        // 3-好心人 (>0 && <1)
        QueryWrapper<Donates> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("add_time");
        
        if ("1".equals(typeId)) {
            queryWrapper.ge("order_amount", 100);
        } else if ("2".equals(typeId)) {
            queryWrapper.ge("order_amount", 1).lt("order_amount", 100);
        } else {
            queryWrapper.gt("order_amount", 0).lt("order_amount", 1);
        }

        Page<Donates> ipage = new Page<>(page, 15);
        donatesService.page(ipage, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("list", ipage.getRecords());
        result.put("pageNum", ipage.getCurrent());
        result.put("totalPages", ipage.getPages());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/remember-me-test")
    public String rememberMeTest(HttpServletRequest request) {
        return "remember-me-test";
    }
}