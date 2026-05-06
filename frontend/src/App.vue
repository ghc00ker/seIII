<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/evaluations')) return '/evaluations'
  if (p.startsWith('/runs'))        return '/runs'
  if (p.startsWith('/datasets'))    return '/datasets'
  return p
})
</script>

<template>
  <el-container style="min-height:100vh">
    <!-- Sidebar -->
    <el-aside width="220px" style="background:#1a1a2e;position:fixed;top:0;left:0;height:100vh;z-index:100">
      <div style="padding:20px 24px 12px;border-bottom:1px solid #2e2e4e">
        <div style="color:#fff;font-size:16px;font-weight:700;letter-spacing:.5px">Agent Eval</div>
        <div style="color:#888;font-size:12px;margin-top:4px">评估平台</div>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#1a1a2e"
        text-color="#adb5bd"
        active-text-color="#409eff"
        :router="true"
        style="border:none;margin-top:8px"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>概览</span>
        </el-menu-item>

        <div style="padding:16px 24px 6px;color:#555;font-size:11px;text-transform:uppercase;letter-spacing:.8px">评估</div>
        <el-menu-item index="/evaluations">
          <el-icon><List /></el-icon>
          <span>评估记录</span>
        </el-menu-item>
        <el-menu-item index="/evaluations/new">
          <el-icon><Plus /></el-icon>
          <span>提交评估</span>
        </el-menu-item>
        <el-menu-item index="/evaluations/compare">
          <el-icon><Switch /></el-icon>
          <span>对比分析</span>
        </el-menu-item>

        <div style="padding:16px 24px 6px;color:#555;font-size:11px;text-transform:uppercase;letter-spacing:.8px">批次</div>
        <el-menu-item index="/runs">
          <el-icon><VideoPlay /></el-icon>
          <span>Run 列表</span>
        </el-menu-item>
        <el-menu-item index="/runs/new">
          <el-icon><CirclePlus /></el-icon>
          <span>发起 Run</span>
        </el-menu-item>

        <div style="padding:16px 24px 6px;color:#555;font-size:11px;text-transform:uppercase;letter-spacing:.8px">数据</div>
        <el-menu-item index="/datasets">
          <el-icon><FolderOpened /></el-icon>
          <span>数据集</span>
        </el-menu-item>
        <el-menu-item index="/datasets/new">
          <el-icon><Upload /></el-icon>
          <span>上传数据集</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main -->
    <el-main style="margin-left:220px;padding:28px 32px;min-height:100vh;background:#f5f7fa">
      <router-view />
    </el-main>
  </el-container>
</template>
