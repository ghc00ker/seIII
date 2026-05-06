<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createDataset } from '@/api'
import type { Reference } from '@/types'
import { ElMessage } from 'element-plus'

const router      = useRouter()
const loading     = ref(false)
const name        = ref('')
const description = ref('')
const mode        = ref<'form' | 'json' | 'file'>('form')

// ─── Form mode ────────────────────────────────────────────────
interface CaseForm {
  task: string
  expected_answer: string
  reference: string
  expected_tools: string
  expected_max_steps: string
}
const cases = ref<CaseForm[]>([
  { task: '', expected_answer: '', reference: '', expected_tools: '', expected_max_steps: '' },
])

function addCase() {
  cases.value.push({ task: '', expected_answer: '', reference: '', expected_tools: '', expected_max_steps: '' })
}
function removeCase(i: number) {
  cases.value.splice(i, 1)
}

function buildCasesFromForm() {
  return cases.value
    .filter(c => c.task.trim())
    .map(c => {
      const ref: Reference = {}
      if (c.reference.trim())       ref.reference = c.reference.trim()
      if (c.expected_answer.trim()) ref.expected_answer = c.expected_answer.trim()
      if (c.expected_tools.trim())  ref.expected_tools = c.expected_tools.split(',').map(s => s.trim()).filter(Boolean)
      if (c.expected_max_steps.trim()) ref.expected_max_steps = parseInt(c.expected_max_steps)
      return { task: c.task.trim(), reference: Object.keys(ref).length ? ref : undefined }
    })
}

// ─── JSON mode ────────────────────────────────────────────────
const jsonText = ref(`[
  {
    "task": "查询北京明天天气",
    "reference": {
      "expected_tools": ["weather_api"],
      "expected_answer": "明天北京晴天，约22度"
    }
  }
]`)

// ─── File mode ────────────────────────────────────────────────
interface ParsedFile {
  filename: string
  caseCount: number
  error?: string
  cases: Array<{ task: string; reference?: Reference }>
}

const parsedFiles  = ref<ParsedFile[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)

function parseOneCaseOrArray(obj: unknown): Array<{ task: string; reference?: Reference }> {
  if (Array.isArray(obj)) return obj as Array<{ task: string; reference?: Reference }>
  if (obj && typeof obj === 'object' && 'task' in obj) return [obj as { task: string; reference?: Reference }]
  throw new Error('格式不识别，需要对象或数组')
}

function handleFileChange(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files) return
  parsedFiles.value = []

  Array.from(files).forEach(file => {
    const reader = new FileReader()
    reader.onload = (ev) => {
      const result: ParsedFile = { filename: file.name, caseCount: 0, cases: [] }
      try {
        const parsed = JSON.parse(ev.target?.result as string)
        result.cases = parseOneCaseOrArray(parsed)
        result.caseCount = result.cases.length
      } catch (err: unknown) {
        result.error = (err as Error).message
      }
      parsedFiles.value.push(result)
    }
    reader.readAsText(file)
  })
}

