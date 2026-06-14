package cn.northpark.np5.utils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.var;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 消息通知对象内容解析与用户信息工具类 (重构: 去除 NPQueryRunner, 手动注入 JdbcTemplate)
 * 
 * @author bruce
 */
@Component
public class NotifyUtil {

    public static Map<String, String> getObjectContent(String topicType, Integer topicId) {
        ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
        try {
            if ("1".equals(topicType)) {
                // 重构：使用 MyBatis-Plus / SpringContextUtils 执行通用查询，不直接用 jdbcTemplateInstance
                List<Map<String, Object>> list = getSqlResult("select * from bc_note where id = ?", topicId);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> bcNote = list.get(0);
                    map.put("title", bcNote.get("brief").toString());
                    map.put("href", "/note");
                    map.put("by", bcNote.get("userid").toString());
                }
            } else if ("2".equals(topicType)) {
                List<Map<String, Object>> list = getSqlResult("select * from bc_lyrics where id = ?", topicId);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> bcLyrics = list.get(0);
                    map.put("title", bcLyrics.get("title").toString());
                    map.put("href", "/love/" + bcLyrics.get("title_code") + ".html");
                }
            } else if ("3".equals(topicType)) {
                List<Map<String, Object>> list = getSqlResult("select * from bc_soft where id = ?", topicId);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> bcSoft = list.get(0);
                    map.put("title", bcSoft.get("title").toString());
                    map.put("href", "/soft/" + bcSoft.get("ret_code") + ".html");
                }
            } else if ("4".equals(topicType)) {
                List<Map<String, Object>> list = getSqlResult("select * from bc_movies where id = ?", topicId);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> bcMovies = list.get(0);
                    map.put("title", bcMovies.get("movie_name").toString());
                    map.put("href", "/movies/post-" + topicId + ".html");
                }
            } else if ("6".equals(topicType)) {
                List<Map<String, Object>> list = getSqlResult("select * from bc_eq where id = ?", topicId);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> bcEq = list.get(0);
                    map.put("title", bcEq.get("title").toString());
                    map.put("href", "/romeo/" + topicId + ".html");
                }
            } else if ("7".equals(topicType)) {
                map.put("title", "赞助本站");
                map.put("href", "/donate");
            } else if ("8".equals(topicType)) {
                List<Map<String, Object>> list = getSqlResult("select * from bc_knowledge where id = ?", topicId);
                if (CollectionUtils.isNotEmpty(list)) {
                    Map<String, Object> bcLearn = list.get(0);
                    map.put("title", bcLearn.get("title").toString());
                    map.put("href", "/learning/post-" + topicId + ".html");
                }
            }
        } catch (Exception ig) {
            map.put("title", "");
            map.put("href", "");
        }
        return map;
    }

    public static String getUserNameByID(String uid) {
        if (StringUtils.isBlank(uid)) {
            return "";
        }
        var list = getSqlResult("select username from bc_user where id = ?", uid);
        if (CollectionUtils.isNotEmpty(list)) {
            return Objects.toString(list.get(0).get("username"), "");
        }
        return "";
    }

    public static String getUserEmailByID(String uid) {
        if (StringUtils.isBlank(uid)) {
            return "";
        }
        var list = getSqlResult("select email from bc_user where id = ?", uid);
        if (CollectionUtils.isNotEmpty(list)) {
            return Objects.toString(list.get(0).get("email"), "");
        }
        return "";
    }

    /**
     * 重构：通过 MyBatis-Plus SoftService 提供的通用SQL方法查询数据，移除对 JdbcTemplate 的依赖
     */
    private static List<Map<String, Object>> getSqlResult(String sql, Object... params) {
        String formattedSql = sql;
        for (Object param : params) {
            if (param instanceof String) {
                formattedSql = formattedSql.replaceFirst("\\?", "'" + param + "'");
            } else {
                formattedSql = formattedSql.replaceFirst("\\?", String.valueOf(param));
            }
        }
        cn.northpark.np5.service.SoftService softService = SpringContextUtils.getBean(cn.northpark.np5.service.SoftService.class);
        return softService.querySqlMap(formattedSql);
    }
}