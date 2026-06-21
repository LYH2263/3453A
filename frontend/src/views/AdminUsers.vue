<template>
  <div class="admin-users">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-input v-model="search" placeholder="搜索用户名/姓名" style="width:220px" clearable prefix-icon="Search" />
        </div>
      </template>

      <el-table :data="filteredUsers" stripe v-loading="loading" class="glass-card">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clubId" label="社团ID" width="80" />
        <el-table-column label="操作" min-width="200">
          <template #default="{ row }">
            <!-- 超级管理员可修改角色 -->
            <el-select
              v-if="isAdmin"
              v-model="row.role"
              size="small"
              style="width:130px;margin-right:8px"
              @change="(val: string) => handleRoleChange(row.id, val)"
            >
              <el-option label="超级管理员" value="ADMIN" />
              <el-option label="社联管理员" value="UNION_ADMIN" />
              <el-option label="社团负责人" value="CLUB_LEADER" />
              <el-option label="普通社员" value="MEMBER" />
              <el-option label="游客" value="GUEST" />
            </el-select>
            <el-popconfirm title="确认禁用该用户？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small" :disabled="row.role === 'ADMIN'">禁用</el-button>
              </template>
            </el-popconfirm>
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { userApi } from '../api/user'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const loading = ref(false)
const search = ref('')
const users = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

const filteredUsers = computed(() =>
  users.value.filter(u =>
    !search.value ||
    u.username?.includes(search.value) ||
    u.realName?.includes(search.value)
  )
)

onMounted(loadUsers)

async function loadUsers() {
  loading.value = true
  try {
    const res: any = await userApi.listUsers(queryParams)
    users.value = res.records
    total.value = res.total
  } catch (err) {
    console.error('Failed to load users:', err)
  } finally {
    loading.value = false
  }
}

async function handleRoleChange(id: number, role: string) {
  try {
    await userApi.updateRole(id, role)
    ElMessage.success('角色已更新')
  } catch (err) {
    console.error('Failed to update role:', err)
  }
}

async function handleDelete(id: number) {
  try {
    await userApi.deleteUser(id)
    ElMessage.success('用户已禁用')
    await loadUsers()
  } catch (err) {
    console.error('Failed to delete user:', err)
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
</script>

<style scoped>
.admin-users { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
