package cn.northpark.np5.controller;

import cn.northpark.np5.entity.Knowledge;
import cn.northpark.np5.entity.Tags;
import cn.northpark.np5.result.Result;
import cn.northpark.np5.service.KnowledgeService;
import cn.northpark.np5.service.TagsService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

@Controller
@RequestMapping("/learning")
@Slf4j
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private TagsService tagsService;

    private static final int PAGE_SIZE = 15;

    @RequestMapping("")
    public String list(Model model,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "orderBy", required = false) String orderBy,
                       HttpSession session) {
        return listPage(model, 1, keyword, orderBy, session);
    }

    @RequestMapping("/page/{pageNo}")
    public String listPage(Model model,
                           @PathVariable("pageNo") Integer pageNo,
                           @RequestParam(value = "keyword", required = false) String keyword,
                           @RequestParam(value = "orderBy", required = false) String orderBy,
                           HttpSession session) {
        session.setAttribute("tabs", "learning");

        String decodedKeyword = null;
        if (StringUtils.isNotBlank(keyword)) {
            try {
                decodedKeyword = URLDecoder.decode(keyword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("decode error", e);
            }
        }

        Page<Knowledge> page = new Page<>(pageNo, PAGE_SIZE);
        QueryWrapper<Knowledge> query = new QueryWrapper<>();
        query.isNull("displayed");

        if (StringUtils.isNotBlank(decodedKeyword)) {
            String cleanKeyword = decodedKeyword.trim();
            query.and(q -> q.like("title", cleanKeyword)
                    .or()
                    .like("title", cleanKeyword.replace(" ", "")));
        }

        if ("hot".equals(orderBy)) {
            query.orderByDesc("hot_index");
        } else if ("latest".equals(orderBy)) {
            query.orderByDesc("id");
        } else {
            query.orderByDesc("post_date").orderByDesc("id");
        }

        knowledgeService.page(page, query);
        List<Knowledge> list = page.getRecords();
        handleTags(list);

        model.addAttribute("list", list);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("keyword", decodedKeyword);
        model.addAttribute("orderBy", orderBy);

        loadSidebar(model);

        return "learning/list";
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
        session.setAttribute("tabs", "learning");

        Page<Knowledge> page = new Page<>(pageNo, PAGE_SIZE);
        QueryWrapper<Knowledge> query = new QueryWrapper<>();
        query.isNull("displayed").like("tags_code", tagCode);
        query.orderByDesc("hot_index").orderByDesc("id");

        knowledgeService.page(page, query);
        List<Knowledge> list = page.getRecords();
        handleTags(list);

        model.addAttribute("list", list);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("selTag", tagCode);

        loadSidebar(model);

        return "learning/list";
    }

    @RequestMapping("/post-{id}.html")
    public String postDetail(Model model, @PathVariable("id") Integer id) {
        Knowledge item = knowledgeService.getById(id);
        if (item != null && !"N".equals(item.getDisplayed())) {
            if (StringUtils.isNotBlank(item.getContent())) {
                model.addAttribute("contentHtml", item.getContent());
                model.addAttribute("contentDescText", Jsoup.parse(item.getContent()).select("p").text());
            }
            handleTags(java.util.Collections.singletonList(item));
            model.addAttribute("model", item);
        }
        return "learning/detail";
    }

    /**
     * 跳转到学习课程编辑页面
     */
    @RequestMapping("/edit/{id}")
    public String edit(Model model, @PathVariable("id") Integer id) {
        Knowledge item = knowledgeService.getById(id);
        if (item != null) {
            model.addAttribute("model", item);
        }
        return "learning/edit";
    }

    private void handleTags(List<Knowledge> list) {
        for (Knowledge k : list) {
            if (StringUtils.isNotBlank(k.getTags()) && StringUtils.isNotBlank(k.getTagsCode())) {
                String[] tags = k.getTags().split(",");
                String[] codes = k.getTagsCode().split(",");
                if (tags.length == codes.length) {
                    List<Map<String, String>> tagList = new ArrayList<>();
                    for (int i = 0; i < tags.length; i++) {
                        Map<String, String> tagMap = new HashMap<>();
                        tagMap.put("tag", tags[i]);
                        tagMap.put("tag_code", codes[i]);
                        tagList.add(tagMap);
                    }
                    k.setTagList(tagList);
                }
            }
        }
    }

    private void loadSidebar(Model model) {
        // 侧边栏课程推荐 (bc_knowledge 中 tags_code 包含 'classhare' 的随机获取)
        QueryWrapper<Knowledge> kQuery = new QueryWrapper<>();
        kQuery.isNull("displayed")
              .like("tags_code", "classhare")
              .last("order by rand() limit 20");
        List<Knowledge> hotList = knowledgeService.list(kQuery);
        model.addAttribute("learnHotList", hotList);

        // 侧边栏标签 (tag_type = '4')
        QueryWrapper<Tags> tagQuery = new QueryWrapper<>();
        tagQuery.eq("tag_type", "4");
        List<Tags> tags = tagsService.list(tagQuery);
        model.addAttribute("learnTags", tags);
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
            String max_hot_sql_id = "select max(hot_index) as hot_index from bc_knowledge ";
            List<Map<String, Object>> list = knowledgeService.querySqlMap(max_hot_sql_id);
            Integer hot_index = 0;
            if (!CollectionUtils.isEmpty(list) && Objects.nonNull(list.get(0).get("hot_index"))) {
                hot_index = ((Number) list.get(0).get("hot_index")).intValue();
                hot_index++;
            }

            if (hot_index > 0) {
                Knowledge m = knowledgeService.getById(Integer.parseInt(id));
                if (m != null) {
                    m.setHotIndex(hot_index);
                    knowledgeService.updateById(m);
                }
            }

        } catch (Exception e) {
            log.error("learning action 置顶异常", e);
            rs = "ex";
        }
        return ResultGenerator.genSuccessResult(rs);
    }

    /**
     * 保存学习资源
     */
    @RequestMapping("/addItem")
    @ResponseBody
    public Result<String> addItem(Knowledge model, HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_ADMIN")) {
            return ResultGenerator.genErrorResult(403, "没有操作权限");
        }
        try {
            if (model.getId() != null && model.getId() != 0) {
                // 更新
                knowledgeService.updateById(model);
            } else {
                // 新增
                model.setPostDate(java.time.LocalDate.now().toString());
                if (StringUtils.isBlank(model.getRetCode()) && StringUtils.isNotBlank(model.getTitle())) {
                    model.setRetCode(cn.northpark.np5.utils.encrypt.MD5Utils.encrypt(model.getTitle(), cn.northpark.np5.utils.encrypt.MD5Utils.MD5_KEY));
                }
                knowledgeService.save(model);
            }
            return ResultGenerator.genSuccessResult("success");
        } catch (Exception e) {
            log.error("保存学习异常", e);
            return ResultGenerator.genErrorResult(500, "操作失败");
        }
    }

    /**
     * 隐藏学习的方法
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
            Knowledge m = knowledgeService.getById(Integer.parseInt(id));
            if (m != null) {
                m.setDisplayed("N");
                knowledgeService.updateById(m);
            }

        } catch (Exception e) {
            log.error("learning action 隐藏异常", e);
            rs = "ex";
        }
        return ResultGenerator.genSuccessResult(rs);
    }
}