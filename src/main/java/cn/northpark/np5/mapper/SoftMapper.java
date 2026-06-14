package cn.northpark.np5.mapper;

import cn.northpark.np5.entity.Soft;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SoftMapper extends BaseMapper<Soft> {

    @Select("${sqlExpr}")
    List<Map<String, Object>> querySqlMap(@Param("sqlExpr") String sql);
}