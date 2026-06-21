<template>
  <div class="assets-page">
    <el-tabs v-model="activeTab" class="glass-card">
      <el-tab-pane label="物资列表" name="assets">
        <div class="toolbar mb-20">
          <el-button
            v-if="canManageAssets"
            type="primary"
            :icon="Plus"
            @click="showAddDialog = true"
          >登记物资</el-button>
          <el-select
            v-if="isUnionAdmin"
            v-model="filterClubId"
            placeholder="筛选社团"
            clearable
            style="margin-left: 16px; width: 180px"
            @change="fetchAssets"
          >
            <el-option
              v-for="c in clubs"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </div>

        <el-table :data="assets" v-loading="loading">
          <el-table-column prop="name" label="器材名称" width="160" />
          <el-table-column prop="specification" label="规格" width="180" />
          <el-table-column prop="stock" label="库存" width="80" />
          <el-table-column label="押金" width="100">
            <template #default="{ row }">
              ¥{{ row.deposit }}
            </template>
          </el-table-column>
          <el-table-column label="所属社团" width="160">
            <template #default="{ row }">
              {{ getClubName(row.clubId) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="openBorrowDialog(row)"
              >申请借用</el-button>
              <el-button
                v-if="canManageClub(row.clubId)"
                type="warning"
                size="small"
                @click="openEditDialog(row)"
              >编辑</el-button>
              <el-button
                v-if="canManageClub(row.clubId)"
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container mt-20" style="display: flex; justify-content: flex-end">
          <el-pagination
            v-model:current-page="assetQuery.pageNum"
            v-model:page-size="assetQuery.pageSize"
            :total="assetTotal"
            layout="total, prev, pager, next"
            @current-change="fetchAssets"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="借还记录" name="records">
        <div class="toolbar mb-20">
          <el-select
            v-model="recordStatusFilter"
            placeholder="状态筛选"
            clearable
            style="width: 150px"
            @change="fetchBorrowRecords"
          >
            <el-option label="待审批" value="PENDING" />
            <el-option label="已借出" value="APPROVED" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-select
            v-if="isUnionAdmin"
            v-model="recordClubId"
            placeholder="筛选社团"
            clearable
            style="margin-left: 16px; width: 180px"
            @change="fetchBorrowRecords"
          >
            <el-option
              v-for="c in clubs"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </div>

        <el-table :data="borrowRecords" v-loading="recordLoading">
          <el-table-column prop="assetName" label="物资名称" width="160" />
          <el-table-column prop="specification" label="规格" width="160" />
          <el-table-column prop="borrowerName" label="借用人" width="100" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="借出时间" width="170">
            <template #default="{ row }">
              {{ row.borrowTime ? formatTime(row.borrowTime) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="归还时间" width="170">
            <template #default="{ row }">
              {{ row.returnTime ? formatTime(row.returnTime) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING' && canManageClub(row.clubId)">
                <el-button type="success" size="small" @click="handleApprove(row)">审批</el-button>
                <el-button type="danger" size="small" @click="handleReject(row)">驳回</el-button>
              </template>
              <el-button
                v-if="row.status === 'APPROVED' && canManageClub(row.clubId)"
                type="primary"
                size="small"
                @click="handleReturn(row)"
              >确认归还</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container mt-20" style="display: flex; justify-content: flex-end">
          <el-pagination
            v-model:current-page="recordQuery.pageNum"
            v-model:page-size="recordQuery.pageSize"
            :total="recordTotal"
            layout="total, prev, pager, next"
            @current-change="fetchBorrowRecords"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑物资' : '登记物资'" width="500px">
      <el-form :model="assetForm" ref="assetFormRef" label-width="80px">
        <el-form-item label="器材名称" required>
          <el-input v-model="assetForm.name" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="assetForm.specification" placeholder="如：UNO R3" />
        </el-form-item>
        <el-form-item label="库存数量" required>
          <el-input-number v-model="assetForm.stock" :min="0" />
        </el-form-item>
        <el-form-item label="押金">
          <el-input-number v-model="assetForm.deposit" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAsset">{{ isEdit ? '保存修改' : '确认登记' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBorrowDialog" title="申请借用" width="400px">
      <el-form :model="borrowForm" label-width="80px">
        <el-form-item label="物资名称">
          <span>{{ borrowForm.assetName }}</span>
        </el-form-item>
        <el-form-item label="规格">
          <span>{{ borrowForm.specification || '-' }}</span>
        </el-form-item>
        <el-form-item label="库存">
          <span>{{ borrowForm.stock }}</span>
        </el-form-item>
        <el-form-item label="押金">
          <span>¥{{ borrowForm.deposit }}</span>
        </el-form-item>
        <el-form-item label="借用数量" required>
          <el-input-number v-model="borrowForm.quantity" :min="1" :max="borrowForm.stock" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBorrowDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBorrow">确认申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showApproveDialog" title="审批借用申请" width="400px">
      <el-form label-width="80px">
        <el-form-item label="物资名称">
          <span>{{ approveRecord?.assetName }}</span>
        </el-form-item>
        <el-form-item label="借用人">
          <span>{{ approveRecord?.borrowerName }}</span>
        </el-form-item>
        <el-form-item label="借用数量">
          <span>{{ approveRecord?.quantity }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReject">驳回</el-button>
        <el-button type="success" @click="submitApprove">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

interface ClubAsset {
  id: number
  name: string
  specification: string
  stock: number
  deposit: number
  clubId: number
}

interface BorrowRecord {
  id: number
  assetId: number
  assetName: string
  specification: string
  borrowerId: number
  borrowerName: string
  quantity: number
  status: string
  borrowTime: string | null
  returnTime: string | null
  clubId: number
}

const userStore = useUserStore()
const activeTab = ref('assets')
const loading = ref(false)
const recordLoading = ref(false)

const isUnionAdmin = computed(() =>
  userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN'
)
const canManageAssets = computed(() =>
  ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(userStore.role ?? '')
)
const canManageClub = (clubId: number) => {
  if (!userStore.userInfo) return false
  if (userStore.role === 'ADMIN' || userStore.role === 'UNION_ADMIN') return true
  if (userStore.role === 'CLUB_LEADER' && userStore.userInfo.clubId === clubId) return true
  return false
}

const clubs = ref<any[]>([])
const fetchClubs = async () => {
  try {
    const res: any = await request.get('/clubs', { params: { pageNum: 1, pageSize: 100 } })
    clubs.value = res.records || []
  } catch { /* ignore */ }
}
const getClubName = (clubId: number) => {
  const c = clubs.value.find((item: any) => item.id === clubId)
  return c ? c.name : '未知社团'
}

const assets = ref<ClubAsset[]>([])
const assetTotal = ref(0)
const filterClubId = ref<number | undefined>(undefined)
const assetQuery = reactive({ pageNum: 1, pageSize: 10 })

const fetchAssets = async () => {
  loading.value = true
  try {
    const params: any = { ...assetQuery }
    if (filterClubId.value) params.clubId = filterClubId.value
    else if (userStore.role === 'CLUB_LEADER' && userStore.userInfo?.clubId) {
      params.clubId = userStore.userInfo.clubId
    }
    const res: any = await request.get('/assets', { params })
    assets.value = res.records
    assetTotal.value = res.total
  } finally {
    loading.value = false
  }
}

const showAddDialog = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const assetForm = ref({ name: '', specification: '', stock: 0, deposit: 0 })

const openEditDialog = (row: ClubAsset) => {
  isEdit.value = true
  editId.value = row.id
  assetForm.value = {
    name: row.name,
    specification: row.specification,
    stock: row.stock,
    deposit: row.deposit
  }
  showAddDialog.value = true
}

const submitAsset = async () => {
  try {
    if (isEdit.value && editId.value) {
      await request.put(`/assets/${editId.value}`, assetForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/assets', assetForm.value)
      ElMessage.success('登记成功')
    }
    showAddDialog.value = false
    fetchAssets()
  } catch { /* handled by interceptor */ }
}

const handleDelete = (row: ClubAsset) => {
  ElMessageBox.confirm(`确定要删除物资 [${row.name}] 吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/assets/${row.id}`)
      ElMessage.success('删除成功')
      fetchAssets()
    } catch { /* handled */ }
  }).catch(() => {})
}

const showBorrowDialog = ref(false)
const borrowForm = ref({
  assetId: 0,
  assetName: '',
  specification: '',
  stock: 0,
  deposit: 0,
  quantity: 1
})

const openBorrowDialog = (row: ClubAsset) => {
  borrowForm.value = {
    assetId: row.id,
    assetName: row.name,
    specification: row.specification,
    stock: row.stock,
    deposit: row.deposit,
    quantity: 1
  }
  showBorrowDialog.value = true
}

const submitBorrow = async () => {
  try {
    await request.post('/assets/borrow', {
      assetId: borrowForm.value.assetId,
      borrowerId: userStore.userInfo?.id,
      quantity: borrowForm.value.quantity
    })
    ElMessage.success('申请提交成功，请等待审批')
    showBorrowDialog.value = false
    fetchBorrowRecords()
  } catch { /* handled */ }
}

const borrowRecords = ref<BorrowRecord[]>([])
const recordTotal = ref(0)
const recordQuery = reactive({ pageNum: 1, pageSize: 10 })
const recordStatusFilter = ref('')
const recordClubId = ref<number | undefined>(undefined)

const fetchBorrowRecords = async () => {
  recordLoading.value = true
  try {
    const params: any = { ...recordQuery }
    if (recordStatusFilter.value) params.status = recordStatusFilter.value
    if (recordClubId.value) params.clubId = recordClubId.value
    else if (userStore.role === 'CLUB_LEADER' && userStore.userInfo?.clubId) {
      params.clubId = userStore.userInfo.clubId
    }
    const res: any = await request.get('/assets/borrow/records', { params })
    borrowRecords.value = res.records
    recordTotal.value = res.total
  } finally {
    recordLoading.value = false
  }
}

const showApproveDialog = ref(false)
const approveRecord = ref<BorrowRecord | null>(null)

const handleApprove = (row: BorrowRecord) => {
  approveRecord.value = row
  showApproveDialog.value = true
}

const handleReject = (row: BorrowRecord) => {
  ElMessageBox.confirm('确定要驳回该借用申请吗？', '驳回确认', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.post(`/assets/borrow/${row.id}/reject`)
      ElMessage.success('已驳回')
      fetchBorrowRecords()
    } catch { /* handled */ }
  }).catch(() => {})
}

const submitApprove = async () => {
  if (!approveRecord.value) return
  try {
    await request.post(`/assets/borrow/${approveRecord.value.id}/approve`)
    ElMessage.success('审批通过')
    showApproveDialog.value = false
    fetchBorrowRecords()
    fetchAssets()
  } catch { /* handled */ }
}

const submitReject = async () => {
  if (!approveRecord.value) return
  try {
    await request.post(`/assets/borrow/${approveRecord.value.id}/reject`)
    ElMessage.success('已驳回')
    showApproveDialog.value = false
    fetchBorrowRecords()
  } catch { /* handled */ }
}

const handleReturn = (row: BorrowRecord) => {
  ElMessageBox.confirm('确认该物资已归还？', '归还确认', {
    confirmButtonText: '确认归还',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      await request.post(`/assets/borrow/${row.id}/return`)
      ElMessage.success('已确认归还')
      fetchBorrowRecords()
      fetchAssets()
    } catch { /* handled */ }
  }).catch(() => {})
}

const statusType = (s: string) => {
  if (s === 'PENDING') return 'warning'
  if (s === 'APPROVED') return 'success'
  if (s === 'RETURNED') return 'info'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

const statusText = (s: string) => {
  const map: any = { PENDING: '待审批', APPROVED: '已借出', RETURNED: '已归还', REJECTED: '已驳回' }
  return map[s] || s
}

const formatTime = (t: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchClubs()
  fetchAssets()
  fetchBorrowRecords()
})
</script>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
.mt-20 {
  margin-top: 20px;
}
.toolbar {
  padding: 15px 0;
  display: flex;
  align-items: center;
}
</style>
