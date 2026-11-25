<template>
  <div class="page">
    <div class="page-head">
      <h1>听评课</h1>
      <span class="sub">公开课与听评课活动的课堂观察记录，评课得分计入活动质量评估</span>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="活动标题" clearable />
      <el-select v-model="q.actType" placeholder="课型" clearable>
        <el-option label="公开课" value="公开课" />
        <el-option label="听评课" value="听评课" />
      </el-select>
      <el-select v-model="q.status" placeholder="状态" clearable>
        <el-option v-for="s in ['已发布', '报名中', '进行中', '待归档', '已归档']" :key="s" :label="s" :value="s" />
      </el-select>
      <div class="tail">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>待评与已评课活动</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="封面" width="86">
          <template #default="{ row }"><img class="cover" :src="row.cover" alt="" /></template>
        </el-table-column>
        <el-table-column label="课题" min-width="240">
          <template #default="{ row }">
            <div style="font-weight: 600">{{ row.title }}</div>
            <div class="muted small">{{ row.actType }} · {{ row.subject }} {{ row.grade }} · {{ row.location }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="speakerName" label="主讲" width="90" />
        <el-table-column prop="groupName" label="教研组" min-width="110" />
        <el-table-column label="活动时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">评课记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="q.page"
          :page-size="q.size" @current-change="load()" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="听评课记录" size="600px">
      <template v-if="cur">
        <div class="flex" style="gap: 10px; margin-bottom: 12px">
          <b>{{ cur.title }}</b>
          <span class="tag" :class="tagClass(cur.status)">{{ cur.status }}</span>
        </div>
        <div class="muted small" style="margin-bottom: 14px">主讲 {{ cur.speakerName }} · {{ formatTime(cur.activityTime) }} · {{ cur.location }}</div>

        <p class="section-title">已有评课（{{ evals.length }}）</p>
        <div v-if="!evals.length" class="empty-tip">暂无评课记录，请在下方提交</div>
        <div v-for="e in evals" :key="e.id" class="card" style="padding: 12px 14px; margin-bottom: 10px">
          <div class="flex"><b>{{ e.evaluatorName }}</b><span class="tag tag-blue" style="margin-left: 8px">总分 {{ e.totalScore }}</span>
            <span class="muted small" style="margin-left: auto">{{ formatTime(e.createTime) }}</span></div>
          <div class="small" style="margin-top: 8px">教学设计 {{ e.designScore }} · 课堂互动 {{ e.interactionScore }} · 目标达成 {{ e.effectScore }} · 素养落实 {{ e.literacyScore }}</div>
          <div class="small" style="margin-top: 6px"><span class="muted">亮点：</span>{{ e.advantage }}</div>
          <div class="small"><span class="muted">建议：</span>{{ e.suggestion }}</div>
        </div>

        <p class="section-title" style="margin-top: 18px">提交评课</p>
        <el-form label-width="86px">
          <el-form-item v-for="(label, key) in KEYS" :key="key" :label="label">
            <el-input-number v-model="form[key]" :min="0" :max="25" size="small" controls-position="right" />
            <span class="muted small" style="margin-left: 8px">/ 25 分</span>
          </el-form-item>
          <el-form-item label="课堂亮点"><el-input v-model="form.advantage" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="改进建议"><el-input v-model="form.suggestion" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <div style="text-align: right"><el-button type="primary" @click="submit">提交评课</el-button></div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', actType: '公开课', status: '' })
const drawer = ref(false); const cur = ref(null); const evals = ref([])
const KEYS = { designScore: '教学设计', interactionScore: '课堂互动', effectScore: '目标达成', literacyScore: '素养落实' }
const form = reactive({ designScore: 20, interactionScore: 20, effectScore: 20, literacyScore: 20, advantage: '', suggestion: '' })

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/activity/page', { params: { ...q, keyword: q.keyword || undefined, actType: q.actType || undefined, status: q.status || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, keyword: '', actType: '公开课', status: '' }); load(1) }
async function open(row) {
  cur.value = row
  drawer.value = true
  const d = await http.get(`/api/activity/${row.id}`)
  evals.value = d.evaluations || []
}
async function submit() {
  await http.post('/api/activity/evaluation', { activityId: cur.value.id, evaluatorId: 1, ...form })
  ElMessage.success('评课已提交')
  const d = await http.get(`/api/activity/${cur.value.id}`)
  evals.value = d.evaluations || []
  load()
}
onMounted(() => load())
</script>
