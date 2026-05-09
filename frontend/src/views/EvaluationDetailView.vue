<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEvaluation } from '@/api'
import type { EvaluationResult } from '@/types'
import TraceViewer from '@/components/TraceViewer.vue'
import MetricResults from '@/components/MetricResults.vue'

const route   = useRoute()
const router  = useRouter()
const ev      = ref<EvaluationResult | null>(null)
const loading = ref(true)
const activeTab = ref('trace')

function scoreClass(s: number) {
  return s >= 0.8 ? 'score-high' : s >= 0.6 ? 'score-mid' : 'score-low'
}
function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}
function shortId(id: string) {
  return '#' + id.replace(/^eval_/i, '').slice(0, 8).toUpperCase()
}
function fmtJson(v: unknown): string {
  try { return JSON.stringify(v, null, 2) } catch { return String(v) }
}

const finalAnswer = computed(() => {
  if (!ev.value) return null
  const fa = ev.value.trace.steps.find(s => s.type === 'final_answer')
  return fa?.content ?? null
})

const toolCalls = computed(() => {
  if (!ev.value) return []
  return ev.value.trace.steps.filter(s => s.type === 'tool_call')
})

onMounted(async () => {
  try {
    ev.value = await getEvaluation(route.params.id as string)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">

    <!-- ── Header ── -->
    <div class="detail-header">
      <div style="display:flex;align-items:center;gap:10px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <div v-if="ev">
          <div style="display:flex;align-items:center;gap:10px">
            <code class="eval-id">{{ shortId(ev.id) }}</code>
            <span class="eval-title">{{ ev.trace.task }}</span>
          </div>
          <div class="eval-meta">
            <span>{{ fmt(ev.created_at) }}</span>
            <span v-if="ev.run_id" class="meta-sep">·</span>
            <span
              v-if="ev.run_id"
              class="run-link"
              @click="router.push(`/runs/${ev.run_id}`)"
            >{{ ev.run_id }}</span>
          </div>
        </div>
      </div>
      <div v-if="ev" :class="['score-badge', scoreClass(ev.overall_score)]">
        {{ (ev.overall_score * 100).toFixed(1) }}%
      </div>
    </div>

    <template v-if="ev">
      <el-row :gutter="16">

        <!-- ── Left: Trace ── -->
        <el-col :span="13">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-hd">
                <el-icon style="color:#6366f1"><List /></el-icon>
                <span class="panel-title">执行轨迹</span>
                <span class="panel-sub">{{ ev.trace.steps.length }} 步</span>
              </div>
            </template>

            <!-- Task input block -->
            <div class="io-block io-block--input">
              <div class="io-block-label">
                <el-icon><Edit /></el-icon> INPUT
              </div>
              <div class="io-block-content task-content">{{ ev.trace.task }}</div>
            </div>

            <!-- Trace steps -->
            <div style="margin: 14px 0 4px">
              <TraceViewer :steps="ev.trace.steps" />
            </div>
          </el-card>
        </el-col>

        <!-- ── Right: Reference + Output + Metrics ── -->
        <el-col :span="11">

          <!-- Reference card (like LangSmith Raw Reference Output) -->
          <el-card v-if="ev.reference && Object.keys(ev.reference).length" shadow="never" class="panel-card" style="margin-bottom:14px">
            <template #header>
              <div class="panel-hd">
                <el-icon style="color:#059669"><Document /></el-icon>
                <span class="panel-title">Reference</span>
                <span class="panel-sub">标准参考</span>
              </div>
            </template>
            <div class="code-block">
              <pre class="code-pre">{{ fmtJson(ev.reference) }}</pre>
            </div>
          </el-card>

          <!-- Final output card -->
          <el-card v-if="finalAnswer" shadow="never" class="panel-card" style="margin-bottom:14px">
            <template #header>
              <div class="panel-hd">
                <el-icon style="color:#7c3aed"><Star /></el-icon>
                <span class="panel-title">最终输出</span>
                <span v-if="toolCalls.length" class="panel-sub">调用 {{ toolCalls.length }} 个工具</span>
              </div>
            </template>
            <div class="io-block io-block--output">
              <div class="io-block-label">
                <el-icon><ChatDotRound /></el-icon> OUTPUT
              </div>
              <div class="io-block-content">{{ finalAnswer }}</div>
            </div>
            <div v-if="toolCalls.length" class="tool-used-row">
              <span class="tool-used-lbl">使用工具：</span>
              <span v-for="t in toolCalls" :key="t.tool" class="tool-chip">
                <el-icon><Tools /></el-icon> {{ t.tool }}
              </span>
            </div>
          </el-card>

          <!-- Metric scores -->
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-hd">
                <el-icon style="color:#f59e0b"><TrendCharts /></el-icon>
                <span class="panel-title">指标评分</span>
                <el-tag
                  :type="ev.results.every(r => r.passed) ? 'success' : 'warning'"
                  size="small" style="margin-left:auto"
                >
                  {{ ev.results.filter(r => r.passed).length }} / {{ ev.results.length }} 通过
                </el-tag>
              </div>
            </template>
            <MetricResults :results="ev.results" />
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped>
/* ── Header ── */
.detail-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 20px; gap: 16px;
}
.eval-id {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px; background: #f3f4f6; color: #374151;
  padding: 2px 8px; border-radius: 5px; flex-shrink: 0;
}
.eval-title {
  font-size: 17px; font-weight: 700; color: #111827;
  max-width: 520px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.eval-meta {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #9ca3af; margin-top: 3px;
}
.meta-sep { color: #d1d5db; }
.run-link { color: #6366f1; cursor: pointer; }
.run-link:hover { text-decoration: underline; }
.score-badge {
  flex-shrink: 0; font-size: 22px; font-weight: 800;
  padding: 6px 18px; border-radius: 12px;
}
.score-badge.score-high { background: #f0fdf4; color: #16a34a; }
.score-badge.score-mid  { background: #fffbeb; color: #d97706; }
.score-badge.score-low  { background: #fef2f2; color: #dc2626; }

/* ── Panel ── */
.panel-card { height: 100%; }
.panel-hd {
  display: flex; align-items: center; gap: 7px;
}
.panel-title { font-weight: 600; font-size: 14px; color: #111827; }
.panel-sub   { font-size: 12px; color: #9ca3af; }

/* ── I/O blocks (LangSmith style) ── */
.io-block {
  border-radius: 8px; overflow: hidden;
  border: 1px solid #e5e7eb;
}
.io-block--input  { border-left: 3px solid #6366f1; }
.io-block--output { border-left: 3px solid #7c3aed; }
.io-block-label {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 12px;
  font-size: 10.5px; font-weight: 700; letter-spacing: .8px;
  text-transform: uppercase; color: #9ca3af;
  background: #f9fafb; border-bottom: 1px solid #f3f4f6;
}
.io-block-content {
  padding: 12px 14px; font-size: 14px; color: #1f2937;
  line-height: 1.65; white-space: pre-wrap;
}
.task-content { color: #374151; font-weight: 500; }

/* ── Code block (JSON reference) ── */
.code-block {
  background: #1e293b; border-radius: 8px;
  overflow: hidden;
}
.code-pre {
  margin: 0; padding: 14px 16px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12.5px; line-height: 1.6;
  color: #e2e8f0; overflow: auto; white-space: pre;
}

/* ── Tool chips ── */
.tool-used-row {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  margin-top: 10px; padding-top: 10px; border-top: 1px solid #f3f4f6;
}
.tool-used-lbl { font-size: 12px; color: #9ca3af; }
.tool-chip {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11.5px; font-weight: 500; color: #92400e;
  background: #fef3c7; padding: 2px 8px; border-radius: 5px;
}
</style>
