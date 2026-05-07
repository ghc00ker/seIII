<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEvaluation } from '@/api'
import type { EvaluationResult } from '@/types'
import TraceViewer from '@/components/TraceViewer.vue'
import MetricResults from '@/components/MetricResults.vue'

const route  = useRoute()
const router = useRouter()
const ev     = ref<EvaluationResult | null>(null)
const loading = ref(true)

function scoreClass(s: number) {
  if (s >= 0.8) return 'score-high'
  if (s >= 0.6) return 'score-mid'
  return 'score-low'
}
function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  try {
    ev.value = await getEvaluation(route.params.id as string)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>评估详情</h2>
      </div>
    </div>

    <template v-if="ev">
      <!-- header card -->
      <el-card shadow="never" style="margin-bottom:16px">
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="ID">{{ ev.id }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fmt(ev.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="综合分">
            <span :class="['score-text', scoreClass(ev.overall_score)]" style="font-size:18px">
              {{ (ev.overall_score * 100).toFixed(1) }}%
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="任务" :span="3">{{ ev.trace.task }}</el-descriptions-item>
          <el-descriptions-item v-if="ev.run_id" label="所属 Run">
            <el-button link type="primary" @click="router.push(`/runs/${ev.run_id}`)">{{ ev.run_id }}</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="评估指标" :span="ev.run_id ? 2 : 3">
            <el-tag v-for="m in ev.metrics_requested" :key="m" size="small" style="margin:2px">{{ m }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="20">
        <!-- trace viewer -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span style="font-weight:600">执行轨迹（Trace）</span></template>
            <TraceViewer :steps="ev.trace.steps" />
          </el-card>
        </el-col>

        <!-- metric results -->
        <el-col :span="12">
          <el-card shadow="never" style="margin-bottom:16px">
            <template #header><span style="font-weight:600">各项指标评分</span></template>
            <MetricResults :results="ev.results" />
          </el-card>

          <!-- reference info -->
          <el-card v-if="ev.reference" shadow="never">
            <template #header><span style="font-weight:600">Reference</span></template>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item v-if="ev.reference.reference" label="金标参考">{{ ev.reference.reference }}</el-descriptions-item>
              <el-descriptions-item v-if="ev.reference.expected_answer" label="期望答案">{{ ev.reference.expected_answer }}</el-descriptions-item>
              <el-descriptions-item v-if="ev.reference.expected_tools?.length" label="期望工具">
                <el-tag v-for="t in ev.reference.expected_tools" :key="t" size="small" style="margin:2px">{{ t }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="ev.reference.expected_max_steps" label="期望最大步骤">{{ ev.reference.expected_max_steps }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>
