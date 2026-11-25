<template>
  <div class="page">
    <div class="page-head">
      <h1>成果申报</h1>
      <span class="sub">课例、论文、资源包、课题成果按「提交 → 组长初审 → 教务复审 → 专家评审 → 认定」流转</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">申报成果</el-button></div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="成果名称 / 编号" clearable />
      <el-select v-model="q.achType" placeholder="成果类型" clearable>
        <el-option v-for="t in ['课例', '论文', '资源包', '课题']" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="q.status" placeholder="状态" clearable>
        <el-option v-for="s in ['草稿', '组长初审', '教务复审', '专家评审', '已认定', '已退回']" :key="s" :label="s" :value="s" />
      </el-select>
      <div class="tail">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>成果列表</h3><span class="hint">共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="封面" width="86">
          <template #default="{ row }"><img class="cover" :src="row.cover" alt="" /></template>
        </el-table-column>
        <el-table-column label="成果" min-width="240">
          <template #default="{ row }">
            <div style="font-weight: 600">{{ row.title }}</div>
            <div class="muted small">{{ row.achNo }} · {{ row.achType }} · {{ row.level }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="申报人" width="90" />
        <el-table-column label="图谱评分" width="120">
          <template #default="{ row }">
            <span class="num">{{ row.graphScore ?? '-' }}</span>
            <span class="tag" :class="levelTag(row.graphLevel)" style="margin-left: 6px">{{ row.graphLevel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="课程应用" width="90" align="center">
          <template #default="{ row }">{{ row.courseCount }} 门</template>
        </el-table-column>
        <el-table-column label="学时" width="80" align="right">
          <template #default="{ row }"><span class="num">{{ row.credit ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }"><span class="tag" :class="tagClass(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/achievements/' + row.id)">详情</el-button>
            <el-button v-if="row.status === '草稿'" link type="primary" @click="submit(row)">提交</el-button>
            <el-button v-if="canAudit(row.status)" link type="primary" @click="$router.push('/achievements/' + row.id)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
          v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="申报教研成果" width="620px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="成果名称" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="成果类型">
          <el-select v-model="form.achType" style="width: 47%; margin-right: 6%">
            <el-option v-for="t in ['课例', '论文', '资源包', '课题']" :key="t" :label="t" :value="t" />
          </el-select>
          <el-select v-model="form.level" style="width: 47%">
            <el-option v-for="l in ['校级', '区级', '市级', '省级']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源活动">
          <el-select v-model="form.activityId" filterable clearable style="width: 100%" placeholder="选择关联的教研活动">
            <el-option v-for="a in acts" :key="a.id" :label="a.title" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程应用数"><el-input-number v-model="form.courseCount" :min="0" :max="30" /></el-form-item>
        <el-form-item label="封面">
          <el-upload :show-file-list="false" :http-request="uploadCover">
            <img v-if="form.cover" :src="form.cover" style="width: 120px; height: 86px; object-fit: cover; border-radius: 6px" alt="" />
            <div v-else class="upload-box">点击上传</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http, { uploadFile } from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const acts = ref([])
const q = reactive({ page: 1, size: 10, keyword: '', achType: '', status: '' })
const dlg = ref(false)
const form = reactive({ title: '', achType: '课例', level: '校级', activityId: null, courseCount: 1, cover: '' })

function levelTag(l) {
  return { A: 'tag-green', B: 'tag-blue', C: 'tag-orange', D: 'tag-gray' }[l] || 'tag-gray'
}
function canAudit(s) { return ['组长初审', '教务复审', '专家评审'].includes(s) }

async function load(page) {
  if (page) q.page = page
  loading.value = true
  try {
    const d = await http.get('/api/achievement/page', { params: { ...q, keyword: q.keyword || undefined, achType: q.achType || undefined, status: q.status || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function reset() { Object.assign(q, { page: 1, keyword: '', achType: '', status: '' }); load(1) }
function openCreate() { Object.assign(form, { title: '', achType: '课例', level: '校级', activityId: null, courseCount: 1, cover: '' }); dlg.value = true }
async function uploadCover(opt) {
  const d = await uploadFile(opt.file, 'achievement')
  form.cover = d.url || d.path
}
async function save() {
  if (!form.title) return ElMessage.warning('请填写成果名称')
  await http.post('/api/achievement', { ...form })
  ElMessage.success('成果已保存为草稿')
  dlg.value = false
  load(1)
}
async function submit(row) {
  await http.post(`/api/achievement/${row.id}/submit`)
  ElMessage.success('已提交初审')
  load()
}
onMounted(async () => {
  load()
  try { acts.value = (await http.get('/api/activity/page', { params: { page: 1, size: 50 } })).list } catch (e) { acts.value = [] }
})
</script>

<style scoped>
.upload-box { width: 120px; height: 86px; border: 1px dashed var(--c-border); border-radius: 6px; display: grid; place-items: center; color: var(--c-text-2); font-size: 12px; cursor: pointer; }
</style>
