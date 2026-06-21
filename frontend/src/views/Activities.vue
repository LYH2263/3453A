<template>
  <div class="activities-page">
    <div class="toolbar glass-card mb-20" v-if="userStore.role && ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(userStore.role)">
      <el-button type="success" :icon="Plus" @click="showAddDialog = true">发起活动</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="act in activities" :key="act.id" class="mb-20">
        <el-card class="act-card hover-lift glass-card" :body-style="{ padding: '0px' }">
          <div class="card-cover" v-if="act.poster">
            <el-image :src="act.poster" fit="cover" style="width: 100%; height: 120px" />
          </div>
          <div class="card-header">
            <h3>{{ act.title }}</h3>
            <el-tag :type="statusType(act.status)">{{ statusText(act.status) }}</el-tag>
          </div>
          <div class="card-body">
            <p class="desc">{{ act.description }}</p>
            <div class="info">
              <span><el-icon><Location /></el-icon> {{ act.location }}</span>
              <span><el-icon><Calendar /></el-icon> {{ act.startTime?.split('T')[0] }}</span>
            </div>
            <div class="actions">
              <el-button type="primary" size="small" @click="handleRegister(act)">立即报名</el-button>
              <el-button v-if="canAudit(act)" type="warning" size="small" @click="handleAudit(act)">审核</el-button>
              <el-button v-if="act.status === 'APPROVED' && userStore.role === 'CLUB_LEADER'" type="success" size="small" @click="handleFinish(act.id)">结束活动</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发起活动弹窗 -->
    <el-dialog v-model="showAddDialog" title="发起活动" width="500px">
      <el-form :model="addForm" ref="addFormRef" label-width="80px">
        <el-form-item label="活动名称" required>
          <el-input v-model="addForm.title" />
        </el-form-item>
        <el-form-item label="活动描述" required>
          <el-input v-model="addForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="活动流程" required>
          <el-input v-model="addForm.process" type="textarea" placeholder="填写活动环节及时间节点" />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="addForm.location" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="addForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="addForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="addForm.maxCount" :min="1" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="addForm.budget" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认发起</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="showAuditDialog" title="活动审核" width="400px">
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.status">
            <el-radio label="APPROVED">通过</el-radio>
            <el-radio label="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注说明" v-if="auditForm.status === 'REJECTED'">
          <el-input v-model="auditForm.reason" type="textarea" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Location, Calendar } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

interface Activity {
  id: number
  title: string
  description: string
  location: string
  startTime: string
  status: string
  poster?: string
}

const userStore = useUserStore()
const activities = ref<Activity[]>([])
const showAddDialog = ref(false)

const fetchActivities = async () => {
  try {
    const res: any = await request.get('/activities')
    activities.value = res
  } catch (err) {
    console.error('Failed to fetch activities:', err)
  }
}

const statusType = (s: string) => {
  if (s === 'PENDING_UNION') return 'warning'
  if (s === 'PENDING_SCHOOL') return 'warning'
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

const statusText = (s: string) => {
  const map: any = { PENDING_UNION: '社联初审', PENDING_SCHOOL: '学校终审', APPROVED: '进行中', REJECTED: '已驳回', FINISHED: '已结束' }
  return map[s] || s
}

const canAudit = (act: any) => {
  if (act.status === 'PENDING_UNION' && userStore.role === 'UNION_ADMIN') return true;
  if (act.status === 'PENDING_SCHOOL' && userStore.role === 'ADMIN') return true;
  return false;
}

const handleRegister = async (act: any) => {
  try {
    await request.post(`/activities/${act.id}/register?userId=${userStore.userInfo?.id}`)
    ElMessage.success('报名成功')
  } catch (err) {
    console.error('Registration failed:', err)
  }
}

const addForm = ref({ title: '', description: '', process: '', location: '', startTime: '', endTime: '', maxCount: 50, budget: 0 })

const submitAdd = async () => {
  try {
    await request.post('/activities', addForm.value)
    ElMessage.success('活动发起成功')
    showAddDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Form submission failed:', err)
  }
}

const showAuditDialog = ref(false)
const currentAuditId = ref(0)
const auditForm = ref({ status: 'APPROVED', reason: '' })

const handleAudit = (act: any) => {
  currentAuditId.value = act.id
  auditForm.value = { status: 'APPROVED', reason: '' }
  showAuditDialog.value = true
}

const submitAudit = async () => {
  try {
    await request.post(`/activities/${currentAuditId.value}/audit`, auditForm.value)
    ElMessage.success('审核完成')
    showAuditDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Audit failed:', err)
  }
}

const handleFinish = async (id: number) => {
  try {
    await request.post(`/activities/${id}/finish`)
    ElMessage.success('活动已结束')
    fetchActivities()
  } catch (err) {
    console.error('Finish activity failed:', err)
  }
}

onMounted(fetchActivities)
</script>

<style scoped>
.act-card {
  height: auto;
  min-height: 220px;
}
.card-cover {
  width: 100%;
  height: 120px;
  overflow: hidden;
}
.card-header {
  padding: 15px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 {
  font-size: 16px;
  margin: 0;
}
.card-body {
  padding: 15px;
}
.desc {
  font-size: 14px;
  color: #666;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.info {
  margin-top: 15px;
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #999;
}
.actions {
  margin-top: 20px;
  text-align: right;
}
.mb-20 {
  margin-bottom: 20px;
}
.toolbar {
  padding: 15px 20px;
}
</style>
