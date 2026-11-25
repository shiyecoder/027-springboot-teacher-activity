/**
 * 状态标签映射：状态不得只用颜色表达，标签内始终带中文文案。
 * 颜色只做辅助，token 与 design-system.md 一致。
 */
const MAP = {
  草稿: 'tag-gray',
  已发布: 'tag-blue',
  报名中: 'tag-orange',
  进行中: 'tag-blue',
  待归档: 'tag-orange',
  已归档: 'tag-green',
  待审批: 'tag-orange',
  已通过: 'tag-green',
  已退回: 'tag-red',
  组长初审: 'tag-orange',
  教务复审: 'tag-orange',
  专家评审: 'tag-blue',
  已认定: 'tag-green',
  待确认: 'tag-orange',
  已确认: 'tag-green',
  待处理: 'tag-orange',
  已处理: 'tag-green',
  已驳回: 'tag-red',
  已审核: 'tag-green',
  待审核: 'tag-orange'
}

export function tagClass(status) {
  return MAP[status] || 'tag-gray'
}
