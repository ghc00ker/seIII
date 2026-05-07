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

// Trace JSON
const traceJson = ref(`{
  "task": "查询北京明天天气",
  "steps": [
    { "type": "thought", "content": "需要调用天气工具" },
    { "type": "tool_call", "tool": "weather_api", "input": { "city": "北京" } },
    { "type": "tool_result", "output": { "temp": 22, "condition": "晴" } },
    { "type": "final_answer", "content": "明天北京22度，晴天" }
  ]
}`)

// Reference
const refExpectedAnswer = ref('')
const refReference      = ref('')
const refExpectedTools  = ref('')
const refMaxSteps       = ref<number | undefined>(undefined)

// MetricConfig
const strictOrderAcc  = ref(false)
const strictOrderF1   = ref(false)
const penaltyFactor   = ref(0.1)

async function submit() {
  let trace: unknown
  try { trace = JSON.parse(traceJson.value) }
  catch { ElMessage.error('Trace JSON 格式有误'); return }

  const reference: Record<string, unknown> = {}
  if (refReference.value) reference.reference = refReference.value
  if (refExpectedAnswer.value) reference.expected_answer = refExpectedAnswer.value
  if (refExpectedTools.value) reference.expected_tools = refExpectedTools.value.split(',').map(s => s.trim()).filter(Boolean)
  if (refMaxSteps.value) reference.expected_max_steps = refMaxSteps.value

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
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>提交单次评估</h2>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <!-- Trace -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">Trace（JSON）</span></template>
          <el-input
            v-model="traceJson"
            type="textarea"
            :rows="16"
            style="font-family:monospace;font-size:13px"
            placeholder="粘贴 Trace JSON"
          />
        </el-card>

        <!-- Reference -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">Reference（可选）</span></template>
          <el-form label-width="120px" size="default">
            <el-form-item label="金标参考答案">
              <el-input v-model="refReference" placeholder="用于 goal_completion LLM 判断" />
            </el-form-item>
            <el-form-item label="期望答案">
              <el-input v-model="refExpectedAnswer" placeholder="expected_answer" />
            </el-form-item>
            <el-form-item label="期望工具">
              <el-input v-model="refExpectedTools" placeholder="逗号分隔，如 weather_api,search_api" />
            </el-form-item>
            <el-form-item label="最大步骤数">
              <el-input-number v-model="refMaxSteps" :min="1" :precision="0" placeholder="不填则启发式估算" style="width:160px" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="10">
        <!-- Metrics selector -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">选择评估指标</span>
              <span style="font-size:12px;color:#909399">{{ selectedMetrics.length }} / {{ metrics.length }} 已选</span>
            </div>
          </template>
          <MetricSelector v-model="selectedMetrics" :metrics="metrics" />
        </el-card>

        <!-- MetricConfig -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header><span style="font-weight:600">指标配置</span></template>
          <el-form label-width="150px" size="small">
            <el-form-item label="tool_call_accuracy 严格顺序">
              <el-switch v-model="strictOrderAcc" />
              <span style="font-size:12px;color:#f56c6c;margin-left:8px" v-if="strictOrderAcc">
                ⚠ LangGraph 请关闭
              </span>
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
          提交评估
        </el-button>
      </el-col>
    </el-row>
  </div>
</template>
