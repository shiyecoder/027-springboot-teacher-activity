/**
 * 全局时间格式化：全系统统一调用，禁止各页面自行拼接格式。
 * 后端返回 ISO 字符串或数组，这里统一成 yyyy-MM-dd HH:mm:ss。
 */
export function formatTime(value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }
  const str = String(value)
  if (/^\d{4}-\d{2}-\d{2}T/.test(str)) {
    return str.replace('T', ' ').slice(0, 19)
  }
  return str.length > 19 ? str.slice(0, 19) : str
}

export function formatDate(value) {
  if (!value) return '-'
  return formatTime(value).slice(0, 10)
}

export function pad(n) {
  return String(n).padStart(2, '0')
}

/** 金额：千分位 + 两位小数 */
export function money(value) {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  if (Number.isNaN(n)) return String(value)
  return '￥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function numberText(value, digits = 0) {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  return Number.isNaN(n) ? String(value) : n.toFixed(digits)
}
