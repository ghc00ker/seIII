<script setup lang="ts">
import type { MetricResult } from '@/types'

defineProps<{ results: MetricResult[] }>()

const judgerLabel: Record<string, string> = {
  goal_completion:    'LLM',
  tool_call_accuracy: '规则',
  tool_call_f1:       '统计',
  step_efficiency:    '规则',
  answer_faithfulness:'LLM',
  task_adherence:     'LLM',
}

function scoreClass(s: number) {
  if (s >= 0.8) return 'score-high'
  if (s >= 0.6) return 'score-mid'
  return 'score-low'
}

function scoreColor(s: number) {
  if (s >= 0.8) return '#67c23a'
  if (s >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

const metricLabels: Record<string, string> = {
  goal_completion:    '任务目标完成度',
  tool_call_accuracy: '工具调用准确率',
  tool_call_f1:       '工具调用 F1',
  step_efficiency:    '步骤效率',
  answer_faithfulness:'答案忠实度',
  task_adherence:     '任务主题合规度',
}
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:12px">
    <el-card
      v-for="r in results"
      :key="r.metric"
      shadow="never"
      style="border:1px solid #ebeef5"
    >
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:10px">
        <el-tag size="small" type="info">{{ judgerLabel[r.metric] ?? '?' }}</el-tag>
        <span style="font-weight:600;font-size:15px;color:#303133">
          {{ metricLabels[r.metric] ?? r.metric }}
        </span>
        <span v-if="r.ragas_metric" style="font-size:12px;color:#909399">{{ r.ragas_metric }}</span>
        <div style="margin-left:auto;display:flex;align-items:center;gap:8px">
          <el-tag :type="r.passed ? 'success' : 'danger'" size="small">
            {{ r.passed ? '通过' : '未通过' }}
          </el-tag>
          <span :class="['score-text', scoreClass(r.score)]">{{ (r.score * 100).toFixed(1) }}%</span>
        </div>
      </div>
      <el-progress
        :percentage="Math.round(r.score * 100)"
        :color="scoreColor(r.score)"
        :stroke-width="8"
        :show-text="false"
        style="margin-bottom:10px"
      />
      <div style="font-size:13px;color:#606266;line-height:1.6;background:#f5f7fa;padding:8px 12px;border-radius:6px">
        {{ r.reason }}
      </div>
    </el-card>
  </div>
</template>
