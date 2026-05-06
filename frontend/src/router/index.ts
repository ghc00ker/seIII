import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '概览' } },

    { path: '/evaluations', component: () => import('@/views/EvaluationListView.vue'), meta: { title: '评估记录' } },
    { path: '/evaluations/new', component: () => import('@/views/EvaluationNewView.vue'), meta: { title: '提交评估' } },
    { path: '/evaluations/compare', component: () => import('@/views/EvaluationCompareView.vue'), meta: { title: '对比分析' } },
    { path: '/evaluations/:id', component: () => import('@/views/EvaluationDetailView.vue'), meta: { title: '评估详情' } },

    { path: '/runs', component: () => import('@/views/RunListView.vue'), meta: { title: '批次 Run' } },
    { path: '/runs/new', component: () => import('@/views/RunNewView.vue'), meta: { title: '发起 Run' } },
    { path: '/runs/:id', component: () => import('@/views/RunDetailView.vue'), meta: { title: 'Run 详情' } },

    { path: '/datasets', component: () => import('@/views/DatasetListView.vue'), meta: { title: '数据集' } },
    { path: '/datasets/new', component: () => import('@/views/DatasetNewView.vue'), meta: { title: '上传数据集' } },
    { path: '/datasets/:id', component: () => import('@/views/DatasetDetailView.vue'), meta: { title: '数据集详情' } },
  ],
})

export default router
