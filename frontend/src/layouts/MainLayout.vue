<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="glass-card sidebar">
      <div class="logo">
        <el-icon><School /></el-icon>
        <span>社团管家</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="transparent" text-color="#606266" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/clubs">
          <el-icon><Collection /></el-icon>
          <span>社团列表</span>
        </el-menu-item>
        <el-menu-item index="/activities">
          <el-icon><Football /></el-icon>
          <span>活动中心</span>
        </el-menu-item>
        <el-menu-item index="/interaction">
          <el-icon><ChatDotRound /></el-icon>
          <span>互动社区</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <!-- 社团负责人及以上可见 -->
        <template v-if="isClubLeader">
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataLine /></el-icon>
            <span>管理看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/clubs">
            <el-icon><Management /></el-icon>
            <span>社团管理</span>
          </el-menu-item>
        </template>
        <!-- 仅管理员/社联可见 -->
        <template v-if="isUnionAdmin">
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Document /></el-icon>
            <span>日志管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/config">
            <el-icon><Setting /></el-icon>
            <span>基础配置</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">{{ currentRouteName }}</div>
        <div class="user-info">
          <el-dropdown>
            <span class="el-dropdown-link">
              <el-avatar :size="28" :src="userInfo?.avatar" style="margin-right:6px" />
              {{ userInfo?.realName }} ({{ roleName }})
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const isUnionAdmin = computed(() => userStore.isUnionAdmin)
const isClubLeader = computed(() => userStore.isClubLeader)

const roleName = computed(() => {
  const map: Record<string, string> = {
    ADMIN: '超级管理员',
    UNION_ADMIN: '社联管理员',
    CLUB_LEADER: '社团负责人',
    MEMBER: '普通成员',
    GUEST: '游客'
  }
  return map[userInfo.value?.role ?? ''] || '未知角色'
})

const currentRouteName = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '数据看板',
    '/clubs': '社团列表',
    '/activities': '活动中心',
    '/interaction': '互动社区',
    '/profile': '个人中心',
    '/admin/users': '用户管理',
    '/admin/dashboard': '管理看板',
    '/admin/clubs': '社团管理',
    '/admin/logs': '日志管理',
    '/admin/config': '基础配置'
  }
  return map[route.path] || ''
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.sidebar {
  margin: 10px;
  height: calc(100vh - 20px);
  border-radius: 12px;
}
.logo {
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255,255,255,0.05);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.breadcrumb { font-size: 16px; font-weight: 600; color: #303133; }
.user-info { color: #303133; }
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #303133;
}
.main { padding: 20px; overflow-y: auto; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
