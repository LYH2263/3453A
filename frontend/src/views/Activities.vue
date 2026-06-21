<template>
  <div class="activities-page">
    <div class="toolbar glass-card mb-20" v-if="userStore.role && ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(userStore.role)">
      <el-button type="success" :icon="Plus" @click="showAddDialog = true">发起活动</el-button>
    </div>

    <el-tabs v-model="activeTab" class="mb-20">
      <el-tab-pane label="全部活动" name="all" />
      <el-tab-pane label="待我确认" name="pending" v-if="userStore.role === 'CLUB_LEADER'">
        <span style="position: relative">
          待我确认
          <el-badge v-if="pendingCoHosts.length > 0" :value="pendingCoHosts.length" :max="99" class="tab-badge" />
        </span>
      </el-tab-pane>
    </el-tabs>

    <div v-show="activeTab === 'all'">
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
              <div class="club-info" v-if="act._detail?.hostClub">
                <span>主办: {{ act._detail.hostClub.name }}</span>
              </div>
              <div class="co-host-info" v-if="act._detail?.coHosts && act._detail.coHosts.length > 0">
                <span>
                  合作: 
                  <span v-for="(co, idx) in act._detail.coHosts" :key="co.id">
                    {{ co.club?.name }}
                    <el-tag :type="coHostStatusType(co.status)" size="small" style="margin-left: 4px">
                      {{ coHostStatusText(co.status) }}
                    </el-tag>
                    <span v-if="idx < act._detail.coHosts.length - 1">, </span>
                  </span>
                </span>
              </div>
              <div class="budget-info" v-if="act.budget > 0">
                <span>预算: ¥{{ act.budget }}</span>
              </div>
              <div class="enrollment-info">
                <span class="enrollment-count">
                  已报名: {{ act._detail?.registeredCount || 0 }} / {{ act.maxCount }}
                  <span v-if="act._detail?.waitlistCount > 0" class="waitlist-count">
                    (候补: {{ act._detail.waitlistCount }})
                  </span>
                </span>
                <el-tag v-if="act._detail?.isFull" type="danger" size="small">已满员</el-tag>
              </div>
              <div class="my-status" v-if="act._detail?.myStatus && act._detail.myStatus !== 'CANCELLED'">
                <el-tag :type="myStatusType(act._detail.myStatus)" size="small">
                  {{ myStatusText(act._detail) }}
                </el-tag>
              </div>
              <div class="actions">
                <template v-if="act.status === 'APPROVED'">
                  <template v-if="!act._detail?.myStatus || act._detail.myStatus === 'CANCELLED'">
                    <el-button 
                      :type="act._detail?.isFull ? 'warning' : 'primary'" 
                      size="small" 
                      @click="handleRegister(act)">
                      {{ act._detail?.isFull ? '加入候补' : '立即报名' }}
                    </el-button>
                  </template>
                  <template v-else-if="act._detail.myStatus === 'WAITLIST'">
                    <el-button type="info" size="small" @click="handleLeaveWaitlist(act)">
                      取消候补
                    </el-button>
                  </template>
                  <template v-else-if="act._detail.myStatus === 'REGISTERED'">
                    <el-button type="danger" size="small" @click="handleCancel(act)">
                      取消报名
                    </el-button>
                  </template>
                  <el-button 
                    v-if="canExpand(act)" 
                    type="success" 
                    size="small" 
                    @click="handleExpand(act)">
                    扩容
                  </el-button>
                </template>
                <el-button v-if="canEdit(act)" type="primary" size="small" @click="handleEdit(act)">编辑</el-button>
                <el-button v-if="canDelete(act)" type="danger" size="small" @click="handleDelete(act)">删除</el-button>
                <el-button v-if="canAudit(act)" type="warning" size="small" @click="handleAudit(act)">审核</el-button>
                <el-button v-if="canConfirmCoHost(act)" type="success" size="small" @click="handleCoHostConfirm(act)">
                  确认合作
                </el-button>
                <el-button v-if="act.status === 'APPROVED' && userStore.role === 'CLUB_LEADER'" type="success" size="small" @click="handleFinish(act.id)">结束活动</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div v-show="activeTab === 'pending'">
      <el-empty v-if="pendingCoHosts.length === 0" description="暂无待确认的合作活动" />
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in pendingCoHosts" :key="item.coHostId" class="mb-20">
          <el-card class="act-card hover-lift glass-card" :body-style="{ padding: '0px' }">
            <div class="card-header">
              <h3>{{ item.activityTitle }}</h3>
              <el-tag type="warning">待确认</el-tag>
            </div>
            <div class="card-body">
              <p class="desc">{{ item.activityDescription }}</p>
              <div class="info">
                <span><el-icon><Location /></el-icon> {{ item.location }}</span>
                <span><el-icon><Calendar /></el-icon> {{ item.startTime?.split('T')[0] }}</span>
              </div>
              <div class="club-info" v-if="item.hostClub">
                <span>主办方: {{ item.hostClub.name }}</span>
              </div>
              <div class="budget-info" v-if="item.budget > 0">
                <span>预算: ¥{{ item.budget }}</span>
              </div>
              <div class="actions">
                <el-button type="success" size="small" @click="handleConfirmFromList(item)">确认合作</el-button>
                <el-button type="danger" size="small" @click="handleRejectFromList(item)">拒绝</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="showAddDialog" title="发起活动" width="600px">
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
        <el-form-item label="合作社团" v-if="userStore.role === 'CLUB_LEADER'">
          <el-select
            v-model="addForm.coHostClubIds"
            multiple
            filterable
            placeholder="请选择合作社团（可选多个）"
            style="width: 100%"
            :disabled="clubOptions.length === 0">
            <el-option
              v-for="club in clubOptions"
              :key="club.id"
              :label="club.name"
              :value="club.id" />
          </el-select>
          <div class="form-tip">选择合作社团后，活动需经所有合作社团负责人确认后才进入审核流程</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBudgetWarningDialog" title="预算超限警告" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 15px">
        <p>该社团当月预算合计已超出月度预算上限！</p>
        <p>当月已用：¥{{ budgetWarningInfo.currentTotal }}</p>
        <p>提交后合计：¥{{ budgetWarningInfo.projectedTotal }}</p>
        <p>月度上限：¥{{ budgetWarningInfo.limit }}</p>
      </el-alert>
      <el-checkbox v-model="budgetRiskAcknowledged">
        我已知晓超支风险，确认继续发起
      </el-checkbox>
      <template #footer>
        <el-button @click="showBudgetWarningDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!budgetRiskAcknowledged" @click="forceSubmitAdd">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑活动" width="500px">
      <el-form :model="editForm" ref="editFormRef" label-width="80px">
        <el-form-item label="活动名称" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="活动描述" required>
          <el-input v-model="editForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="活动流程" required>
          <el-input v-model="editForm.process" type="textarea" />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="editForm.location" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="editForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="editForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="editForm.maxCount" :min="1" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="editForm.budget" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBudgetWarningEditDialog" title="预算超限警告" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 15px">
        <p>修改后该社团当月预算合计将超出月度预算上限！</p>
        <p>当月已用：¥{{ budgetWarningInfo.currentTotal }}</p>
        <p>修改后合计：¥{{ budgetWarningInfo.projectedTotal }}</p>
        <p>月度上限：¥{{ budgetWarningInfo.limit }}</p>
      </el-alert>
      <el-checkbox v-model="budgetRiskAcknowledged">
        我已知晓超支风险，确认继续修改
      </el-checkbox>
      <template #footer>
        <el-button @click="showBudgetWarningEditDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!budgetRiskAcknowledged" @click="forceSubmitEdit">确认修改</el-button>
      </template>
    </el-dialog>

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

    <el-dialog v-model="showExpandDialog" title="活动扩容" width="400px">
      <el-form :model="expandForm" label-width="100px">
        <el-form-item label="活动名称">
          <span>{{ expandForm.activityTitle }}</span>
        </el-form-item>
        <el-form-item label="当前上限">
          <span>{{ expandForm.currentMaxCount }}</span>
        </el-form-item>
        <el-form-item label="当前报名">
          <span>{{ expandForm.registeredCount }} 人</span>
        </el-form-item>
        <el-form-item label="候补人数">
          <span>{{ expandForm.waitlistCount }} 人</span>
        </el-form-item>
        <el-form-item label="新的上限" required>
          <el-input-number 
            v-model="expandForm.newMaxCount" 
            :min="expandForm.currentMaxCount + 1"
            :max="9999" />
        </el-form-item>
        <div v-if="expandForm.waitlistCount > 0" class="expand-hint">
          <el-icon><InfoFilled /></el-icon>
          扩容后将自动递补最多 {{ Math.min(expandForm.waitlistCount, expandForm.newMaxCount - expandForm.currentMaxCount) }} 名候补给用户
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showExpandDialog = false">取消</el-button>
        <el-button type="primary" @click="submitExpand">确认扩容</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCoHostConfirmDialog" title="合作活动确认" width="500px">
      <div class="co-host-confirm-content">
        <div class="co-host-activity-title">{{ coHostConfirmForm.activityTitle }}</div>
        <div class="co-host-activity-desc">{{ coHostConfirmForm.activityDescription }}</div>
        <el-divider />
        <div class="co-host-info-row">
          <span class="label">主办方:</span>
          <span>{{ coHostConfirmForm.hostClubName }}</span>
        </div>
        <div class="co-host-info-row">
          <span class="label">活动地点:</span>
          <span>{{ coHostConfirmForm.location }}</span>
        </div>
        <div class="co-host-info-row">
          <span class="label">活动时间:</span>
          <span>{{ coHostConfirmForm.startTime?.split('T')[0] }} 至 {{ coHostConfirmForm.endTime?.split('T')[0] }}</span>
        </div>
        <div class="co-host-info-row">
          <span class="label">预算:</span>
          <span>¥{{ coHostConfirmForm.budget }}</span>
        </div>
      </div>
      <el-form :model="coHostConfirmForm" label-width="80px" style="margin-top: 20px">
        <el-form-item label="确认结果">
          <el-radio-group v-model="coHostConfirmForm.status">
            <el-radio label="CONFIRMED">确认合作</el-radio>
            <el-radio label="REJECTED">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注说明" v-if="coHostConfirmForm.status === 'REJECTED'">
          <el-input v-model="coHostConfirmForm.reason" type="textarea" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCoHostConfirmDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCoHostConfirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus, Location, Calendar, InfoFilled } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

