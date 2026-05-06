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
const filterAgentId = ref('')
const filterRunId   = ref('')

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
    const res = await listEvaluations({
      page: page.value,
      page_size: page_size.value,
      agent_id: filterAgentId.value || undefined,
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

    <!-- filters -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-row :gutter="12" align="middle">
        <el-col :span="7">
          <el-input v-model="filterAgentId" placeholder="按 Agent ID 过滤" clearable @change="load" />
        </el-col>
        <el-col :span="7">
          <el-input v-model="filterRunId" placeholder="按 Run ID 过滤" clearable @change="load" />
        </el-col>
        <el-col :span="4">
          <el-button @click="load">搜索</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-table :data="data" v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
        <el-table-column prop="task" label="任务" show-overflow-tooltip />
        <el-table-column label="综合分" width="100">
          <template #default="{ row }">
            <span :class="['score-text', scoreClass(row.overall_score)]">
              {{ (row.overall_score * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="指标" width="220">
          <template #default="{ row }">
            <el-tag
              v-for="m in row.metrics_requested"
              :key="m"
              size="small"
              style="margin:2px"
            >{{ m }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="router.push(`/evaluations/${row.id}`)">详情</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="page_size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="load"
      />
    </el-card>
  </div>
</template>
