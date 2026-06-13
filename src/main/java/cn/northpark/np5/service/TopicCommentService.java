package cn.northpark.np5.service;

import cn.northpark.np5.entity.TopicComment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 主题评论服务接口
 * 
 * @author bruce
 */
public interface TopicCommentService extends IService<TopicComment> {

    /**
     * 添加评论
     * 
     * @param topicComment 评论对象
     * @return 是否成功
     */
    boolean addComment(TopicComment topicComment);

    /**
     * 根据主题ID和类型查询评论列表
     * 
     * @param topicId 主题ID
     * @param topicType 主题类型
     * @return 评论列表
     */
    List<TopicComment> getCommentsByTopicAndType(Integer topicId, String topicType);
}