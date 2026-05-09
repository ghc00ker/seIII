<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listEvaluations, listRuns, listDatasets } from '@/api'
import type { EvaluationListItem, RunListItem } from '@/types'

const router = useRouter()

const stats      = ref({ evaluations: 0, runs: 0, datasets: 0, avgScore: 0 })
const recentEvals = ref<EvaluationListItem[]>([])
const recentRuns  = ref<RunListItem[]>([])
const loading     = ref(true)

function scoreClass(s: number) {
  return s >= 0.8 ? 'score-high' : s >= 0.6 ? 'score-mid' : 'score-low'
}
function fmt(iso: string) {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

const statusMap: Record<string, { label: string; type: 'success'|'warning'|'info'|'danger' }> = {
  completed: { label: '完成',  type: 'success' },
  running:   { label: '运行中', type: 'warning' },
  pending:   { label: '等待',  type: 'info' },
  failed:    { label: '失败',  type: 'danger' },
}

const isEmpty = ref(false)

onMounted(async () => {
  try {
    const [eData, rData, dData] = await Promise.all([
      listEvaluations({ page: 1, page_size: 5 }),
      listRuns({ page: 1, page_size: 5 }),
      listDatasets({ page: 1, page_size: 1 }),
    ])
    stats.value = {
      evaluations: eData.total,
      runs: rData.total,
      datasets: dData.total,
      avgScore: eData.items.length
        ? eData.items.reduce((a, b) => a + b.overall_score, 0) / eData.items.length
        : 0,
    }
    recentEvals.value = eData.items
    recentRuns.value  = rData.items
    isEmpty.value = dData.total === 0 && rData.total === 0
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="dashboard">

    <!-- ── Hero Banner ── -->
    <div class="hero">
      <div class="hero-content">
        <div class="hero-text">
          <div class="hero-eyebrow">智能体评估平台</div>
          <h1 class="hero-title">Agent Eval</h1>
          <p class="hero-desc">端到端测评你的 AI Agent，覆盖任务合规、目标达成、工具调用与响应质量</p>
          <div class="hero-actions">
            <button class="hero-btn hero-btn-solid" @click="router.push('/runs/new')">
              <el-icon style="margin-right:6px"><VideoPlay /></el-icon> 发起新 Run
            </button>
            <button class="hero-btn hero-btn-outline" @click="router.push('/datasets/new')">
              <el-icon style="margin-right:6px"><Upload /></el-icon> 上传数据集
            </button>
          </div>
        </div>
        <div class="hero-visual">
          <div class="hero-rings">
            <div class="ring r1" />
            <div class="ring r2" />
            <div class="ring r3" />
          </div>
          <div class="hero-badge">
            <el-icon size="32" color="#fff"><TrendCharts /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Stat cards ── -->
    <el-row :gutter="16" style="margin-bottom:28px">
      <el-col :span="6">
        <div class="stat-card" @click="router.push('/evaluations')" style="cursor:pointer">
          <div class="stat-icon-wrap blue"><el-icon><Document /></el-icon></div>
          <div>
            <div class="stat-num">{{ stats.evaluations }}</div>
            <div class="stat-label">评估总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" @click="router.push('/runs')" style="cursor:pointer">
          <div class="stat-icon-wrap green"><el-icon><Cpu /></el-icon></div>
          <div>
            <div class="stat-num">{{ stats.runs }}</div>
            <div class="stat-label">Run 总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" @click="router.push('/datasets')" style="cursor:pointer">
          <div class="stat-icon-wrap orange"><el-icon><FolderOpened /></el-icon></div>
          <div>
            <div class="stat-num">{{ stats.datasets }}</div>
            <div class="stat-label">数据集数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap purple"><el-icon><TrendCharts /></el-icon></div>
          <div>
            <div class="stat-num">
              {{ (stats.avgScore * 100).toFixed(0) }}<span class="stat-unit">%</span>
            </div>
            <div class="stat-label">近期平均分</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ── Getting started guide（首次使用时显示） ── -->
    <div v-if="isEmpty" style="margin-bottom:28px">
      <div class="section-title">快速开始</div>
      <div class="wf-guide">
        <div class="wf-guide-card wf-guide-card--1" @click="router.push('/datasets/new')">
          <div class="wf-guide-step">Step 1</div>
          <div class="wf-guide-title">上传数据集</div>
          <div class="wf-guide-desc">准备测试用例 JSON 文件，支持批量导入</div>
          <span class="wf-guide-arrow">→</span>
        </div>
        <div class="wf-guide-card wf-guide-card--2" @click="router.push('/runs/new')">
          <div class="wf-guide-step">Step 2</div>
          <div class="wf-guide-title">发起批次 Run</div>
          <div class="wf-guide-desc">选择数据集和指标，填写 Agent 接口地址</div>
          <span class="wf-guide-arrow">→</span>
        </div>
        <div class="wf-guide-card wf-guide-card--3" @click="router.push('/runs')">
          <div class="wf-guide-step">Step 3</div>
          <div class="wf-guide-title">查看评估结果</div>
          <div class="wf-guide-desc">查看各指标得分、Trace 详情与对比分析</div>
          <span class="wf-guide-arrow">→</span>
        </div>
      </div>
    </div>

    <!-- ── Recent data ── -->
    <el-row :gutter="16">
      <!-- Recent evaluations -->
      <el-col :span="15">
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="card-hd">
              <span class="card-hd-title">最近评估</span>
              <span class="card-link" @click="router.push('/evaluations')">查看全部 →</span>
            </div>
          </template>
          <el-table :data="recentEvals" size="small" style="--el-table-header-bg-color:#f8fafc">
            <el-table-column prop="task" label="任务" show-overflow-tooltip min-width="180" />
            <el-table-column label="综合分" width="88" align="center">
              <template #default="{ row }">
                <span :class="['score-text', scoreClass(row.overall_score)]">
                  {{ (row.overall_score * 100).toFixed(1) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="148">
              <template #default="{ row }">
                <span style="color:#94a3b8;font-size:12px">{{ fmt(row.created_at) }}</span>
              </template>
            </el-table-column>
            <el-table-column width="58" fixed="right">
              <template #default="{ row }">
                <span class="card-link" @click="router.push(`/evaluations/${row.id}`)">详情</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!recentEvals.length" class="empty-tip">
            暂无评估记录，先上传数据集并发起 Run
          </div>
        </el-card>
      </el-col>

      <!-- Recent runs -->
      <el-col :span="9">
        <el-card shadow="never" class="data-card">
          <template #header>
            <div class="card-hd">
              <span class="card-hd-title">最近 Run</span>
              <span class="card-link" @click="router.push('/runs')">查看全部 →</span>
            </div>
          </template>
          <div
            v-for="run in recentRuns" :key="run.id"
            class="run-item"
            @click="router.push(`/runs/${run.id}`)"
          >
            <el-tag :type="statusMap[run.status]?.type ?? 'info'" size="small" style="flex-shrink:0">
              {{ statusMap[run.status]?.label ?? run.status }}
            </el-tag>
            <span class="run-endpoint">{{ run.agent_endpoint }}</span>
            <span :class="['score-text', scoreClass(run.summary?.overall_score ?? 0)]" style="flex-shrink:0;font-size:13px">
              {{ ((run.summary?.overall_score ?? 0) * 100).toFixed(0) }}%
            </span>
          </div>
          <div v-if="!recentRuns.length" class="empty-tip">暂无 Run 记录</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard { padding-bottom: 32px; }

/* ── Hero ── */
.hero {
  background: linear-gradient(135deg, #4338ca 0%, #6d28d9 55%, #7e22ce 100%);
  border-radius: 16px;
  margin-bottom: 24px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8px 32px rgba(99,102,241,.25);
}
.hero::after {
  content: '';
  position: absolute; inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23fff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
}
.hero-content {
  display: flex; align-items: center; justify-content: space-between;
  padding: 32px 36px;
  position: relative; z-index: 1;
}
.hero-text { flex: 1; }
.hero-eyebrow {
  font-size: 11px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 1.5px; color: rgba(255,255,255,0.65); margin-bottom: 10px;
}
.hero-title {
  margin: 0 0 10px;
  font-size: 32px; font-weight: 800;
  color: #fff; letter-spacing: -0.5px; line-height: 1.1;
}
.hero-desc {
  margin: 0 0 22px;
  font-size: 14px; color: rgba(255,255,255,0.75);
  max-width: 460px; line-height: 1.6;
}
.hero-actions { display: flex; gap: 10px; }
.hero-btn {
  display: inline-flex; align-items: center;
  padding: 9px 18px; border-radius: 8px;
  font-size: 14px; font-weight: 600;
  cursor: pointer; border: 1.5px solid rgba(255,255,255,0.35);
  transition: all .15s; outline: none; font-family: inherit;
  color: #fff;
}
.hero-btn-solid {
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(4px);
}
.hero-btn-solid:hover { background: rgba(255,255,255,0.28); }
.hero-btn-outline {
  background: transparent;
}
.hero-btn-outline:hover { background: rgba(255,255,255,0.1); }
/* Hero decorative rings */
.hero-visual {
  flex-shrink: 0;
  width: 120px; height: 120px;
  position: relative; display: flex;
  align-items: center; justify-content: center;
}
.hero-rings { position: absolute; inset: 0; }
.ring {
  position: absolute; border-radius: 50%;
  border: 1.5px solid rgba(255,255,255,0.15);
}
.r1 { inset: 0; }
.r2 { inset: 15px; }
.r3 { inset: 30px; }
.hero-badge {
  width: 56px; height: 56px; border-radius: 14px;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  position: relative; z-index: 1;
}

/* ── Stat cards (horizontal) ── */
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  display: flex; align-items: center; gap: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: transform .2s, box-shadow .2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}
.stat-unit { font-size: 18px; font-weight: 600; }

/* ── Section title ── */
.section-title {
  font-size: 13px; color: #64748b; font-weight: 600;
  margin-bottom: 12px; text-transform: uppercase; letter-spacing: .6px;
}

/* ── Data cards ── */
.data-card { border-radius: 12px !important; }
.card-hd {
  display: flex; justify-content: space-between; align-items: center;
}
.card-hd-title { font-weight: 700; color: #0f172a; font-size: 14px; }

/* ── Run item ── */
.run-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background .15s;
}
.run-item:last-child { border-bottom: none; }
.run-item:hover { background: #f8fafc; border-radius: 6px; padding-left: 4px; }
.run-endpoint {
  flex: 1; font-size: 12px; color: #475569;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* ── Empty tip ── */
.empty-tip {
  text-align: center; padding: 32px;
  color: #94a3b8; font-size: 13px;
}

/* ── Card header link ── */
.card-link {
  font-size: 12px; color: #6366f1; font-weight: 500;
  cursor: pointer; transition: color .15s;
}
.card-link:hover { color: #4f46e5; }
</style>
