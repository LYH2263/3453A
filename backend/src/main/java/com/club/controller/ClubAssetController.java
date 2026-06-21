package com.club.controller;

import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.entity.ClubAsset;
import com.club.service.ClubAssetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class ClubAssetController {

    @Autowired
    private ClubAssetService assetService;

    @Log("查询物资列表")
    @GetMapping
    public Result<?> listAssets(
            @RequestParam(required = false) Integer clubId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return assetService.listAssets(clubId, pageNum, pageSize);
    }

    @Log("登记物资")
    @PostMapping
    public Result<?> createAsset(@Valid @RequestBody ClubAsset asset) {
        return assetService.createAsset(asset);
    }

    @Log("更新物资")
    @PutMapping("/{id}")
    public Result<?> updateAsset(@PathVariable Integer id, @RequestBody ClubAsset asset) {
        return assetService.updateAsset(id, asset);
    }

    @Log("删除物资")
    @DeleteMapping("/{id}")
    public Result<?> deleteAsset(@PathVariable Integer id) {
        return assetService.deleteAsset(id);
    }

    @Log("申请借用物资")
    @PostMapping("/borrow")
    public Result<?> applyBorrow(@RequestBody Map<String, Integer> params) {
        Integer assetId = params.get("assetId");
        Integer borrowerId = params.get("borrowerId");
        Integer quantity = params.getOrDefault("quantity", 1);
        return assetService.applyBorrow(assetId, borrowerId, quantity);
    }

    @Log("审批借用申请")
    @PostMapping("/borrow/{id}/approve")
    public Result<?> approveBorrow(@PathVariable Integer id) {
        return assetService.approveBorrow(id);
    }

    @Log("驳回借用申请")
    @PostMapping("/borrow/{id}/reject")
    public Result<?> rejectBorrow(@PathVariable Integer id) {
        return assetService.rejectBorrow(id);
    }

    @Log("确认归还物资")
    @PostMapping("/borrow/{id}/return")
    public Result<?> confirmReturn(@PathVariable Integer id) {
        return assetService.confirmReturn(id);
    }

    @Log("查询借还记录")
    @GetMapping("/borrow/records")
    public Result<?> listBorrowRecords(
            @RequestParam(required = false) Integer clubId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return assetService.listBorrowRecords(clubId, status, pageNum, pageSize);
    }

    @Log("查询我的借还记录")
    @GetMapping("/borrow/my")
    public Result<?> listMyBorrowRecords(
            @RequestParam Integer borrowerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return assetService.listMyBorrowRecords(borrowerId, pageNum, pageSize);
    }
}
