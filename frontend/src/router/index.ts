import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: () => import('../views/Login.vue'),
            meta: { public: true }
        },
        {
            path: '/',
            component: () => import('../layouts/MainLayout.vue'),
            redirect: '/dashboard',
            children: [
                {
                    path: 'dashboard',
                    name: 'Dashboard',
                    component: () => import('../views/Dashboard.vue')
                },
                {
                    path: 'clubs',
                    name: 'Clubs',
                    component: () => import('../views/Clubs.vue')
                },
                {
                    path: 'activities',
                    name: 'Activities',
                    component: () => import('../views/Activities.vue')
                },
                {
                    path: 'profile',
                    name: 'Profile',
                    component: () => import('../views/Profile.vue'),
                    meta: { requiresAuth: true }
                },
                {
                    path: 'interaction',
                    name: 'Interaction',
                    component: () => import('../views/Interaction.vue')
                },
                // 管理员专属：用户管理
                {
                    path: 'admin/dashboard',
                    name: 'AdminDashboard',
                    component: () => import('../views/admin/Dashboard.vue'),
                    meta: { requiresAuth: true, roles: ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'] }
                },
                {
                    path: 'admin/clubs',
                    name: 'AdminClubs',
                    component: () => import('../views/admin/Clubs.vue'),
                    meta: { requiresAuth: true, roles: ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'] }
                },
                {
                    path: 'admin/logs',
                    name: 'AdminLogs',
                    component: () => import('../views/admin/Logs.vue'),
                    meta: { requiresAuth: true, roles: ['ADMIN', 'UNION_ADMIN'] }
                },
                {
                    path: 'admin/config',
                    name: 'AdminConfig',
                    component: () => import('../views/admin/Config.vue'),
                    meta: { requiresAuth: true, roles: ['ADMIN', 'UNION_ADMIN'] }
                },
                {
                    path: 'admin/users',
                    name: 'AdminUsers',
                    component: () => import('../views/AdminUsers.vue'),
                    meta: { requiresAuth: true, roles: ['ADMIN', 'UNION_ADMIN'] }
                }
            ]
        },
        // 404 兜底
        { path: '/:pathMatch(.*)*', redirect: '/' }
    ]
})

router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()

    // 公开页面直接放行
    if (to.meta.public) {
        // 已登录时访问 /login 跳转首页
        if (to.path === '/login' && userStore.isLoggedIn) {
            return next('/')
        }
        return next()
    }

    // 未登录跳转登录页
    if (!userStore.isLoggedIn) {
        return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // 角色权限校验
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && requiredRoles.length > 0) {
        const userRole = userStore.role
        if (!userRole || !requiredRoles.includes(userRole)) {
            return next('/dashboard')
        }
    }

    next()
})

export default router
