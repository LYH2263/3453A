<template>
  <div class="badges-page">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 公共徽章墙 -->
      <el-tab-pane label="徽章墙" name="wall">
        <div class="filter-bar">
          <span class="filter-label">按社团筛选：</span>
          <el-select v-model="selectedClubId" placeholder="全部社团" clearable @change="loadPublicBadges" style="width: 200px">
            <el-option label="全部社团" :value="null" />
            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
          </el-select>
          <span class="badge-count">共 {{ publicBadges.length }} 枚公开徽章</span>
        </div>

        <div class="badges-grid" v-loading="wallLoading">
          <div v-if="!publicBadges.length && !wallLoading" class="empty-state">
            <el-empty description="暂无公开徽章" />
          </div>
          <div v-for="badge in publicBadges" :key="badge.id" class="badge-card" @click="openBadgeDetail(badge)">
            <div class="badge-icon-wrap">
              <img v-if="badge.iconUrl" :src="badge.iconUrl" :alt="badge.name" class="badge-icon-img" />
              <div v-else class="badge-icon-placeholder"><el-icon :size="36"><Medal /></el-icon></div>
            </div>
            <div class="badge-info">
              <h3 class="badge-name">{{ badge.name }}</h3>
              <p class="badge-desc">{{ badge.description || '暂无描述' }}</p>
              <div class="badge-meta">
                <span class="club-tag">{{ badge.clubName }}</span>
                <span class="recipient-count"><el-icon><User /></el-icon> {{ badge.recipientCount || 0 }} 人获得</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 社团徽章管理 -->
      <el-tab-pane label="徽章管理" name="manage" v-if="isManager">
        <div class="manage-toolbar">
          <el-button type="primary" @click="openCreateDialog"><el-icon><Plus /></el-icon> 新建徽章</el-button>
          <el-button @click="loadManagedBadges" :loading="manageLoading">刷新</el-button>
        </div>

        <el-empty v-if="!managedBadges.length && !manageLoading" description="暂无徽章，点击上方按钮创建" />

        <div v-for="badge in managedBadges" :key="badge.id" class="manage-badge-card">
          <div class="manage-badge-header">
            <div class="manage-badge-icon">
              <img v-if="badge.iconUrl" :src="badge.iconUrl" :alt="badge.name" />
              <el-icon v-else :size="28"><Medal /></el-icon>
            </div>
            <div class="manage-badge-info">
              <div class="manage-badge-title">
                <span class="manage-badge-name">{{ badge.name }}</span>
                <el-tag v-if="badge.isPublic === 0" type="info" size="small">不公开</el-tag>
                <el-tag v-else type="success" size="small">公开</el-tag>
              </div>
              <p class="manage-badge-desc">{{ badge.description || '暂无描述' }}</p>
              <div class="manage-badge-meta">
                <span>{{ badge.clubName }}</span>
                <span>{{ badge.recipientCount || 0 }} 人获得</span>
              </div>
            </div>
            <div class="manage-badge-actions">
              <el-button size="small" type="primary" plain @click="openGrantDialog(badge)">发放</el-button>
              <el-button size="small" type="warning" plain @click="openEditDialog(badge)">编辑</el-button>
              <el-popconfirm title="确定删除该徽章？" @confirm="handleDeleteBadge(badge.id)">
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>

          <div class="grant-list" v-if="badge.grants && badge.grants.length">
            <div class="grant-list-header">已获得者 ({{ badge.grants.length }})</div>
            <div class="grant-items">
              <div v-for="g in badge.grants" :key="g.userBadgeId" class="grant-item">
                <el-avatar :size="28" :src="g.userAvatar" />
                <span class="grant-user-name">{{ g.userName }}</span>
                <span class="grant-time">{{ formatDate(g.grantedTime) }}</span>
                <el-popconfirm title="确定收回该徽章？" @confirm="handleRevoke(g.userBadgeId)">
                  <template #reference>
                    <el-button size="small" type="danger" link>收回</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 公共徽章详情弹窗 -->
    <el-dialog v-model="showDetailDialog" :title="currentBadge?.name" width="500px">
      <div class="detail-content" v-if="currentBadge">
        <div class="detail-icon">
          <img v-if="currentBadge.iconUrl" :src="currentBadge.iconUrl" :alt="currentBadge.name" />
          <div v-else class="detail-icon-placeholder"><el-icon :size="48"><Medal /></el-icon></div>
        </div>
        <p class="detail-desc">{{ currentBadge.description || '暂无描述' }}</p>
        <div class="detail-meta">
          <div class="meta-item"><span class="meta-label">所属社团</span><span class="meta-value">{{ currentBadge.clubName }}</span></div>
          <div class="meta-item"><span class="meta-label">获得人数</span><span class="meta-value">{{ currentBadge.recipientCount || 0 }} 人</span></div>
          <div class="meta-item"><span class="meta-label">创建时间</span><span class="meta-value">{{ formatDate(currentBadge.createTime) }}</span></div>
        </div>
        <el-divider>获得者</el-divider>
        <div class="recipients-list" v-loading="recipientsLoading">
          <div v-if="!recipients.length && !recipientsLoading" class="no-recipients">暂无获得者</div>
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

    <!-- 新建/编辑徽章弹窗 -->
    <el-dialog v-model="showFormDialog" :title="formBadge.id ? '编辑徽章' : '新建徽章'" width="480px" @close="resetForm">
      <el-form :model="formBadge" label-width="80px">
        <el-form-item label="徽章名称" required>
          <el-input v-model="formBadge.name" placeholder="输入徽章名称" />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="formBadge.iconUrl" placeholder="输入图标图片链接（可选）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formBadge.description" type="textarea" :rows="3" placeholder="输入徽章描述" />
        </el-form-item>
        <el-form-item label="是否公开">
          <el-switch v-model="formBadge.isPublicBool" active-text="公开" inactive-text="不公开" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveBadge" :loading="formSaving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发放徽章弹窗 -->
    <el-dialog v-model="showGrantDialog" :title="`发放徽章 — ${grantBadgeName}`" width="480px">
      <div class="grant-member-list" v-loading="membersLoading">
        <el-input v-model="memberSearch" placeholder="搜索社员姓名" clearable style="margin-bottom: 12px" />
        <div v-for="m in filteredMembers" :key="m.id" class="grant-member-item">
          <el-avatar :size="32" :src="m.avatar" />
          <div class="grant-member-info">
            <span class="grant-member-name">{{ m.realName }}</span>
            <span class="grant-member-role">{{ roleLabel(m.role) }}</span>
          </div>
          <el-button size="small" type="primary" plain @click="handleGrant(m.id)" :loading="grantingUserId === m.id">
            发放
          </el-button>
        </div>
        <el-empty v-if="!filteredMembers.length && !membersLoading" description="无符合条件的社员" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Medal, User, Plus } from '@element-plus/icons-vue'
