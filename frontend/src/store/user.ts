import { defineStore } from 'pinia'

export type UserRole = 'ADMIN' | 'UNION_ADMIN' | 'CLUB_LEADER' | 'MEMBER' | 'GUEST'

export interface UserInfo {
    id: number
    username: string
    realName: string
    role: UserRole
    clubId?: number
    avatar?: string
    token: string
}

export const useUserStore = defineStore('user', {
    state: () => ({
        userInfo: JSON.parse(localStorage.getItem('user') || 'null') as UserInfo | null
    }),
    actions: {
        setUserInfo(user: UserInfo) {
            this.userInfo = user
            localStorage.setItem('user', JSON.stringify(user))
        },
        logout() {
            this.userInfo = null
            localStorage.removeItem('user')
            localStorage.removeItem('token')
        }
    },
    getters: {
        isLoggedIn: (state) => !!state.userInfo,
        role: (state): UserRole | undefined => state.userInfo?.role,
        isAdmin: (state) => state.userInfo?.role === 'ADMIN',
        isUnionAdmin: (state) =>
            state.userInfo?.role === 'ADMIN' || state.userInfo?.role === 'UNION_ADMIN',
        isClubLeader: (state) =>
            ['ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'].includes(state.userInfo?.role ?? ''),
        /** 是否有权管理指定社团（社团负责人只能管理自己的社团） */
        canManageClub: (state) => (clubId: number) => {
            const u = state.userInfo
            if (!u) return false
            if (u.role === 'ADMIN' || u.role === 'UNION_ADMIN') return true
            if (u.role === 'CLUB_LEADER' && u.clubId === clubId) return true
            return false
        }
    }
})
