import { createRouter, createWebHistory } from 'vue-router'

/** 路由与后端菜单 path 一一对应，菜单下发的 path 可直接跳转 */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '教研看板' } },
      { path: 'plans', component: () => import('../views/PlanList.vue'), meta: { title: '学期计划' } },
      { path: 'activities', component: () => import('../views/ActivityList.vue'), meta: { title: '教研活动' } },
      { path: 'activities/:id', component: () => import('../views/ActivityDetail.vue'), meta: { title: '活动详情' } },
      { path: 'evaluations', component: () => import('../views/EvaluationList.vue'), meta: { title: '听评课' } },
      { path: 'materials', component: () => import('../views/MaterialList.vue'), meta: { title: '材料归档' } },
      { path: 'resources', component: () => import('../views/ResourceList.vue'), meta: { title: '资源库' } },
      { path: 'achievements', component: () => import('../views/AchievementList.vue'), meta: { title: '成果申报' } },
      { path: 'achievements/:id', component: () => import('../views/AchievementDetail.vue'), meta: { title: '成果详情' } },
      { path: 'graph', component: () => import('../views/GraphView.vue'), meta: { title: '成果关联图谱' } },
      { path: 'credits', component: () => import('../views/CreditList.vue'), meta: { title: '学时台账' } },
      { path: 'credits/appeal', component: () => import('../views/CreditAppeal.vue'), meta: { title: '学时申诉' } },
      { path: 'groups', component: () => import('../views/GroupList.vue'), meta: { title: '教研组' } },
      { path: 'messages', component: () => import('../views/MessageList.vue'), meta: { title: '站内消息' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'portal', component: () => import('../views/PortalHome.vue'), meta: { title: '我的教研' } },
      { path: 'system/users', component: () => import('../views/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', component: () => import('../views/RoleList.vue'), meta: { title: '角色权限' } },
      { path: 'system/menus', component: () => import('../views/MenuList.vue'), meta: { title: '菜单管理' } },
      { path: 'system/logs', component: () => import('../views/LogList.vue'), meta: { title: '操作日志' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('trs_token')
  if (to.meta.public) return true
  if (!token) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
