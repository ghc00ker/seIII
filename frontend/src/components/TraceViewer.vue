<script setup lang="ts">
import { ref } from 'vue'
import type { Step } from '@/types'

defineProps<{ steps: Step[] }>()

const expanded = ref<Record<number, boolean>>({})
function toggle(i: number) { expanded.value[i] = !expanded.value[i] }
function isOpen(i: number) { return expanded.value[i] !== false }

const stepMeta: Record<string, { label: string; color: string; bg: string; textColor: string }> = {
  thought:      { label: '思考',     color: '#3b82f6', bg: '#eff6ff', textColor: '#1d4ed8' },
  tool_call:    { label: '工具调用', color: '#f59e0b', bg: '#fffbeb', textColor: '#92400e' },
  tool_result:  { label: '工具结果', color: '#10b981', bg: '#f0fdf4', textColor: '#065f46' },
  final_answer: { label: '最终答案', color: '#8b5cf6', bg: '#faf5ff', textColor: '#5b21b6' },
}

function fmtJson(v: unknown): string {
  try { return JSON.stringify(v, null, 2) } catch { return String(v) }
}
</script>

<template>
  <div class="trace-wrap">
    <div
      v-for="(step, i) in steps"
      :key="i"
      class="step-block"
      :style="{ '--step-color': stepMeta[step.type]?.color ?? '#9ca3af' }"
    >
      <!-- Step header (always visible, click to collapse) -->
      <div class="step-hd" @click="toggle(i)">
        <span class="step-num">{{ i + 1 }}</span>
        <span
          class="step-type-badge"
          :style="{
            background: stepMeta[step.type]?.bg,
            color: stepMeta[step.type]?.textColor,
          }"
        >{{ stepMeta[step.type]?.label ?? step.type }}</span>
        <span v-if="step.tool" class="step-tool">{{ step.tool }}</span>
        <span v-else-if="step.content" class="step-preview">
          {{ step.content.length > 60 ? step.content.slice(0, 60) + '…' : step.content }}
        </span>
        <el-icon class="step-toggle" :class="{ 'is-open': isOpen(i) }"><ArrowRight /></el-icon>
      </div>

      <!-- Step body (collapsible) -->
      <div v-if="isOpen(i)" class="step-body">
        <!-- thought / final_answer text -->
        <div v-if="step.content" class="step-text"
          :class="{ 'step-text--answer': step.type === 'final_answer' }"
        >{{ step.content }}</div>

        <!-- tool_call input -->
        <div v-if="step.input">
          <div class="code-label">入参</div>
          <pre class="code-block">{{ fmtJson(step.input) }}</pre>
        </div>

        <!-- tool_result output -->
        <div v-if="step.output !== undefined">
          <div class="code-label">返回值</div>
          <pre class="code-block code-block--result">{{ fmtJson(step.output) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.trace-wrap { display: flex; flex-direction: column; gap: 6px; }

.step-block {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  border-left: 3px solid var(--step-color);
  overflow: hidden;
  transition: box-shadow .15s;
}
.step-block:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.07); }

.step-hd {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 12px;
  cursor: pointer; background: #fff;
  transition: background .12s;
}
.step-hd:hover { background: #fafbfc; }

.step-num {
  width: 20px; height: 20px; border-radius: 50%; flex-shrink: 0;
  background: #f3f4f6; color: #6b7280;
  font-size: 10.5px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.step-type-badge {
  font-size: 11px; font-weight: 700; letter-spacing: .3px;
  padding: 2px 8px; border-radius: 5px; flex-shrink: 0;
}
.step-tool {
  font-size: 13px; font-weight: 600; color: #374151;
  font-family: 'JetBrains Mono', Consolas, monospace;
}
.step-preview {
  font-size: 12.5px; color: #6b7280; flex: 1;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.step-toggle {
  margin-left: auto; color: #9ca3af; font-size: 12px;
  transition: transform .2s; flex-shrink: 0;
}
.step-toggle.is-open { transform: rotate(90deg); }

.step-body {
  padding: 10px 14px 12px;
  background: #fafbfc;
  border-top: 1px solid #f3f4f6;
  display: flex; flex-direction: column; gap: 10px;
}

.step-text {
  font-size: 13.5px; color: #374151; line-height: 1.65;
  white-space: pre-wrap;
}
.step-text--answer {
  font-size: 14px; font-weight: 500; color: #1f2937;
  background: #faf5ff; padding: 10px 12px; border-radius: 6px;
  border-left: 3px solid #8b5cf6;
}

.code-label {
  font-size: 10.5px; font-weight: 700; text-transform: uppercase;
  letter-spacing: .6px; color: #9ca3af; margin-bottom: 5px;
}
.code-block {
  margin: 0; padding: 10px 12px;
  background: #1e293b; color: #e2e8f0;
  border-radius: 7px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px; line-height: 1.6;
  overflow: auto; white-space: pre;
}
.code-block--result {
  background: #022c22; color: #6ee7b7;
}
</style>
