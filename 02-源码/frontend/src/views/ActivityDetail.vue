<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <h1>活动详情</h1>
      <span class="sub">{{ act.actNo }} · {{ act.actType }}</span>
      <div class="actions">
        <el-button @click="$router.back()">返回</el-button>
        <el-button v-if="next" type="primary" @click="openFlow">流转至「{{ next }}」</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-b" style="display: flex; gap: 18px">
        <div style="flex: 1">
          <div class="flex" style="gap: 10px">
            <h2 style="margin: 0; font-size: 19px">{{ act.title }}</h2>
            <span class="tag" :class="tagClass(act.status)">{{ act.status }}</span>
          </div>
          <div class="detail-grid" style="margin-top: 14px">
            <div><div class="lab">教研组</div><div class="val">{{ act.groupName }}</div></div>
            <div><div class="lab">学科 / 年级</div><div class="val">{{ act.subject }} {{ act.grade }}</div></div>
            <div><div class="lab">主持 / 主讲 / 记录</div><div class="val">{{ act.hostName }} / {{ act.speakerName }} / {{ act.recorderName }}</div></div>
            <div><div class="lab">活动时间</div><div class="val">{{ formatTime(act.activityTime) }} · {{ act.duration }} 分钟</div></div>
            <div><div class="lab">地点</div><div class="val">{{ act.location }}</div></div>
            <div><div class="lab">签到 / 分工人数</div><div class="val">{{ act.signCount }} / {{ act.memberCount }}</div></div>
          </div>
        </div>
        <img :src="act.cover" style="width: 200px; height: 140px; border-radius: 8px; object-fit: cover; flex: none" alt="" />
      </div>
    </div>

    <div class="grid2" style="margin-top: 14px">
      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>分工与签到</h3><span class="hint">签到码 {{ act.signCode }}</span>
          <div class="r"><el-button size="small" :icon="Plus" @click="memberDlg = true">添加分工</el-button></div>
        </div>
        <div class="card-b">
          <el-table :data="members" size="small">
            <el-table-column prop="teacherName" label="教师" width="100" />
            <el-table-column prop="roleType" label="分工" width="90" />
            <el-table-column label="签到" width="100">
              <template #default="{ row }"><span class="tag" :class="row.signStatus ? 'tag-green' : 'tag-gray'">{{ row.signStatus ? '已签到' : '未签到' }}</span></template>
            </el-table-column>
            <el-table-column label="材料" width="100">
              <template #default="{ row }"><span class="tag" :class="row.materialDone ? 'tag-green' : 'tag-orange'">{{ row.materialDone ? '已提交' : '待提交' }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="!row.signStatus" link type="primary" @click="sign(row)">签到</el-button>
                <el-button link type="danger" @click="removeMember(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>归档自检</h3><span class="hint">归档前系统自动校验</span></div>
        <div class="card-b">
          <div class="flex" style="gap: 10px; margin-bottom: 10px">
            <span class="tag" :class="act.archivePass ? 'tag-green' : 'tag-orange'">{{ act.archivePass ? '材料齐全' : '存在缺失' }}</span>
            <span class="muted small">签到 {{ check.signCount || 0 }} 人 / 分工 {{ check.memberCount || 0 }} 人</span>
          </div>
          <div v-if="(check.missing || []).length" class="conclusion" style="border-left-color: #B22B31">
            待补齐材料：{{ check.missing.join('、') }}
          </div>
          <div v-else class="conclusion">材料齐全，可归档并自动生成学时台账。</div>
          <div class="section-title" style="margin-top: 16px">材料清单（{{ materials.length }}）</div>
          <div v-if="!materials.length" class="empty-tip">暂无材料</div>
          <div v-for="m in materials" :key="m.id" class="step">
            <div class="dot"></div>
            <div class="txt"><b>{{ m.matType }}</b> · {{ m.fileName }}</div>
            <div class="time">{{ formatTime(m.createTime) }}</div>
          </div>
          <el-button size="small" style="margin-top: 10px" :icon="Upload" @click="matDlg = true">上传材料</el-button>
        </div>
      </div>
    </div>

    <div class="grid2" style="margin-top: 14px">
      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>听评课记录</h3>
          <div class="r"><el-button size="small" @click="evalDlg = true">提交评课</el-button></div>
        </div>
        <div class="card-b">
          <div v-if="!evaluations.length" class="empty-tip">暂无评课记录</div>
          <div v-for="e in evaluations" :key="e.id" style="padding: 8px 0; border-bottom: 1px dashed var(--c-border-l)">
            <div class="flex"><b>{{ e.evaluatorName }}</b><span class="muted small" style="margin-left: 8px">{{ formatTime(e.createTime) }}</span>
              <span class="tag tag-blue" style="margin-left: auto">总分 {{ e.totalScore }}</span></div>
            <div class="muted small" style="margin-top: 4px">亮点：{{ e.advantage }}</div>
            <div class="muted small">建议：{{ e.suggestion }}</div>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>活动反思与流转留痕</h3>
          <div class="r"><el-button size="small" @click="refDlg = true">提交反思</el-button></div>
        </div>
        <div class="card-b">
          <div v-for="r in reflections" :key="r.id" class="step">
            <div class="dot"></div>
            <div class="txt"><b>{{ r.teacherName }}</b> {{ r.content }}</div>
            <div class="time">{{ formatTime(r.createTime) }}</div>
          </div>
          <div v-if="!reflections.length" class="empty-tip">暂无反思</div>
          <div class="section-title" style="margin-top: 14px">流转留痕</div>
          <div class="timeline-box">
            <div v-for="l in logs" :key="l.id" class="step">
              <div class="dot"></div>
              <div class="txt"><b>{{ l.operatorName }}</b> {{ l.fromStatus }} → {{ l.toStatus }}（{{ l.action }}）<span class="muted small" v-if="l.reason"> · {{ l.reason }}</span></div>
              <div class="time">{{ formatTime(l.createTime) }}</div>
            </div>
            <div v-if="!logs.length" class="empty-tip">暂无流转记录</div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="flowDlg" title="活动状态流转" width="500px">
      <el-form label-width="90px">
        <el-form-item label="当前"><span class="tag" :class="tagClass(act.status)">{{ act.status }}</span></el-form-item>
        <el-form-item label="目标"><span class="tag tag-blue">{{ next }}</span></el-form-item>
        <el-form-item label="流转说明"><el-input v-model="flowReason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="flowDlg = false">取消</el-button><el-button type="primary" @click="doFlow">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="memberDlg" title="添加分工" width="460px">
      <el-form label-width="80px">
        <el-form-item label="教师 ID"><el-input-number v-model="mForm.teacherId" :min="1" /></el-form-item>
        <el-form-item label="分工">
          <el-select v-model="mForm.roleType" style="width: 100%">
            <el-option v-for="r in roleTypes" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="memberDlg = false">取消</el-button><el-button type="primary" @click="addMember">添加</el-button></template>
    </el-dialog>

    <el-dialog v-model="matDlg" title="上传活动材料" width="520px">
      <el-form label-width="90px">
        <el-form-item label="材料类型">
          <el-select v-model="matForm.matType" style="width: 100%">
            <el-option v-for="t in ['教案', '课件', '照片', '记录', '评课表', '反思']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload :show-file-list="false" :http-request="doUpload" :disabled="uploading">
            <el-button :loading="uploading" :icon="Upload">选择文件</el-button>
            <span class="muted small" style="margin-left: 8px">{{ matForm.fileName || '未选择' }}</span>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="matDlg = false">取消</el-button><el-button type="primary" @click="saveMat">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="evalDlg" title="提交听评课记录" width="560px">
      <el-form label-width="90px">
        <el-form-item label="评课维度">
          <el-input v-model="evForm.advantage" type="textarea" :rows="2" placeholder="课堂亮点" />
        </el-form-item>
        <el-form-item label="改进建议">
          <el-input v-model="evForm.suggestion" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="各项得分">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; width: 100%">
            <div v-for="(label, key) in EVAL_KEYS" :key="key" class="flex">
              <span class="muted small" style="width: 72px">{{ label }}</span>
              <el-input-number v-model="evForm[key]" :min="0" :max="25" size="small" controls-position="right" style="width: 100px" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="evalDlg = false">取消</el-button><el-button type="primary" @click="saveEval">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="refDlg" title="提交活动反思" width="520px">
      <el-form label-width="80px">
        <el-form-item label="反思内容"><el-input v-model="rfForm.content" type="textarea" :rows="4" placeholder="本次教研的收获与后续改进" /></el-form-item>
        <el-form-item label="改进点"><el-input v-model="rfForm.improvePoints" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="refDlg = false">取消</el-button><el-button type="primary" @click="saveRef">提交</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http, { uploadFile } from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const route = useRoute()
