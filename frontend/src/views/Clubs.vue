<template>
  <div class="clubs-page">
    <div class="toolbar glass-card mb-20">
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">申请创建社团</el-button>
    </div>

    <el-table :data="clubs" class="glass-card" v-loading="loading">
      <el-table-column label="Logo" width="80">
        <template #default="{ row }">
          <el-avatar :size="40" :src="row.logo" shape="square" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="社团名称" width="200" />
      <el-table-column prop="description" label="简介" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'">
            {{ row.status === 'NORMAL' ? '正常' : '已注销' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button link type="success" @click="showClubBadges(row)">徽章</el-button>
          <el-button v-if="isAdmin" link type="danger" @click="handleDelete(row)">确认删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container mt-20" style="display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchClubs"
      />
    </div>

    <!-- 申请创建社团弹窗 -->
    <el-dialog v-model="showAddDialog" title="申请创建社团" width="500px">
      <div style="text-align: center; padding: 20px 0;">
        <el-icon :size="50" color="#409eff" style="margin-bottom: 20px"><InfoFilled /></el-icon>
        <p>线上申请功能正在加紧开发中！</p>
        <p style="color: #909399; font-size: 13px; margin-top: 10px;">目前请携带领表前往校社联办公室（学生活动中心204）办理纸质申请手续。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="showAddDialog = false">我知道了</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 社团公开徽章弹窗 -->
    <el-dialog v-model="showBadgesDialog" :title="`${currentClub?.name || ''} - 公开徽章`" width="600px">
      <div v-loading="badgesLoading">
        <el-empty v-if="!clubBadges.length && !badgesLoading" description="该社团暂无公开徽章" />
        <div v-else class="club-badges-list">
          <div v-for="badge in clubBadges" :key="badge.id" class="club-badge-item">
            <div class="club-badge-icon">
              <img v-if="badge.iconUrl" :src="badge.iconUrl" :alt="badge.name" />
              <el-icon v-else :size="28"><Medal /></el-icon>
            </div>
            <div class="club-badge-info">
              <h4 class="club-badge-name">{{ badge.name }}</h4>
              <p class="club-badge-desc">{{ badge.description || '暂无描述' }}</p>
              <div class="club-badge-meta">
                <span class="recipient-count">
                  <el-icon><User /></el-icon>
                  {{ badge.recipientCount || 0 }} 人获得
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { Plus, InfoFilled, Medal, User } from '@element-plus/icons-vue'
import request from '../utils/request'
import { badgeApi } from '../api/badge'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const clubs = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})
const showAddDialog = ref(false)
const showBadgesDialog = ref(false)
const currentClub = ref<any>(null)
const clubBadges = ref<any[]>([])
const badgesLoading = ref(false)

const fetchClubs = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/clubs', { params: queryParams })
    clubs.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要注销社团 [${row.name}] 吗?`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(async () => {
    // 调用删除API
    ElMessage.success('已申请注销')
  }).catch(() => {
    // 用户取消删除，防止抛出 Uncaught (in promise) cancel
  })
}

const handleDetail = (row: any) => {
  ElMessage.info(`查看 [${row.name}] 的详细信息功能暂未开放`)
}

const showClubBadges = async (row: any) => {
  currentClub.value = row
  showBadgesDialog.value = true
  clubBadges.value = []
  badgesLoading.value = true
  try {
    const res: any = await badgeApi.getClubPublicBadges(row.id)
    clubBadges.value = res
  } catch (err) {
    console.error('Failed to load club badges:', err)
    ElMessage.error('加载徽章失败')
  } finally {
    badgesLoading.value = false
  }
}

onMounted(fetchClubs)
</script>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
.toolbar {
  padding: 15px 20px;
}

.club-badges-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.club-badge-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.club-badge-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  overflow: hidden;
}

.club-badge-icon img {
  width: 30px;
  height: 30px;
  object-fit: cover;
  border-radius: 50%;
}

.club-badge-info {
  flex: 1;
  min-width: 0;
}

.club-badge-name {
  margin: 0 0 4px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.club-badge-desc {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.club-badge-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.recipient-count {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