import { badgeApi, type Badge } from '../api/badge'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const isManager = computed(() => userStore.isClubLeader)
const myClubId = computed(() => userStore.userInfo?.clubId)

const activeTab = ref('wall')
const clubs = ref<any[]>([])
const selectedClubId = ref<number | null>(null)

const publicBadges = ref<Badge[]>([])
const wallLoading = ref(false)

const managedBadges = ref<any[]>([])
const manageLoading = ref(false)

const showDetailDialog = ref(false)
const currentBadge = ref<Badge | null>(null)
const recipients = ref<any[]>([])
const recipientsLoading = ref(false)

const showFormDialog = ref(false)
const formSaving = ref(false)
const formBadge = ref<{ id: number | null; name: string; iconUrl: string; description: string; isPublicBool: boolean }>({
  id: null, name: '', iconUrl: '', description: '', isPublicBool: true
})

const showGrantDialog = ref(false)
const grantBadgeId = ref<number>(0)
const grantBadgeName = ref('')
const members = ref<any[]>([])
const membersLoading = ref(false)
const memberSearch = ref('')
const grantingUserId = ref<number | null>(null)

const filteredMembers = computed(() => {
  if (!memberSearch.value) return members.value
  const kw = memberSearch.value.toLowerCase()
  return members.value.filter((m: any) => m.realName?.toLowerCase().includes(kw))
})

