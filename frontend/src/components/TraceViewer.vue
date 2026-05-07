<script setup lang="ts">
import type { Step } from '@/types'

const props = defineProps<{ steps: Step[] }>()

const stepMeta: Record<string, { label: string; color: string; icon: string }> = {
  thought:      { label: '思考',     color: '#409eff', icon: 'ChatDotRound' },
  tool_call:    { label: '工具调用', color: '#e6a23c', icon: 'Tools' },
  tool_result:  { label: '工具结果', color: '#67c23a', icon: 'CircleCheck' },
  final_answer: { label: '最终答案', color: '#9b59b6', icon: 'Star' },
}

function formatJson(v: unknown): string {
  try { return JSON.stringify(v, null, 2) }
  catch { return String(v) }
}
</script>

<template>
  <el-timeline class="trace-timeline">
    <el-timeline-item
      v-for="(step, i) in steps"
      :key="i"
      :color="stepMeta[step.type]?.color ?? '#909399'"
      :timestamp="`步骤 ${i + 1}`"
      placement="top"
    >
      <el-card shadow="never" style="border:1px solid #ebeef5">
        <template #header>
          <div style="display:flex;align-items:center;gap:8px">
            <el-tag
              :color="stepMeta[step.type]?.color"
              style="color:#fff;border:none;font-size:12px"
              size="small"
            >
              {{ stepMeta[step.type]?.label ?? step.type }}
            </el-tag>
            <span v-if="step.tool" style="font-weight:600;font-size:14px;color:#303133">
              {{ step.tool }}
            </span>
          </div>
        </template>

        <!-- thought / final_answer -->
        <div v-if="step.content" style="font-size:14px;color:#606266;line-height:1.7;white-space:pre-wrap">
          {{ step.content }}
        </div>

        <!-- tool_call input -->
        <div v-if="step.input">
          <div style="font-size:12px;color:#909399;margin-bottom:4px">入参</div>
          <pre style="margin:0;background:#f5f7fa;padding:10px 12px;border-radius:6px;font-size:13px;overflow:auto">{{ formatJson(step.input) }}</pre>
        </div>

        <!-- tool_result output -->
        <div v-if="step.output !== undefined">
          <div style="font-size:12px;color:#909399;margin-bottom:4px">返回值</div>
          <pre style="margin:0;background:#f5f7fa;padding:10px 12px;border-radius:6px;font-size:13px;overflow:auto">{{ formatJson(step.output) }}</pre>
        </div>
      </el-card>
    </el-timeline-item>
  </el-timeline>
</template>
