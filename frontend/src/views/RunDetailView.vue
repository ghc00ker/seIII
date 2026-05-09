<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRun, listEvaluations } from '@/api'
import type { RunResult, EvaluationListItem, RunStatus } from '@/types'
import * as echarts from 'echarts/core'
import { RadarChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, RadarComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  RadarChart, BarChart, PieChart,
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, RadarComponent,
  CanvasRenderer,
])

const route   = useRoute()
const router  = useRouter()
const run     = ref<RunResult | null>(null)
const evals   = ref<EvaluationListItem[]>([])
const loading = ref(true)

const radarEl = ref<HTMLElement | null>(null)
const donutEl = ref<HTMLElement | null>(null)
const barEl   = ref<HTMLElement | null>(null)

const statusMap: Record<RunStatus, { label: string; type: 'success'|'warning'|'info'|'danger' }> = {
  completed: { label: '完成',  type: 'success' },
  running:   { label: '运行中', type: 'warning' },
  pending:   { label: '等待',  type: 'info' },
  failed:    { label: '失败',  type: 'danger' },
}
const metricLabels: Record<string, string> = {
  goal_completion:    '目标达成',
  tool_call_accuracy: '工具准确',
  tool_call_f1:       '工具 F1',
  step_efficiency:    '步骤效率',
  answer_faithfulness:'答案忠实',
  task_adherence:     '任务合规',
}

function scoreClass(s: number) { return s >= 0.8 ? 'score-high' : s >= 0.6 ? 'score-mid' : 'score-low' }
function fmt(iso: string) { return new Date(iso).toLocaleString('zh-CN', { hour12: false }) }
function progressPct(r: RunResult) {
  if (!r.total_cases) return 0
  return Math.round((r.completed_cases / r.total_cases) * 100)
}

function initCharts(r: RunResult, evalList: EvaluationListItem[]) {
  const scores = r.summary?.metric_scores ?? {}
  const keys   = Object.keys(scores)
  if (!keys.length) return

  nextTick(() => {
    // ── Radar ──
    if (radarEl.value) {
      const rc = echarts.init(radarEl.value)
      rc.setOption({
        tooltip: { trigger: 'item' },
        radar: {
          indicator: keys.map(k => ({
            name: metricLabels[k] ?? k,
            max: 100,
          })),
          shape: 'polygon',
          splitNumber: 4,
          axisName: { color: '#6b7280', fontSize: 12 },
          splitLine: { lineStyle: { color: '#e5e7eb' } },
          splitArea: { areaStyle: { color: ['#fafbfc', '#f3f4f6'] } },
          axisLine: { lineStyle: { color: '#e5e7eb' } },
        },
        series: [{
          type: 'radar',
          data: [{
            value: keys.map(k => Math.round(scores[k] * 100)),
            name: '指标均分',
            areaStyle: { color: 'rgba(99,102,241,0.15)' },
            lineStyle: { color: '#6366f1', width: 2 },
            itemStyle: { color: '#6366f1' },
          }],
        }],
      })
    }

    // ── Donut (score distribution) ──
    if (donutEl.value) {
      const high = evalList.filter(e => e.overall_score >= 0.8).length
      const mid  = evalList.filter(e => e.overall_score >= 0.6 && e.overall_score < 0.8).length
      const low  = evalList.filter(e => e.overall_score < 0.6).length
      const dc = echarts.init(donutEl.value)
      dc.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
        legend: {
          bottom: 4, left: 'center',
          textStyle: { color: '#6b7280', fontSize: 11 },
          itemWidth: 10, itemHeight: 10,
        },
        series: [{
          type: 'pie', radius: ['42%', '68%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: false,
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 700 } },
          data: [
            { value: high, name: '优秀 ≥80%', itemStyle: { color: '#22c55e' } },
            { value: mid,  name: '良好 60-80%', itemStyle: { color: '#f59e0b' } },
            { value: low,  name: '待改进 <60%', itemStyle: { color: '#f87171' } },
          ],
        }],
      })
    }

    // ── Bar (per-eval scores) ──
    if (barEl.value && evalList.length) {
      const sorted = [...evalList].sort((a, b) =>
        new Date(a.created_at).getTime() - new Date(b.created_at).getTime()
      )
      const bc = echarts.init(barEl.value)
      bc.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: (params: { name: string; value: number }[]) => {
            const p = params[0]
            return `${p.name}<br/>综合分: <b>${p.value.toFixed(1)}%</b>`
          },
        },
        grid: { left: 10, right: 10, top: 10, bottom: 24, containLabel: true },
        xAxis: {
          type: 'category',
          data: sorted.map((_, i) => `#${i + 1}`),
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          axisLabel: { color: '#9ca3af', fontSize: 11 },
        },
        yAxis: {
          type: 'value', min: 0, max: 100,
          axisLabel: { color: '#9ca3af', fontSize: 11, formatter: '{value}%' },
          splitLine: { lineStyle: { color: '#f3f4f6' } },
        },
        series: [{
          type: 'bar', barMaxWidth: 28,
          data: sorted.map(e => ({
            value: +(e.overall_score * 100).toFixed(1),
            itemStyle: {
              color: e.overall_score >= 0.8 ? '#6366f1'
                   : e.overall_score >= 0.6 ? '#f59e0b' : '#f87171',
              borderRadius: [4, 4, 0, 0],
            },
          })),
        }],
      })
    }
  })
}

