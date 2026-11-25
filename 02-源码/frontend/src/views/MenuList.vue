<template>
  <div class="page">
    <div class="page-head">
      <h1>菜单管理</h1>
      <span class="sub">目录、菜单、按钮三级结构，菜单按角色动态下发</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">新增菜单</el-button></div>
    </div>

    <div class="card">
      <div class="card-h"><h3>菜单树</h3><span class="hint">M 目录 / C 菜单 / B 按钮</span></div>
      <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="名称" min-width="220" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <span class="tag" :class="{ M: 'tag-blue', C: 'tag-green', B: 'tag-gray' }[row.type]">{{ { M: '目录', C: '菜单', B: '按钮' }[row.type] }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" min-width="170" />
        <el-table-column prop="perm" label="权限标识" min-width="180" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">{{ row.visible ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑菜单' : '新增菜单'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="顶级（目录）" :value="0" />
            <el-option v-for="n in flatDirs" :key="n.id" :label="n.name" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="B">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由"><el-input v-model="form.path" placeholder="如 /activities" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perm" placeholder="如 activity:list" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="999" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const tree = ref([]); const dlg = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', type: 'C', path: '', perm: '', sort: 10 })
const flatDirs = computed(() => (tree.value || []).filter((n) => n.type === 'M' || n.type === 'C'))

async function load() { tree.value = await http.get('/api/system/menu/tree') }
function openCreate() { Object.assign(form, { id: null, parentId: 0, name: '', type: 'C', path: '', perm: '', sort: 10 }); dlg.value = true }
function openEdit(row) { Object.assign(form, row); dlg.value = true }
async function save() {
  if (!form.name) return ElMessage.warning('请填写菜单名称')
  await http.post('/api/system/menu', { ...form })
  ElMessage.success('已保存')
  dlg.value = false
  load()
}
onMounted(load)
</script>
