<template>
  <div class="page">
    <div class="page-head">
      <h1>角色权限</h1>
      <span class="sub">按角色分配菜单与按钮权限，登录后动态下发</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>角色列表</h3><span class="hint">共 {{ rows.length }} 个角色</span></div>
      <el-table :data="rows">
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleCode" label="角色编码" width="130" />
        <el-table-column prop="dataScope" label="数据范围" width="130" />
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="86">
          <template #default="{ row }"><span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '启用' : '停用' }}</span></template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssign(row)">分配菜单</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名称" required><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码" required><el-input v-model="form.roleCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option v-for="s in ['ALL', 'ORG_TREE', 'ORG', 'DEPT', 'SELF']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignDlg" :title="`为「${cur?.roleName}」分配菜单`" width="560px">
      <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="id" :props="{ label: 'name', children: 'children' }"
        :default-checked-keys="checked" check-strictly style="max-height: 420px; overflow: auto" />
      <template #footer><el-button @click="assignDlg = false">取消</el-button><el-button type="primary" @click="saveAssign">保存权限</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'

const rows = ref([]); const dlg = ref(false); const assignDlg = ref(false)
const menuTree = ref([]); const treeRef = ref(); const cur = ref(null); const checked = ref([])
const form = reactive({ id: null, roleName: '', roleCode: '', dataScope: 'SELF', remark: '' })

/** 递归剔除「菜单管理」节点：该页面仅超级管理员可见，不参与分配 */
function stripMenuNode(nodes) {
  return (nodes || [])
    .filter((n) => !(n.name === '菜单管理' || n.path === '/system/menus'))
    .map((n) => ({ ...n, children: n.children ? stripMenuNode(n.children) : undefined }))
}

async function load() { rows.value = await http.get('/api/system/role/list') }
function openCreate() { Object.assign(form, { id: null, roleName: '', roleCode: '', dataScope: 'SELF', remark: '' }); dlg.value = true }
function openEdit(row) { Object.assign(form, row); dlg.value = true }
async function save() {
  if (!form.roleName || !form.roleCode) return ElMessage.warning('请填写角色名称与编码')
  await http.post('/api/system/role', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}
async function openAssign(row) {
  cur.value = row
  assignDlg.value = true
  if (!menuTree.value.length) menuTree.value = stripMenuNode(await http.get('/api/system/menu/tree'))
  checked.value = await http.get(`/api/system/role/${row.id}/menus`)
  await nextTick()
  treeRef.value?.setCheckedKeys(checked.value.filter((id) => findNode(menuTree.value, id)))
}
function findNode(nodes, id) {
  for (const n of nodes) {
    if (n.id === id) return true
    if (n.children && findNode(n.children, id)) return true
  }
  return false
}
async function saveAssign() {
  const ids = [...treeRef.value.getCheckedKeys(), ...treeRef.value.getHalfCheckedKeys()]
  await http.post(`/api/system/role/${cur.value.id}/menus`, { menuIds: ids })
  ElMessage.success('权限已保存')
  assignDlg.value = false
}
onMounted(load)
</script>