function removeFile(filename: string) {
  parsedFiles.value = parsedFiles.value.filter(f => f.filename !== filename)
  // reset input so the same files can be re-selected
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function totalFileCases() {
  return parsedFiles.value.filter(f => !f.error).reduce((sum, f) => sum + f.caseCount, 0)
}

// ─── Submit ───────────────────────────────────────────────────
async function submit() {
  if (!name.value.trim()) { ElMessage.error('请填写数据集名称'); return }

  let builtCases: Array<{ task: string; reference?: Reference }> = []

  if (mode.value === 'json') {
    try { builtCases = JSON.parse(jsonText.value) }
    catch { ElMessage.error('JSON 格式有误'); return }
  } else if (mode.value === 'file') {
    const valid = parsedFiles.value.filter(f => !f.error)
    if (!valid.length) { ElMessage.error('请先选择有效的 JSON 文件'); return }
    builtCases = valid.flatMap(f => f.cases)
  } else {
    builtCases = buildCasesFromForm()
  }

  if (!builtCases.length) { ElMessage.error('请至少添加一条用例'); return }

  loading.value = true
  try {
    await createDataset({
      name: name.value.trim(),
      description: description.value.trim() || undefined,
      cases: builtCases,
    })
    ElMessage.success(`数据集创建成功（共 ${builtCases.length} 条用例）`)
    router.push('/datasets')
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { error?: string } } })?.response?.data?.error ?? '创建失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>上传数据集</h2>
      </div>
    </div>

    <!-- 基本信息 -->
    <el-card shadow="never" style="margin-bottom:16px;max-width:900px">
      <template #header><span style="font-weight:600">基本信息</span></template>
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="name" placeholder="数据集名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="description" placeholder="可选描述" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用例输入区 -->
    <el-card shadow="never" style="max-width:900px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:600">测试用例</span>
          <el-radio-group v-model="mode" size="small">
            <el-radio-button value="form">表单</el-radio-button>
            <el-radio-button value="json">JSON</el-radio-button>
            <el-radio-button value="file">本地文件</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- ── 表单模式 ── -->
      <div v-if="mode === 'form'">
        <div
          v-for="(c, i) in cases"
          :key="i"
          style="padding:12px;border:1px solid #ebeef5;border-radius:8px;margin-bottom:12px"
        >
          <div style="display:flex;justify-content:space-between;margin-bottom:8px">
            <span style="font-size:13px;font-weight:500;color:#606266">用例 {{ i + 1 }}</span>
            <el-button link type="danger" size="small" @click="removeCase(i)" :disabled="cases.length === 1">删除</el-button>
          </div>
          <el-form label-width="120px" size="small">
            <el-form-item label="任务描述" required>
              <el-input v-model="c.task" placeholder="e.g. 查询北京明天天气" />
            </el-form-item>
            <el-form-item label="金标参考">
              <el-input v-model="c.reference" placeholder="用于 goal_completion LLM 判断" />
            </el-form-item>
            <el-form-item label="期望答案">
              <el-input v-model="c.expected_answer" placeholder="expected_answer（可选）" />
            </el-form-item>
            <el-form-item label="期望工具">
              <el-input v-model="c.expected_tools" placeholder="逗号分隔，如 weather_api,search_api" />
            </el-form-item>
            <el-form-item label="最大步骤数">
              <el-input v-model="c.expected_max_steps" type="number" placeholder="可选整数" style="width:160px" />
            </el-form-item>
          </el-form>
        </div>
        <el-button @click="addCase" style="width:100%;margin-top:4px">
          <el-icon><Plus /></el-icon> 添加用例
        </el-button>
      </div>

      <!-- ── JSON 模式 ── -->
      <div v-else-if="mode === 'json'">
        <div style="font-size:12px;color:#909399;margin-bottom:8px">
          格式：<code>[{"task": "...", "reference": {"expected_tools": [...], "expected_answer": "..."}}]</code>
        </div>
        <el-input v-model="jsonText" type="textarea" :rows="14" style="font-family:monospace;font-size:13px" />
      </div>

      <!-- ── 本地文件模式 ── -->
      <div v-else>
        <!-- 说明 -->
        <el-alert type="info" :closable="false" style="margin-bottom:16px">
          <template #default>
            <div style="font-size:13px;line-height:1.8">
              支持选择<strong>一个或多个 .json 文件</strong>，也可以全选文件夹中的文件一次导入。<br>
              每个文件可以是 <code>单个用例对象</code> 或 <code>用例数组</code>，所有文件的用例会合并到一个数据集中。<br>
              格式：<code>{"task":"...", "reference":{...}}</code> 或 <code>[{"task":"..."},...]</code>
            </div>
          </template>
        </el-alert>

        <!-- 文件选择器（隐藏原生 input，用按钮触发）-->
        <input
          ref="fileInputRef"
          type="file"
          accept=".json,application/json"
          multiple
          style="display:none"
          @change="handleFileChange"
        />

        <div
          style="border:2px dashed #dcdfe6;border-radius:8px;padding:32px;text-align:center;cursor:pointer;transition:border-color .2s"
          @click="fileInputRef?.click()"
          @dragover.prevent
          @drop.prevent="(e) => { if(fileInputRef) { fileInputRef.files = e.dataTransfer!.files; handleFileChange({ target: fileInputRef } as unknown as Event) } }"
          @mouseenter="($event.currentTarget as HTMLElement).style.borderColor='#409eff'"
          @mouseleave="($event.currentTarget as HTMLElement).style.borderColor='#dcdfe6'"
        >
          <el-icon style="font-size:40px;color:#c0c4cc;margin-bottom:8px"><FolderOpened /></el-icon>
          <div style="color:#606266;font-size:14px">点击选择文件，或将文件拖拽到此处</div>
          <div style="color:#909399;font-size:12px;margin-top:4px">支持多选 .json 文件</div>
        </div>

        <!-- 解析结果预览 -->
        <div v-if="parsedFiles.length" style="margin-top:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
            <span style="font-size:13px;font-weight:500;color:#303133">
              已选 {{ parsedFiles.length }} 个文件，共
              <span style="color:#409eff;font-weight:700">{{ totalFileCases() }}</span> 条用例
            </span>
            <el-button link type="danger" size="small" @click="parsedFiles=[];if(fileInputRef) fileInputRef.value=''">清空</el-button>
          </div>

          <div
            v-for="f in parsedFiles"
            :key="f.filename"
            style="display:flex;align-items:center;gap:10px;padding:8px 12px;border-radius:6px;margin-bottom:6px"
            :style="{ background: f.error ? '#fff2f0' : '#f6ffed', border: `1px solid ${f.error ? '#ffccc7' : '#b7eb8f'}` }"
          >
            <el-icon :color="f.error ? '#f56c6c' : '#67c23a'" style="flex-shrink:0">
              <component :is="f.error ? 'CircleClose' : 'CircleCheck'" />
            </el-icon>
            <span style="flex:1;font-size:13px;font-family:monospace;color:#303133;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
              {{ f.filename }}
            </span>
            <span v-if="!f.error" style="font-size:12px;color:#52c41a;flex-shrink:0">{{ f.caseCount }} 条</span>
            <span v-else style="font-size:12px;color:#f56c6c;flex-shrink:0">{{ f.error }}</span>
            <el-button link type="danger" size="small" @click="removeFile(f.filename)">移除</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <div style="margin-top:16px;max-width:900px;display:flex;align-items:center;gap:12px">
      <el-button type="primary" size="large" :loading="loading" @click="submit">
        创建数据集
      </el-button>
      <span v-if="mode === 'file' && totalFileCases() > 0" style="font-size:13px;color:#909399">
        将导入 {{ totalFileCases() }} 条用例
      </span>
    </div>
  </div>
</template>
