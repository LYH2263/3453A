<template>
  <div class="login-container">
    <div class="login-card glass-card">
      <h2>高校学生社团管理系统</h2>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码"
                prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">
                登录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="demo-accounts">
            <p class="demo-accounts-title">测试账号（点击填充）</p>
            <div class="demo-accounts-list">
              <button
                v-for="account in demoAccounts"
                :key="account.username"
                type="button"
                class="demo-account-item"
                @click="fillAccount(account)"
              >
                <span class="demo-account-label">{{ account.label }}</span>
                <span class="demo-account-info">{{ account.username }} / {{ account.password }}</span>
              </button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名（登录账号）" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="真实姓名" prefix-icon="Avatar" />
            </el-form-item>
            <el-form-item prop="studentId">
              <el-input v-model="registerForm.studentId" placeholder="学号（用于找回密码）" prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）"
                prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码"
                prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 密码重置 -->
        <el-tab-pane label="找回密码" name="reset">
          <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" @submit.prevent="handleReset">
            <el-form-item prop="username">
              <el-input v-model="resetForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="studentId">
              <el-input v-model="resetForm.studentId" placeholder="学号（验证身份）" prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item prop="newPassword">
              <el-input v-model="resetForm.newPassword" type="password" placeholder="新密码（至少6位）"
                prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="resetForm.confirmPassword" type="password" placeholder="确认新密码"
                prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { authApi } from '../api/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('login')

// ---- 登录 ----
const loginFormRef = ref<FormInstance>()
const loginForm = reactive({ username: '', password: '' })
const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '超级管理员', username: 'admin', password: '123456' },
  { label: '社团负责人', username: 'club_leader1', password: '123456' },
  { label: '普通学生', username: 'student1', password: '123456' }
]

const fillAccount = (account: { username: string; password: string; label: string }) => {
  loginForm.username = account.username
  loginForm.password = account.password
  loginFormRef.value?.clearValidate()
  ElMessage.success(`已填充${account.label}账号`)
}

const handleLogin = async () => {
  if (!await loginFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    const res: any = await authApi.login(loginForm)
    userStore.setUserInfo(res)
    ElMessage.success('欢迎回来，' + res.realName)
    router.push('/')
  } finally {
    loading.value = false
  }
}

// ---- 注册 ----
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '', realName: '', studentId: '', password: '', confirmPassword: ''
})
const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== registerForm.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  if (!await registerFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    await authApi.register({
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
      studentId: registerForm.studentId,
      role: 'MEMBER'
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
  } finally {
    loading.value = false
  }
}

// ---- 密码重置 ----
const resetFormRef = ref<FormInstance>()
const resetForm = reactive({
  username: '', studentId: '', newPassword: '', confirmPassword: ''
})
const resetRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== resetForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const handleReset = async () => {
  if (!await resetFormRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    await authApi.resetPassword({
      username: resetForm.username,
      studentId: resetForm.studentId,
      newPassword: resetForm.newPassword
    })
    ElMessage.success('密码重置成功，请重新登录')
    activeTab.value = 'login'
    loginForm.username = resetForm.username
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}
.login-card {
  width: 420px;
  padding: 40px;
}
h2 {
  text-align: center;
  margin-bottom: 24px;
  color: #333; /* Dark color for readability on light glass card */
  font-weight: 600;
  font-size: 24px;
}
.login-tabs :deep(.el-tabs__item) {
  color: #333; /* Make text dark inside the card since the background of glass card seems light */
  font-weight: 500;
  font-size: 16px;
}
.login-tabs :deep(.el-tabs__item.is-active) {
  color: #409eff; /* Primary blue for active tab */
  font-weight: bold;
}
.login-tabs :deep(.el-tabs__active-bar) {
  background-color: #409eff;
}
.hint {
  text-align: center;
  font-size: 12px;
  color: #888;
  margin-top: 8px;
}
.demo-accounts {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed rgba(64, 158, 255, 0.25);
}
.demo-accounts-title {
  margin: 0 0 10px;
  text-align: center;
  font-size: 12px;
  color: #888;
}
.demo-accounts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.demo-account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
}
.demo-account-item:hover {
  border-color: rgba(64, 158, 255, 0.45);
  background: rgba(64, 158, 255, 0.12);
}
.demo-account-label {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  white-space: nowrap;
}
.demo-account-info {
  font-size: 12px;
  color: #666;
}
</style>
