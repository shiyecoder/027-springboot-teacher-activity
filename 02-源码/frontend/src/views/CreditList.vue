<template>
  <div class="page">
    <div class="page-head">
      <h1>学时台账</h1>
      <span class="sub">学时由规则自动核算，计算依据逐条留痕，支持批量确认与申诉</span>
      <div class="actions"><el-button @click="$router.push('/credits/appeal')">学时申诉</el-button></div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="学时台账" name="ledger">
        <div class="filters">
          <el-input v-model="q.keyword" placeholder="台账编号 / 教师" clearable />
          <el-select v-model="q.status" placeholder="状态" clearable>
            <el-option v-for="s in ['待确认', '已确认']" :key="s" :label="s" :value="s" />
          </el-select>
          <div class="tail">
            <el-button @click="reset">重置</el-button>
            <el-button type="primary" @click="load(1)">查询</el-button>
          </div>
        </div>
        <div class="card">
          <div class="card-h"><h3>台账明细</h3><span class="hint">共 {{ total }} 条</span>
            <div class="r"><el-button size="small" type="primary" :disabled="!sel.length" @click="confirmBatch">批量确认（{{ sel.length }}）</el-button></div>
          </div>
          <el-table :data="rows" v-loading="loading" @selection-change="sel = $event">
            <el-table-column type="selection" width="46" :selectable="(r) => r.status === '待确认'" />
            <el-table-column prop="ledgerNo" label="台账编号" min-width="150" />
            <el-table-column prop="teacherName" label="教师" width="96" />
            <el-table-column prop="sourceType" label="来源" width="96" />
            <el-table-column prop="sourceNo" label="关联单据" min-width="140" />
            <el-table-column label="学时" width="80" align="right">
              <template #default="{ row }"><span class="num" style="font-weight:600">{{ row.credit }}</span></template>
            </el-table-column>
            <el-table-column prop="term" label="学期" width="110" />
            <el-table-column prop="calcBasis" label="计算依据" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="92">
              <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="生成时间" min-width="150">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
              v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学时规则" name="rules">
        <div class="card" style="margin-top: 0">
          <div class="card-h"><h3>规则版本</h3><span class="hint">基础学时 + 材料加分 − 缺签扣分，按角色与活动类型区分</span>
            <div class="r"><el-button size="small" :icon="Plus" @click="ruleDlg = true">新增规则</el-button></div>
          </div>
          <el-table :data="ruleRows">
            <el-table-column prop="ruleName" label="规则名称" min-width="180" />
            <el-table-column prop="actType" label="活动类型" width="110">
              <template #default="{ row }">{{ row.actType === '*' ? '全部' : row.actType }}</template>
            </el-table-column>
            <el-table-column prop="roleType" label="角色" width="100" />
            <el-table-column prop="baseCredit" label="基础学时" width="96" align="right" />
            <el-table-column prop="materialBonus" label="材料加分" width="96" align="right" />
            <el-table-column prop="signPenalty" label="缺签扣分" width="96" align="right" />
            <el-table-column prop="version" label="版本" width="70" align="center" />
            <el-table-column label="状态" width="92">
              <template #default="{ row }"><span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '生效中' : '历史' }}</span></template>
            </el-table-column>
            <el-table-column label="生效时间" min-width="150">
              <template #default="{ row }">{{ formatTime(row.effectTime) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="ruleDlg" title="新增学时规则" width="560px">
      <el-form :model="rule" label-width="96px">
        <el-form-item label="规则名称"><el-input v-model="rule.ruleName" /></el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="rule.actType" style="width: 100%">
            <el-option label="全部类型" value="*" />
            <el-option v-for="t in ['集体备课', '公开课', '听评课', '专题研讨', '课题分享']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="rule.roleType" style="width: 100%">
            <el-option v-for="r in ['主持', '主讲', '记录', '评课人', '参与者']" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础学时"><el-input-number v-model="rule.baseCredit" :min="0" :step="0.5" :precision="1" /></el-form-item>
        <el-form-item label="材料加分"><el-input-number v-model="rule.materialBonus" :min="0" :step="0.5" :precision="1" /></el-form-item>
        <el-form-item label="缺签扣分"><el-input-number v-model="rule.signPenalty" :min="0" :step="0.5" :precision="1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="ruleDlg = false">取消</el-button><el-button type="primary" @click="saveRule">保存并生效</el-button></template>
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

const tab = ref('ledger')
const rows = ref([]); const total = ref(0); const loading = ref(false); const sel = ref([])
const q = reactive({ page: 1, size: 10, keyword: '', status: '' })
const ruleRows = ref([]); const ruleDlg = ref(false)
const rule = reactive({ ruleName: '', actType: '*', roleType: '参与者', baseCredit: 1.0, materialBonus: 0.5, signPenalty: 0.5 })

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/credit/page', { params: { ...q, keyword: q.keyword || undefined, status: q.status || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, keyword: '', status: '' }); load(1) }
async function confirmBatch() {
  const n = await http.post('/api/credit/confirm', { ids: sel.value.map((r) => r.id) })
  ElMessage.success(`已确认 ${n} 条学时`)
  load()
}
async function loadRules() { ruleRows.value = await http.get('/api/credit/rules') }
async function saveRule() {
  if (!rule.ruleName) return ElMessage.warning('请填写规则名称')
  await http.post('/api/credit/rule', { ...rule })
  ElMessage.success('规则已生效')
  ruleDlg.value = false
  loadRules()
}
onMounted(() => { load(); loadRules() })
</script>
