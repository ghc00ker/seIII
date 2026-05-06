<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listDatasets, deleteDataset } from '@/api'
import type { DatasetListItem } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const data    = ref<DatasetListItem[]>([])
const total   = ref(0)
const page    = ref(1)
const page_size = ref(20)
const loading = ref(false)

function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    const res = await listDatasets({ page: page.value, page_size: page_size.value })
    data.value  = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: string) {
  await ElMessageBox.confirm('确认删除该数据集？若有关联 Run 则无法删除。', '提示', { type: 'warning' })
  try {
    await deleteDataset(id)
    ElMessage.success('已删除')
    load()
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { error?: string } } })?.response?.data?.error ?? '删除失败'
    ElMessage.error(msg)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>数据集</h2>
      <el-button type="primary" @click="router.push('/datasets/new')">
        <el-icon><Upload /></el-icon> 上传数据集
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="data" v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="用例数" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.case_count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ fmt(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="router.push(`/datasets/${row.id}`)">详情</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
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
