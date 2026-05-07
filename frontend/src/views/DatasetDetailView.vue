<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDataset } from '@/api'
import type { Dataset } from '@/types'

const route   = useRoute()
const router  = useRouter()
const dataset = ref<Dataset | null>(null)
const loading = ref(true)

function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function formatJson(v: unknown): string {
  try { return JSON.stringify(v, null, 2) }
  catch { return String(v) }
}

onMounted(async () => {
  try {
    dataset.value = await getDataset(route.params.id as string)
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
        <h2>数据集详情</h2>
      </div>
      <el-button type="primary" @click="router.push('/runs/new')">
        <el-icon><VideoPlay /></el-icon> 用此数据集发起 Run
      </el-button>
    </div>

    <template v-if="dataset">
      <el-card shadow="never" style="margin-bottom:16px">
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="ID">{{ dataset.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ dataset.name }}</el-descriptions-item>
          <el-descriptions-item label="用例数">{{ dataset.case_count }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ dataset.description ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fmt(dataset.created_at) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never">
        <template #header><span style="font-weight:600">测试用例（{{ dataset.cases.length }} 条）</span></template>
        <el-table :data="dataset.cases" size="small">
          <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
          <el-table-column prop="task" label="任务" show-overflow-tooltip />
          <el-table-column label="期望工具" width="200">
            <template #default="{ row }">
              <el-tag
                v-for="t in (row.reference?.expected_tools ?? [])"
                :key="t"
                size="small"
                style="margin:2px"
              >{{ t }}</el-tag>
              <span v-if="!row.reference?.expected_tools?.length" style="color:#c0c4cc">—</span>
            </template>
          </el-table-column>
          <el-table-column label="期望最大步骤" width="110" align="center">
            <template #default="{ row }">
              {{ row.reference?.expected_max_steps ?? '—' }}
            </template>
          </el-table-column>
          <el-table-column label="参考答案" width="100" align="center">
            <template #default="{ row }">
              <el-popover v-if="row.reference?.reference || row.reference?.expected_answer" trigger="hover" :width="300">
                <template #reference>
                  <el-tag size="small" type="success">查看</el-tag>
                </template>
                <pre style="margin:0;font-size:12px;white-space:pre-wrap">{{ formatJson(row.reference) }}</pre>
              </el-popover>
              <span v-else style="color:#c0c4cc">—</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>
