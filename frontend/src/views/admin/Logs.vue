<template>
  <div class="admin-logs">
    <div class="toolbar">
      <el-button type="success" @click="handleExport">导出日志 Excel</el-button>
    </div>

    <el-table :data="logs" class="glass-card" style="margin-top: 20px" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="operation" label="操作描述" />
      <el-table-column prop="method" label="请求方法" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="time" label="耗时(ms)" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" />
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'

const logs = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/logs/operation', { params: queryParams })
    logs.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleExport = () => {
  window.open(`${(import.meta as any).env.VITE_API_BASE_URL || '/api'}/admin/export/logs`, '_blank')
}

onMounted(fetchLogs)
</script>

<style scoped>
.admin-logs { padding: 20px; }
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
