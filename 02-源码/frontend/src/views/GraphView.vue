<template>
  <div class="page">
    <div class="page-head">
      <h1>成果关联图谱</h1>
      <span class="sub">创新点：六维权重规则 → 量化计算 → 结果解释 → 人工修正，全过程留痕</span>
      <div class="actions">
        <el-select v-model="achId" filterable placeholder="选择成果" style="width: 260px" @change="loadDetail">
          <el-option v-for="x in achs" :key="x.id" :label="x.title" :value="x.id" />
        </el-select>
        <el-button type="primary" :icon="Refresh" :loading="computing" @click="recompute">执行计算</el-button>
      </div>
    </div>

    <div class="graph-box">
      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>评分明细</h3><span class="hint">按当前生效规则 v{{ rule.version }} 计算</span></div>
        <div class="card-b">
          <div class="flex" style="gap: 14px; margin-bottom: 16px">
            <div class="level-badge">{{ d.score?.resultLevel || '-' }}</div>
            <div>
              <div style="font-size: 12px; color: var(--c-text-2)">综合得分</div>
              <div style="font-size: 26px; font-weight: 600; color: var(--c-primary)" class="num">{{ d.score?.totalScore ?? '-' }}</div>
              <div class="muted small">推荐等级 {{ d.score?.resultLevel || '-' }} · 规则版本 v{{ d.score?.ruleVersion ?? '-' }}</div>
            </div>
          </div>

          <div v-for="it in dims" :key="it.key" class="score-bar">
            <span class="name">{{ it.label }}</span>
            <span class="track"><span class="fill" :style="{ width: pct(it.key) + '%' }"></span></span>
            <span class="val">{{ Number(d.details?.[it.key] || 0).toFixed(1) }}</span>
          </div>

          <p class="section-title" style="margin-top: 18px">结果解释</p>
          <div class="conclusion">{{ d.score?.conclusion || '尚未执行计算' }}</div>

          <p class="section-title" style="margin-top: 16px">计算输入</p>
          <div class="detail-grid">
            <div><div class="lab">来源活动数</div><div class="val">{{ d.inputs?.activityCount ?? '-' }}</div></div>
            <div><div class="lab">是否关联来源活动</div><div class="val">{{ d.inputs?.hasSource ? '是' : '否' }}</div></div>
            <div><div class="lab">课程应用数</div><div class="val">{{ d.inputs?.courseCount ?? '-' }}</div></div>
            <div><div class="lab">成果等级</div><div class="val">{{ d.inputs?.level ?? '-' }}</div></div>
            <div><div class="lab">是否负责人</div><div class="val">{{ d.inputs?.leader ? '是' : '否' }}</div></div>
            <div><div class="lab">时间衰减(月)</div><div class="val">{{ d.inputs?.timeGap ?? '-' }}</div></div>
          </div>
        </div>
      </div>

      <div>
        <div class="card" style="margin-top: 0">
          <div class="card-h"><h3>当前权重规则</h3><span class="hint">合计需为 100%</span></div>
          <div class="card-b">
            <div v-for="r in rules" :key="r.id" style="padding: 7px 0; border-bottom: 1px dashed var(--c-border-l)">
              <div class="flex"><b style="font-size: 13px">{{ r.ruleName }}</b>
                <span class="tag" :class="r.status ? 'tag-green' : 'tag-gray'" style="margin-left: auto">{{ r.status ? '生效中' : '历史版本' }}</span></div>
              <div class="muted small">活动 {{ r.activityWeight }} / 成果 {{ r.achievementWeight }} / 课程 {{ r.courseWeight }} / 负责人 {{ r.leaderWeight }} / 等级 {{ r.levelWeight }} / 时效 {{ r.timeWeight }}</div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-h"><h3>人工修正</h3><span class="hint">需填写理由方可生效</span></div>
          <div class="card-b">
            <el-form label-width="80px">
              <el-form-item label="修正等级">
                <el-select v-model="adj.afterLevel" style="width: 140px">
                  <el-option v-for="l in ['A', 'B', 'C', 'D']" :key="l" :label="l" :value="l" />
                </el-select>
              </el-form-item>
              <el-form-item label="修正理由">
                <el-input v-model="adj.reason" type="textarea" :rows="2" placeholder="例：补充校外应用证明，经教务复核提升一级" />
              </el-form-item>
              <el-form-item><el-button type="primary" :disabled="!d.score" @click="doAdjust">提交修正</el-button></el-form-item>
            </el-form>
            <div class="section-title">修正留痕（{{ d.adjusts?.length || 0 }}）</div>
            <div v-for="x in d.adjusts || []" :key="x.id" class="step">
              <div class="dot"></div>
              <div class="txt">{{ x.beforeLevel }} → <b>{{ x.afterLevel }}</b> · {{ x.reason }}</div>
              <div class="time">{{ formatTime(x.createTime) }}</div>
            </div>
            <div v-if="!(d.adjusts || []).length" class="empty-tip">暂无修正记录</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'

const route = useRoute()
const achs = ref([]); const achId = ref(null)
const d = ref({}); const rules = ref([]); const computing = ref(false)
const adj = reactive({ afterLevel: 'A', reason: '' })
const dims = [
  { key: 'activity', label: '活动参与' }, { key: 'source', label: '来源清晰' },
  { key: 'course', label: '课程应用' }, { key: 'leader', label: '负责人' },
  { key: 'level', label: '成果等级' }, { key: 'time', label: '时效性' }
]
const rule = ref({ version: 3 })

function pct(key) {
  const v = Number(d.value.details?.[key] || 0)
  return Math.min(100, Math.round((v / 30) * 100))
}

async function loadDetail() {
  if (!achId.value) return
  d.value = await http.get(`/api/graph/${achId.value}`)
}
async function recompute() {
  if (!achId.value) return ElMessage.warning('请先选择成果')
  computing.value = true
  try {
    await http.post(`/api/graph/evaluate/${achId.value}`)
    await loadDetail()
    ElMessage.success('计算完成，已生成评分留痕')
  } finally { computing.value = false }
}
async function doAdjust() {
  if (!adj.reason.trim()) return ElMessage.warning('请填写修正理由')
  await http.post('/api/graph/adjust', { scoreId: d.value.score.id, afterLevel: adj.afterLevel, reason: adj.reason })
  ElMessage.success('修正已留痕')
  await loadDetail()
}

onMounted(async () => {
  rules.value = await http.get('/api/graph/rules')
  const eff = rules.value.find((r) => r.status === 1) || rules.value[0]
  if (eff) rule.value = eff
  achs.value = (await http.get('/api/achievement/page', { params: { page: 1, size: 50 } })).list
  achId.value = Number(route.query.ach) || achs.value[0]?.id || null
  await loadDetail()
})
</script>
