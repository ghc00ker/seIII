<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listEvaluations, compareEvaluations } from '@/api'
import type { EvaluationListItem, CompareResult } from '@/types'
import { ElMessage } from 'element-plus'

const router = useRouter()
const pool    = ref<EvaluationListItem[]>([])
const selected = ref<string[]>([])
const result  = ref<CompareResult | null>(null)
const loading = ref(false)
const poolLoading = ref(false)

function scoreClass(s: number) {
  if (s >= 0.8) return 'score-high'
  if (s >= 0.6) return 'score-mid'
  return 'score-low'
}

function isBest(evalId: string, metric: string) {
  return result.value?.summary.metric_winners[metric] === evalId
}
function isBestOverall(evalId: string) {
  return result.value?.summary.best_overall === evalId
}

async function loadPool() {
  poolLoading.value = true
  try {
    const res = await listEvaluations({ page: 1, page_size: 50 })
    pool.value = res.items
  } finally {
    poolLoading.value = false
  }
}

async function doCompare() {
  if (selected.value.length < 2) { ElMessage.warning('请至少选择 2 条评估'); return }
  if (selected.value.length > 10) { ElMessage.warning('最多选择 10 条'); return }
  loading.value = true
  try {
    result.value = await compareEvaluations(selected.value)
  } catch {
    ElMessage.error('对比失败')
  } finally {
    loading.value = false
  }
}

// collect all metric names from results
function allMetrics() {
  if (!result.value) return []
  const set = new Set<string>()
  result.value.evaluations.forEach(e => e.results.forEach(r => set.add(r.metric)))
  return [...set]
}

function scoreForMetric(evalId: string, metric: string) {
  const ev = result.value?.evaluations.find(e => e.id === evalId)
  return ev?.results.find(r => r.metric === metric)?.score ?? null
}

onMounted(loadPool)
</script>

<template>
  <div>
    <div class="page-header"><h2>对比分析</h2></div>

    <el-row :gutter="20">
      <!-- selector -->
      <el-col :span="10">
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">选择评估（2~10 条）</span>
              <el-button size="small" @click="loadPool">刷新</el-button>
            </div>
          </template>
          <div v-loading="poolLoading" style="max-height:400px;overflow:auto">
            <el-checkbox-group v-model="selected">
              <div
                v-for="item in pool"
                :key="item.id"
                style="padding:6px 0;border-bottom:1px solid #f0f0f0;display:flex;align-items:center;gap:8px"
              >
                <el-checkbox :value="item.id" />
                <div style="flex:1;min-width:0">
                  <div style="font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ item.task }}</div>
                  <div style="font-size:11px;color:#909399">{{ item.id }}</div>
                </div>
                <span :class="['score-text', scoreClass(item.overall_score)]" style="flex-shrink:0">
                  {{ (item.overall_score * 100).toFixed(1) }}%
                </span>
              </div>
            </el-checkbox-group>
          </div>
          <div style="margin-top:12px;display:flex;gap:8px">
            <el-button type="primary" :loading="loading" @click="doCompare" style="flex:1">开始对比</el-button>
            <el-button @click="selected=[]">清空</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- result table -->
      <el-col :span="14">
        <el-card shadow="never" v-if="result">
          <template #header>
            <span style="font-weight:600">对比结果</span>
            <el-tag type="success" size="small" style="margin-left:8px">
              最优：{{ result.summary.best_overall }}
            </el-tag>
          </template>
          <el-table :data="result.evaluations" border size="small">
            <el-table-column label="任务" prop="task" show-overflow-tooltip />
            <el-table-column label="综合分" width="90">
              <template #default="{ row }">
                <span :class="['score-text', scoreClass(row.overall_score)]">
                  {{ (row.overall_score * 100).toFixed(1) }}%
                </span>
                <el-icon v-if="isBestOverall(row.id)" color="#e6a23c" style="margin-left:4px"><StarFilled /></el-icon>
              </template>
            </el-table-column>
            <el-table-column
              v-for="metric in allMetrics()"
              :key="metric"
              :label="metric"
              width="110"
            >
              <template #default="{ row }">
                <template v-if="scoreForMetric(row.id, metric) !== null">
                  <span :class="['score-text', scoreClass(scoreForMetric(row.id, metric)!)]">
                    {{ (scoreForMetric(row.id, metric)! * 100).toFixed(1) }}%
                  </span>
                  <el-icon v-if="isBest(row.id, metric)" color="#e6a23c" style="margin-left:2px"><StarFilled /></el-icon>
                </template>
                <span v-else style="color:#c0c4cc">—</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="60">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="router.push(`/evaluations/${row.id}`)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-empty v-else description="选择左侧评估后点击「开始对比」" />
      </el-col>
    </el-row>
  </div>
</template>
