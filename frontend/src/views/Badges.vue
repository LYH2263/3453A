<template>
  <div class="badges-page">
    <div class="page-header glass-card">
      <h2 class="page-title">徽章墙</h2>
      <p class="page-desc">探索各社团的荣誉徽章，见证社员的成长与荣耀</p>
    </div>

    <div class="filter-bar glass-card">
      <span class="filter-label">按社团筛选：</span>
      <el-select v-model="selectedClubId" placeholder="全部社团" clearable @change="loadBadges" style="width: 200px">
        <el-option label="全部社团" :value="null" />
        <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
      </el-select>
      <span class="badge-count">共 {{ badges.length }} 枚徽章</span>
    </div>

    <div class="badges-grid" v-loading="loading">
      <div v-if="!badges.length && !loading" class="empty-state">
        <el-empty description="暂无徽章" />
      </div>
      <div v-for="badge in badges" :key="badge.id" class="badge-card" @click="showBadgeDetail(badge)">
        <div class="badge-icon-wrap">
          <img v-if="badge.iconUrl" :src="badge.iconUrl" :alt="badge.name" class="badge-icon" />
          <div v-else class="badge-icon-placeholder">
            <el-icon :size="36"><Medal /></el-icon>
          </div>
          <div v-if="badge.isPublic === 0" class="private-badge">
            <el-icon><Lock /></el-icon>
          </div>
        </div>
        <div class="badge-info">
          <h3 class="badge-name">{{ badge.name }}</h3>
          <p class="badge-desc">{{ badge.description || '暂无描述' }}</p>
          <div class="badge-meta">
            <span class="club-tag">{{ badge.clubName }}</span>
            <span class="recipient-count">
              <el-icon><User /></el-icon>
              {{ badge.recipientCount || 0 }} 人获得
            </span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showDetail" :title="currentBadge?.name" width="500px">
      <div class="detail-content" v-if="currentBadge">
        <div class="detail-icon">
          <img v-if="currentBadge.iconUrl" :src="currentBadge.iconUrl" :alt="currentBadge.name" />
          <div v-else class="detail-icon-placeholder">
            <el-icon :size="48"><Medal /></el-icon>
          </div>
        </div>
        <p class="detail-desc">{{ currentBadge.description || '暂无描述' }}</p>
        <div class="detail-meta">
          <div class="meta-item">
            <span class="meta-label">所属社团</span>
            <span class="meta-value">{{ currentBadge.clubName }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">获得人数</span>
            <span class="meta-value">{{ currentBadge.recipientCount || 0 }} 人</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">创建时间</span>
            <span class="meta-value">{{ formatDate(currentBadge.createTime) }}</span>
          </div>
        </div>

        <el-divider>获得者</el-divider>
        <div class="recipients-list" v-loading="recipientsLoading">
          <div v-if="!recipients.length && !recipientsLoading" class="no-recipients">
            暂无获得者
          </div>
          <div v-for="r in recipients" :key="r.userBadgeId" class="recipient-item">
            <el-avatar :size="32" :src="r.userAvatar" />
            <div class="recipient-info">
              <span class="recipient-name">{{ r.userName }}</span>
              <span class="recipient-time">{{ formatDate(r.grantedTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Medal, Lock, User } from '@element-plus/icons-vue'
import { badgeApi, type Badge } from '../api/badge'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const badges = ref<Badge[]>([])
const clubs = ref<any[]>([])
const selectedClubId = ref<number | null>(null)
const loading = ref(false)

const showDetail = ref(false)
const currentBadge = ref<Badge | null>(null)
const recipients = ref<any[]>([])
const recipientsLoading = ref(false)

const loadClubs = async () => {
  try {
    const res: any = await request.get('/clubs', { params: { pageNum: 1, pageSize: 100 } })
    clubs.value = res.records || []
  } catch (err) {
    console.error('Failed to load clubs:', err)
  }
}

const loadBadges = async () => {
  loading.value = true
  try {
    const params: any = { isPublic: true }
    if (selectedClubId.value) {
      params.clubId = selectedClubId.value
    }
    const res: any = await badgeApi.getBadgeList(params)
    badges.value = res
  } catch (err) {
    console.error('Failed to load badges:', err)
    ElMessage.error('加载徽章失败')
  } finally {
    loading.value = false
  }
}

const showBadgeDetail = async (badge: Badge) => {
  currentBadge.value = badge
  showDetail.value = true
  recipients.value = []
  recipientsLoading.value = true
  try {
    const res: any = await badgeApi.getBadgeRecipients(badge.id)
    recipients.value = res
  } catch (err) {
    console.error('Failed to load recipients:', err)
  } finally {
    recipientsLoading.value = false
  }
}

const formatDate = (dt: string | undefined) => {
  if (!dt) return '-'
  return new Date(dt).toLocaleDateString('zh-CN')
}

onMounted(async () => {
  await loadClubs()
  await loadBadges()
})
</script>

<style scoped>
.badges-page {
  padding: 20px;
}

.page-header {
  padding: 24px;
  margin-bottom: 20px;
  text-align: center;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.page-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
}

.badge-count {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.badges-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.badge-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.badge-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.badge-icon-wrap {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.badge-icon {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 50%;
}

.badge-icon-placeholder {
  color: #fff;
}

.private-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.badge-info {
  text-align: center;
}

.badge-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.badge-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.badge-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.club-tag {
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 11px;
}

.recipient-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

.detail-content {
  text-align: center;
}

.detail-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  overflow: hidden;
}

.detail-icon img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 50%;
}

.detail-desc {
  color: #606266;
  margin-bottom: 20px;
  line-height: 1.6;
}

.detail-meta {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  background: #f5f7fa;
  border-radius: 8px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label {
  font-size: 12px;
  color: #909399;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.recipients-list {
  max-height: 240px;
  overflow-y: auto;
}

.recipient-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.recipient-item:last-child {
  border-bottom: none;
}

.recipient-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.recipient-name {
  font-size: 14px;
  color: #303133;
}

.recipient-time {
  font-size: 12px;
  color: #909399;
}

.no-recipients {
  text-align: center;
  padding: 20px 0;
  color: #909399;
  font-size: 13px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}
</style>
