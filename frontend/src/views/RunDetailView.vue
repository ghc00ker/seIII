<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRun, listEvaluations } from '@/api'
import type { RunResult, EvaluationListItem, RunStatus } from '@/types'

const route  = useRoute()
const router = useRouter()
const run    = ref<RunResult | null>(null)
const evals  = ref<EvaluationListItem[]>([])
const loading = ref(true)

const statusMap: Record<RunStatus, { label: string; type: 'success'|'warning'|'info'|'danger' }> = {
  completed: { label: '完成',  type: 'success' },
  running:   { label: '运行中', type: 'warning' },
  pending:   { label: '等待',  type: 'info' },
  failed:    { label: '失败',  type: 'danger' },
}

function scoreClass(s: number) {
  if (s >= 0.8) return 'score-high'
  if (s >= 0.6) return 'score-mid'
  return 'score-low'
}
function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function progressPct(r: RunResult) {
  if (!r.total_cases) return 0
  return Math.round((r.completed_cases / r.total_cases) * 100)
}

onMounted(async () => {
  try {
    const runId = route.params.id as string
    const [r, e] = await Promise.all([
      getRun(runId),
      listEvaluations({ run_id: runId, page: 1, page_size: 100 }),
    ])
    run.value   = r
    evals.value = e.items
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
        <h2>Run 详情</h2>
      </div>
    </div>

    <template v-if="run">
      <!-- info card -->
      <el-card shadow="never" style="margin-bottom:16px">
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="ID">{{ run.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[run.status]?.type" size="small">{{ statusMap[run.status]?.label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fmt(run.created_at) }}</el-descriptions-item>
          <el-descriptions-item label="Agent Endpoint" :span="2">
            <span style="font-family:monospace">{{ run.agent_endpoint }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="Agent 版本">{{ run.agent_version ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="数据集">{{ run.dataset_id }}</el-descriptions-item>
          <el-descriptions-item label="综合分">
            <span :class="['score-text', scoreClass(run.summary?.overall_score ?? 0)]" style="font-size:18px">
              {{ ((run.summary?.overall_score ?? 0) * 100).toFixed(1) }}%
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="用例数">
            {{ run.completed_cases }} / {{ run.total_cases }}
            <span v-if="run.failed_cases" style="color:#f56c6c;margin-left:4px">（{{ run.failed_cases }} 失败）</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- progress -->
      <el-card shadow="never" style="margin-bottom:16px">
        <div style="margin-bottom:8px;font-weight:500">执行进度</div>
        <el-progress :percentage="progressPct(run)" :status="run.status === 'failed' ? 'exception' : run.status === 'completed' ? 'success' : undefined" />
      </el-card>

      <!-- metric scores -->
      <el-card v-if="run.summary?.metric_scores" shadow="never" style="margin-bottom:16px">
        <template #header><span style="font-weight:600">各指标均分</span></template>
        <div style="display:flex;flex-wrap:wrap;gap:16px">
          <div v-for="(score, metric) in run.summary.metric_scores" :key="metric" style="min-width:160px">
            <div style="font-size:13px;color:#606266;margin-bottom:4px">{{ metric }}</div>
            <el-progress
              :percentage="Math.round(score * 100)"
              :color="score >= 0.8 ? '#67c23a' : score >= 0.6 ? '#e6a23c' : '#f56c6c'"
              :format="(p: number) => p.toFixed(1) + '%'"
            />
          </div>
        </div>
      </el-card>

      <!-- evaluation list -->
      <el-card shadow="never">
        <template #header><span style="font-weight:600">评估记录（{{ evals.length }} 条）</span></template>
        <el-table :data="evals" size="small">
          <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
          <el-table-column prop="task" label="任务" show-overflow-tooltip />
          <el-table-column label="综合分" width="90">
            <template #default="{ row }">
              <span :class="['score-text', scoreClass(row.overall_score)]">{{ (row.overall_score * 100).toFixed(1) }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ fmt(row.created_at) }}</template>
          </el-table-column>
          <el-table-column label="" width="60">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="router.push(`/evaluations/${row.id}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>
