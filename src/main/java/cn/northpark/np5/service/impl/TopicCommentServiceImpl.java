package cn.northpark.np5.service.impl;

import cn.northpark.np5.constant.TopicTypeEnum;
import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.entity.TopicComment;
import cn.northpark.np5.mapper.TopicCommentMapper;
import cn.northpark.np5.notify.NotifyEnum;
import cn.northpark.np5.service.NotifyRemindService;
import cn.northpark.np5.service.TopicCommentService;
import cn.northpark.np5.utils.NotifyUtil;
import cn.northpark.np5.utils.PinyinUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 主题评论服务实现类
 * 
 * @author bruce
 */
@Slf4j
@Service
public class TopicCommentServiceImpl extends ServiceImpl<TopicCommentMapper, TopicComment> implements TopicCommentService {

    @Autowired
    private TopicCommentMapper topicCommentMapper;

    @Autowired
    private NotifyRemindService notifyRemindService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addComment(TopicComment topicComment) {
        try {
            // 生成用户名首字母
            if (topicComment.getFromUname() != null) {
                String firstChar = PinyinUtil.getFirstChar(topicComment.getFromUname());
                topicComment.setFromSpan(firstChar);
            }

            // 设置添加时间
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            topicComment.setAddTime(currentTime);

            // 保存评论
            boolean result = this.save(topicComment);

            // 异步发送消息提醒
            if (result) {
                sendNotifyAsync(topicComment);
            }

            return result;
        } catch (Exception e) {
            log.error("添加评论失败", e);
            throw new RuntimeException("添加评论失败: " + e.getMessage());
        }
    }

    @Override
    public List<TopicComment> getCommentsByTopicAndType(Integer topicId, String topicType) {
        return topicCommentMapper.selectByTopicAndType(topicId, topicType);
    }

    /**
     * 异步发送消息提醒
     * 
     * @param topicComment 评论对象
     */
    @Async
    protected void sendNotifyAsync(TopicComment topicComment) {
        try {
            // 获取通知类型名称
            String notifyName = TopicTypeEnum.getMatchNotifyName(topicComment.getTopicType());
            if (notifyName == null) {
                log.warn("未匹配到通知类型: {}", topicComment.getTopicType());
                return;
            }

            // 获取目标内容信息
            Map<String, String> objectContent = NotifyUtil.getObjectContent(
                topicComment.getTopicType(), 
                topicComment.getTopicId()
            );

            if (objectContent == null || objectContent.isEmpty()) {
                log.warn("未找到目标内容: topicType={}, topicId={}", 
                    topicComment.getTopicType(), topicComment.getTopicId());
                return;
            }

            // 确定消息接收者
            String recipientId = topicComment.getToUid() != null 
                ? topicComment.getToUid().toString() 
                : objectContent.get("by"); // 如果是回复评论则通知被回复者,否则通知内容作者

            if (recipientId == null || recipientId.equals(topicComment.getFromUid().toString())) {
                // 不给自己发通知
                return;
            }

            // 匹配通知枚举
            NotifyEnum notifyEnum = NotifyEnum.match(notifyName);
            if (notifyEnum == null) {
                log.warn("未匹配到通知枚举: {}", notifyName);
                return;
            }

            // 构建通知消息
            NotifyRemind notifyRemind = NotifyRemind.builder()
                .remindId(getRemindIdByNotifyName(notifyName))
                .senderId(topicComment.getFromUid().toString())
                .senderName(topicComment.getFromUname())
                .senderAction("1") // 1: 评论
                .objectId(topicComment.getTopicId().toString())
                .object(objectContent.get("title"))
                .objectType("2") // 2: 文章
                .objectLinks(objectContent.get("href"))
                .recipientId(recipientId)
                .message(topicComment.getContent())
                .createdAt(new Date())
                .status("0") // 0: 未读
                .build();

            // 保存通知
            notifyRemindService.save(notifyRemind);

            log.info("评论通知发送成功: topicId={}, fromUser={}, toUser={}", 
                topicComment.getTopicId(), topicComment.getFromUname(), recipientId);

        } catch (Exception e) {
            log.error("发送评论通知失败", e);
        }
    }

    /**
     * 根据通知名称获取 remindId
     */
    private Integer getRemindIdByNotifyName(String notifyName) {
        switch (notifyName) {
            case "ART_REPLY":
                return 1; // 评论回复
            case "NOTE_REPLY":
                return 3; // 留言回复
            default:
                return 1;
        }
    }
}