<template>
  <div class="page">
    <div class="page-head">
      <h1>教研组</h1>
      <span class="sub">教研组是计划编制、活动开展与学时考核的组织单元</span>
    </div>

    <div class="grid3">
      <div class="card" v-for="g in rows" :key="g.id" style="margin-top: 0">
        <div class="card-h"><h3>{{ g.groupName }}</h3>
          <span class="tag tag-blue" style="margin-left: 8px">{{ g.subject }}</span>
        </div>
        <div class="card-b">
          <div class="detail-grid" style="grid-template-columns: 1fr">
            <div><div class="lab">组长</div><div class="val">{{ g.leaderName }}</div></div>
            <div><div class="lab">覆盖年级</div><div class="val">{{ g.grade }}</div></div>
            <div><div class="lab">成员数 / 目标学时</div><div class="val">{{ g.memberCount }} 人 / {{ g.targetCredit }} 学时</div></div>
          </div>
          <div style="margin-top: 14px">
            <div class="flex small" style="justify-content: space-between; margin-bottom: 6px">
              <span class="muted">活动归档进度</span>
              <span class="num">{{ g.activityArchived }} / {{ g.activityTotal }}</span>
            </div>
            <el-progress :percentage="g.activityTotal ? Math.round((g.activityArchived / g.activityTotal) * 100) : 0" :stroke-width="8" :show-text="false" color="#2878B5" />
            <div class="flex small" style="justify-content: space-between; margin: 12px 0 6px">
              <span class="muted">累计学时</span>
              <span class="num">{{ g.creditSum }} / {{ g.targetCredit }}</span>
            </div>
            <el-progress :percentage="Math.min(100, Math.round((g.creditSum / g.targetCredit) * 100))" :stroke-width="8" :show-text="false" color="#1F7653" />
          </div>
          <div class="small muted" style="margin-top: 14px">成员：{{ (g.members || []).map((m) => m.name).join('、') }}</div>
          <div class="small muted" style="margin-top: 4px">备注：{{ g.remark || '-' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'

const rows = ref([])
onMounted(async () => { rows.value = await http.get('/api/group/list') })
</script>
