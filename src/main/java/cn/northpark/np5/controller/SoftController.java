package cn.northpark.np5.controller;

import cn.northpark.np5.result.Result;
import cn.northpark.np5.entity.Soft;
import cn.northpark.np5.service.SoftService;
import cn.northpark.np5.utils.MinioUtils;
import cn.northpark.np5.utils.ResultGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/soft")
@Slf4j
public class SoftController {

    @Autowired
    private SoftService softService;

    private static final int PAGE_SIZE = 15;

    @RequestMapping("")
    public String index(Model model,
                        @RequestParam(value = "keyword", required = false) String keyword,
                        HttpSession session) {
        return listPage(model, 1, keyword, session);
    }

    @RequestMapping("/mac")
    public String list(Model model,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       HttpSession session) {
        return listPage(model, 1, keyword, session);
    }

    @RequestMapping("/mac/page/{pageNo}")
    public String listPage(Model model,
                           @PathVariable("pageNo") Integer pageNo,
                           @RequestParam(value = "keyword", required = false) String keyword,
                           HttpSession session) {
        session.setAttribute("tabs", "soft");

        String decodedKeyword = null;
        if (StringUtils.isNotBlank(keyword)) {
            try {
                decodedKeyword = URLDecoder.decode(keyword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("decode error", e);
            }
        }

        Page<Soft> page = new Page<>(pageNo, PAGE_SIZE);
        QueryWrapper<Soft> query = new QueryWrapper<>();
        query.isNull("displayed");

        if (StringUtils.isNotBlank(decodedKeyword)) {
            String cleanKeyword = decodedKeyword.trim();
            query.and(q -> q.like("title", cleanKeyword)
                    .or()
                    .like("title", cleanKeyword.replace(" ", "")));
        }

        query.orderByDesc("hot_index").orderByDesc("post_date").orderByDesc("id");

        softService.page(page, query);
        List<Soft> list = page.getRecords();
        handleTags(list);

        model.addAttribute("list", list);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("keyword", decodedKeyword);

        loadSidebar(model);

        return "soft/list";
    }

    @RequestMapping("/tag/{tagCode}")
    public String tagSearch(Model model, @PathVariable("tagCode") String tagCode, HttpSession session) {
        return tagSearchPage(model, tagCode, 1, session);
    }

    @RequestMapping("/tag/{tagCode}/page/{pageNo}")
    public String tagSearchPage(Model model,
                               @PathVariable("tagCode") String tagCode,
                               @PathVariable("pageNo") Integer pageNo,
                               HttpSession session) {
        session.setAttribute("tabs", "soft");

        Page<Soft> page = new Page<>(pageNo, PAGE_SIZE);
        QueryWrapper<Soft> query = new QueryWrapper<>();
        query.isNull("displayed").eq("tags_code", tagCode);
        query.orderByDesc("hot_index").orderByDesc("post_date").orderByDesc("id");

        softService.page(page, query);
        List<Soft> list = page.getRecords();
        handleTags(list);

        model.addAttribute("list", list);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("selTag", tagCode);

        loadSidebar(model);

        return "soft/list";
    }

    @RequestMapping("/month/{month}")
    public String monthSearch(Model model, @PathVariable("month") String month, HttpSession session) {
        return monthSearchPage(model, month, 1, session);
    }

    @RequestMapping("/month/{month}/page/{pageNo}")
    public String monthSearchPage(Model model,
                                 @PathVariable("month") String month,
                                 @PathVariable("pageNo") Integer pageNo,
                                 HttpSession session) {
        session.setAttribute("tabs", "soft");

        Page<Soft> page = new Page<>(pageNo, PAGE_SIZE);
        QueryWrapper<Soft> query = new QueryWrapper<>();
        query.isNull("displayed").eq("month", month);
        query.orderByDesc("hot_index").orderByDesc("post_date").orderByDesc("id");

        softService.page(page, query);
        List<Soft> list = page.getRecords();
        handleTags(list);

        model.addAttribute("list", list);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("selMonth", month);

        loadSidebar(model);

        return "soft/list";
    }

    @RequestMapping("/{retCode}.html")
    public String softDetail(Model model, @PathVariable("retCode") String retCode) {
        QueryWrapper<Soft> query = new QueryWrapper<>();
        query.eq("ret_code", retCode);
        Soft item = softService.getOne(query);

        if (item != null && !"N".equals(item.getDisplayed())) {
            // BRUCETIPS! 富文本处理 -- 从minio读取
            if ("1".equals(item.getUseMinio()) && StringUtils.isNotBlank(item.getContentMinio())) {
                item.setContent(MinioUtils.readText(item.getContentMinio()));
            }

            if (StringUtils.isNotBlank(item.getBrief())) {
                model.addAttribute("softDescText", Jsoup.parse(item.getBrief()).text());
            }

            handleTags(java.util.Collections.singletonList(item));
            model.addAttribute("model", item);
        }

        return "soft/detail";
    }

    private void handleTags(List<Soft> list) {
        for (Soft s : list) {
            // 解析 brief 里的 img 标签
            if (StringUtils.isNotBlank(s.getBrief())) {
                org.jsoup.nodes.Document doc = Jsoup.parse(s.getBrief());
                org.jsoup.select.Elements imgs = doc.select("img");
                if (!imgs.isEmpty()) {
                    s.setBriefImg(imgs.get(0).attr("src"));
                }
                // 去除 HTML 标签后的纯文本，用于简要描述
                s.setBriefShow(doc.text());
            }

            if (StringUtils.isNotBlank(s.getTags()) && StringUtils.isNotBlank(s.getTagsCode())) {
                String[] tags = s.getTags().split(",");
                String[] codes = s.getTagsCode().split(",");
                if (tags.length == codes.length) {
                    List<Map<String, String>> tagList = new ArrayList<>();
                    for (int i = 0; i < tags.length; i++) {
                        Map<String, String> tagMap = new HashMap<>();
                        tagMap.put("tag", tags[i]);
                        tagMap.put("tag_code", codes[i]);
                        tagList.add(tagMap);
                    }
                    s.setTagList(tagList);
                }
            }
        }
    }

    /**
     * 跳转到软件编辑页面
     */
    @RequestMapping("/edit/{id}")
    public String edit(Model model, @PathVariable("id") Integer id) {
        Soft item = softService.getById(id);
        if (item != null) {
            model.addAttribute("model", item);
        }
        return "soft/edit";
    }

    private void loadSidebar(Model model) {
        // 侧边栏随机推荐或者热门软件
        QueryWrapper<Soft> softQuery = new QueryWrapper<>();
        softQuery.isNull("displayed").last("order by rand() limit 10");
        List<Soft> hotList = softService.list(softQuery);
        model.addAttribute("softHotList", hotList);

        // 软件标签 (根据 soft 表的标签分组)
        List<Map<String, Object>> rawTags = softService.querySqlMap("select count(tags) as num,tags,tags_code from bc_soft group by tags,tags_code order by num desc");
        List<Map<String, String>> tags = new ArrayList<>();
        if (rawTags != null) {
            for (Map<String, Object> map : rawTags) {
                String tag = (String) map.get("tags");
                String tagCode = (String) map.get("tags_code");
                if (StringUtils.isNotBlank(tag) && StringUtils.isNotBlank(tagCode)) {
                    Map<String, String> tagMap = new HashMap<>();
                    tagMap.put("tag", tag);
                    tagMap.put("tagCode", tagCode);
                    tags.add(tagMap);
                }
            }
        }
        model.addAttribute("softTags", tags);

        // 月份归档
        QueryWrapper<Soft> monthQuery = new QueryWrapper<>();
        monthQuery.select("distinct month").isNotNull("month").orderByDesc("month");
        List<Soft> months = softService.list(monthQuery);
        List<String> monthList = new ArrayList<>();
        for (Soft s : months) {
            if (StringUtils.isNotBlank(s.getMonth())) {
                monthList.add(s.getMonth());
            }
        }
        model.addAttribute("monthList", monthList);
    }

    /**
     * 置顶的方法
     */
    @RequestMapping("/handup")
    @ResponseBody
    public Result<String> handup(HttpServletRequest request) {
        String rs = "success";
        try {
            // 获取当前登录用户权限
            if (!request.isUserInRole("ROLE_ADMIN")) {
                return ResultGenerator.genErrorResult(403, "没有操作权限");
            }

            String id = request.getParameter("id");
            String max_hot_sql_id = "select max(hot_index) as hot_index from bc_soft ";
            List<Map<String, Object>> list = softService.querySqlMap(max_hot_sql_id);
            Integer hot_index = 0;
            if (!CollectionUtils.isEmpty(list) && Objects.nonNull(list.get(0).get("hot_index"))) {
                hot_index = ((Number) list.get(0).get("hot_index")).intValue();
                hot_index++;
            }

            if (hot_index > 0) {
                Soft m = softService.getById(Integer.parseInt(id));
                if (m != null) {
                    m.setHotIndex(hot_index);
                    softService.updateById(m);
                }
            }

        } catch (Exception e) {
            log.error("soft action 置顶异常", e);
            rs = "ex";
        }
        return ResultGenerator.genSuccessResult(rs);
    }

    /**
     * 保存软件资源
     */
    @RequestMapping("/addItem")
    @ResponseBody
    public Result<String> addItem(Soft model, HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_ADMIN")) {
            return ResultGenerator.genErrorResult(403, "没有操作权限");
        }
        try {
            if (model.getId() != null && model.getId() != 0) {
                // 更新
                if ("1".equals(model.getUseMinio()) && StringUtils.isNotBlank(model.getContent())) {
                    model.setContentMinio(MinioUtils.uploadText(model.getContent()));
                }
                // 保持格式
                model.setPostDate(java.time.LocalDate.now().toString());
                softService.updateById(model);
            } else {
                // 新增
                model.setPostDate(java.time.LocalDate.now().toString());
                model.setYear(String.valueOf(java.time.LocalDate.now().getYear()));
                model.setMonth(java.time.LocalDate.now().toString().substring(0, 7));
                if ("1".equals(model.getUseMinio()) && StringUtils.isNotBlank(model.getContent())) {
                    model.setContentMinio(MinioUtils.uploadText(model.getContent()));
                }
                softService.save(model);
            }
            return ResultGenerator.genSuccessResult("success");
        } catch (Exception e) {
            log.error("保存软件异常", e);
            return ResultGenerator.genErrorResult(500, "操作失败");
        }
    }

    /**
     * 隐藏软件的方法
     */
    @RequestMapping("/hideup")
    @ResponseBody
    public Result<String> hideup(HttpServletRequest request) {
        String rs = "success";
        try {
            // 获取当前登录用户权限
            if (!request.isUserInRole("ROLE_ADMIN")) {
                return ResultGenerator.genErrorResult(403, "没有操作权限");
            }

            String id = request.getParameter("id");
            Soft m = softService.getById(Integer.parseInt(id));
            if (m != null) {
                m.setDisplayed("N");
                softService.updateById(m);
            }

        } catch (Exception e) {
            log.error("soft action 隐藏异常", e);
            rs = "ex";
        }
        return ResultGenerator.genSuccessResult(rs);
    }
}