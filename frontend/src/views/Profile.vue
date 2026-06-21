<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <!-- 左侧：用户信息卡片 -->
      <el-col :span="7">
        <el-card class="user-card">
          <div class="avatar-wrap">
            <el-avatar :size="80" :src="profile.avatar || defaultAvatar" />
            <div class="user-name">{{ profile.realName }}</div>
            <el-tag :type="roleTagType(profile.role)" size="small">{{ roleLabel(profile.role) }}</el-tag>
          </div>
          <el-divider />
          <div class="info-item"><span class="label">用户名</span><span>{{ profile.username }}</span></div>
          <div class="info-item"><span class="label">学号</span><span>{{ profile.studentId || '未填写' }}</span></div>
          <div class="info-item"><span class="label">注册时间</span><span>{{ formatDate(profile.createTime) }}</span></div>
        </el-card>
      </el-col>

      <!-- 右侧：Tab 内容 -->
      <el-col :span="17">
        <el-card>
          <el-tabs v-model="activeTab">
            <!-- 资料编辑 -->
            <el-tab-pane label="编辑资料" name="edit">
              <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px" style="max-width:480px">
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="editForm.realName" />
                </el-form-item>
                <el-form-item label="学号" prop="studentId">
                  <el-input v-model="editForm.studentId" />
                </el-form-item>
                <el-form-item label="头像URL" prop="avatar">
                  <el-input v-model="editForm.avatar" placeholder="输入头像图片链接" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 我的活动 -->
            <el-tab-pane label="我的活动" name="activities">
              <el-empty v-if="!myActivities.length" description="暂无报名记录" />
              <el-table v-else :data="myActivities" stripe class="glass-card">
                <el-table-column prop="title" label="活动名称" min-width="160" />
                <el-table-column prop="location" label="地点" width="120" />
                <el-table-column label="开始时间" width="160">
                  <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
                </el-table-column>
                <el-table-column label="活动状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="activityStatusType(row.status)" size="small">
                      {{ activityStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="报名状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.regStatus === 'SIGNED_IN' ? 'success' : 'info'" size="small">
                      {{ row.regStatus === 'SIGNED_IN' ? '已签到' : '已报名' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="参与操作/评分" width="160">
                  <template #default="{ row }">
                    <el-button v-if="row.regStatus === 'REGISTERED'" size="small" type="primary" @click="handleSignin(row.activityId)">模拟签到</el-button>
                    <div v-else-if="row.regStatus === 'SIGNED_IN' && !row.rating">
                      <el-button size="small" type="success" @click="openFeedback(row.activityId)">评分反馈</el-button>
                    </div>
                    <el-rate v-else-if="row.rating" :model-value="row.rating" disabled size="small" />
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 互动记录 -->
            <el-tab-pane label="互动记录" name="interactions">
              <el-empty v-if="!myInteractions.length" description="暂无互动记录" />
              <div v-else class="interaction-list">
                <el-card v-for="item in myInteractions" :key="item.registrationId" class="interaction-item">
                  <div class="interaction-header">
                    <span class="activity-title">{{ item.activityTitle }}</span>
                    <span class="time">{{ formatDate(item.updateTime) }}</span>
                  </div>
                  <el-rate :model-value="item.rating" disabled size="small" />
                  <p class="feedback">{{ item.feedback }}</p>
                  <div v-if="item.reply" class="reply">
                    <el-icon><ChatDotRound /></el-icon>
                    负责人回复：{{ item.reply }}
                  </div>
                </el-card>
              </div>
            </el-tab-pane>

            <!-- 消息通知 -->
            <el-tab-pane name="notifications">
              <template #label>
                <el-badge :value="notifications.length" :hidden="!notifications.length" type="danger">
                  消息通知
                </el-badge>
              </template>
              <el-empty v-if="!notifications.length" description="暂无消息" />
              <el-timeline v-else>
                <el-timeline-item
                  v-for="(n, i) in notifications"
                  :key="i"
                  :timestamp="formatDate(n.time)"
                  placement="top"
                  type="primary"
                >
                  <el-card>
                    <p class="notif-title">活动「{{ n.activityTitle }}」有新回复</p>
                    <p class="notif-content">{{ n.content }}</p>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
    <!-- 评价反馈弹窗 -->
    <el-dialog v-model="showFeedbackDialog" title="评价反馈" width="400px">
      <el-form :model="feedbackForm" label-width="60px">
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" />
        </el-form-item>
        <el-form-item label="反馈">
          <el-input v-model="feedbackForm.feedback" type="textarea" placeholder="填写活动建议或心得" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFeedbackDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { userApi } from '../api/user'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const activeTab = ref('edit')
const saving = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea0722952d4a0e3e8b0e8e8e8e8e.png'

const profile = reactive<any>({})
const myActivities = ref<any[]>([])
const myInteractions = ref<any[]>([])
const notifications = ref<any[]>([])

const editFormRef = ref<FormInstance>()
const editForm = reactive({ realName: '', studentId: '', avatar: '' })
const editRules: FormRules = {
  realName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }]
}

onMounted(async () => {
  await loadProfile()
  await Promise.all([loadActivities(), loadInteractions(), loadNotifications()])
})

async function loadProfile() {
  try {
    const data: any = await userApi.getProfile()
    Object.assign(profile, data)
    editForm.realName = data.realName || ''
    editForm.studentId = data.studentId || ''
    editForm.avatar = data.avatar || ''
  } catch {}
}

async function loadActivities() {
  try {
    myActivities.value = (await userApi.getMyActivities()) as any
  } catch (err) {
    console.error('Failed to fetch activities:', err)
  }
}

async function loadInteractions() {
  try {
    myInteractions.value = (await userApi.getMyInteractions()) as any
  } catch (err) {
    console.error('Failed to fetch interactions:', err)
  }
}

async function loadNotifications() {
  try {
    notifications.value = (await userApi.getNotifications()) as any
  } catch (err) {
    console.error('Failed to fetch notifications:', err)
  }
}

async function handleSaveProfile() {
  if (!await editFormRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    await userApi.updateProfile({
      realName: editForm.realName,
      studentId: editForm.studentId,
      avatar: editForm.avatar
    })
    ElMessage.success('资料已更新')
    await loadProfile()
  } finally {
    saving.value = false
  }
}

// 签到与评价
import request from '../utils/request'
const handleSignin = async (actId: number) => {
  try {
    await request.post(`/activities/${actId}/signin?userId=${profile.id}`)
    ElMessage.success('签到成功')
    loadActivities()
  } catch (err) {
    console.error('Signin failed:', err)
  }
}

const showFeedbackDialog = ref(false)
const currentFeedbackActId = ref(0)
const feedbackForm = ref({ rating: 5, feedback: '' })

const openFeedback = (actId: number) => {
  currentFeedbackActId.value = actId
  feedbackForm.value = { rating: 5, feedback: '' }
  showFeedbackDialog.value = true
}

const submitFeedback = async () => {
  try {
    await request.post(`/activities/${currentFeedbackActId.value}/feedback`, {
      userId: profile.id,
      rating: feedbackForm.value.rating,
      feedback: feedbackForm.value.feedback
    })
    ElMessage.success('评价提交成功')
    showFeedbackDialog.value = false
    loadActivities()
    loadInteractions()
  } catch (err) {
    console.error('Feedback submission failed:', err)
  }
}

function roleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '超级管理员', UNION_ADMIN: '社联管理员',
    CLUB_LEADER: '社团负责人', MEMBER: '普通社员', GUEST: '游客'
  }
  return map[role] || role
}

