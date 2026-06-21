<template>
  <div class="admin-config">
    <el-tabs>
      <el-tab-pane label="角色权限配置">
        <el-table :data="users" v-loading="loading" class="glass-card" style="margin-top: 20px">
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="realName" label="真实姓名" />
          <el-table-column prop="role" label="当前角色">
            <template #default="{ row }">
              <el-select v-model="row.role" @change="(val: string) => updateRole(row.id, val)">
                <el-option v-for="r in roles" :key="r" :label="roleLabel(r)" :value="r" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="审核流程配置">
        <el-table :data="auditConfigs" class="glass-card" style="margin-top: 20px">
          <el-table-column prop="type" label="业务类型" />
          <el-table-column prop="nodes" label="审核节点" />
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button size="small" @click="editAudit(row)">配置节点</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="配置审核节点">
      <el-form :model="auditForm">
        <el-form-item label="节点列表(JSON)">
          <el-input type="textarea" v-model="auditForm.nodes" placeholder='["UNION_ADMIN", "ADMIN"]' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAudit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const users = ref([])
const loading = ref(false)
const roles = ref([])
const auditConfigs = ref([])
const dialogVisible = ref(false)
const auditForm = reactive({ id: null, type: '', nodes: '' })

const fetchData = async () => {
  loading.value = true
  try {
    // Current user list for role management
    const usersRes: any = await request.get('/user/list')
    users.value = usersRes.records
    
    const rolesRes: any = await request.get('/admin/config/roles')
    roles.value = rolesRes

    const auditRes: any = await request.get('/admin/config/audit')
    auditConfigs.value = auditRes
  } finally {
    loading.value = false
  }
}

const updateRole = async (userId: number, role: string) => {
  await request.put('/admin/config/user-role', null, { params: { userId, role } })
  ElMessage.success('更新成功')
}

const roleLabel = (role: string) => {
  const map: Record<string, string> = {
    ADMIN: '超级管理员',
    UNION_ADMIN: '社联管理员',
    CLUB_LEADER: '社团负责人',
    MEMBER: '普通社员',
    GUEST: '游客'
  }
  return map[role] || role
}

const editAudit = (row: any) => {
  Object.assign(auditForm, row)
  dialogVisible.value = true
}

const saveAudit = async () => {
  await request.post('/admin/config/audit', auditForm)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.admin-config { padding: 20px; }
</style>
