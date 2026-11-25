<template>
  <div class="login-wrap">
    <div class="bg-layer"></div>
    <div class="bg-photo"></div>
    <div class="login-card">
      <div class="left">
        <div class="mark">研</div>
        <h1>教师教研活动<br />与成果管理系统</h1>
        <p class="slogan">活动留痕 · 成果可溯 · 学时可信</p>
        <ul class="feats">
          <li><i></i>活动全流程状态机，归档前自动校验材料完整性</li>
          <li><i></i>成果关联图谱量化评分，逐项留痕可人工修正</li>
          <li><i></i>学时自动核算，规则版本与计算依据全程可查</li>
        </ul>
        <div class="foot">教师发展中心 · 教研数字化平台</div>
      </div>
      <div class="right">
        <h2>账号登录</h2>
        <p class="tip">请使用学校统一分配的教研账号登录</p>
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <div class="row">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <span class="muted small">忘记密码请联系教务处</span>
          </div>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="demo">
          <div class="dt">演示账号（密码均为 123456）</div>
          <div class="chips">
            <span v-for="a in accounts" :key="a.u" @click="fill(a.u)">{{ a.n }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const remember = ref(true)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const accounts = [
  { u: 'admin', n: '系统管理员' }, { u: 'jiaowu', n: '教务管理员' },
  { u: 'zuzhang', n: '教研组长' }, { u: 'teacher', n: '教师' }, { u: 'expert', n: '专家评委' }
]

function fill(u) {
  form.username = u
  form.password = u === 'admin' ? 'admin123' : '123456'
}

async function submit() {
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      const data = await http.post('/api/auth/login', { username: form.username, password: form.password })
      store.setLogin(data)
      if (remember.value) localStorage.setItem('trs_last_user', form.username)
      ElMessage.success('登录成功')
      const target = route.query.redirect || (data.user.roleCode === 'TEACHER' ? '/portal' : '/dashboard')
      router.push(target)
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  const last = localStorage.getItem('trs_last_user')
  if (last) { form.username = last; form.password = last === 'admin' ? 'admin123' : '123456' }
})
</script>

<style scoped>
.login-wrap { min-height: 100vh; display: grid; place-items: center; position: relative; overflow: hidden; background: #0B2740; }
/* 主题背景图：真实教研室场景（本地静态资源），叠加钴蓝色蒙版统一色调 */
.bg-photo { position: absolute; inset: 0; background-image: url('../assets/login-bg.jpg'); background-size: cover; background-position: center; opacity: .5; }
.bg-photo::after { content: ''; position: absolute; inset: 0; background: linear-gradient(120deg, rgba(10,36,56,.92) 0%, rgba(16,62,95,.72) 48%, rgba(11,44,70,.9) 100%); }
.bg-layer { position: absolute; inset: 0;
  background:
    radial-gradient(900px 520px at 12% 18%, rgba(40,120,181,.55), transparent 62%),
    radial-gradient(760px 500px at 88% 78%, rgba(15,59,93,.85), transparent 60%),
    linear-gradient(135deg, #0A2438 0%, #103E5F 48%, #0B2C46 100%);
}
.bg-layer::after { content: ''; position: absolute; inset: 0;
  background-image: linear-gradient(rgba(255,255,255,.045) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.045) 1px, transparent 1px);
  background-size: 46px 46px; mask-image: radial-gradient(circle at 50% 50%, #000 40%, transparent 78%);
}
.login-card { position: relative; width: 940px; max-width: 94vw; min-height: 520px; display: grid; grid-template-columns: 1.05fr .95fr; background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 24px 60px rgba(3,20,34,.45); }
.left { background: linear-gradient(160deg, #0F3B5D 0%, #16527C 100%); color: #fff; padding: 42px 40px; display: flex; flex-direction: column; position: relative; }
.left::after { content: ''; position: absolute; right: -60px; bottom: -60px; width: 220px; height: 220px; border-radius: 50%; background: rgba(255,255,255,.05); }
.mark { width: 44px; height: 44px; border-radius: 10px; background: rgba(255,255,255,.16); display: grid; place-items: center; font-size: 20px; font-weight: 700; }
.left h1 { font-size: 27px; line-height: 1.42; margin: 26px 0 0; font-weight: 600; letter-spacing: .5px; }
.slogan { margin: 12px 0 0; font-size: 13px; color: rgba(255,255,255,.72); letter-spacing: 2px; }
.feats { list-style: none; padding: 0; margin: 34px 0 0; display: grid; gap: 14px; }
.feats li { font-size: 12.5px; color: rgba(255,255,255,.8); line-height: 1.6; padding-left: 16px; position: relative; }
.feats li i { position: absolute; left: 0; top: 8px; width: 6px; height: 6px; border-radius: 50%; background: #7FB6DC; }
.foot { margin-top: auto; font-size: 11.5px; color: rgba(255,255,255,.45); }
.right { padding: 46px 44px; display: flex; flex-direction: column; }
.right h2 { margin: 0; font-size: 22px; font-weight: 600; }
.right .tip { margin: 8px 0 26px; font-size: 12.5px; color: var(--c-text-2); }
.row { display: flex; align-items: center; justify-content: space-between; margin: -6px 0 18px; }
.submit { width: 100%; letter-spacing: 4px; height: 42px; }
.demo { margin-top: auto; padding-top: 18px; border-top: 1px dashed var(--c-border); }
.demo .dt { font-size: 12px; color: var(--c-text-2); margin-bottom: 8px; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chips span { font-size: 12px; padding: 4px 10px; border-radius: 20px; background: #F1F6FA; color: var(--c-accent); cursor: pointer; border: 1px solid #DCE6EE; }
.chips span:hover { background: #E3EEF7; }
</style>
