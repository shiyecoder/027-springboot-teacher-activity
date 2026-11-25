import { reactive, computed } from 'vue'

/**
 * 轻量用户状态：保存登录态、用户信息、按角色下发的菜单树。
 * 使用 reactive 单例，避免为了一个状态引入额外依赖。
 */
const state = reactive({
  token: localStorage.getItem('trs_token') || '',
  user: JSON.parse(localStorage.getItem('trs_user') || 'null'),
  menus: JSON.parse(localStorage.getItem('trs_menus') || '[]')
})

export function useUserStore() {
  const isLogin = computed(() => !!state.token)
  /** 教师为普通用户，走门户风格；其余角色走后台管理风格 */
  const isTeacher = computed(() => state.user?.roleCode === 'TEACHER')

  function restore() {
    state.token = localStorage.getItem('trs_token') || ''
    state.user = JSON.parse(localStorage.getItem('trs_user') || 'null')
    state.menus = JSON.parse(localStorage.getItem('trs_menus') || '[]')
  }

  function setLogin(data) {
    state.token = data.token
    state.user = data.user
    state.menus = data.menus || []
    localStorage.setItem('trs_token', data.token)
    localStorage.setItem('trs_user', JSON.stringify(data.user))
    localStorage.setItem('trs_menus', JSON.stringify(state.menus))
  }

  function setUser(user) {
    state.user = user
    localStorage.setItem('trs_user', JSON.stringify(user))
  }

  function logout() {
    state.token = ''
    state.user = null
    state.menus = []
    localStorage.removeItem('trs_token')
    localStorage.removeItem('trs_user')
    localStorage.removeItem('trs_menus')
  }

  return { state, isLogin, isTeacher, restore, setLogin, setUser, logout }
}
