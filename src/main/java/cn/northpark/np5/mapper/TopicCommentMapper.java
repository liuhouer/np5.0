package cn.northpark.np5.mapper;

import cn.northpark.np5.entity.TopicComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 主题评论 Mapper 接口
 * 
 * @author bruce
 */
@Mapper
public interface TopicCommentMapper extends BaseMapper<TopicComment> {

    /**
     * 根据主题ID和类型查询评论列表
     * 
     * @param topicId 主题ID
     * @param topicType 主题类型
     * @return 评论列表
     */
    @Select("SELECT * FROM bc_topic_comment WHERE topic_id = #{topicId} AND topic_type = #{topicType} ORDER BY add_time DESC")
    List<TopicComment> selectByTopicAndType(@Param("topicId") Integer topicId, 
                                             @Param("topicType") String topicType);
}