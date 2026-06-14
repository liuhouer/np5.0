package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@TableName("bc_movies")
public class Movies implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String retCode;

    private String movieName;

    private Integer price;

    private String addTime;

    private String tag;

    private String tagCode;

    private Integer viewNum;

    private String color;

    private Integer hotIndex;

    private String displayed;

    private String brief;

    private String movieDesc;

    private String path;

    @TableField(exist = false)
    private List<Map<String, String>> tagList;

    @TableField(exist = false)
    private String imgUrl;
}