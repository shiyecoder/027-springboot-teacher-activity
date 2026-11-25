<template>
  <div class="page">
    <div class="page-head">
      <h1>站内消息</h1>
      <span class="sub">审批提醒、活动通知与学时变动通知</span>
      <div class="actions"><el-button :disabled="!unread" @click="readAll">全部标记已读（{{ unread }}）</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>消息列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <div class="card-b" style="padding: 6px 16px">
        <div v-for="m in rows" :key="m.id" class="msg" :class="{ unread: !m.isRead }" @click="read(m)">
          <div style="flex: 1">
            <div class="t">{{ m.title }}</div>
            <div class="c">{{ m.content }}</div>
          </div>
          <div style="text-align: right; flex: none; width: 150px">
            <span class="tag" :class="m.isRead ? 'tag-gray' : 'tag-orange'">{{ m.isRead ? '已读' : '未读' }}</span>
            <div class="muted small" style="margin-top: 4px">{{ formatTime(m.createTime) }}</div>
          </div>
        </div>
        <el-empty v-if="!rows.length" description="暂无消息" :image-size="70" />
      </div>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="page"
          :page-size="10" @current-change="load()" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const page = ref(1); const unread = ref(0)

async function load(p) {
  if (p) page.value = p
  const d = await http.get('/api/message/page', { params: { page: page.value, size: 10 } })
  rows.value = d.list
  total.value = d.total
  try { unread.value = await http.get('/api/message/unread') } catch (e) { unread.value = 0 }
}
async function read(m) {
  if (m.isRead) return
  await http.post(`/api/message/${m.id}/read`)
  load()
}
async function readAll() {
  await http.post('/api/message/read-all')
  load()
}
onMounted(() => load())
</script>

<style scoped>
.msg { display: flex; gap: 14px; padding: 12px 0; border-bottom: 1px dashed var(--c-border-l); cursor: pointer; }
.msg:last-child { border-bottom: 0; }
.msg:hover { background: #F8FBFE; }
.msg .t { font-size: 13.5px; font-weight: 600; }
.msg.unread .t::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #B22B31; margin-right: 6px; vertical-align: middle; }
.msg .c { font-size: 12.5px; color: var(--c-text-2); margin-top: 4px; line-height: 1.6; }
</style>
