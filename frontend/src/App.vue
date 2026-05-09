<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route  = useRoute()
const router = useRouter()

function go(path: string) { router.push(path) }
function isExact(path: string) { return route.path === path }

function activeStep() {
  if (route.path.startsWith('/datasets')) return 1
  if (route.path === '/runs/new' || route.path === '/evaluations/new') return 2
  if (route.path.startsWith('/runs') || route.path.startsWith('/evaluations') || route.path === '/dashboard') return 3
  return 0
}
</script>

<template>
  <div class="app-shell">
    <div class="app-body">

      <!-- ── Sidebar ─────────────────────────────────────── -->
      <nav class="sidebar">

        <!-- Brand -->
        <div class="sb-brand" @click="go('/dashboard')">
          <div class="sb-logo">AE</div>
          <div>
            <div class="sb-name">Agent Eval</div>
            <div class="sb-sub">智能体评估平台</div>
          </div>
        </div>

        <div class="nav-grp">
          <div class="nav-item" :class="{ active: isExact('/dashboard') }" @click="go('/dashboard')">
            <el-icon><Odometer /></el-icon><span>概览</span>
          </div>
        </div>

        <div class="s-divider">工作流程</div>

        <!-- Step 1 -->
        <div class="wf-step" :class="{ on: activeStep() === 1 }">
          <div class="wf-hd">
            <span class="wf-dot d1">1</span>
            <span class="wf-lbl">准备数据</span>
            <span class="wf-tag">先做</span>
          </div>
          <div class="wf-items">
            <div class="nav-item sub" :class="{ active: isExact('/datasets') }" @click="go('/datasets')">
              <el-icon><FolderOpened /></el-icon><span>数据集列表</span>
            </div>
            <div class="nav-item sub" :class="{ active: isExact('/datasets/new') }" @click="go('/datasets/new')">
              <el-icon><Upload /></el-icon><span>上传数据集</span>
            </div>
          </div>
        </div>
        <div class="wf-wire" />

        <!-- Step 2 -->
        <div class="wf-step" :class="{ on: activeStep() === 2 }">
          <div class="wf-hd">
            <span class="wf-dot d2">2</span>
            <span class="wf-lbl">发起测评</span>
          </div>
          <div class="wf-items">
            <div class="nav-item sub" :class="{ active: isExact('/runs/new') }" @click="go('/runs/new')">
              <el-icon><Cpu /></el-icon><span>批次 Run</span>
              <span class="nav-pill">推荐</span>
            </div>
            <div class="nav-item sub" :class="{ active: isExact('/evaluations/new') }" @click="go('/evaluations/new')">
              <el-icon><EditPen /></el-icon><span>单次评估</span>
            </div>
          </div>
        </div>
        <div class="wf-wire" />

        <!-- Step 3 -->
        <div class="wf-step" :class="{ on: activeStep() === 3 }">
          <div class="wf-hd">
            <span class="wf-dot d3">3</span>
            <span class="wf-lbl">分析结果</span>
          </div>
          <div class="wf-items">
            <div class="nav-item sub" :class="{ active: isExact('/runs') }" @click="go('/runs')">
              <el-icon><DataLine /></el-icon><span>Run 列表</span>
            </div>
            <div class="nav-item sub" :class="{ active: isExact('/evaluations') }" @click="go('/evaluations')">
              <el-icon><List /></el-icon><span>评估记录</span>
            </div>
            <div class="nav-item sub" :class="{ active: isExact('/evaluations/compare') }" @click="go('/evaluations/compare')">
              <el-icon><Switch /></el-icon><span>对比分析</span>
            </div>
          </div>
        </div>

        <div class="s-footer">
          <el-icon size="12"><InfoFilled /></el-icon> 按步骤操作效果最佳
        </div>
      </nav>

      <!-- ── Main ───────────────────────────────────────── -->
      <main class="main-wrap">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; display: flex; flex-direction: column; background: #f2f3f5; }
.app-body  { display: flex; flex: 1; }

/* ── Sidebar ── */
.sidebar {
  width: 250px;
  position: fixed; top: 0; left: 0;
  height: 100vh;
  background: #0f172a;
  overflow-y: auto; overflow-x: hidden;
  z-index: 100;
  display: flex; flex-direction: column;
  border-right: 1px solid rgba(255,255,255,0.04);
}
.sidebar::-webkit-scrollbar { width: 3px; }
.sidebar::-webkit-scrollbar-thumb { background: #1e293b; border-radius: 2px; }

/* Brand */
.sb-brand {
  padding: 16px 14px 14px;
  display: flex; align-items: center; gap: 10px;
  cursor: pointer; flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  transition: opacity .15s;
}
.sb-brand:hover { opacity: .85; }
.sb-logo {
  width: 32px; height: 32px; border-radius: 8px; flex-shrink: 0;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 800; color: #fff;
  box-shadow: 0 3px 10px rgba(99,102,241,.35);
}
.sb-name { font-size: 13px; font-weight: 700; color: #e2e8f0; line-height: 1.3; }
.sb-sub  { font-size: 10px; color: #475569; }

/* Nav */
.nav-grp { padding: 10px 10px 6px; }
.nav-item {
  display: flex; align-items: center; gap: 9px;
  padding: 9px 12px; border-radius: 8px;
  cursor: pointer; color: #64748b;
  font-size: 14px; transition: all .15s; user-select: none; position: relative;
}
.nav-item:hover { background: rgba(255,255,255,0.06); color: #cbd5e1; }
.nav-item.active { background: rgba(99,102,241,.15); color: #818cf8; }
.nav-item.active .el-icon { color: #818cf8; }
.nav-item.sub { padding: 8px 10px 8px 12px; font-size: 13px; margin-left: 2px; }
.nav-pill {
  margin-left: auto; font-size: 10px; font-weight: 600;
  background: rgba(99,102,241,.2); color: #818cf8;
  padding: 1px 6px; border-radius: 10px;
}

.s-divider {
  padding: 14px 18px 8px;
  color: #1e293b; font-size: 11px; font-weight: 700;
  text-transform: uppercase; letter-spacing: 1.2px;
}

/* Workflow steps */
.wf-step { padding: 2px 12px; }
.wf-hd   { display: flex; align-items: center; gap: 8px; padding: 5px 4px; }
.wf-dot  {
  width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: #fff;
  transition: box-shadow .2s;
}
.d1 { background: #0891b2; }
.d2 { background: #7c3aed; }
.d3 { background: #059669; }
.wf-step.on .wf-dot { box-shadow: 0 0 0 4px rgba(99,102,241,.2); }
.wf-lbl {
  color: #334155; font-size: 11px; font-weight: 700;
  text-transform: uppercase; letter-spacing: .7px; flex: 1;
}
.wf-step.on .wf-lbl { color: #94a3b8; }
.wf-tag {
  font-size: 10px; color: #0891b2;
  background: rgba(8,145,178,.15); padding: 1px 6px; border-radius: 8px;
}
.wf-items { padding-left: 4px; margin-left: 11px; border-left: 1.5px solid #1e293b; padding-bottom: 4px; }
.wf-wire  { width: 1.5px; height: 10px; background: #1e293b; margin-left: 21px; }

.s-footer {
  margin-top: auto; padding: 14px 18px;
  border-top: 1px solid rgba(255,255,255,0.04);
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #334155; flex-shrink: 0;
}

/* Main */
.main-wrap {
  margin-left: 250px; flex: 1;
  padding: 28px 32px;
  min-height: 100vh;
  background: #f2f3f5;
}
</style>
