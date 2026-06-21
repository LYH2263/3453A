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
        <el-menu-item index="/assets">
          <el-icon><Box /></el-icon>
          <span>物资管理</span>
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
        <div class="header-right">
          <el-popover
            placement="bottom-end"
            :width="360"
            trigger="click"
            :visible-arrow="false"
            popper-class="notification-popover"
            @show="loadNotifications"
          >
            <template #reference>
              <div class="notification-icon-wrapper">
                <el-icon class="notification-icon"><Bell /></el-icon>
                <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="notification-badge" />
              </div>
            </template>
            <div class="notification-panel">
              <div class="notification-header">
                <span class="notification-title">消息通知</span>
                <span v-if="unreadCount > 0" class="mark-all-read" @click="markAllRead">全部已读</span>
              </div>
              <div class="notification-list" v-if="notificationList.length">
                <div
                  v-for="item in notificationList"
                  :key="item.id"
                  class="notification-item"
                  :class="{ unread: item.isRead === 0 }"
                  @click="handleNotificationClick(item)"
                >
                  <el-avatar :size="36" :src="item.triggerUserAvatar || defaultAvatar" />
                  <div class="notification-content">
                    <div class="notification-text">
                      <span class="notification-trigger">{{ item.triggerUserName || '系统' }}</span>
                      <span class="notification-type" v-if="item.type === 'MENTION'">@了你</span>
                    </div>
                    <div class="notification-preview">{{ item.topicTitle || item.content }}</div>
                    <div class="notification-time">{{ formatTime(item.createTime) }}</div>
                  </div>
                  <div class="unread-dot" v-if="item.isRead === 0"></div>
                </div>
              </div>
              <el-empty v-else description="暂无新通知" :image-size="60" />
              <div class="notification-footer" v-if="notificationList.length">
                <span @click="goToInteraction">查看全部</span>
              </div>
            </div>
          </el-popover>

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
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { userApi } from '../api/user'
import { Bell, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const isUnionAdmin = computed(() => userStore.isUnionAdmin)
const isClubLeader = computed(() => userStore.isClubLeader)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea0722952d4a0e3e8b0e8e8e8e8e.png'
const unreadCount = ref(0)
const notificationList = ref<any[]>([])
let pollTimer: number | null = null

const loadUnreadCount = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const data = await userApi.getUnreadNotificationCount() as any
    unreadCount.value = data.unreadCount || 0
  } catch (err) {
    console.error('Failed to load unread count:', err)
  }
}

const loadNotifications = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const data = await userApi.getNotificationList({ pageNum: 1, pageSize: 10 }) as any
    notificationList.value = data.list || []
  } catch (err) {
    console.error('Failed to load notifications:', err)
  }
}

const markAllRead = async () => {
  try {
    await userApi.markAllNotificationsRead()
    unreadCount.value = 0
    notificationList.value.forEach(item => { item.isRead = 1 })
  } catch (err) {
    console.error('Failed to mark all read:', err)
  }
}

const handleNotificationClick = async (item: any) => {
  if (item.isRead === 0) {
    try {
      await userApi.markNotificationRead(item.id)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (err) {
      console.error('Failed to mark notification read:', err)
    }
  }

  if (item.type === 'MENTION' && item.topicId) {
    router.push({
      path: '/interaction',
      query: {
        tab: 'topics',
        topicId: item.topicId,
        commentId: item.commentId
      }
    })
  }
}

const goToInteraction = () => {
  router.push('/interaction?tab=topics')
}

const formatTime = (dt: string) => {
  if (!dt) return ''
  const date = new Date(dt)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return minutes + '分钟前'
  if (hours < 24) return hours + '小时前'
  if (days < 7) return days + '天前'
  return date.toLocaleDateString('zh-CN')
}

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
    '/assets': '物资管理',
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

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadUnreadCount()
    pollTimer = window.setInterval(loadUnreadCount, 30000)
  }
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
})
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
.header-right { display: flex; align-items: center; gap: 20px; color: #303133; }
.notification-icon-wrapper {
  position: relative;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: background 0.2s;
}
.notification-icon-wrapper:hover {
  background: rgba(0, 0, 0, 0.05);
}
.notification-icon {
  font-size: 20px;
  color: #606266;
}
.notification-badge {
  position: absolute;
  top: 0;
  right: 0;
  transform: translate(30%, -20%);
}
.notification-panel {
  max-height: 400px;
  overflow-y: auto;
}
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}
.notification-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.mark-all-read {
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
}
.mark-all-read:hover {
  text-decoration: underline;
}
.notification-list {
  max-height: 300px;
  overflow-y: auto;
}
.notification-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  position: relative;
  border-bottom: 1px solid #f5f7fa;
}
.notification-item:last-child {
  border-bottom: none;
}
.notification-item:hover {
  background: #f5f7fa;
  margin: 0 -8px;
  padding: 10px 8px;
  border-radius: 4px;
}
.notification-item.unread {
  background: #ecf5ff;
  margin: 0 -8px;
  padding: 10px 8px;
  border-radius: 4px;
  border-bottom: 1px solid #d9ecff;
}
.notification-content {
  flex: 1;
  min-width: 0;
}
.notification-text {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}
.notification-trigger {
  font-weight: 600;
}
.notification-type {
  color: #409eff;
  margin-left: 6px;
}
.notification-preview {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}
.notification-time {
  font-size: 11px;
  color: #c0c4cc;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  position: absolute;
  top: 14px;
  right: 4px;
}
.notification-footer {
  text-align: center;
  padding: 10px 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
}
.notification-footer:hover {
  text-decoration: underline;
}
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