onMounted(async () => {
  try {
    const runId = route.params.id as string
    const [r, e] = await Promise.all([
      getRun(runId),
      listEvaluations({ run_id: runId, page: 1, page_size: 100 }),
    ])
    run.value   = r
    evals.value = e.items
    initCharts(r, e.items)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:10px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>Run 详情</h2>
      </div>
    </div>

    <template v-if="run">

      <!-- ── Top stat cards ── -->
      <el-row :gutter="16" style="margin-bottom:18px">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon-wrap purple"><el-icon><TrendCharts /></el-icon></div>
            <div>
              <div :class="['stat-num', scoreClass(run.summary?.overall_score ?? 0)]">
                {{ ((run.summary?.overall_score ?? 0) * 100).toFixed(1) }}<span class="stat-unit">%</span>
              </div>
              <div class="stat-label">综合得分</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon-wrap blue"><el-icon><Document /></el-icon></div>
            <div>
              <div class="stat-num">{{ run.total_cases }}</div>
              <div class="stat-label">总用例数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon-wrap green"><el-icon><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-num">{{ run.completed_cases }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon-wrap orange"><el-icon><Timer /></el-icon></div>
            <div>
              <div class="stat-num" style="font-size:18px;letter-spacing:0">{{ fmt(run.created_at).split(' ')[1] }}</div>
              <div class="stat-label">{{ fmt(run.created_at).split(' ')[0] }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- ── Info row ── -->
      <el-card shadow="never" style="margin-bottom:18px">
        <div class="info-row">
          <div class="info-item">
            <span class="info-lbl">Run ID</span>
            <code class="info-val-mono">{{ run.id }}</code>
          </div>
          <div class="info-item">
            <span class="info-lbl">Agent Endpoint</span>
            <code class="info-val-mono">{{ run.agent_endpoint }}</code>
          </div>
          <div class="info-item">
            <span class="info-lbl">数据集</span>
            <code class="info-val-mono">{{ run.dataset_id }}</code>
          </div>
          <div class="info-item">
            <span class="info-lbl">状态</span>
            <el-tag :type="statusMap[run.status]?.type" size="small">{{ statusMap[run.status]?.label }}</el-tag>
          </div>
          <div class="info-item" v-if="run.failed_cases">
            <span class="info-lbl">失败</span>
            <span style="color:#f56c6c;font-weight:600">{{ run.failed_cases }} 条</span>
          </div>
        </div>
        <div style="margin-top:14px">
          <div style="display:flex;justify-content:space-between;font-size:12px;color:#9ca3af;margin-bottom:6px">
            <span>执行进度</span>
            <span>{{ run.completed_cases }} / {{ run.total_cases }}</span>
          </div>
          <el-progress
            :percentage="progressPct(run)"
            :status="run.status === 'failed' ? 'exception' : run.status === 'completed' ? 'success' : undefined"
            :stroke-width="8"
            style="border-radius:4px"
          />
        </div>
      </el-card>

      <!-- ── Charts row ── -->
      <el-row v-if="run.summary?.metric_scores" :gutter="16" style="margin-bottom:18px">
        <!-- Radar -->
        <el-col :span="10">
          <el-card shadow="never" style="height:320px">
            <template #header><span class="card-title">指标雷达图</span></template>
            <div ref="radarEl" style="height:240px" />
          </el-card>
        </el-col>
        <!-- Donut -->
        <el-col :span="6">
          <el-card shadow="never" style="height:320px">
            <template #header><span class="card-title">评分分布</span></template>
            <div ref="donutEl" style="height:240px" />
          </el-card>
        </el-col>
        <!-- Bar -->
        <el-col :span="8">
          <el-card shadow="never" style="height:320px">
            <template #header><span class="card-title">逐条得分</span></template>
            <div ref="barEl" style="height:240px" />
          </el-card>
        </el-col>
      </el-row>

      <!-- ── Metric score strip ── -->
      <el-card v-if="run.summary?.metric_scores" shadow="never" style="margin-bottom:18px">
        <template #header><span class="card-title">各指标均分</span></template>
        <div class="metric-grid">
          <div v-for="(score, metric) in run.summary.metric_scores" :key="metric" class="metric-item">
            <div class="metric-head">
              <span class="metric-name">{{ metricLabels[metric as string] ?? metric }}</span>
              <span :class="['metric-score', scoreClass(score)]">{{ (score * 100).toFixed(1) }}%</span>
            </div>
            <div class="metric-bar-track">
              <div
                class="metric-bar-fill"
                :style="{
                  width: (score * 100) + '%',
                  background: score >= 0.8 ? '#6366f1' : score >= 0.6 ? '#f59e0b' : '#f87171'
                }"
              />
            </div>
          </div>
        </div>
      </el-card>

      <!-- ── Evaluation list ── -->
      <el-card shadow="never">
        <template #header><span class="card-title">评估记录（{{ evals.length }} 条）</span></template>
        <el-table :data="evals" size="small">
          <el-table-column prop="id" label="ID" width="160" show-overflow-tooltip />
          <el-table-column prop="task" label="任务" show-overflow-tooltip />
          <el-table-column label="综合分" width="110" align="center">
            <template #default="{ row }">
              <div class="score-chip" :class="scoreClass(row.overall_score)">
                {{ (row.overall_score * 100).toFixed(1) }}%
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">
              <span style="color:#9ca3af;font-size:12px">{{ fmt(row.created_at) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="" width="60" align="center">
            <template #default="{ row }">
              <span class="tbl-link" @click="router.push(`/evaluations/${row.id}`)">详情</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.card-title { font-weight: 600; font-size: 14px; color: #111827; }

/* Info row */
.info-row {
  display: flex; flex-wrap: wrap; gap: 20px 40px;
}
.info-item { display: flex; flex-direction: column; gap: 3px; }
.info-lbl  { font-size: 11px; color: #9ca3af; text-transform: uppercase; letter-spacing: .5px; }
.info-val-mono { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 12.5px; color: #374151; }

/* Metric grid */
.metric-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px 28px;
}
.metric-item { display: flex; flex-direction: column; gap: 6px; }
.metric-head { display: flex; justify-content: space-between; align-items: baseline; }
.metric-name { font-size: 12.5px; color: #6b7280; }
.metric-score { font-size: 14px; font-weight: 700; }
.metric-bar-track {
  height: 6px; background: #f3f4f6; border-radius: 99px; overflow: hidden;
}
.metric-bar-fill {
  height: 100%; border-radius: 99px;
  transition: width .6s cubic-bezier(.4,0,.2,1);
}

/* Score chip in table */
.score-chip {
  display: inline-block; padding: 2px 10px;
  border-radius: 99px; font-size: 12.5px; font-weight: 700;
}
.score-chip.score-high { background: #f0fdf4; color: #16a34a; }
.score-chip.score-mid  { background: #fffbeb; color: #d97706; }
.score-chip.score-low  { background: #fef2f2; color: #dc2626; }

/* Table link */
.tbl-link {
  font-size: 12.5px; color: #6366f1; font-weight: 500;
  cursor: pointer; transition: color .15s;
}
.tbl-link:hover { color: #4f46e5; }
</style>
