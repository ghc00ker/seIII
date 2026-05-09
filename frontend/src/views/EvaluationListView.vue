<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listEvaluations, deleteEvaluation } from '@/api'
import type { EvaluationListItem } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const data   = ref<EvaluationListItem[]>([])
const total  = ref(0)
const page   = ref(1)
const page_size = ref(20)
const loading = ref(false)
const filterRunId = ref('')

function scoreClass(s: number) {
  return s >= 0.8 ? 'score-high' : s >= 0.6 ? 'score-mid' : 'score-low'
}
function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}
function shortId(id: string) {
  return '#' + id.replace(/^eval_/i, '').slice(0, 6)
}

async function load() {
  loading.value = true
  try {
    const res = await listEvaluations({
      page: page.value,
      page_size: page_size.value,
      run_id: filterRunId.value || undefined,
    })
    data.value  = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: string) {
  await ElMessageBox.confirm('确认删除该评估记录？', '提示', { type: 'warning' })
  await deleteEvaluation(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>评估记录</h2>
      <el-button type="primary" @click="router.push('/evaluations/new')">
        <el-icon><Plus /></el-icon> 提交评估
      </el-button>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <el-input
        v-model="filterRunId"
        placeholder="按 Run ID 过滤"
        clearable style="width:260px"
        @change="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button @click="load">刷新</el-button>
      <span class="filter-total">共 {{ total }} 条记录</span>
    </div>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="data"
        v-loading="loading"
        style="width:100%"
        row-class-name="eval-row"
        @row-click="(row: EvaluationListItem) => router.push(`/evaluations/${row.id}`)"
      >
        <!-- ID hash -->
        <el-table-column label="ID" width="90">
          <template #default="{ row }">
            <span class="id-chip">{{ shortId(row.id) }}</span>
          </template>
        </el-table-column>

        <!-- Task / Input -->
        <el-table-column label="任务输入" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="task-text">{{ row.task }}</span>
          </template>
        </el-table-column>

        <!-- Metrics as compact score pills -->
        <el-table-column label="评估指标" min-width="200">
          <template #default="{ row }">
            <div class="metric-badges">
              <span
                v-for="m in row.metrics_requested"
                :key="m"
                class="metric-badge"
              >{{ m.replace(/_/g, ' ') }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- Overall score -->
        <el-table-column label="综合分" width="96" align="center">
          <template #default="{ row }">
            <span :class="['score-pill', scoreClass(row.overall_score)]">
              {{ (row.overall_score * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>

        <!-- Time -->
        <el-table-column label="时间" width="148">
          <template #default="{ row }">
            <span class="time-text">{{ fmt(row.created_at) }}</span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="" width="80" align="center" @click.stop>
          <template #default="{ row }">
            <el-button
              link type="danger" size="small"
              @click.stop="handleDelete(row.id)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="page_size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @change="load"
      />
    </el-card>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 14px;
}
.filter-total { margin-left: auto; font-size: 12.5px; color: #9ca3af; }

.table-card :deep(.eval-row) { cursor: pointer; }
.table-card :deep(.eval-row:hover td) { background: #f5f3ff !important; }

.id-chip {
  display: inline-block;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px; font-weight: 600;
  background: #f3f4f6; color: #374151;
  padding: 2px 8px; border-radius: 5px;
}
.task-text { font-size: 13px; color: #1f2937; }
.time-text { font-size: 12px; color: #9ca3af; }

.metric-badges { display: flex; flex-wrap: wrap; gap: 3px; }
.metric-badge {
  font-size: 10.5px; color: #6366f1; font-weight: 500;
  background: #eef0ff; padding: 1px 6px; border-radius: 4px;
}

.score-pill {
  display: inline-block; padding: 3px 10px;
  border-radius: 99px; font-size: 12.5px; font-weight: 700;
}
.score-pill.score-high { background: #f0fdf4; color: #16a34a; }
.score-pill.score-mid  { background: #fffbeb; color: #d97706; }
.score-pill.score-low  { background: #fef2f2; color: #dc2626; }

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
