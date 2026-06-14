package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("bc_env_cfg")
public class EnvCfg implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer lCfgId;

    private String vcCfgName;

    private String vcCfgValue;

    private String vcDesc;

    private String cStatus;

    private Date dMdfTime;
}