interface Activity {
  id: number
  title: string
  description: string
  process: string
  location: string
  startTime: string
  endTime: string
  budget: number
  maxCount: number
  clubId: number
  status: string
  poster?: string
  _detail?: {
    registeredCount: number
    waitlistCount: number
    isFull: boolean
    myStatus: string | null
    myPosition: number | null
    coHosts: any[]
    hostClub: any
  }
}

const userStore = useUserStore()
const activities = ref<Activity[]>([])
const pendingCoHosts = ref<any[]>([])
const showAddDialog = ref(false)
const activeTab = ref('all')
const clubOptions = ref<any[]>([])

const fetchActivities = async () => {
  try {
    const res: any = await request.get('/activities')
    activities.value = res
    for (const act of activities.value) {
      await loadActivityDetail(act)
    }
  } catch (err) {
    console.error('Failed to fetch activities:', err)
  }
}

const loadActivityDetail = async (act: Activity) => {
  try {
    const res: any = await request.get(`/activities/${act.id}/detail?userId=${userStore.userInfo?.id || ''}`)
    act._detail = res
  } catch (err) {
    console.error('Failed to load activity detail:', err)
  }
}

const fetchClubs = async () => {
  try {
    const res: any = await request.get('/clubs')
    const allClubs = res.records || res || []
    const myClubId = userStore.userInfo?.clubId
    clubOptions.value = allClubs.filter((club: any) => club.id !== myClubId && club.status === 'NORMAL')
  } catch (err) {
    console.error('Failed to fetch clubs:', err)
  }
}