const loadClubs = async () => {
  try {
    const res: any = await request.get('/clubs', { params: { pageNum: 1, pageSize: 100 } })
    clubs.value = res.records || []
  } catch (err) {
    console.error('Failed to load clubs:', err)
  }
}

const loadPublicBadges = async () => {
  wallLoading.value = true
  try {
    const params: any = {}
    if (selectedClubId.value) params.clubId = selectedClubId.value
    const res: any = await badgeApi.getPublicBadgeList(params)
    publicBadges.value = res
  } catch (err) {
    console.error('Failed to load badges:', err)
    ElMessage.error('加载徽章失败')
  } finally {
    wallLoading.value = false
  }
}

const loadManagedBadges = async () => {
  manageLoading.value = true
  try {
    const res: any = await badgeApi.getManagedBadges()
    managedBadges.value = res
  } catch (err) {
    console.error('Failed to load managed badges:', err)
    ElMessage.error('加载管理徽章失败')
  } finally {
    manageLoading.value = false
  }
}

const openBadgeDetail = async (badge: Badge) => {
  currentBadge.value = badge
  showDetailDialog.value = true
  recipients.value = []
  recipientsLoading.value = true
  try {
    const res: any = await badgeApi.getBadgeRecipients(badge.id)
    recipients.value = res
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || ''
    if (msg.includes('不公开') || msg.includes('无权限')) {
      ElMessage.warning('该徽章不公开，无法查看详情')
      showDetailDialog.value = false
    }
  } finally {
    recipientsLoading.value = false
  }
}

const openCreateDialog = () => {
  formBadge.value = { id: null, name: '', iconUrl: '', description: '', isPublicBool: true }
  showFormDialog.value = true
}

const openEditDialog = (badge: any) => {
  formBadge.value = {
    id: badge.id,
    name: badge.name,
    iconUrl: badge.iconUrl || '',
    description: badge.description || '',
    isPublicBool: badge.isPublic === 1
  }
  showFormDialog.value = true
}

const resetForm = () => {
  formBadge.value = { id: null, name: '', iconUrl: '', description: '', isPublicBool: true }
}

const handleSaveBadge = async () => {
  if (!formBadge.value.name.trim()) {
    ElMessage.warning('请输入徽章名称')
    return
  }
  formSaving.value = true
  try {
    const payload: any = {
      clubId: myClubId.value,
      name: formBadge.value.name,
      iconUrl: formBadge.value.iconUrl || undefined,
      description: formBadge.value.description || undefined,
      isPublic: formBadge.value.isPublicBool ? 1 : 0
    }
    if (formBadge.value.id) {
      await badgeApi.updateBadge(formBadge.value.id, payload)
      ElMessage.success('徽章已更新')
    } else {
      await badgeApi.createBadge(payload)
      ElMessage.success('徽章已创建')
    }
    showFormDialog.value = false
    await loadManagedBadges()
    await loadPublicBadges()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  } finally {
    formSaving.value = false
  }
}

const handleDeleteBadge = async (id: number) => {
  try {
    await badgeApi.deleteBadge(id)
    ElMessage.success('徽章已删除')
    await loadManagedBadges()
    await loadPublicBadges()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '删除失败')
  }
}

const openGrantDialog = async (badge: any) => {
  grantBadgeId.value = badge.id
  grantBadgeName.value = badge.name
  memberSearch.value = ''
  showGrantDialog.value = true
  membersLoading.value = true
  try {
    const res: any = await badgeApi.getClubMembers(badge.clubId)
    members.value = res
  } catch (err) {
    console.error('Failed to load members:', err)
    ElMessage.error('加载社员列表失败')
  } finally {
    membersLoading.value = false
  }
}

const handleGrant = async (userId: number) => {
  grantingUserId.value = userId
  try {
    await badgeApi.grantBadge({ badgeId: grantBadgeId.value, userId })
    ElMessage.success('发放成功')
    await loadManagedBadges()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '发放失败')
  } finally {
    grantingUserId.value = null
  }
}

const handleRevoke = async (userBadgeId: number) => {
  try {
    await badgeApi.revokeBadge(userBadgeId)
    ElMessage.success('已收回')
    await loadManagedBadges()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '收回失败')
  }
}

