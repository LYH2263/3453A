import axios from 'axios'
import { ElMessage } from 'element-plus'

const instance = axios.create({
    baseURL: '/api',
    timeout: 10000
})

instance.interceptors.request.use(
    (config) => {
        const userStr = localStorage.getItem('user')
        if (userStr) {
            const user = JSON.parse(userStr)
            if (user.token) {
                config.headers.Authorization = `Bearer ${user.token}`
            }
        }
        return config
    },
    (error) => Promise.reject(error)
)

instance.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '操作失败')
            return Promise.reject(new Error(res.message || '操作失败'))
        }
        return res.data
    },
    (error) => {
        let message = '网络错误，请稍后再试'
        if (error.response) {
            const { status, data } = error.response
            if (status === 401) {
                message = '登录已过期，请重新登录'
                localStorage.removeItem('user')
                window.location.href = '/login'
            } else if (status === 403) {
                message = '没有权限访问该资源'
            } else if (data && data.message) {
                message = data.message
            }
        } else if (error.message && error.message.includes('timeout')) {
            message = '请求超时'
        }
        ElMessage.error(message)
        return Promise.reject(error)
    }
)

export default instance
