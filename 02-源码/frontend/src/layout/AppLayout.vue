<template>
  <!-- 教师（普通用户）使用门户风格；管理员/教务/组长/专家使用后台管理风格 -->
  <div v-if="isTeacher" class="portal">
    <header class="portal-top">
      <div class="brand"><i>研</i>教师教研与成果平台</div>
      <nav>
        <a v-for="n in PORTAL_NAV" :key="n.to" :class="{ on: route.path === n.to }" @click="go(n.to)">{{ n.name }}</a>
      </nav>
      <div class="who">
        <el-avatar :size="30" :src="avatar">{{ (user.realName || '师').slice(0, 1) }}</el-avatar>
        <span>{{ user.realName }}</span>
        <el-button link style="color: rgba(255,255,255,.85)" @click="logout">退出登录</el-button>
      </div>
    </header>
    <div class="portal-body"><router-view /></div>
  </div>

  <div v-else class="app">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand"><span class="logo">研</span><b v-show="!collapsed">教研成果管理平台</b></div>
      <nav class="menu">
        <template v-for="node in menuNodes" :key="node.key">
          <div v-if="node.children" class="grp" v-show="!collapsed">{{ node.name }}</div>
          <template v-if="node.children">
            <a v-for="c in node.children" :key="c.key" :class="{ on: isActive(c.to) }" @click="go(c.to)" :title="c.name">
              <el-icon class="ic"><component :is="c.icon" /></el-icon>
              <span v-show="!collapsed">{{ c.name }}</span>
            </a>
          </template>
          <a v-else :class="{ on: isActive(node.to) }" @click="go(node.to)" :title="node.name">
            <el-icon class="ic"><component :is="node.icon" /></el-icon>
            <span v-show="!collapsed">{{ node.name }}</span>
          </a>
        </template>
      </nav>
      <div class="side-foot" v-show="!collapsed">通道 lane1 · v1.0.0</div>
    </aside>

    <div class="main">
      <header class="topbar">
        <el-icon class="fold" @click="collapsed = !collapsed"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        <span class="crumb">{{ crumb }}</span>
        <span class="spacer"></span>
        <el-popover placement="bottom-end" :width="320" trigger="click" @show="loadLatest">
          <template #reference>
            <span class="icon-btn">
              <el-badge :value="unread" :hidden="!unread" :max="99"><el-icon><Bell /></el-icon></el-badge>
            </span>
          </template>
          <div class="msg-pop">
            <div class="flex" style="padding: 4px 2px 10px; border-bottom: 1px solid var(--c-border-l)">
              <b>消息</b>
              <el-button link type="primary" class="right" style="margin-left:auto" @click="readAll">全部已读</el-button>
            </div>
            <div v-for="m in latest" :key="m.id" class="msg-item" @click="go('/messages')">
              <div class="t">{{ m.title }}</div>
              <div class="m">{{ formatTime(m.createTime) }}</div>
            </div>
            <el-empty v-if="!latest.length" description="暂无消息" :image-size="56" />
            <div style="text-align: center; padding-top: 8px">
              <el-button link type="primary" @click="go('/messages')">查看全部</el-button>
            </div>
          </div>
        </el-popover>
        <span class="icon-btn" @click="go('/messages')" title="站内消息"><el-icon><ChatDotSquare /></el-icon></span>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="who">
            <el-avatar :size="30" :src="avatar">{{ (user.realName || '师').slice(0, 1) }}</el-avatar>
            <div>
              <div class="n">{{ user.realName }}</div>
              <div class="r">{{ user.roleName }}</div>
            </div>
            <el-icon style="margin-left: 4px; color: #5F7484"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="messages">站内消息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <div class="content"><router-view /></div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Bell, ChatDotSquare, ArrowDown, Odometer, Calendar, Tickets, EditPen, FolderOpened, Files, Trophy, Share, Coin, ChatLineSquare, UserFilled, Setting, User, Lock, Menu, Document } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import http from '../api/http'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const user = computed(() => store.state.user || {})
const isTeacher = computed(() => user.value.roleCode === 'TEACHER')
const avatar = computed(() => user.value.avatar || '')
const collapsed = ref(false)
const unread = ref(0)
const latest = ref([])

const PORTAL_NAV = [
  { to: '/portal', name: '我的教研' },
  { to: '/activities', name: '教研活动' },
  { to: '/achievements', name: '成果申报' },
  { to: '/credits', name: '学时台账' },
  { to: '/messages', name: '消息' },
  { to: '/profile', name: '个人中心' }
]

/** 菜单 path → 图标；后端只下发 path，图标在前端按业务语义绑定 */
const ICONS = {
  '/dashboard': Odometer, '/plans': Calendar, '/activities': Tickets, '/evaluations': EditPen,
  '/materials': FolderOpened, '/resources': Files, '/achievements': Trophy, '/credits': Coin,
  '/graph': Share, '/messages': ChatLineSquare, '/groups': UserFilled,
  '/system/users': User, '/system/roles': Lock, '/system/menus': Menu, '/system/logs': Document
}
const GROUP_ICONS = { '/biz': Odometer, '/system': Setting }

