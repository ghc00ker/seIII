<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createRun, listDatasets, listMetrics } from '@/api'
import type { DatasetListItem, MetricDescriptor, MetricConfig } from '@/types'
import { ElMessage } from 'element-plus'
import MetricSelector from '@/components/MetricSelector.vue'

const router   = useRouter()
const datasets = ref<DatasetListItem[]>([])
const metrics  = ref<MetricDescriptor[]>([])
const loading  = ref(false)

const form = ref({
  agent_endpoint: '',
  agent_version: '',
  dataset_id: '',
  selectedMetrics: [] as string[],
})
const strictOrderAcc = ref(false)
const strictOrderF1  = ref(false)
const penaltyFactor  = ref(0.1)

async function submit() {
  if (!form.value.agent_endpoint) { ElMessage.error('请填写 Agent Endpoint'); return }
  if (!form.value.dataset_id) { ElMessage.error('请选择数据集'); return }

  const metric_config: MetricConfig = {
    tool_call_accuracy: { strict_order: strictOrderAcc.value },
    tool_call_f1: { strict_order: strictOrderF1.value },
    step_efficiency: { penalty_factor: penaltyFactor.value },
  }

  loading.value = true
  try {
    const res = await createRun({
      agent_endpoint: form.value.agent_endpoint,
      agent_version: form.value.agent_version || undefined,
      dataset_id: form.value.dataset_id,
      metrics: form.value.selectedMetrics.length ? form.value.selectedMetrics : undefined,
      metric_config,
    })
    ElMessage.success('Run 已发起')
    router.push(`/runs/${res.id}`)
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { error?: string } } })?.response?.data?.error ?? '发起失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const [d, m] = await Promise.all([
    listDatasets({ page: 1, page_size: 100 }),
    listMetrics(),
  ])
  datasets.value = d.items
  metrics.value  = m
  form.value.selectedMetrics = m.map(x => x.name)
})
</script>

<template>
  <div>
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>发起批次 Run</h2>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">基本信息</span></template>
          <el-form :model="form" label-width="140px" size="default">
            <el-form-item label="Agent Endpoint" required>
              <el-input v-model="form.agent_endpoint" placeholder="http://your-agent/chat" />
            </el-form-item>
            <el-form-item label="Agent 版本">
              <el-input v-model="form.agent_version" placeholder="v1.0（可选）" />
            </el-form-item>
            <el-form-item label="数据集" required>
              <el-select v-model="form.dataset_id" placeholder="选择数据集" style="width:100%">
                <el-option
                  v-for="d in datasets"
                  :key="d.id"
                  :value="d.id"
                  :label="`${d.name}（${d.case_count} 条）`"
                />
              </el-select>
              <div style="margin-top:4px">
                <el-button link type="primary" size="small" @click="router.push('/datasets/new')">上传新数据集</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">选择评估指标</span>
              <span style="font-size:12px;color:#909399">{{ form.selectedMetrics.length }} / {{ metrics.length }} 已选</span>
            </div>
          </template>
          <MetricSelector v-model="form.selectedMetrics" :metrics="metrics" />
        </el-card>

        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">指标配置</span></template>
          <el-form label-width="150px" size="small">
            <el-form-item label="tool_call_accuracy 严格顺序">
              <el-switch v-model="strictOrderAcc" />
              <span v-if="strictOrderAcc" style="font-size:12px;color:#f56c6c;margin-left:6px">⚠ LangGraph 请关闭</span>
            </el-form-item>
            <el-form-item label="tool_call_f1 严格顺序">
              <el-switch v-model="strictOrderF1" />
            </el-form-item>
            <el-form-item label="步骤效率惩罚系数">
              <el-input-number v-model="penaltyFactor" :min="0.01" :max="1" :step="0.05" :precision="2" style="width:130px" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">
          发起 Run
        </el-button>
      </el-col>
    </el-row>
  </div>
</template>
