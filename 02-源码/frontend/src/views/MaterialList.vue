<template>
  <div class="page">
    <div class="page-head">
      <h1>材料归档</h1>
      <span class="sub">活动材料按类型归档，材料齐全后才允许流转至「已归档」并自动生成学时</span>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="活动标题 / 编号" clearable />
      <el-select v-model="q.status" placeholder="状态" clearable>
        <el-option v-for="s in ['进行中', '待归档', '已归档']" :key="s" :label="s" :value="s" />
      </el-select>
      <div class="tail">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>待归档活动</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="封面" width="86">
          <template #default="{ row }"><img class="cover" :src="row.cover" alt="" /></template>
        </el-table-column>
        <el-table-column label="活动" min-width="250">
          <template #default="{ row }">
            <div style="font-weight: 600">{{ row.title }}</div>
            <div class="muted small">{{ row.actNo }} · {{ row.actType }} · {{ row.groupName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="归档校验" width="140">
          <template #default="{ row }">
            <span class="tag" :class="row.archivePass ? 'tag-green' : 'tag-orange'">{{ row.archivePass ? '材料齐全' : '待补齐' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="签到/分工" width="110" align="center">
          <template #default="{ row }"><span class="num">{{ row.signCount }} / {{ row.memberCount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">材料管理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="q.page"
          :page-size="q.size" @current-change="load()" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="活动材料管理" size="620px">
      <template v-if="cur">
        <div class="flex" style="gap: 10px"><b>{{ cur.title }}</b><span class="tag" :class="tagClass(cur.status)">{{ cur.status }}</span></div>
        <div class="muted small" style="margin: 6px 0 14px">{{ cur.actNo }} · {{ cur.groupName }} · {{ formatTime(cur.activityTime) }}</div>

        <div class="conclusion" :style="check.passed ? '' : 'border-left-color:#B22B31'">
          <span v-if="check.passed">材料齐全（签到 {{ check.signCount }} 人 / 分工 {{ check.memberCount }} 人），可归档。</span>
          <span v-else>待补齐：{{ (check.missing || []).join('、') }}</span>
        </div>

        <p class="section-title" style="margin-top: 16px">已归档材料（{{ mats.length }}）</p>
        <el-table :data="mats" size="small" empty-text="暂无材料">
          <el-table-column prop="matType" label="类型" width="90" />
          <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
          <el-table-column label="大小" width="90">
            <template #default="{ row }">{{ (row.fileSize / 1024).toFixed(0) }} KB</template>
          </el-table-column>
          <el-table-column label="上传时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>

        <p class="section-title" style="margin-top: 18px">上传新材料</p>
        <el-form label-width="86px">
          <el-form-item label="材料类型">
            <el-select v-model="form.matType" style="width: 220px">
              <el-option v-for="t in ['教案', '课件', '照片', '记录', '评课表', '反思']" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="文件">
            <el-upload :show-file-list="false" :http-request="doUpload">
              <el-button :loading="uploading" :icon="Upload">选择文件</el-button>
              <span class="muted small" style="margin-left: 8px">{{ form.fileName || '未选择' }}</span>
            </el-upload>
          </el-form-item>
        </el-form>
        <div style="text-align: right"><el-button type="primary" @click="save">提交归档</el-button></div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http, { uploadFile } from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', status: '待归档' })
const drawer = ref(false); const cur = ref(null); const mats = ref([]); const check = ref({}); const uploading = ref(false)
const form = reactive({ matType: '教案', filePath: '', fileName: '', fileSize: 0 })

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/activity/page', { params: { ...q, keyword: q.keyword || undefined, status: q.status || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, keyword: '', status: '待归档' }); load(1) }
async function open(row) {
  cur.value = row
  drawer.value = true
  const d = await http.get(`/api/activity/${row.id}`)
  mats.value = d.materials || []
  check.value = d.archiveCheck || {}
}
async function doUpload(opt) {
  uploading.value = true
  try {
    const d = await uploadFile(opt.file, 'materials')
    form.filePath = d.url || d.path
    form.fileName = d.name || opt.file.name
    form.fileSize = d.size || opt.file.size
  } finally { uploading.value = false }
}
async function save() {
  if (!form.filePath) return ElMessage.warning('请先选择文件')
  await http.post('/api/activity/material', { activityId: cur.value.id, ...form })
  ElMessage.success('材料已提交')
  await open(cur.value)
  load()
}
onMounted(() => load())
</script>
