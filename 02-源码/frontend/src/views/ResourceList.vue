<template>
  <div class="page">
    <div class="page-head">
      <h1>教研资源库</h1>
      <span class="sub">教案、课件、试题、反思四类资源统一入库，支持版本迭代与审核</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">上传资源</el-button></div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="资源名称 / 编号" clearable />
      <el-select v-model="q.resType" placeholder="资源类型" clearable>
        <el-option v-for="t in ['教案', '课件', '试题', '反思']" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="q.auditStatus" placeholder="审核状态" clearable>
        <el-option v-for="s in ['待审核', '已审核', '已退回']" :key="s" :label="s" :value="s" />
      </el-select>
      <div class="tail">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>资源列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="封面" width="86">
          <template #default="{ row }"><img class="cover" :src="row.cover" alt="" /></template>
        </el-table-column>
        <el-table-column label="资源" min-width="250">
          <template #default="{ row }">
            <div style="font-weight: 600">{{ row.title }}</div>
            <div class="muted small">{{ row.resNo }} · {{ row.subject }} {{ row.grade }} · {{ row.topic }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="resType" label="类型" width="80" />
        <el-table-column prop="ownerName" label="上传人" width="90" />
        <el-table-column label="下载量" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.downloadCount }}</span></template>
        </el-table-column>
        <el-table-column label="审核" width="92">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.auditStatus)">{{ row.auditStatus }}</span></template>
        </el-table-column>
        <el-table-column label="入库时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openVersions(row)">版本</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
          v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑资源' : '上传资源'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="资源名称" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型 / 学科">
          <el-select v-model="form.resType" style="width: 47%; margin-right: 6%">
            <el-option v-for="t in ['教案', '课件', '试题', '反思']" :key="t" :label="t" :value="t" />
          </el-select>
          <el-input v-model="form.subject" placeholder="学科" style="width: 47%" />
        </el-form-item>
        <el-form-item label="年级 / 主题">
          <el-input v-model="form.grade" placeholder="年级" style="width: 47%; margin-right: 6%" />
          <el-input v-model="form.topic" placeholder="主题" style="width: 47%" />
        </el-form-item>
        <el-form-item label="封面">
          <el-upload :show-file-list="false" :http-request="uploadCover">
            <img v-if="form.cover" :src="form.cover" style="width: 120px; height: 86px; object-fit: cover; border-radius: 6px; cursor: pointer" alt="" />
            <div v-else class="upload-box">点击上传封面</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="verDlg" title="资源版本记录" size="520px">
      <div v-if="cur" class="flex" style="gap: 10px; margin-bottom: 12px"><b>{{ cur.title }}</b><span class="muted small">{{ cur.resNo }}</span></div>
      <div v-for="v in versions" :key="v.id" class="step">
        <div class="dot"></div>
        <div class="txt"><b>v{{ v.version }}</b> {{ v.changeNote || '常规更新' }}</div>
        <div class="time">{{ formatTime(v.createTime) }}</div>
      </div>
      <el-empty v-if="!versions.length" description="暂无版本记录" :image-size="60" />
      <div style="margin-top: 16px">
        <el-input v-model="verNote" placeholder="本次修改说明" style="margin-bottom: 10px" />
        <el-button type="primary" @click="addVersion">新增版本</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http, { uploadFile } from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', resType: '', auditStatus: '' })
const dlg = ref(false); const verDlg = ref(false); const cur = ref(null); const versions = ref([]); const verNote = ref('')
const form = reactive({ id: null, title: '', resType: '教案', subject: '', grade: '', topic: '', cover: '' })

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/resource/page', { params: { ...q, keyword: q.keyword || undefined, resType: q.resType || undefined, auditStatus: q.auditStatus || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, keyword: '', resType: '', auditStatus: '' }); load(1) }
function openCreate() { Object.assign(form, { id: null, title: '', resType: '教案', subject: '', grade: '', topic: '', cover: '' }); dlg.value = true }
function openEdit(row) { Object.assign(form, row); dlg.value = true }
async function uploadCover(opt) {
  const d = await uploadFile(opt.file, 'resource')
  form.cover = d.url || d.path
}
async function save() {
  if (!form.title) return ElMessage.warning('请填写资源名称')
  if (form.id) await http.put(`/api/resource/${form.id}`, { ...form })
  else await http.post('/api/resource', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}
async function openVersions(row) {
  cur.value = row
  verDlg.value = true
  verNote.value = ''
  try { versions.value = await http.get(`/api/resource/${row.id}/versions`) } catch (e) { versions.value = [] }
}
async function addVersion() {
  await http.post(`/api/resource/${cur.value.id}/version`, { changeNote: verNote.value })
  ElMessage.success('版本已新增')
  versions.value = await http.get(`/api/resource/${cur.value.id}/versions`)
}
onMounted(() => load())
</script>

<style scoped>
.upload-box { width: 120px; height: 86px; border: 1px dashed var(--c-border); border-radius: 6px; display: grid; place-items: center; color: var(--c-text-2); font-size: 12px; cursor: pointer; }
</style>
