package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 通知提醒实体类
 * 对应表: bc_notify_remind_b
 *
 * @author bruce
 * @date 2024-03-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bc_notify_remind_b")
public class NotifyRemind {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 通知提醒类型编号
     * 1: 评论回复, 2: 点赞, 3: 留言回复, 4: 关注, 5: 站长通知, 6: 站内反馈
     */
    @TableField("remind_id")
    private Integer remindId;

    /**
     * 操作者的ID，"000"代表系统发送
     */
    @TableField("sender_id")
    private String senderId;

    /**
     * 操作者用户名
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 操作者的动作，如：捐款、更新、评论、收藏
     * 【1：评论, 2：收藏（爱上），3：关注，5：站内通知】
     */
    @TableField("sender_action")
    private String senderAction;

    /**
     * 目标对象ID
     */
    @TableField("object_id")
    private String objectId;

    /**
     * 目标对象内容或简介，比如：文章标题
     */
    @TableField("object")
    private String object;

    /**
     * 被操作对象类型，如：人、文章、活动、视频等[1：人，2：文章，3：推送]
     */
    @TableField("object_type")
    private String objectType;

    /**
     * 关联资源链接
     */
    @TableField("object_links")
    private String objectLinks;

    /**
     * 消息接收者ID
     */
    @TableField("recipient_id")
    private String recipientId;

    /**
     * 消息内容
     */
    @TableField("message")
    private String message;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 是否阅读，【0：未读，1：已读】
     */
    @TableField("status")
    private String status;

    /**
     * 阅读时间
     */
    @TableField("read_at")
    private Date readAt;

    /**
     * 格式化后的创建时间 (仅用于展示)
     */
    @TableField(exist = false)
    private String createTime;
}