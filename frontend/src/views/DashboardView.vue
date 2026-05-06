<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listEvaluations, listRuns, listDatasets } from '@/api'
import type { EvaluationListItem, RunListItem, DatasetListItem } from '@/types'

const router = useRouter()

const stats = ref({ evaluations: 0, runs: 0, datasets: 0, avgScore: 0 })
const recentEvals = ref<EvaluationListItem[]>([])
const recentRuns  = ref<RunListItem[]>([])
const loading = ref(true)

function scoreClass(s: number) {
  if (s >= 0.8) return 'score-high'
  if (s >= 0.6) return 'score-mid'
  return 'score-low'
}

function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

const runStatusMap: Record<string, { label: string; type: 'success'|'warning'|'info'|'danger' }> = {
  completed: { label: '完成', type: 'success' },
  running:   { label: '运行中', type: 'warning' },
  pending:   { label: '等待', type: 'info' },
  failed:    { label: '失败', type: 'danger' },
}

onMounted(async () => {
  try {
    const [eData, rData, dData] = await Promise.all([
      listEvaluations({ page: 1, page_size: 5 }),
      listRuns({ page: 1, page_size: 5 }),
      listDatasets({ page: 1, page_size: 1 }),
    ])
    stats.value.evaluations = eData.total
    stats.value.runs = rData.total
    stats.value.datasets = dData.total
    recentEvals.value = eData.items
    recentRuns.value = rData.items
    if (eData.items.length) {
      stats.value.avgScore = eData.items.reduce((a, b) => a + b.overall_score, 0) / eData.items.length
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>概览</h2>
    </div>

    <!-- stat cards -->
    <el-row :gutter="20" style="margin-bottom:24px">
      <el-col :span="6">
        <el-card shadow="never" style="border-left:4px solid #409eff">
          <el-statistic title="评估总数" :value="stats.evaluations" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" style="border-left:4px solid #67c23a">
          <el-statistic title="Run 总数" :value="stats.runs" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" style="border-left:4px solid #e6a23c">
          <el-statistic title="数据集数" :value="stats.datasets" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" style="border-left:4px solid #9b59b6">
          <el-statistic
            title="近期均分"
            :value="stats.avgScore * 100"
            :precision="1"
            suffix="%"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- recent evaluations -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">最近评估</span>
              <el-button link type="primary" @click="router.push('/evaluations')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentEvals" size="small" :show-header="true">
            <el-table-column prop="task" label="任务" show-overflow-tooltip />
            <el-table-column label="综合分" width="90">
              <template #default="{ row }">
                <span :class="['score-text', scoreClass(row.overall_score)]">
                  {{ (row.overall_score * 100).toFixed(1) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="140">
              <template #default="{ row }">{{ fmt(row.created_at) }}</template>
            </el-table-column>
            <el-table-column label="" width="60">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="router.push(`/evaluations/${row.id}`)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- recent runs -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">最近 Run</span>
              <el-button link type="primary" @click="router.push('/runs')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentRuns" size="small">
            <el-table-column label="状态" width="76">
              <template #default="{ row }">
                <el-tag :type="runStatusMap[row.status]?.type ?? 'info'" size="small">
                  {{ runStatusMap[row.status]?.label ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="综合分" width="72">
              <template #default="{ row }">
                <span :class="['score-text', scoreClass(row.summary?.overall_score ?? 0)]">
                  {{ ((row.summary?.overall_score ?? 0) * 100).toFixed(1) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="" width="60">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="router.push(`/runs/${row.id}`)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