const menuNodes = computed(() => {
  const out = []
  const walk = (items, depth) => {
    for (const m of items || []) {
      if (m.type === 'B') continue
      if (m.type === 'M') {
        out.push({
          key: 'g' + m.id, name: m.name, icon: GROUP_ICONS[m.path] || Setting,
          children: (m.children || []).filter((c) => c.type === 'C').map((c) => ({
            key: c.id, name: c.name, to: c.path, icon: ICONS[c.path] || Files
          }))
        })
      } else if (m.type === 'C' && depth === 0) {
        out.push({ key: m.id, name: m.name, to: m.path, icon: ICONS[m.path] || Files })
      }
    }
  }
  walk(store.state.menus, 0)
  if (!out.length) {
    // 菜单未下发时的兜底，保证页面可用
    out.push({ key: 'g1', name: '业务中心', icon: Odometer, children: [
      { key: 'f1', name: '教研看板', to: '/dashboard', icon: Odometer },
      { key: 'f2', name: '教研活动', to: '/activities', icon: Tickets }
    ] })
  }
  return out
})

const crumb = computed(() => route.meta.title || '教研看板')

function isActive(path) {
  if (!path) return false
  return route.path === path || route.path.startsWith(path + '/')
}
function go(path) {
  if (path && route.path !== path) router.push(path)
}
async function logout() {
  try {
    await ElMessageBox.confirm('确定退出当前账号？', '退出登录', { type: 'warning' })
  } catch (e) { return }
  store.logout()
  router.push('/login')
}
function onCommand(cmd) {
  if (cmd === 'logout') logout()
  else if (cmd === 'profile') go('/profile')
  else if (cmd === 'messages') go('/messages')
}
async function loadUnread() {
  try { unread.value = await http.get('/api/message/unread') } catch (e) { unread.value = 0 }
}
async function loadLatest() {
  try { latest.value = (await http.get('/api/message/latest')) || [] } catch (e) { latest.value = [] }
}
async function readAll() {
  await http.post('/api/message/read-all')
  unread.value = 0
  latest.value = []
}

let timer = null
onMounted(() => {
  loadUnread()
  timer = setInterval(loadUnread, 60000)
})
onUnmounted(() => timer && clearInterval(timer))
</script>

<style scoped>
.app { display: flex; height: 100vh; overflow: hidden; }
.sidebar { width: 220px; background: var(--c-primary); color: #fff; display: flex; flex-direction: column; transition: width .18s; flex: none; }
.sidebar.collapsed { width: 64px; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 16px; border-bottom: 1px solid rgba(255,255,255,.1); }
.brand .logo { width: 28px; height: 28px; border-radius: 6px; background: rgba(255,255,255,.16); display: grid; place-items: center; font-size: 14px; flex: none; }
.brand b { font-size: 14px; font-weight: 600; white-space: nowrap; }
.menu { flex: 1; overflow-y: auto; padding: 8px 0; }
.menu .grp { font-size: 11.5px; color: rgba(255,255,255,.45); padding: 10px 18px 6px; letter-spacing: .5px; }
.menu a { display: flex; align-items: center; gap: 10px; height: 38px; padding: 0 18px; color: rgba(255,255,255,.8); font-size: 13px; cursor: pointer; border-left: 3px solid transparent; }
.menu a:hover { background: rgba(255,255,255,.07); color: #fff; }
.menu a.on { background: rgba(40,120,181,.38); color: #fff; border-left-color: #6FB3E0; }
.menu a .ic { font-size: 16px; flex: none; }
.side-foot { padding: 12px 18px; font-size: 11.5px; color: rgba(255,255,255,.4); border-top: 1px solid rgba(255,255,255,.08); }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid var(--c-border-l); display: flex; align-items: center; gap: 12px; padding: 0 20px; flex: none; }
.fold { font-size: 18px; color: var(--c-text-2); cursor: pointer; }
.crumb { font-size: 14px; font-weight: 600; }
.icon-btn { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 6px; cursor: pointer; color: var(--c-text-2); }
.icon-btn:hover { background: #F4F8FB; }
.who { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 6px; border-radius: 6px; }
.who:hover { background: #F4F8FB; }
.who .n { font-size: 13px; line-height: 16px; }
.who .r { font-size: 11.5px; color: var(--c-text-2); }
.content { flex: 1; overflow: auto; }
.msg-pop { max-height: 360px; }
.msg-item { padding: 9px 2px; border-bottom: 1px dashed var(--c-border-l); cursor: pointer; }
.msg-item:hover { background: #F7FAFD; }
.msg-item .t { font-size: 13px; }
.msg-item .m { font-size: 11.5px; color: var(--c-text-2); margin-top: 2px; }
</style>
