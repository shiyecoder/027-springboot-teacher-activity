<template>
  <div class="page">
    <div class="page-head">
      <h1>用户管理</h1>
      <span class="sub">账号、角色与所属机构维护</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button></div>
    </div>

    <div class="filters">
      <el-input v-model="keyword" placeholder="用户名 / 姓名" clearable />
      <div class="tail"><el-button @click="load(1)">查询</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>用户列表</h3><span class="hint">共 {{ total }} 人</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="flex" style="gap: 10px">
              <el-avatar :size="30" :src="row.avatar">{{ row.realName?.slice(0, 1) }}</el-avatar>
              <div>
                <div style="font-weight: 600">{{ row.realName }}</div>
                <div class="muted small">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="120" />
        <el-table-column prop="orgName" label="所属机构" min-width="140" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="86">
          <template #default="{ row }"><span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '启用' : '停用' }}</span></template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="150">
          <template #default="{ row }">{{ formatTime(row.lastLogin) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="page" :page-size="10" @current-change="load()" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属机构">
          <el-select v-model="form.orgId" style="width: 100%">
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const loading = ref(false); const page = ref(1); const keyword = ref('')
const roles = ref([]); const orgs = ref([]); const dlg = ref(false)
const form = reactive({ id: null, username: '', realName: '', roleId: null, orgId: null, phone: '', email: '', status: 1 })

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const d = await http.get('/api/system/user/page', { params: { page: page.value, size: 10, keyword: keyword.value || undefined } })
    rows.value = d.list
    total.value = d.total
  } finally { loading.value = false }
}
function openCreate() { Object.assign(form, { id: null, username: '', realName: '', roleId: roles.value[3]?.id, orgId: orgs.value[0]?.id, phone: '', email: '', status: 1 }); dlg.value = true }
function openEdit(row) { Object.assign(form, row); dlg.value = true }
async function save() {
  if (!form.username || !form.realName) return ElMessage.warning('请填写用户名与姓名')
  if (form.id) await http.put(`/api/system/user/${form.id}`, { ...form })
  else await http.post('/api/system/user', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}
async function resetPwd(row) {
  try { await ElMessageBox.confirm(`重置 ${row.realName} 的密码为 123456？`, '重置密码', { type: 'warning' }) } catch (e) { return }
  await http.post(`/api/system/user/${row.id}/reset-password`)
  ElMessage.success('密码已重置为 123456')
}
onMounted(async () => {
  load()
  roles.value = await http.get('/api/system/role/list')
  orgs.value = await http.get('/api/system/org/list')
})
</script>
