package com.club.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.ClubAsset;

public interface ClubAssetService extends IService<ClubAsset> {
    Result<?> listAssets(Integer clubId, Integer pageNum, Integer pageSize);
    Result<?> createAsset(ClubAsset asset);
    Result<?> updateAsset(Integer id, ClubAsset asset);
    Result<?> deleteAsset(Integer id);
    Result<?> applyBorrow(Integer assetId, Integer borrowerId, Integer quantity);
    Result<?> approveBorrow(Integer recordId);
    Result<?> rejectBorrow(Integer recordId);
    Result<?> confirmReturn(Integer recordId);
    Result<?> listBorrowRecords(Integer clubId, String status, Integer pageNum, Integer pageSize);
    Result<?> listMyBorrowRecords(Integer borrowerId, Integer pageNum, Integer pageSize);
}
