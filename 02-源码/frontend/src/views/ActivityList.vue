<template>
  <div class="page">
    <div class="page-head">
      <h1>教研活动</h1>
      <span class="sub">集体备课、公开课、听评课、专题研讨、课题分享全流程管理</span>
      <div class="actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建活动</el-button>
      </div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="活动标题 / 编号" clearable />
      <el-select v-model="q.actType" placeholder="活动类型" clearable>
        <el-option v-for="t in dicts.actType" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="q.status" placeholder="状态" clearable>
        <el-option v-for="s in dicts.status" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="q.groupId" placeholder="教研组" clearable>
        <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
      </el-select>
      <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="w130" style="width: 250px" />
      <div class="tail">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>活动列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="封面" width="86">
          <template #default="{ row }"><img class="cover" :src="row.cover" alt="" /></template>
        </el-table-column>
        <el-table-column label="活动信息" min-width="240">
          <template #default="{ row }">
            <div style="font-weight: 600">{{ row.title }}</div>
            <div class="muted small">{{ row.actNo }} · {{ row.actType }} · {{ row.subject }} {{ row.grade }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="教研组" min-width="110" />
        <el-table-column label="主持 / 主讲" min-width="120">
          <template #default="{ row }">{{ row.hostName }} / {{ row.speakerName }}</template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="130" show-overflow-tooltip />
        <el-table-column label="签到 / 分工" width="100" align="center">
          <template #default="{ row }"><span class="num">{{ row.signCount }} / {{ row.memberCount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/activities/' + row.id)">详情</el-button>
            <el-button v-if="nextStatus(row.status)" link type="primary" @click="openFlow(row)">流转</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
          v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新建教研活动" width="680px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="活动标题" required><el-input v-model="form.title" placeholder="例：高二数学集体备课：导数应用单元" /></el-form-item>
        <el-form-item label="活动类型" required>
          <el-select v-model="form.actType" style="width: 100%">
            <el-option v-for="t in dicts.actType" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="教研组" required>
          <el-select v-model="form.groupId" style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科 / 年级">
          <el-input v-model="form.subject" placeholder="学科" style="width: 48%; margin-right: 4%" />
          <el-input v-model="form.grade" placeholder="年级" style="width: 48%" />
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker v-model="form.activityTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地点"><el-input v-model="form.location" placeholder="例：第三教学楼 102" /></el-form-item>
        <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="30" :max="240" :step="15" /></el-form-item>
        <el-form-item label="活动说明"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存为草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="flowDlg" title="活动状态流转" width="520px">
      <el-form label-width="90px">
        <el-form-item label="当前状态"><span class="tag" :class="tagClass(cur?.status)">{{ cur?.status }}</span></el-form-item>
        <el-form-item label="目标状态"><span class="tag tag-blue">{{ nextStatus(cur?.status) }}</span></el-form-item>
        <el-form-item label="流转说明">
          <el-input v-model="flowReason" type="textarea" :rows="3" placeholder="退回或特殊流转时必须填写原因" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="状态机只允许前进一步，不允许越级；转入「已归档」前系统会校验材料完整性。" />
      </el-form>
      <template #footer>
        <el-button @click="flowDlg = false">取消</el-button>
        <el-button type="primary" @click="doFlow">确认流转</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const groups = ref([]); const dicts = ref({ actType: [], status: [] })
const q = reactive({ page: 1, size: 10, keyword: '', actType: '', status: '', groupId: null })
const dates = ref([])
const dlg = ref(false); const flowDlg = ref(false); const cur = ref(null); const flowReason = ref('')
const form = reactive({ title: '', actType: '集体备课', groupId: null, subject: '', grade: '', activityTime: '', location: '', duration: 90, summary: '' })

const FLOW = ['草稿', '已发布', '报名中', '进行中', '待归档', '已归档']
function nextStatus(s) {
  const i = FLOW.indexOf(s)
  return i >= 0 && i < FLOW.length - 1 ? FLOW[i + 1] : ''
}

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/activity/page', {
      params: { ...q, keyword: q.keyword || undefined, actType: q.actType || undefined, status: q.status || undefined,
        groupId: q.groupId || undefined, start: dates.value?.[0], end: dates.value?.[1] }
    })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, keyword: '', actType: '', status: '', groupId: null }); dates.value = []; load(1) }
function openCreate() {
  Object.assign(form, { title: '', actType: '集体备课', groupId: groups.value[0]?.id || null, subject: '', grade: '', activityTime: '', location: '', duration: 90, summary: '' })
  dlg.value = true
}
async function save() {
  if (!form.title || !form.activityTime) return ElMessage.warning('请填写标题与活动时间')
  await http.post('/api/activity', { ...form })
  ElMessage.success('活动已创建（草稿）')
  dlg.value = false
  load(1)
}
function openFlow(row) { cur.value = row; flowReason.value = ''; flowDlg.value = true }
async function doFlow() {
  await http.post(`/api/activity/${cur.value.id}/transition`, { to: nextStatus(cur.value.status), reason: flowReason.value })
  ElMessage.success('状态已流转')
  flowDlg.value = false
  load()
}

onMounted(async () => {
  load()
  try {
    dicts.value = await http.get('/api/activity/dicts')
    groups.value = await http.get('/api/group/list')
  } catch (e) { /* 字典失败不阻塞列表 */ }
})
</script>
