<template>
  <div class="page dash">
    <div class="page-head">
      <h1>教研看板</h1>
      <span class="sub">本学期教研活动、成果与学时整体运行情况</span>
      <div class="actions">
        <span class="muted small">累计学时 {{ data.creditTotal ?? 0 }} 学时</span>
        <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi" v-for="k in data.kpis" :key="k.label">
        <div class="k">{{ k.label }}</div>
        <div class="v">{{ k.value }}</div>
        <div class="d">{{ k.desc }}</div>
      </div>
    </div>

    <div class="grid3 charts" style="margin-top: 12px">
      <div class="card">
        <div class="card-h"><h3>活动开展趋势</h3><span class="hint">按月</span></div>
        <div class="card-b"><div ref="c1" class="chart"></div></div>
      </div>
      <div class="card">
        <div class="card-h"><h3>活动状态分布</h3><span class="hint">状态机实时统计</span></div>
        <div class="card-b"><div ref="c2" class="chart"></div></div>
      </div>
      <div class="card">
        <div class="card-h"><h3>活动类型构成</h3><span class="hint">5 类教研形态</span></div>
        <div class="card-b"><div ref="c3" class="chart"></div></div>
      </div>
    </div>

    <div class="grid3 charts" style="margin-top: 12px">
      <div class="card">
        <div class="card-h"><h3>教研组归档率</h3><span class="hint">已归档 / 活动数</span></div>
        <div class="card-b"><div ref="c4" class="chart"></div></div>
      </div>
      <div class="card">
        <div class="card-h"><h3>图谱评分区间</h3><span class="hint">关联图谱量化结果</span></div>
        <div class="card-b"><div ref="c5" class="chart"></div></div>
      </div>
      <div class="card">
        <div class="card-h"><h3>成果等级分布</h3><span class="hint">A/B/C/D 推荐等级</span></div>
        <div class="card-b"><div ref="c6" class="chart"></div></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import http from '../api/http'

const data = ref({ kpis: [], trend: [], statusDist: [], typeDist: [], groupRank: [], scoreDist: [], levelDist: [] })
const c1 = ref(); const c2 = ref(); const c3 = ref(); const c4 = ref(); const c5 = ref(); const c6 = ref()
const charts = []
const AXIS = { axisLine: { lineStyle: { color: '#DCE6EE' } }, axisLabel: { color: '#5F7484', fontSize: 11 }, splitLine: { lineStyle: { color: '#EEF4F9' } } }

function mk(el, option) {
  if (!el) return
  const ins = echarts.init(el)
  ins.setOption(option)
  charts.push(ins)
}

async function load() {
  data.value = await http.get('/api/dashboard')
  charts.splice(0).forEach((c) => c.dispose())
  await nextTick()
  const d = data.value
  mk(c1.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 34, right: 12, top: 18, bottom: 24 },
    xAxis: { type: 'category', data: d.trend.map((x) => x.name), ...AXIS },
    yAxis: { type: 'value', ...AXIS },
    series: [{ type: 'line', smooth: true, symbolSize: 6, data: d.trend.map((x) => x.value), areaStyle: { color: 'rgba(40,120,181,.16)' }, lineStyle: { width: 2, color: '#2878B5' }, itemStyle: { color: '#2878B5' } }]
  })
  mk(c2.value, {
    tooltip: { trigger: 'item' },
    legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 10.5, color: '#5F7484' }, itemWidth: 8, itemHeight: 8 },
    series: [{ type: 'pie', radius: ['38%', '64%'], center: ['50%', '44%'], avoidLabelOverlap: true,
      label: { fontSize: 10, color: '#5F7484', formatter: '{d}%' },
      data: d.statusDist.map((x) => ({ name: x.name, value: x.value })),
      color: ['#2878B5', '#8A5A12', '#9AA9B5', '#1F7653', '#0F3B5D', '#B22B31'] }]
  })
  mk(c3.value, {
    tooltip: { trigger: 'axis' }, grid: { left: 34, right: 12, top: 18, bottom: 34 },
    xAxis: { type: 'category', data: d.typeDist.map((x) => x.name), ...AXIS, axisLabel: { ...AXIS.axisLabel, interval: 0, rotate: 0 } },
    yAxis: { type: 'value', ...AXIS },
    series: [{ type: 'bar', barWidth: 20, itemStyle: { color: '#2878B5', borderRadius: [4, 4, 0, 0] }, data: d.typeDist.map((x) => x.value) }]
  })
  mk(c4.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } }, grid: { left: 76, right: 24, top: 12, bottom: 20 },
    xAxis: { type: 'value', max: 100, ...AXIS, axisLabel: { ...AXIS.axisLabel, formatter: '{value}%' } },
    yAxis: { type: 'category', data: d.groupRank.map((x) => x.name).reverse(), ...AXIS },
    series: [{ type: 'bar', barWidth: 12, itemStyle: { color: '#0F3B5D', borderRadius: [0, 3, 3, 0] }, data: d.groupRank.map((x) => x.value).reverse(),
      label: { show: true, position: 'right', formatter: '{c}%', fontSize: 10.5, color: '#5F7484' } }]
  })
  mk(c5.value, {
    tooltip: { trigger: 'axis' }, grid: { left: 30, right: 12, top: 18, bottom: 24 },
    xAxis: { type: 'category', data: d.scoreDist.map((x) => x.name), ...AXIS },
    yAxis: { type: 'value', ...AXIS },
    series: [{ type: 'bar', barWidth: 26, itemStyle: { color: '#1F7653', borderRadius: [4, 4, 0, 0] }, data: d.scoreDist.map((x) => x.value) }]
  })
  mk(c6.value, {
    tooltip: { trigger: 'item' }, legend: { bottom: 0, textStyle: { fontSize: 10.5, color: '#5F7484' }, itemWidth: 8, itemHeight: 8 },
    series: [{ type: 'pie', radius: '62%', center: ['50%', '44%'],
      label: { fontSize: 10, color: '#5F7484', formatter: '{b} {c}' },
      data: d.levelDist.map((x) => ({ name: x.name, value: x.value })),
      color: ['#0F3B5D', '#2878B5', '#5B96C4', '#B7CCDC'] }]
  })
}

function resize() { charts.forEach((c) => c.resize()) }
onMounted(() => { load(); window.addEventListener('resize', resize) })
onUnmounted(() => { window.removeEventListener('resize', resize); charts.forEach((c) => c.dispose()) })
</script>

<style scoped>
.dash .card-b { padding: 8px 10px 4px; }
.dash .kpi .d { margin-top: 2px; }
</style>
