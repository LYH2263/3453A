<template>
  <div class="admin-clubs">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增社团</el-button>
      <el-button type="success" @click="handleExport">导出 Excel</el-button>
    </div>

    <el-table :data="clubs" class="glass-card" style="width: 100%; margin-top: 20px" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="社团名称" />
      <el-table-column prop="leaderId" label="负责人ID" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'">
            {{ row.status === 'NORMAL' ? '正常' : '已注销' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleRetire(row)" :disabled="row.status === 'RETIRED'">注销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="fetchClubs"
        @current-change="fetchClubs"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑社团' : '新增社团'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="社团名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="社团简介">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input-number v-model="form.leaderId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClub">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const clubs = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})
const dialogVisible = ref(false)
const form = reactive({ id: null, name: '', description: '', leaderId: null })

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

const handleAdd = () => {
  Object.assign(form, { id: null, name: '', description: '', leaderId: null })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveClub = async () => {
  if (form.id) {
    await request.put(`/clubs/${form.id}`, form)
  } else {
    await request.post('/clubs', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchClubs()
}

const handleRetire = (row: any) => {
  ElMessageBox.confirm('确定要注销该社团吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/clubs/${row.id}`)
    ElMessage.success('注销成功')
    fetchClubs()
  })
}

const handleExport = () => {
  window.open(`${(import.meta as any).env.VITE_API_BASE_URL || '/api'}/admin/export/clubs`, '_blank')
}

onMounted(fetchClubs)
</script>

<style scoped>
.admin-clubs { padding: 20px; }
.toolbar { margin-bottom: 20px; }
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
