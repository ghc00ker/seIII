<script setup lang="ts">
import type { MetricDescriptor } from '@/types'

const props = defineProps<{
  metrics: MetricDescriptor[]
  modelValue: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [val: string[]]
}>()

function toggle(name: string) {
  const cur = [...props.modelValue]
  const idx = cur.indexOf(name)
  if (idx === -1) cur.push(name)
  else cur.splice(idx, 1)
  emit('update:modelValue', cur)
}

function isSelected(name: string) {
  return props.modelValue.includes(name)
}

const metricCN: Record<string, string> = {
  goal_completion:     '任务目标完成度',
  tool_call_accuracy:  '工具调用准确率',
  tool_call_f1:        '工具调用 F1',
  step_efficiency:     '步骤效率',
  answer_faithfulness: '答案忠实度',
  task_adherence:      '任务主题合规度',
}

const judgerStyle: Record<string, { bg: string; color: string; label: string }> = {
  llm:        { bg: '#f0e6ff', color: '#7c3aed', label: 'LLM 裁判' },
  rule:       { bg: '#e6f7e6', color: '#18a058', label: '规则' },
  statistical:{ bg: '#fff3e0', color: '#d97706', label: '统计算法' },
}

const dimensionStyle: Record<string, { bg: string; color: string; label: string }> = {
  effectiveness: { bg: '#e8f4ff', color: '#1677ff', label: '效果' },
  safety:        { bg: '#fff1f0', color: '#cf1322', label: '安全' },
  performance:   { bg: '#f0fff4', color: '#2e8b57', label: '性能' },
}

const modeLabel: Record<string, string> = {
  result:  '面向结果',
  process: '面向过程',
}
</script>

<template>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
    <div
      v-for="m in metrics"
      :key="m.name"
      @click="toggle(m.name)"
      :style="{
        border: isSelected(m.name) ? '2px solid #409eff' : '2px solid #e4e7ed',
        borderRadius: '8px',
        padding: '12px',
        cursor: 'pointer',
        background: isSelected(m.name) ? '#f0f7ff' : '#fff',
        transition: 'all .15s',
        userSelect: 'none',
      }"
    >
      <!-- header row -->
      <div style="display:flex;align-items:flex-start;gap:8px;margin-bottom:6px">
        <el-checkbox
          :model-value="isSelected(m.name)"
          style="flex-shrink:0;margin-top:1px"
          @click.stop
          @change="toggle(m.name)"
        />
        <div>
          <div style="font-weight:600;font-size:13px;color:#1a1a2e;line-height:1.3">
            {{ metricCN[m.name] ?? m.name }}
          </div>
          <div style="font-size:11px;color:#909399;margin-top:1px;font-family:monospace">
            {{ m.name }}
          </div>
        </div>
      </div>

      <!-- description -->
      <div style="font-size:12px;color:#606266;line-height:1.5;margin-bottom:8px;padding-left:26px">
        {{ m.description }}
      </div>

      <!-- tags -->
      <div style="display:flex;flex-wrap:wrap;gap:4px;padding-left:26px">
        <span
          v-if="judgerStyle[m.judger]"
          :style="{
            display:'inline-block', padding:'1px 7px', borderRadius:'4px', fontSize:'11px',
            background: judgerStyle[m.judger].bg,
            color: judgerStyle[m.judger].color,
            fontWeight: 500,
          }"
        >{{ judgerStyle[m.judger].label }}</span>

        <span
          v-if="dimensionStyle[m.eval_dimension]"
          :style="{
            display:'inline-block', padding:'1px 7px', borderRadius:'4px', fontSize:'11px',
            background: dimensionStyle[m.eval_dimension].bg,
            color: dimensionStyle[m.eval_dimension].color,
            fontWeight: 500,
          }"
        >{{ dimensionStyle[m.eval_dimension].label }}</span>

        <span style="display:inline-block;padding:1px 7px;border-radius:4px;font-size:11px;background:#f5f5f5;color:#666">
          {{ modeLabel[m.eval_mode] ?? m.eval_mode }}
        </span>
      </div>
    </div>
  </div>
</template>
