<template>
  <div class="page">
    <div class="page-head"><h1>操作日志</h1><span class="sub">关键业务操作留痕，便于审计追溯</span></div>

    <div class="filters">
      <el-input v-model="keyword" placeholder="模块 / 操作人 / 单据号" clearable />
      <div class="tail"><el-button type="primary" @click="load(1)">查询</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>日志列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="userName" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="bizNo" label="关联单据" min-width="160" />
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="page" :page-size="10" @current-change="load()" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const page = ref(1); const keyword = ref('')
async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const d = await http.get('/api/system/log/page', { params: { page: page.value, size: 10, keyword: keyword.value || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
onMounted(() => load())
</script>
