<template>
  <div class="page">
    <div class="page-head"><h1>个人中心</h1><span class="sub">维护个人资料、头像与登录密码</span></div>

    <div class="grid2">
      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>基本信息</h3></div>
        <div class="card-b">
          <div class="flex" style="gap: 16px; margin-bottom: 18px">
            <el-upload :show-file-list="false" :http-request="uploadAvatar">
              <el-avatar :size="64" :src="form.avatar">{{ (form.realName || '师').slice(0, 1) }}</el-avatar>
            </el-upload>
            <div>
              <div style="font-size: 15px; font-weight: 600">{{ form.realName }}</div>
              <div class="muted small">{{ form.roleName }} · {{ form.orgName }}</div>
              <el-button link type="primary" style="padding: 0; margin-top: 6px">点击头像更换（jpg/png，≤2MB）</el-button>
            </div>
          </div>
          <el-form :model="form" label-width="80px">
            <el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item>
            <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
            <el-form-item><el-button type="primary" @click="saveProfile">保存资料</el-button></el-form-item>
          </el-form>
        </div>
      </div>

      <div class="card" style="margin-top: 0">
        <div class="card-h"><h3>修改密码</h3><span class="hint">建议 8 位以上，含字母与数字</span></div>
        <div class="card-b">
          <el-form :model="pwd" label-width="90px">
            <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
            <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
            <el-form-item><el-button type="primary" @click="savePwd">修改密码</el-button></el-form-item>
          </el-form>
          <div class="conclusion" style="margin-top: 8px">
            账号信息由教务处统一维护，如需变更所属教研组请联系管理员。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http, { uploadFile } from '../api/http'
import { useUserStore } from '../store/user'

const store = useUserStore()
const form = reactive({ username: '', realName: '', phone: '', email: '', avatar: '', roleName: '', orgName: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function load() {
  const d = await http.get('/api/auth/profile')
  Object.assign(form, d)
}
async function uploadAvatar(opt) {
  const d = await uploadFile(opt.file, 'avatar')
  form.avatar = d.url || d.path
  await http.put('/api/auth/profile', { ...form })
  ElMessage.success('头像已更新')
  const p = await http.get('/api/auth/profile')
  store.setUser({ ...store.state.user, ...p })
  load()
}
async function saveProfile() {
  await http.put('/api/auth/profile', { ...form })
  ElMessage.success('资料已保存')
  const p = await http.get('/api/auth/profile')
  store.setUser({ ...store.state.user, ...p })
}
async function savePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await http.put('/api/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改')
  Object.assign(pwd, { oldPassword: '', newPassword: '', confirm: '' })
}
onMounted(load)
</script>