const formatDate = (dt: string | undefined) => {
  if (!dt) return '-'
  return new Date(dt).toLocaleDateString('zh-CN')
}

const roleLabel = (role: string) => {
  const map: Record<string, string> = {
    CLUB_LEADER: '负责人', MEMBER: '社员'
  }
  return map[role] || role
}

onMounted(async () => {
  await loadClubs()
  await loadPublicBadges()
  if (isManager.value) {
    await loadManagedBadges()
  }
})
</script>

<style scoped>
.badges-page { padding: 20px; }

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}
.filter-label { font-size: 14px; color: #606266; }
.badge-count { margin-left: auto; font-size: 13px; color: #909399; }

.badges-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}
.badge-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  text-align: center;
}
.badge-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.12);
}
.badge-icon-wrap {
  width: 72px;
  height: 72px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.badge-icon-img { width: 46px; height: 46px; object-fit: cover; border-radius: 50%; }
.badge-icon-placeholder { color: #fff; }
.badge-info { text-align: center; }
.badge-name { margin: 0 0 6px; font-size: 15px; font-weight: 600; color: #303133; }
.badge-desc { margin: 0 0 10px; font-size: 13px; color: #909399; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.badge-meta { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: 12px; color: #909399; }
.club-tag { padding: 2px 8px; background: #ecf5ff; color: #409eff; border-radius: 4px; font-size: 11px; }
.recipient-count { display: flex; align-items: center; gap: 4px; }
.empty-state { grid-column: 1 / -1; padding: 60px 0; }

.manage-toolbar { display: flex; gap: 12px; margin-bottom: 20px; }

.manage-badge-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}
.manage-badge-header { display: flex; align-items: flex-start; gap: 16px; }
.manage-badge-icon {
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
.manage-badge-icon img { width: 30px; height: 30px; object-fit: cover; border-radius: 50%; }
.manage-badge-info { flex: 1; min-width: 0; }
.manage-badge-title { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.manage-badge-name { font-size: 16px; font-weight: 600; color: #303133; }
.manage-badge-desc { margin: 0 0 6px; font-size: 13px; color: #909399; }
.manage-badge-meta { font-size: 12px; color: #909399; display: flex; gap: 16px; }
.manage-badge-actions { display: flex; gap: 6px; flex-shrink: 0; }

.grant-list { margin-top: 14px; border-top: 1px solid #f0f0f0; padding-top: 12px; }
.grant-list-header { font-size: 13px; font-weight: 600; color: #606266; margin-bottom: 8px; }
.grant-items { display: flex; flex-wrap: wrap; gap: 8px; }
.grant-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
}
.grant-user-name { color: #303133; font-weight: 500; }
.grant-time { color: #909399; font-size: 11px; }

.detail-content { text-align: center; }
.detail-icon {
  width: 96px; height: 96px; margin: 0 auto 20px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex; align-items: center; justify-content: center; color: #fff; overflow: hidden;
}
.detail-icon img { width: 60px; height: 60px; object-fit: cover; border-radius: 50%; }
.detail-desc { color: #606266; margin-bottom: 20px; line-height: 1.6; }
.detail-meta { display: flex; justify-content: space-around; padding: 16px 0; background: #f5f7fa; border-radius: 8px; }
.meta-item { display: flex; flex-direction: column; gap: 6px; }
.meta-label { font-size: 12px; color: #909399; }
.meta-value { font-size: 14px; font-weight: 600; color: #303133; }
.recipients-list { max-height: 240px; overflow-y: auto; }
.recipient-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.recipient-item:last-child { border-bottom: none; }
.recipient-info { display: flex; flex-direction: column; gap: 2px; }
.recipient-name { font-size: 14px; color: #303133; }
.recipient-time { font-size: 12px; color: #909399; }
.no-recipients { text-align: center; padding: 20px 0; color: #909399; font-size: 13px; }

.grant-member-list { max-height: 400px; overflow-y: auto; }
.grant-member-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.grant-member-info { flex: 1; display: flex; align-items: center; gap: 8px; }
.grant-member-name { font-size: 14px; color: #303133; font-weight: 500; }
.grant-member-role { font-size: 12px; color: #909399; }
</style>
