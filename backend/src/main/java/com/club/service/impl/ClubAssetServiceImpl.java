package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.entity.AssetBorrowRecord;
import com.club.entity.ClubAsset;
import com.club.entity.User;
import com.club.mapper.AssetBorrowRecordMapper;
import com.club.mapper.ClubAssetMapper;
import com.club.mapper.UserMapper;
import com.club.service.ClubAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClubAssetServiceImpl extends ServiceImpl<ClubAssetMapper, ClubAsset> implements ClubAssetService {

    @Autowired
    private ClubAssetMapper assetMapper;

    @Autowired
    private AssetBorrowRecordMapper borrowRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<?> listAssets(Integer clubId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ClubAsset> wrapper = new LambdaQueryWrapper<>();
        User currentUser = getCurrentUser();
        if (currentUser != null && RoleConstants.MEMBER.equals(currentUser.getRole())) {
            if (currentUser.getClubId() == null) {
                return Result.success(new Page<>(pageNum, pageSize));
            }
            wrapper.eq(ClubAsset::getClubId, currentUser.getClubId());
        } else if (clubId != null) {
            wrapper.eq(ClubAsset::getClubId, clubId);
        }
        wrapper.orderByDesc(ClubAsset::getCreateTime);
        Page<ClubAsset> page = assetMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @Override
    public Result<?> createAsset(ClubAsset asset) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("用户未认证");

        if (RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
            if (currentUser.getClubId() == null) return Result.error("您尚未绑定任何社团");
            asset.setClubId(currentUser.getClubId());
        }

        if (asset.getStock() == null || asset.getStock() < 0) {
            return Result.error("库存数量不能为负");
        }

        this.save(asset);
        return Result.success(null);
    }

    @Override
    public Result<?> updateAsset(Integer id, ClubAsset asset) {
        ClubAsset existing = this.getById(id);
        if (existing == null) return Result.error("物资不存在");

        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("用户未认证");

        if (RoleConstants.CLUB_LEADER.equals(currentUser.getRole()) && !existing.getClubId().equals(currentUser.getClubId())) {
            return Result.error("无权修改其他社团的物资");
        }

        asset.setId(id);
        asset.setClubId(existing.getClubId());
        if (asset.getStock() != null && asset.getStock() < 0) {
            return Result.error("库存数量不能为负");
        }
        this.updateById(asset);
        return Result.success(null);
    }

    @Override
    public Result<?> deleteAsset(Integer id) {
        ClubAsset existing = this.getById(id);
        if (existing == null) return Result.error("物资不存在");

        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("用户未认证");

        if (RoleConstants.CLUB_LEADER.equals(currentUser.getRole()) && !existing.getClubId().equals(currentUser.getClubId())) {
            return Result.error("无权删除其他社团的物资");
        }

        long pendingCount = borrowRecordMapper.selectCount(new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getAssetId, id)
                .in(AssetBorrowRecord::getStatus, "PENDING", "APPROVED"));
        if (pendingCount > 0) {
            return Result.error("该物资存在未归还的借还记录，无法删除");
        }

        this.removeById(id);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<?> applyBorrow(Integer assetId, Integer borrowerId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return Result.error("借用数量必须大于0");
        }

        ClubAsset asset = assetMapper.selectByIdForUpdate(assetId);
        if (asset == null) return Result.error("物资不存在");

        if (quantity > asset.getStock()) {
            return Result.error("可用库存不足，当前可借数量：" + asset.getStock());
        }

        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(assetId);
        record.setBorrowerId(borrowerId);
        record.setQuantity(quantity);
        record.setStatus("PENDING");
        borrowRecordMapper.insert(record);

        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<?> approveBorrow(Integer recordId) {
        AssetBorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) return Result.error("借还记录不存在");
        if (!"PENDING".equals(record.getStatus())) return Result.error("只有待审批的申请才能审批");

        ClubAsset asset = assetMapper.selectByIdForUpdate(record.getAssetId());
        if (asset == null) return Result.error("物资不存在");

        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("用户未认证");

        if (RoleConstants.CLUB_LEADER.equals(currentUser.getRole()) && !asset.getClubId().equals(currentUser.getClubId())) {
            return Result.error("无权审批其他社团的物资");
        }

        if (asset.getStock() < record.getQuantity()) {
            return Result.error("可用库存不足，当前可借数量：" + asset.getStock());
        }

        asset.setStock(asset.getStock() - record.getQuantity());
        assetMapper.updateById(asset);

        record.setStatus("APPROVED");
        record.setBorrowTime(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        return Result.success(null);
    }

    @Override
    public Result<?> rejectBorrow(Integer recordId) {
        AssetBorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) return Result.error("借还记录不存在");
        if (!"PENDING".equals(record.getStatus())) return Result.error("只有待审批的申请才能驳回");

        ClubAsset asset = assetMapper.selectById(record.getAssetId());
        if (asset == null) return Result.error("物资不存在");

        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("用户未认证");

        if (RoleConstants.CLUB_LEADER.equals(currentUser.getRole()) && !asset.getClubId().equals(currentUser.getClubId())) {
            return Result.error("无权驳回其他社团的物资申请");
        }

        record.setStatus("REJECTED");
        borrowRecordMapper.updateById(record);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<?> confirmReturn(Integer recordId) {
        AssetBorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) return Result.error("借还记录不存在");
        if (!"APPROVED".equals(record.getStatus())) return Result.error("只有已借出的记录才能确认归还");

        ClubAsset asset = assetMapper.selectByIdForUpdate(record.getAssetId());
        if (asset == null) return Result.error("物资不存在");

        User currentUser = getCurrentUser();
        if (currentUser == null) return Result.error("用户未认证");

        if (RoleConstants.CLUB_LEADER.equals(currentUser.getRole()) && !asset.getClubId().equals(currentUser.getClubId())) {
            return Result.error("无权确认其他社团的物资归还");
        }

        asset.setStock(asset.getStock() + record.getQuantity());
        assetMapper.updateById(asset);

        record.setStatus("RETURNED");
        record.setReturnTime(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        return Result.success(null);
    }

    @Override
    public Result<?> listBorrowRecords(Integer clubId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AssetBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        User currentUser = getCurrentUser();

        Integer targetClubId = null;
        if (currentUser != null && RoleConstants.MEMBER.equals(currentUser.getRole())) {
            targetClubId = currentUser.getClubId();
        } else if (currentUser != null && RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
            targetClubId = currentUser.getClubId();
            if (clubId != null) targetClubId = clubId;
        } else if (clubId != null) {
            targetClubId = clubId;
        }

        if (targetClubId != null) {
            wrapper.inSql(AssetBorrowRecord::getAssetId,
                    "SELECT id FROM club_assets WHERE club_id = " + targetClubId);
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq(AssetBorrowRecord::getStatus, status);
        }
        wrapper.orderByDesc(AssetBorrowRecord::getCreateTime);

        Page<AssetBorrowRecord> page = borrowRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        for (AssetBorrowRecord record : page.getRecords()) {
            enrichRecord(record);
        }

        return Result.success(page);
    }

    @Override
    public Result<?> listMyBorrowRecords(Integer borrowerId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AssetBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetBorrowRecord::getBorrowerId, borrowerId);
        wrapper.orderByDesc(AssetBorrowRecord::getCreateTime);

        Page<AssetBorrowRecord> page = borrowRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        for (AssetBorrowRecord record : page.getRecords()) {
            enrichRecord(record);
        }

        return Result.success(page);
    }

    private void enrichRecord(AssetBorrowRecord record) {
        ClubAsset asset = assetMapper.selectById(record.getAssetId());
        if (asset != null) {
            record.setAssetName(asset.getName());
            record.setClubId(asset.getClubId());
            record.setSpecification(asset.getSpecification());
        }

        User borrower = userMapper.selectById(record.getBorrowerId());
        if (borrower != null) {
            record.setBorrowerName(borrower.getRealName());
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, auth.getName()));
    }
}
