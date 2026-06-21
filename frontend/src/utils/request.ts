import axios from 'axios'
import { ElMessage } from 'element-plus'

const instance = axios.create({
    baseURL: '/api',
    timeout: 30000
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
        const contentType = String(response.headers['content-type'] || '')
        if (response.config.responseType === 'blob' || contentType.includes('application/vnd.openxmlformats') || contentType.includes('application/octet-stream')) {
            return response
        }
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
            } else if (typeof data === 'string' && data.includes('code')) {
                try {
                    const parsed = JSON.parse(data)
                    if (parsed.message) message = parsed.message
                } catch (_) {}
            }
        } else if (error.message && error.message.includes('timeout')) {
            message = '请求超时'
        }
        ElMessage.error(message)
        return Promise.reject(error)
    }
)

export function downloadFile(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
}

export function getFilenameFromContentDisposition(disposition: string | null, defaultName: string): string {
    if (!disposition) return defaultName
    const utf8Match = disposition.match(/filename\*=utf-8''([^;]+)/i)
    if (utf8Match && utf8Match[1]) {
        try {
            return decodeURIComponent(utf8Match[1])
        } catch (_) {}
    }
    const match = disposition.match(/filename="?([^";]+)"?/i)
    if (match && match[1]) return match[1]
    return defaultName
}

export default instance