const fetchPendingCoHosts = async () => {
  try {
    const res: any = await request.get('/activities/my-pending-co-hosts')
    pendingCoHosts.value = res || []
  } catch (err) {
    console.error('Failed to fetch pending co-hosts:', err)
  }
}

const statusType = (s: string) => {
  if (s === 'DRAFT_COCONFIRM') return 'info'
  if (s === 'PENDING_UNION') return 'warning'
  if (s === 'PENDING_SCHOOL') return 'warning'
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

const statusText = (s: string) => {
  const map: any = { 
    DRAFT_COCONFIRM: '合作确认中', 
    PENDING_UNION: '社联初审', 
    PENDING_SCHOOL: '学校终审', 
    APPROVED: '进行中', 
    REJECTED: '已驳回', 
    FINISHED: '已结束' 
  }
  return map[s] || s
}

const coHostStatusType = (s: string) => {
  if (s === 'PENDING') return 'warning'
  if (s === 'CONFIRMED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

const coHostStatusText = (s: string) => {
  const map: any = { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝' }
  return map[s] || s
}

const myStatusType = (s: string) => {
  if (s === 'REGISTERED') return 'success'
  if (s === 'SIGNED_IN') return 'primary'
  if (s === 'WAITLIST') return 'warning'
  if (s === 'CANCELLED') return 'info'
  return 'info'
}

const myStatusText = (detail: any) => {
  const map: any = { 
    REGISTERED: '已报名', 
    SIGNED_IN: '已签到', 
    WAITLIST: `候补中 - 第${detail.myPosition}位`, 
    CANCELLED: '已取消' 
  }
  return map[detail.myStatus] || detail.myStatus
}

const canAudit = (act: any) => {
  if (act.status === 'PENDING_UNION' && userStore.role === 'UNION_ADMIN') return true;
  if (act.status === 'PENDING_SCHOOL' && userStore.role === 'ADMIN') return true;
  return false;
}

const canExpand = (act: any) => {
  return userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId
}

const canEdit = (act: any) => {
  if (userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN') return true
  if (userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId) {
    return !['APPROVED', 'FINISHED'].includes(act.status)
  }
  return false
}

const canDelete = (act: any) => {
  if (userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN') return true
  if (userStore.role === 'CLUB_LEADER' && act.clubId === userStore.userInfo?.clubId) {
    return !['APPROVED', 'FINISHED'].includes(act.status)
  }
  return false
}

const canConfirmCoHost = (act: any) => {
  if (userStore.role !== 'CLUB_LEADER') return false
  if (act.status !== 'DRAFT_COCONFIRM') return false
  if (!act._detail?.coHosts) return false
  const myClubId = userStore.userInfo?.clubId
  const myCoHost = act._detail.coHosts.find((co: any) => co.clubId === myClubId)
  return myCoHost && myCoHost.status === 'PENDING'
}

const handleRegister = async (act: any) => {
  try {
    const res: any = await request.post(`/activities/${act.id}/register?userId=${userStore.userInfo?.id}`)
    ElMessage.success(res.message || '操作成功')
    loadActivityDetail(act)
  } catch (err: any) {
    console.error('Registration failed:', err)
    ElMessage.error(err.message || '操作失败')
  }
}

const handleCancel = async (act: any) => {
  try {
    await ElMessageBox.confirm('确定要取消报名吗？取消后名额将自动让给候补给用户。', '确认取消', {
      type: 'warning'
    })
    const res: any = await request.post(`/activities/${act.id}/cancel?userId=${userStore.userInfo?.id}`)
    ElMessage.success(res.message || '取消报名成功')
    loadActivityDetail(act)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error('Cancel failed:', err)
      ElMessage.error(err.message || '操作失败')
    }
  }
}

const handleLeaveWaitlist = async (act: any) => {
  try {
    await ElMessageBox.confirm('确定要退出候补队列吗？', '确认退出', {
      type: 'warning'
    })
    const res: any = await request.post(`/activities/${act.id}/leave-waitlist?userId=${userStore.userInfo?.id}`)
    ElMessage.success(res.message || '已退出候补')
    loadActivityDetail(act)
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error('Leave waitlist failed:', err)
      ElMessage.error(err.message || '操作失败')
    }
  }
}

const addForm = ref({ 
  title: '', 
  description: '', 
  process: '', 
  location: '', 
  startTime: '', 
  endTime: '', 
  maxCount: 50, 
  budget: 0,
  coHostClubIds: [] as number[]
})

const showBudgetWarningDialog = ref(false)
const budgetWarningInfo = ref<any>({})
const budgetRiskAcknowledged = ref(false)

const submitAdd = async () => {
  try {
    const requestData: any = {
      title: addForm.value.title,
      description: addForm.value.description,
      process: addForm.value.process,
      location: addForm.value.location,
      startTime: addForm.value.startTime,
      endTime: addForm.value.endTime,
      maxCount: addForm.value.maxCount,
      budget: addForm.value.budget
    }
    if (addForm.value.coHostClubIds && addForm.value.coHostClubIds.length > 0) {
      requestData.coHostClubIds = addForm.value.coHostClubIds
    }
    const res: any = await request.post('/activities', requestData)
    if (res && res.budgetWarning) {
      budgetWarningInfo.value = res
      budgetRiskAcknowledged.value = false
      showBudgetWarningDialog.value = true
      return
    }
    ElMessage.success('活动发起成功')
    showAddDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Form submission failed:', err)
  }
}

const forceSubmitAdd = async () => {
  try {
    const requestData: any = {
      title: addForm.value.title,
      description: addForm.value.description,
      process: addForm.value.process,
      location: addForm.value.location,
      startTime: addForm.value.startTime,
      endTime: addForm.value.endTime,
      maxCount: addForm.value.maxCount,
      budget: addForm.value.budget
    }
    if (addForm.value.coHostClubIds && addForm.value.coHostClubIds.length > 0) {
      requestData.coHostClubIds = addForm.value.coHostClubIds
    }
    await request.post('/activities?forceBudget=true', requestData)
    ElMessage.success('活动发起成功')
    showBudgetWarningDialog.value = false
    showAddDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Force submit failed:', err)
  }
}

const showEditDialog = ref(false)
const editForm = ref<any>({})
const editingActivityId = ref(0)
const showBudgetWarningEditDialog = ref(false)

const handleEdit = (act: any) => {
  editingActivityId.value = act.id
  editForm.value = {
    title: act.title,
    description: act.description,
    process: act.process || '',
    location: act.location,
    startTime: act.startTime,
    endTime: act.endTime,
    maxCount: act.maxCount,
    budget: act.budget
  }
  showEditDialog.value = true
}

const submitEdit = async () => {
  try {
    const res: any = await request.put(`/activities/${editingActivityId.value}`, editForm.value)
    if (res && res.budgetWarning) {
      budgetWarningInfo.value = res
      budgetRiskAcknowledged.value = false
      showBudgetWarningEditDialog.value = true
      return
    }
    ElMessage.success('活动修改成功')
    showEditDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Edit submission failed:', err)
  }
}

const forceSubmitEdit = async () => {
  try {
    await request.put(`/activities/${editingActivityId.value}?forceBudget=true`, editForm.value)
    ElMessage.success('活动修改成功')
    showBudgetWarningEditDialog.value = false
    showEditDialog.value = false
    fetchActivities()
  } catch (err) {
    console.error('Force edit failed:', err)
  }
}

const handleDelete = async (act: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗？删除后预算将回滚。', '确认删除', {
      type: 'warning'
    })
    await request.delete(`/activities/${act.id}`)
    ElMessage.success('活动已删除')
    fetchActivities()
  } catch (err: any) {
    if (err !== 'cancel') {
      console.error('Delete failed:', err)
      ElMessage.error(err.message || '删除失败')
    }
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

const showExpandDialog = ref(false)
const expandForm = ref({
  activityId: 0,
  activityTitle: '',
  currentMaxCount: 0,
  registeredCount: 0,
  waitlistCount: 0,
  newMaxCount: 0
})

const handleExpand = (act: any) => {
  expandForm.value = {
    activityId: act.id,
    activityTitle: act.title,
    currentMaxCount: act.maxCount,
    registeredCount: act._detail?.registeredCount || 0,
    waitlistCount: act._detail?.waitlistCount || 0,
    newMaxCount: act.maxCount + 1
  }
  showExpandDialog.value = true
}

const submitExpand = async () => {
  try {
    const res: any = await request.post(`/activities/${expandForm.value.activityId}/expand`, {
      newMaxCount: expandForm.value.newMaxCount,
      operatorId: userStore.userInfo?.id
    })
    ElMessage.success(`扩容成功！已自动递补 ${res.promotedCount} 名候补给用户`)
    showExpandDialog.value = false
    fetchActivities()
  } catch (err: any) {
    console.error('Expand failed:', err)
    ElMessage.error(err.message || '扩容失败')
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

const showCoHostConfirmDialog = ref(false)
const coHostConfirmForm = ref({
  activityId: 0,
  coHostId: 0,
  activityTitle: '',
  activityDescription: '',
  hostClubName: '',
  location: '',
  startTime: '',
  endTime: '',
  budget: 0,
  status: 'CONFIRMED',
  reason: ''
})

const handleCoHostConfirm = (act: any) => {
  const myClubId = userStore.userInfo?.clubId
  const myCoHost = act._detail?.coHosts?.find((co: any) => co.clubId === myClubId)
  if (!myCoHost) return

  coHostConfirmForm.value = {
    activityId: act.id,
    coHostId: myCoHost.id,
    activityTitle: act.title,
    activityDescription: act.description,
    hostClubName: act._detail?.hostClub?.name || '',
    location: act.location,
    startTime: act.startTime,
    endTime: act.endTime,
    budget: act.budget,
    status: 'CONFIRMED',
    reason: ''
  }
  showCoHostConfirmDialog.value = true
}

const handleConfirmFromList = (item: any) => {
  coHostConfirmForm.value = {
    activityId: item.activityId,
    coHostId: item.coHostId,
    activityTitle: item.activityTitle,
    activityDescription: item.activityDescription,
    hostClubName: item.hostClub?.name || '',
    location: item.location,
    startTime: item.startTime,
    endTime: item.endTime,
    budget: item.budget,
    status: 'CONFIRMED',
    reason: ''
  }
  showCoHostConfirmDialog.value = true
}

const handleRejectFromList = (item: any) => {
  coHostConfirmForm.value = {
    activityId: item.activityId,
    coHostId: item.coHostId,
    activityTitle: item.activityTitle,
    activityDescription: item.activityDescription,
    hostClubName: item.hostClub?.name || '',
    location: item.location,
    startTime: item.startTime,
    endTime: item.endTime,
    budget: item.budget,
    status: 'REJECTED',
    reason: ''
  }
  showCoHostConfirmDialog.value = true
}

const submitCoHostConfirm = async () => {
  try {
    const { activityId, coHostId, status, reason } = coHostConfirmForm.value
    await request.post(`/activities/${activityId}/co-hosts/${coHostId}/confirm`, { status, reason })
    ElMessage.success('操作成功')
    showCoHostConfirmDialog.value = false
    fetchActivities()
    fetchPendingCoHosts()
  } catch (err: any) {
    console.error('Co-host confirm failed:', err)
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => {
  fetchActivities()
  if (userStore.role === 'CLUB_LEADER') {
    fetchClubs()
    fetchPendingCoHosts()
  }
})
</script>

<style scoped>
.act-card {
  height: auto;
  min-height: 320px;
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
.club-info {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}
.co-host-info {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.budget-info {
  margin-top: 10px;
  font-size: 13px;
  color: #e6a23c;
  font-weight: 500;
}
.enrollment-info {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #666;
}
.enrollment-count {
  font-weight: 500;
}
.waitlist-count {
  color: #e6a23c;
  margin-left: 5px;
}
.my-status {
  margin-top: 10px;
}
.actions {
  margin-top: 15px;
  text-align: right;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.mb-20 {
  margin-bottom: 20px;
}
.toolbar {
  padding: 15px 20px;
}
.expand-hint {
  margin-top: 10px;
  padding: 10px;
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  color: #409eff;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.tab-badge {
  margin-left: 8px;
}
.co-host-confirm-content {
  padding: 10px 0;
}
.co-host-activity-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.co-host-activity-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 15px;
}
.co-host-info-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 14px;
}
.co-host-info-row .label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}
</style>
