<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listRuns } from '@/api'
import type { RunListItem, RunStatus } from '@/types'

const router = useRouter()
const data    = ref<RunListItem[]>([])
const total   = ref(0)
const page    = ref(1)
const page_size = ref(20)
const loading = ref(false)

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

async function load() {
  loading.value = true
  try {
    const res = await listRuns({ page: page.value, page_size: page_size.value })
    data.value  = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>批次 Run</h2>
      <el-button type="primary" @click="router.push('/runs/new')">
        <el-icon><VideoPlay /></el-icon> 发起 Run
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="data" v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status as RunStatus]?.type" size="small">
              {{ statusMap[row.status as RunStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Agent Endpoint" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-size:13px;font-family:monospace">{{ row.agent_endpoint }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataset_id" label="数据集" width="140" show-overflow-tooltip />
        <el-table-column label="进度" width="110">
          <template #default="{ row }">
            {{ row.total_cases > 0 ? `${row.total_cases}` : '—' }} 条
          </template>
        </el-table-column>
        <el-table-column label="综合分" width="90">
          <template #default="{ row }">
            <span :class="['score-text', scoreClass(row.summary?.overall_score ?? 0)]">
              {{ ((row.summary?.overall_score ?? 0) * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="router.push(`/runs/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="page_size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="load"
      />
    </el-card>
  </div>
</template>
