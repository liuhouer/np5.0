package cn.northpark.np5.utils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    private static JdbcTemplate jdbcTemplateInstance;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplateInstance = this.jdbcTemplate;
    }

    private static List<Map<String, Object>> querySql(String sql, Object... params) {
        return jdbcTemplateInstance.queryForList(sql, params);
    }

    private static Map<String, Object> findById(String tableName, Integer id) {
        String sql = "select * from " + tableName + " where id = ?";
        List<Map<String, Object>> list = querySql(sql, id);
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    public static Map<String, String> getObjectContent(String topicType, Integer topicId) {
        ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
        try {
            if ("1".equals(topicType)) {
                Map<String, Object> bcNote = findById("bc_note", topicId);
                if (bcNote != null) {
                    map.put("title", bcNote.get("brief").toString());
                    map.put("href", "/note");
                    map.put("by", bcNote.get("userid").toString());
                }
            } else if ("2".equals(topicType)) {
                Map<String, Object> bcLyrics = findById("bc_lyrics", topicId);
                if (bcLyrics != null) {
                    map.put("title", bcLyrics.get("title").toString());
                    map.put("href", "/love/" + bcLyrics.get("title_code") + ".html");
                }
            } else if ("3".equals(topicType)) {
                Map<String, Object> bcSoft = findById("bc_soft", topicId);
                if (bcSoft != null) {
                    map.put("title", bcSoft.get("title").toString());
                    map.put("href", "/soft/" + bcSoft.get("ret_code") + ".html");
                }
            } else if ("4".equals(topicType)) {
                Map<String, Object> bcMovies = findById("bc_movies", topicId);
                if (bcMovies != null) {
                    map.put("title", bcMovies.get("movie_name").toString());
                    map.put("href", "/movies/post-" + topicId + ".html");
                }
            } else if ("6".equals(topicType)) {
                Map<String, Object> bcEq = findById("bc_eq", topicId);
                if (bcEq != null) {
                    map.put("title", bcEq.get("title").toString());
                    map.put("href", "/romeo/" + topicId + ".html");
                }
            } else if ("7".equals(topicType)) {
                map.put("title", "赞助本站");
                map.put("href", "/donate");
            } else if ("8".equals(topicType)) {
                Map<String, Object> bcLearn = findById("bc_knowledge", topicId);
                if (bcLearn != null) {
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
        var list = querySql("select username from bc_user where id = ?", uid);
        if (CollectionUtils.isNotEmpty(list)) {
            return Objects.toString(list.get(0).get("username"), "");
        }
        return "";
    }

    public static String getUserEmailByID(String uid) {
        if (StringUtils.isBlank(uid)) {
            return "";
        }
        var list = querySql("select email from bc_user where id = ?", uid);
        if (CollectionUtils.isNotEmpty(list)) {
            return Objects.toString(list.get(0).get("email"), "");
        }
        return "";
    }
}