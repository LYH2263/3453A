import request from '../utils/request'

export const authApi = {
    login: (data: { username: string; password: string }) =>
        request.post('/auth/login', data),

    register: (data: {
        username: string
        password: string
        realName: string
        studentId?: string
        role?: string
    }) => request.post('/auth/register', data),

    resetPassword: (data: {
        username: string
        studentId: string
        newPassword: string
    }) => request.post('/auth/reset-password', data)
}

export const userApi = {
    getProfile: () => request.get('/user/profile'),

    updateProfile: (data: {
        realName?: string
        avatar?: string
        studentId?: string
    }) => request.put('/user/profile', data),

    getMyActivities: () => request.get('/user/my-activities'),

    getMyInteractions: () => request.get('/user/my-interactions'),

    getNotifications: () => request.get('/user/notifications'),

    // 管理员接口
    listUsers: (params?: { pageNum?: number; pageSize?: number }) =>
        request.get('/user/list', { params }),

    updateRole: (id: number, role: string) =>
        request.put(`/user/${id}/role`, null, { params: { role } }),

    deleteUser: (id: number) => request.delete(`/user/${id}`)
}
