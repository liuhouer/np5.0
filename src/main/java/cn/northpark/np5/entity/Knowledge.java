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
@TableName("bc_knowledge")
public class Knowledge implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String retCode;

    private String title;

    private String briefImg;

    private String brief;

    private String postDate;

    private Integer price;

    private String tags;

    private String tagsCode;

    private String retUrl;

    private String linkUrl;

    private Long viewTimes;

    private String color;

    private Integer hotIndex;

    private String displayed;

    private String content;

    private String path;

    @TableField(exist = false)
    private List<Map<String, String>> tagList;
}