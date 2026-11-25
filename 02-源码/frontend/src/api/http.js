import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/** 全局 axios 实例：统一携带令牌、统一处理业务码与登录过期 */
const http = axios.create({ baseURL: '', timeout: 20000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('trs_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        localStorage.removeItem('trs_token')
        localStorage.removeItem('trs_user')
        localStorage.removeItem('trs_menus')
        router.push('/login')
        ElMessage.error(body.msg || '登录已过期')
        return Promise.reject(new Error(body.msg))
      }
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject(new Error(body.msg || '操作失败'))
    }
    return body
  },
  (err) => {
    const msg = err.response?.data?.msg || err.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

/** 统一上传：走后端 /api/file/upload，返回相对路径 /uploads/xxx */
export async function uploadFile(file, type = 'files') {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', type)
  return http.post('/api/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export default http