const id = route.params.id
const loading = ref(false)
const act = ref({})
const members = ref([]); const materials = ref([]); const evaluations = ref([]); const reflections = ref([]); const logs = ref([])
const check = ref({})
const roleTypes = ref(['主持', '主讲', '记录', '评课人', '参与者'])
const flowDlg = ref(false); const memberDlg = ref(false); const matDlg = ref(false); const evalDlg = ref(false); const refDlg = ref(false)
const flowReason = ref(''); const uploading = ref(false)
const mForm = reactive({ teacherId: 3, roleType: '参与者' })
const matForm = reactive({ matType: '教案', filePath: '', fileName: '', fileSize: 0 })
const EVAL_KEYS = { design: '教学设计', interaction: '课堂互动', effect: '目标达成', literacy: '素养落实' }
const evForm = reactive({ design: 20, interaction: 20, effect: 20, literacy: 20, advantage: '', suggestion: '' })
const rfForm = reactive({ content: '', improvePoints: '' })

const FLOW = ['草稿', '已发布', '报名中', '进行中', '待归档', '已归档']
const next = computed(() => {
  const i = FLOW.indexOf(act.value.status)
  return i >= 0 && i < FLOW.length - 1 ? FLOW[i + 1] : ''
})

async function load() {
  loading.value = true
  try {
    const d = await http.get(`/api/activity/${id}`)
    act.value = d.activity
    members.value = d.members || []
    materials.value = d.materials || []
    evaluations.value = d.evaluations || []
    reflections.value = d.reflections || []
    logs.value = d.logs || []
    check.value = d.archiveCheck || {}
  } finally { loading.value = false }
}
function openFlow() { flowReason.value = ''; flowDlg.value = true }
async function doFlow() {
  await http.post(`/api/activity/${id}/transition`, { to: next.value, reason: flowReason.value })
  ElMessage.success('状态已流转')
  flowDlg.value = false
  load()
}
async function sign(row) {
  await http.post(`/api/activity/${id}/sign`, { code: act.value.signCode, teacherId: row.teacherId })
  ElMessage.success(`${row.teacherName} 签到成功`)
  load()
}
async function addMember() {
  await http.post(`/api/activity/${id}/member`, { ...mForm })
  ElMessage.success('已添加分工')
  memberDlg.value = false
  load()
}
async function removeMember(row) {
  await http.delete(`/api/activity/member/${row.id}`)
  ElMessage.success('已移除')
  load()
}
async function doUpload(opt) {
  uploading.value = true
  try {
    const d = await uploadFile(opt.file, 'materials')
    matForm.filePath = d.url || d.path
    matForm.fileName = d.name || opt.file.name
    matForm.fileSize = d.size || opt.file.size
  } finally { uploading.value = false }
}
async function saveMat() {
  if (!matForm.filePath) return ElMessage.warning('请先选择文件')
  await http.post('/api/activity/material', { activityId: Number(id), ...matForm })
  ElMessage.success('材料已上传')
  matDlg.value = false
  load()
}
async function saveEval() {
  await http.post('/api/activity/evaluation', { activityId: Number(id), evaluatorId: 1, ...evForm })
  ElMessage.success('评课已提交')
  evalDlg.value = false
  load()
}
async function saveRef() {
  if (!rfForm.content) return ElMessage.warning('请填写反思内容')
  await http.post('/api/activity/reflection', { activityId: Number(id), ...rfForm })
  ElMessage.success('反思已提交')
  refDlg.value = false
  load()
}

onMounted(load)
</script>
