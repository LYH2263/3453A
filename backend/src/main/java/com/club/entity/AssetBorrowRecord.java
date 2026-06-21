package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("asset_borrow_records")
public class AssetBorrowRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer assetId;
    private Integer borrowerId;
    private Integer quantity;
    private String status;
    private LocalDateTime borrowTime;
    private LocalDateTime returnTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String assetName;
    @TableField(exist = false)
    private String borrowerName;
    @TableField(exist = false)
    private Integer clubId;
    @TableField(exist = false)
    private String specification;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getAssetId() { return assetId; }
    public void setAssetId(Integer assetId) { this.assetId = assetId; }

    public Integer getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Integer borrowerId) { this.borrowerId = borrowerId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBorrowTime() { return borrowTime; }
    public void setBorrowTime(LocalDateTime borrowTime) { this.borrowTime = borrowTime; }

    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public Integer getClubId() { return clubId; }
    public void setClubId(Integer clubId) { this.clubId = clubId; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
}
