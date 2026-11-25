<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <h1>成果详情</h1>
      <span class="sub">{{ a.achNo }} · {{ a.achType }}</span>
      <div class="actions">
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" @click="$router.push('/graph?ach=' + id)">查看关联图谱</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-b" style="display: flex; gap: 18px">
        <div style="flex: 1">
          <div class="flex" style="gap: 10px">
            <h2 style="margin: 0; font-size: 19px">{{ a.title }}</h2>
            <span class="tag" :class="tagClass(a.status)">{{ a.status }}</span>
          </div>
          <div class="detail-grid" style="margin-top: 14px">
            <div><div class="lab">申报人</div><div class="val">{{ a.teacherName }}</div></div>
            <div><div class="lab">成果等级</div><div class="val">{{ a.level }}</div></div>
            <div><div class="lab">来源活动</div><div class="val">{{ a.activityTitle || '未关联' }}</div></div>
            <div><div class="lab">课程应用</div><div class="val">{{ a.courseCount }} 门</div></div>
            <div><div class="lab">图谱评分 / 等级</div><div class="val">{{ a.graphScore ?? '-' }} / {{ a.graphLevel || '-' }}</div></div>
            <div><div class="lab">认定学时</div><div class="val">{{ a.credit ?? 0 }} 学时</div></div>
            <div><div class="lab">提交时间</div><div class="val">{{ formatTime(a.submitTime) }}</div></div>
            <div><div class="lab">专家评分</div><div class="val">{{ a.expertScore ?? '-' }}</div></div>
          </div>
        </div>
        <img :src="a.cover" style="width: 210px; height: 148px; border-radius: 8px; object-fit: cover; flex: none" alt="" />
      </div>
    </div>

    <div class="grid2" style="margin-top: 14px">
      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>评审记录</h3><span class="hint">共 {{ reviews.length }} 轮</span></div>
        <div class="card-b">
          <div v-if="!reviews.length" class="empty-tip">暂无评审记录</div>
          <div v-for="r in reviews" :key="r.id" style="padding: 9px 0; border-bottom: 1px dashed var(--c-border-l)">
            <div class="flex">
              <b>{{ r.stage }}</b>
              <span class="tag" :class="r.result === '通过' ? 'tag-green' : 'tag-red'" style="margin-left: 8px">{{ r.result }}</span>
              <span class="muted small" style="margin-left: 8px">{{ r.reviewerName }}</span>
              <span class="num" style="margin-left: auto; font-weight: 600">{{ r.score ?? '-' }}</span>
            </div>
            <div class="muted small" style="margin-top: 4px">{{ r.opinion }}</div>
            <div class="muted small">{{ formatTime(r.createTime) }}</div>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>流转留痕</h3><span class="hint">状态机全链路</span></div>
        <div class="card-b">
          <div class="timeline-box">
            <div v-for="l in logs" :key="l.id" class="step">
              <div class="dot"></div>
              <div class="txt"><b>{{ l.operatorName }}</b> {{ l.fromStatus }} → {{ l.toStatus }}（{{ l.action }}）
                <span class="muted small" v-if="l.reason"> · {{ l.reason }}</span></div>
              <div class="time">{{ formatTime(l.createTime) }}</div>
            </div>
            <div v-if="!logs.length" class="empty-tip">暂无流转记录</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card" v-if="canAudit">
      <div class="card-h"><h3>审核处理</h3><span class="hint">当前环节：{{ a.status }}</span></div>
      <div class="card-b">
        <el-form :inline="true" label-width="80px">
          <el-form-item label="审核结论">
            <el-radio-group v-model="audit.pass">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">退回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="评分"><el-input-number v-model="audit.score" :min="0" :max="100" /></el-form-item>
          <el-form-item label="意见" style="width: 420px">
            <el-input v-model="audit.opinion" placeholder="退回时必须写明原因" />
          </el-form-item>
          <el-form-item><el-button type="primary" @click="doAudit">提交审核</el-button></el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { tagClass } from '../utils/status'
import { formatTime } from '../utils/format'

const route = useRoute()
const id = route.params.id
const loading = ref(false)
const a = ref({}); const reviews = ref([]); const logs = ref([])
const audit = reactive({ pass: true, score: 80, opinion: '' })
const canAudit = computed(() => ['组长初审', '教务复审', '专家评审'].includes(a.value.status))

async function load() {
  loading.value = true
  try {
    const d = await http.get(`/api/achievement/${id}`)
    a.value = d.achievement
    reviews.value = d.reviews || []
    logs.value = d.logs || []
  } finally { loading.value = false }
}
async function doAudit() {
  if (!audit.pass && !audit.opinion.trim()) return ElMessage.warning('退回必须填写原因')
  await http.post(`/api/achievement/${id}/audit`, { ...audit })
  ElMessage.success('审核已提交')
  load()
}
onMounted(load)
</script>
