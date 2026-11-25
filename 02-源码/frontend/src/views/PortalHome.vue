<template>
  <div>
    <div class="portal-hero">
      <el-avatar :size="54" :src="user.avatar">{{ (user.realName || '师').slice(0, 1) }}</el-avatar>
      <div>
        <div class="hi">{{ greeting }}，{{ user.realName }}</div>
        <div class="meta">{{ user.orgName }} · {{ user.roleName }} · 本学期教研学时 {{ creditSum }} 学时</div>
      </div>
      <div class="stat">
        <div><b>{{ acts.length }}</b><span>参与活动</span></div>
        <div><b>{{ achs.length }}</b><span>申报成果</span></div>
        <div><b>{{ creditSum }}</b><span>已确认学时</span></div>
      </div>
    </div>

    <div class="grid2" style="margin-top: 16px">
      <div class="tile">
        <h4>我的教研活动<span class="r"><a @click="$router.push('/activities')">全部</a></span></h4>
        <div class="b">
          <div v-for="a in acts.slice(0, 6)" :key="a.id" class="act-item">
            <img :src="a.cover" style="width: 52px; height: 38px; border-radius: 5px; object-fit: cover" alt="" />
            <div>
              <div class="t">{{ a.title }}</div>
              <div class="m">{{ a.actType }} · {{ formatTime(a.activityTime) }} · {{ a.location }}</div>
            </div>
            <div class="r"><span class="tag" :class="tagClass(a.status)">{{ a.status }}</span></div>
          </div>
          <div v-if="!acts.length" class="empty-tip">暂无教研活动</div>
        </div>
      </div>

      <div class="tile">
        <h4>我的成果<span class="r"><a @click="$router.push('/achievements')">全部</a></span></h4>
        <div class="b">
          <div v-for="x in achs.slice(0, 6)" :key="x.id" class="act-item">
            <img :src="x.cover" style="width: 52px; height: 38px; border-radius: 5px; object-fit: cover" alt="" />
            <div>
              <div class="t">{{ x.title }}</div>
              <div class="m">{{ x.achType }} · {{ x.level }} · 图谱 {{ x.graphScore ?? '-' }}（{{ x.graphLevel || '-' }}）</div>
            </div>
            <div class="r"><span class="tag" :class="tagClass(x.status)">{{ x.status }}</span></div>
          </div>
          <div v-if="!achs.length" class="empty-tip">暂无申报成果</div>
        </div>
      </div>
    </div>

    <div class="grid2" style="margin-top: 16px">
      <div class="tile">
        <h4>学时明细<span class="r"><a @click="$router.push('/credits')">台账</a></span></h4>
        <div class="b">
          <div v-for="c in credits.slice(0, 6)" :key="c.id" class="act-item">
            <div>
              <div class="t">{{ c.sourceType }} · {{ c.sourceNo }}</div>
              <div class="m">{{ c.calcBasis }}</div>
            </div>
            <div class="r">
              <b class="num" style="color: var(--c-primary)">{{ c.credit }}</b>
              <span class="tag" :class="tagClass(c.status)">{{ c.status }}</span>
            </div>
          </div>
          <div v-if="!credits.length" class="empty-tip">暂无学时记录</div>
        </div>
      </div>

      <div class="tile">
        <h4>消息通知<span class="r"><a @click="$router.push('/messages')">全部</a></span></h4>
        <div class="b">
          <div v-for="m in msgs.slice(0, 6)" :key="m.id" class="act-item">
            <div>
              <div class="t">{{ m.title }}</div>
              <div class="m">{{ m.content }}</div>
            </div>
            <div class="r muted small">{{ formatTime(m.createTime) }}</div>
          </div>
          <div v-if="!msgs.length" class="empty-tip">暂无消息</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../api/http'
import { useUserStore } from '../store/user'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const store = useUserStore()
const user = computed(() => store.state.user || {})
const acts = ref([]); const achs = ref([]); const credits = ref([]); const msgs = ref([]); const creditSum = ref(0)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 11) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  const [a, c, m] = await Promise.all([
    http.get('/api/activity/page', { params: { page: 1, size: 12 } }),
    http.get('/api/credit/page', { params: { page: 1, size: 12 } }),
    http.get('/api/message/page', { params: { page: 1, size: 6 } })
  ])
  acts.value = a.list; credits.value = c.list; msgs.value = m.list
  creditSum.value = c.list.filter((x) => x.status === '已确认').reduce((s, x) => s + Number(x.credit || 0), 0).toFixed(1)
  try { achs.value = (await http.get('/api/achievement/page', { params: { page: 1, size: 12 } })).list } catch (e) { achs.value = [] }
})
</script>
