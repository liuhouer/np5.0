package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 主题评论实体类
 * 对应表: bc_topic_comment
 * 
 * @author bruce
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bc_topic_comment")
public class TopicComment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 主题ID
     */
    @TableField("topic_id")
    private Integer topicId;

    /**
     * 主题类型(1-碎碎念, 3-软件, 4-电影, 8-学习等)
     */
    @TableField("topic_type")
    private String topicType;

    /**
     * 评论内容
     */
    @TableField("content")
    private String content;

    /**
     * 评论者用户ID
     */
    @TableField("from_uid")
    private Integer fromUid;

    /**
     * 评论者用户名首字母拼音
     */
    @TableField("from_span")
    private String fromSpan;

    /**
     * 被回复者用户ID(回复评论时使用)
     */
    @TableField("to_uid")
    private Integer toUid;

    /**
     * 评论者用户名
     */
    @TableField("from_uname")
    private String fromUname;

    /**
     * 被回复者用户名
     */
    @TableField("to_uname")
    private String toUname;

    /**
     * 添加时间
     */
    @TableField("add_time")
    private String addTime;
}