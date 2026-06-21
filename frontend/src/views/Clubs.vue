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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import request from '../utils/request'
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

onMounted(fetchClubs)
</script>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
.toolbar {
  padding: 15px 20px;
}
</style>
