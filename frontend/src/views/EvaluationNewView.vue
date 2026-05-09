<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createEvaluation, listMetrics } from '@/api'
import type { MetricDescriptor, MetricConfig } from '@/types'
import { ElMessage } from 'element-plus'
import MetricSelector from '@/components/MetricSelector.vue'

const router = useRouter()
const metrics = ref<MetricDescriptor[]>([])
const selectedMetrics = ref<string[]>([])
const loading = ref(false)

const traceJson = ref(`{
  "task": "查询北京明天天气",
  "steps": [
    { "type": "thought", "content": "需要调用天气工具" },
    { "type": "tool_call", "tool": "weather_api", "input": { "city": "北京" } },
    { "type": "tool_result", "output": { "temp": 22, "condition": "晴" } },
    { "type": "final_answer", "content": "明天北京22度，晴天" }
  ]
}`)

// 一个参考答案字段，同时用于 LLM 评判和词汇匹配
const refAnswer       = ref('')
const refExpectedTools = ref('')
const refMaxSteps     = ref<number | undefined>(undefined)

const strictOrderAcc = ref(false)
const strictOrderF1  = ref(false)
const penaltyFactor  = ref(0.1)

async function submit() {
  let trace: unknown
  try { trace = JSON.parse(traceJson.value) }
  catch { ElMessage.error('Trace JSON 格式有误'); return }

  const reference: Record<string, unknown> = {}
  if (refAnswer.value) {
    reference.reference       = refAnswer.value
    reference.expected_answer = refAnswer.value
  }
  if (refExpectedTools.value)
    reference.expected_tools = refExpectedTools.value.split(',').map(s => s.trim()).filter(Boolean)
  if (refMaxSteps.value)
    reference.expected_max_steps = refMaxSteps.value

  const metric_config: MetricConfig = {
    tool_call_accuracy: { strict_order: strictOrderAcc.value },
    tool_call_f1: { strict_order: strictOrderF1.value },
    step_efficiency: { penalty_factor: penaltyFactor.value },
  }

  loading.value = true
  try {
    const res = await createEvaluation({
      trace: trace as never,
      reference: Object.keys(reference).length ? reference as never : undefined,
      metrics: selectedMetrics.value.length ? selectedMetrics.value : undefined,
      metric_config,
    })
    ElMessage.success('评估完成')
    router.push(`/evaluations/${res.id}`)
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { error?: string } } })?.response?.data?.error ?? '提交失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  metrics.value = await listMetrics()
  selectedMetrics.value = metrics.value.map((m: MetricDescriptor) => m.name)
})
</script>

<template>
  <div>
    <!-- Header -->
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:10px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div>
          <h2 style="margin:0">单次评估</h2>
          <p class="page-sub">直接粘贴已有 Trace JSON 进行评估，无需启动 Agent 接口</p>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- Left: Trace + Reference -->
      <el-col :span="14">
        <el-card shadow="never" style="margin-bottom:14px">
          <template #header><span class="card-title">Trace JSON</span></template>
          <el-input
            v-model="traceJson"
            type="textarea"
            :rows="14"
            class="trace-input"
            placeholder="粘贴 Agent 执行的 Trace JSON"
          />
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div style="display:flex;align-items:center;gap:8px">
              <span class="card-title">参考信息</span>
              <el-tag size="small" type="info">可选</el-tag>
            </div>
          </template>
          <el-form label-width="100px" size="default">
            <el-form-item label="参考答案">
              <el-input
                v-model="refAnswer"
                placeholder="期望的正确答案，用于评判任务合规度和目标达成"
              />
              <div class="field-hint">同时用于 LLM 评判和词汇重叠计算</div>
            </el-form-item>
            <el-form-item label="期望工具">
              <el-input
                v-model="refExpectedTools"
                placeholder="逗号分隔，如 weather_api,search_api"
              />
            </el-form-item>
            <el-form-item label="最大步骤数">
              <el-input-number
                v-model="refMaxSteps" :min="1" :precision="0"
                placeholder="不填则自动估算" style="width:160px"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- Right: 指标 + 配置 + 提交 -->
      <el-col :span="10">
        <el-card shadow="never" style="margin-bottom:14px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span class="card-title">评估指标</span>
              <span style="font-size:12px;color:#94a3b8">{{ selectedMetrics.length }} / {{ metrics.length }} 已选</span>
            </div>
          </template>
          <MetricSelector v-model="selectedMetrics" :metrics="metrics" />
        </el-card>

        <el-card shadow="never" style="margin-bottom:14px">
          <template #header><span class="card-title">指标配置</span></template>
          <el-form label-width="130px" size="small">
            <el-form-item label="工具调用严格顺序">
              <div style="display:flex;align-items:center;gap:8px">
                <el-switch v-model="strictOrderAcc" />
                <span style="font-size:12px;color:#94a3b8">accuracy</span>
                <el-switch v-model="strictOrderF1" />
                <span style="font-size:12px;color:#94a3b8">f1</span>
              </div>
            </el-form-item>
            <el-form-item label="步骤惩罚系数">
              <el-input-number v-model="penaltyFactor" :min="0.01" :max="1" :step="0.05" :precision="2" style="width:130px" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-button type="primary" size="large" style="width:100%;font-size:15px" :loading="loading" @click="submit">
          提交评估
        </el-button>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.card-title { font-weight: 600; font-size: 14px; color: #0f172a; }
.page-sub   { margin: 2px 0 0; font-size: 12px; color: #94a3b8; font-weight: 400; }
.field-hint { margin-top: 4px; font-size: 11px; color: #94a3b8; }
.trace-input :deep(textarea) { font-family: 'Menlo', 'Consolas', monospace; font-size: 12.5px; }
</style>
