<template>
  <div class="page">
    <div class="page-head">
      <h1>学时申诉</h1>
      <span class="sub">教师对学时核算结果有异议时提出申诉，教务复核后补记或驳回</span>
      <div class="actions"><el-button @click="$router.push('/credits')">返回台账</el-button></div>
    </div>

    <div class="filters">
      <el-select v-model="status" placeholder="处理状态" clearable>
        <el-option v-for="s in ['待处理', '已处理', '已驳回']" :key="s" :label="s" :value="s" />
      </el-select>
      <div class="tail"><el-button @click="load(1)">查询</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>申诉列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="teacherName" label="申诉人" width="96" />
        <el-table-column prop="reason" label="申诉理由" min-width="300" show-overflow-tooltip />
        <el-table-column label="期望学时" width="100" align="right">
          <template #default="{ row }"><span class="num">{{ row.expectCredit ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column prop="handleName" label="处理人" width="96" />
        <el-table-column label="提交时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待处理'" link type="primary" @click="open(row)">处理</el-button>
            <span v-else class="muted small">{{ row.reply || '已归档' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="page"
          :page-size="10" @current-change="load()" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="处理学时申诉" width="540px">
      <el-form label-width="90px">
        <el-form-item label="申诉人">{{ cur?.teacherName }}</el-form-item>
        <el-form-item label="申诉理由"><div class="conclusion">{{ cur?.reason }}</div></el-form-item>
        <el-form-item label="处理结论">
          <el-radio-group v-model="form.status">
            <el-radio value="已处理">通过并补记</el-radio>
            <el-radio value="已驳回">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="答复说明"><el-input v-model="form.reply" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="submit">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const page = ref(1); const status = ref('')
const dlg = ref(false); const cur = ref(null)
const form = reactive({ status: '已处理', reply: '' })

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const d = await http.get('/api/credit/appeal/page', { params: { page: page.value, size: 10, status: status.value || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function open(row) { cur.value = row; Object.assign(form, { status: '已处理', reply: '' }); dlg.value = true }
async function submit() {
  await http.post(`/api/credit/appeal/${cur.value.id}/handle`, { ...form })
  ElMessage.success('已处理')
  dlg.value = false
  load()
}
onMounted(() => load())
</script>
