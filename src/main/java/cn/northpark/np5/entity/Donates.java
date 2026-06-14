package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bc_donates")
public class Donates implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String addTime;

    private String productName;

    private String mercOrderId;

    private String alipayTransId;

    private String accountId;

    private String accountName;

    private String orderAmount;

    private String refundAmount;

    private String serviceCharge;

    private String tradingStatus;

    private String serviceRefund;

    private String merchantReceive;

    private String merchantOffers;

    private String branchOffice;

    private String bandOrderId;

    private String transactionType;

    private String transactionMeans;

    private String storeName;

    private String rewardMsg;
}