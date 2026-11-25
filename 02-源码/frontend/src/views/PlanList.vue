<template>
  <div class="page">
    <div class="page-head">
      <h1>学期计划</h1>
      <span class="sub">教研组按学期编制计划，教务处审批后作为活动开展依据</span>
      <div class="actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">编制计划</el-button>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="q.term" placeholder="学期" clearable>
        <el-option v-for="t in terms" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="q.status" placeholder="状态" clearable>
        <el-option v-for="s in ['草稿', '待审批', '已通过', '已退回']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-input v-model="q.keyword" placeholder="计划编号 / 教研组" clearable />
      <div class="tail">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>计划列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="planNo" label="计划编号" min-width="150" />
        <el-table-column prop="groupName" label="教研组" min-width="110" />
        <el-table-column prop="term" label="学期" min-width="110" />
        <el-table-column prop="goal" label="教研目标" min-width="200" show-overflow-tooltip />
        <el-table-column prop="activityTypes" label="活动安排" min-width="180" show-overflow-tooltip />
        <el-table-column prop="planCount" label="计划场次" width="90" align="center" />
        <el-table-column label="起止日期" min-width="170">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="submitName" label="提交人" width="90" />
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '草稿'" link type="primary" @click="submit(row)">提交审批</el-button>
            <el-button v-if="row.status === '待审批'" link type="primary" @click="audit(row)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
          v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <!-- 编制 / 编辑 -->
    <el-dialog v-model="dlg" :title="form.id ? '编辑计划' : '编制学期计划'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="教研组" required>
          <el-select v-model="form.groupId" style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期" required>
          <el-select v-model="form.term" style="width: 100%">
            <el-option v-for="t in terms" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="教研目标" required>
          <el-input v-model="form.goal" type="textarea" :rows="2" placeholder="本学期教研要解决的核心问题" />
        </el-form-item>
        <el-form-item label="活动安排">
          <el-input v-model="form.activityTypes" placeholder="例：集体备课 6 次 / 公开课 2 次" />
        </el-form-item>
        <el-form-item label="计划场次">
          <el-input-number v-model="form.planCount" :min="1" :max="60" />
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item label="预期成果">
          <el-input v-model="form.expectedResult" placeholder="例：完成单元课例 2 个" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="drawer" title="计划详情" size="520px">
      <template v-if="cur">
        <div class="detail-grid">
          <div><div class="lab">计划编号</div><div class="val">{{ cur.planNo }}</div></div>
          <div><div class="lab">状态</div><div class="val"><span class="tag" :class="tagClass(cur.status)">{{ cur.status }}</span></div></div>
          <div><div class="lab">教研组</div><div class="val">{{ cur.groupName }}</div></div>
          <div><div class="lab">学期</div><div class="val">{{ cur.term }}</div></div>
          <div><div class="lab">计划场次</div><div class="val">{{ cur.planCount }} 场</div></div>
          <div><div class="lab">提交人</div><div class="val">{{ cur.submitName || '-' }}</div></div>
          <div><div class="lab">开始日期</div><div class="val">{{ cur.startDate }}</div></div>
          <div><div class="lab">结束日期</div><div class="val">{{ cur.endDate }}</div></div>
        </div>
        <p class="section-title" style="margin-top: 20px">教研目标</p>
        <div class="conclusion">{{ cur.goal }}</div>
        <p class="section-title" style="margin-top: 16px">活动安排</p>
        <div class="conclusion">{{ cur.activityTypes || '-' }}</div>
        <p class="section-title" style="margin-top: 16px">预期成果</p>
        <div class="conclusion">{{ cur.expectedResult || '-' }}</div>
        <p class="section-title" style="margin-top: 16px">审批意见</p>
        <div class="conclusion">{{ cur.auditOpinion || '暂无审批意见' }}</div>
      </template>
    </el-drawer>

    <!-- 审批 -->
    <el-dialog v-model="auditDlg" title="计划审批" width="520px">
      <el-form label-width="90px">
        <el-form-item label="审批结论">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">退回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="退回时必须填写具体原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDlg = false">取消</el-button>
        <el-button type="primary" @click="doAudit">提交</el-button>
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

const rows = ref([]); const total = ref(0); const loading = ref(false)
const groups = ref([]); const terms = ['2025-2026-1', '2025-2026-2', '2026-2027-1', '2026-2027-2']
const q = reactive({ page: 1, size: 10, term: '', status: '', keyword: '' })
const dlg = ref(false); const drawer = ref(false); const auditDlg = ref(false)
const cur = ref(null); const range = ref([])
const form = reactive({ id: null, groupId: null, term: '2026-2027-1', goal: '', activityTypes: '', planCount: 8, expectedResult: '' })
const auditForm = reactive({ pass: true, opinion: '' })

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/plan/page', { params: { ...q, term: q.term || undefined, status: q.status || undefined, keyword: q.keyword || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, term: '', status: '', keyword: '' }); load(1) }
function openCreate() {
  Object.assign(form, { id: null, groupId: groups.value[0]?.id || null, term: '2026-2027-1', goal: '', activityTypes: '', planCount: 8, expectedResult: '' })
  range.value = []
  dlg.value = true
}
async function save() {
  if (!form.goal) return ElMessage.warning('请填写教研目标')
  const body = { ...form, startDate: range.value?.[0], endDate: range.value?.[1] }
  if (form.id) await http.put(`/api/plan/${form.id}`, body)
  else await http.post('/api/plan', body)
  ElMessage.success('已保存')
  dlg.value = false
  load()
}
function openDetail(row) { cur.value = row; drawer.value = true }
async function submit(row) {
  await http.post(`/api/plan/${row.id}/submit`)
  ElMessage.success('已提交审批')
  load()
}
function audit(row) { cur.value = row; Object.assign(auditForm, { pass: true, opinion: '' }); auditDlg.value = true }
async function doAudit() {
  if (!auditForm.pass && !auditForm.opinion.trim()) return ElMessage.warning('退回必须填写原因')
  await http.post(`/api/plan/${cur.value.id}/audit`, { ...auditForm })
  ElMessage.success('审批完成')
  auditDlg.value = false
  load()
}

onMounted(async () => {
  load()
  try { groups.value = await http.get('/api/group/list') } catch (e) { groups.value = [] }
})
</script>
