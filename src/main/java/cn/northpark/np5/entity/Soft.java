package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@TableName("bc_soft")
@Alias("soft_linked")
public class Soft implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String retCode;

    private String title;

    private String contentMinio;

    private String year;

    private String month;

    private String postDate;

    private String os;

    private String tags;

    private String tagsCode;

    private String retUrl;

    private String color;

    private Integer hotIndex;

    private String displayed;

    private String useMinio;

    private String brief;

    private String content;

    private String path;

    @TableField(exist = false)
    private List<Map<String, String>> tagList;

    @TableField(exist = false)
    private String briefImg;

    @TableField(exist = false)
    private String briefShow;
}