function roleTagType(role: string) {
  const map: Record<string, string> = {
    ADMIN: 'danger', UNION_ADMIN: 'warning',
    CLUB_LEADER: 'success', MEMBER: '', GUEST: 'info'
  }
  return (map[role] || '') as any
}

function activityStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_UNION: '社联审核中', PENDING_SCHOOL: '学校审核中', APPROVED: '已通过', REJECTED: '已驳回', FINISHED: '已结束'
  }
  return map[status] || status
}

function activityStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING_UNION: 'warning', PENDING_SCHOOL: 'warning', APPROVED: 'success', REJECTED: 'danger', FINISHED: 'info'
  }
  return (map[status] || '') as any
}

function formatDate(dt: string) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.profile-container { padding: 20px; }
.user-card { text-align: center; }
.avatar-wrap { padding: 16px 0; }
.user-name { font-size: 18px; font-weight: 600; margin: 12px 0 8px; }
.info-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
  color: #606266;
}
.label { color: #909399; }
.interaction-list { display: flex; flex-direction: column; gap: 12px; }
.interaction-item { border-left: 3px solid #409eff; }
.interaction-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.activity-title { font-weight: 600; }
.time { font-size: 12px; color: #909399; }
.feedback { margin: 8px 0; color: #606266; font-size: 14px; }
.reply {
  background: #f0f9ff;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 6px;
}
.notif-title { font-weight: 600; margin-bottom: 4px; }
.notif-content { color: #606266; font-size: 14px; }
.text-muted { color: #c0c4cc; font-size: 12px; }
</style>
