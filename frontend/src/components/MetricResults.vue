<script setup lang="ts">
import { ref } from 'vue'
import type { MetricResult } from '@/types'

defineProps<{ results: MetricResult[] }>()

const expanded = ref<Record<string, boolean>>({})
function toggle(metric: string) { expanded.value[metric] = !expanded.value[metric] }

const judgerLabel: Record<string, { text: string; color: string }> = {
  goal_completion:    { text: 'LLM',  color: '#6366f1' },
  tool_call_accuracy: { text: '规则', color: '#0891b2' },
  tool_call_f1:       { text: '统计', color: '#0891b2' },
  step_efficiency:    { text: '规则', color: '#0891b2' },
  answer_faithfulness:{ text: 'LLM',  color: '#6366f1' },
  task_adherence:     { text: 'LLM',  color: '#6366f1' },
}
const metricLabels: Record<string, string> = {
  goal_completion:    '目标完成度',
  tool_call_accuracy: '工具调用准确率',
  tool_call_f1:       '工具调用 F1',
  step_efficiency:    '步骤效率',
  answer_faithfulness:'答案忠实度',
  task_adherence:     '任务主题合规',
}
function scoreClass(s: number) { return s >= 0.8 ? 'score-high' : s >= 0.6 ? 'score-mid' : 'score-low' }
function barColor(s: number)   { return s >= 0.8 ? '#6366f1' : s >= 0.6 ? '#f59e0b' : '#f87171' }
</script>

<template>
  <div class="metric-list">
    <div
      v-for="r in results"
      :key="r.metric"
      class="metric-row"
      @click="toggle(r.metric)"
    >
      <!-- Main row -->
      <div class="metric-main">
        <!-- Left: judger + name -->
        <div class="metric-left">
          <span
            class="judger-badge"
            :style="{ color: judgerLabel[r.metric]?.color ?? '#6b7280',
                      background: (judgerLabel[r.metric]?.color ?? '#6b7280') + '18' }"
          >{{ judgerLabel[r.metric]?.text ?? '?' }}</span>
          <span class="metric-name">{{ metricLabels[r.metric] ?? r.metric }}</span>
        </div>

        <!-- Right: bar + score + pass -->
        <div class="metric-right">
          <div class="metric-bar-wrap">
            <div
              class="metric-bar"
              :style="{ width: (r.score * 100) + '%', background: barColor(r.score) }"
            />
          </div>
          <span :class="['metric-pct', scoreClass(r.score)]">{{ (r.score * 100).toFixed(1) }}%</span>
          <el-icon
            class="pass-icon"
            :class="r.passed ? 'pass-icon--ok' : 'pass-icon--fail'"
          >
            <component :is="r.passed ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
          <el-icon class="expand-icon" :class="{ open: expanded[r.metric] }">
            <ArrowRight />
          </el-icon>
        </div>
      </div>

      <!-- Reason (expandable) -->
      <div v-if="expanded[r.metric] && r.reason" class="metric-reason">
        {{ r.reason }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.metric-list { display: flex; flex-direction: column; gap: 4px; }

.metric-row {
  border-radius: 8px; border: 1px solid #f3f4f6;
  overflow: hidden; cursor: pointer;
  transition: border-color .15s, box-shadow .15s;
}
.metric-row:hover { border-color: #e0e7ff; box-shadow: 0 1px 6px rgba(99,102,241,.08); }

.metric-main {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 12px; gap: 12px;
}
.metric-left { display: flex; align-items: center; gap: 8px; min-width: 0; }
.judger-badge {
  font-size: 10px; font-weight: 700; padding: 1px 6px;
  border-radius: 4px; flex-shrink: 0; letter-spacing: .3px;
}
.metric-name { font-size: 13px; font-weight: 500; color: #374151; }

.metric-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.metric-bar-wrap {
  width: 80px; height: 5px;
  background: #f3f4f6; border-radius: 99px; overflow: hidden;
}
.metric-bar {
  height: 100%; border-radius: 99px;
  transition: width .5s cubic-bezier(.4,0,.2,1);
}
.metric-pct { font-size: 13px; font-weight: 700; width: 46px; text-align: right; }
.metric-pct.score-high { color: #16a34a; }
.metric-pct.score-mid  { color: #d97706; }
.metric-pct.score-low  { color: #dc2626; }

.pass-icon { font-size: 15px; }
.pass-icon--ok   { color: #22c55e; }
.pass-icon--fail { color: #f87171; }
.expand-icon {
  font-size: 11px; color: #9ca3af;
  transition: transform .2s;
}
.expand-icon.open { transform: rotate(90deg); }

.metric-reason {
  padding: 8px 14px 10px;
  font-size: 12.5px; color: #6b7280; line-height: 1.6;
  background: #fafbfc; border-top: 1px solid #f3f4f6;
  white-space: pre-wrap;
}
</style>
