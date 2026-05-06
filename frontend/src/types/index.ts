// ─── Step types ───────────────────────────────────────────────
export type StepType = 'thought' | 'tool_call' | 'tool_result' | 'final_answer'

export interface Step {
  type: StepType
  content?: string
  tool?: string
  input?: Record<string, unknown>
  output?: unknown
}

export interface Trace {
  task: string
  steps: Step[]
  metadata?: Record<string, unknown>
}

// ─── Reference ────────────────────────────────────────────────
export interface Reference {
  expected_answer?: string
  reference?: string
  expected_tools?: string[]
  expected_max_steps?: number
}

// ─── MetricConfig ─────────────────────────────────────────────
export interface MetricConfig {
  tool_call_accuracy?: { strict_order?: boolean }
  tool_call_f1?: { strict_order?: boolean }
  step_efficiency?: { penalty_factor?: number }
}

// ─── MetricResult ─────────────────────────────────────────────
export interface MetricResult {
  metric: string
  ragas_metric?: string
  score: number
  passed: boolean
  reason: string
}

// ─── EvaluationResult ─────────────────────────────────────────
export interface EvaluationResult {
  id: string
  created_at: string
  trace: Trace
  reference?: Reference
  metrics_requested: string[]
  metric_config?: MetricConfig
  results: MetricResult[]
  overall_score: number
  run_id?: string
}

export interface EvaluationListItem {
  id: string
  created_at: string
  task: string
  overall_score: number
  metrics_requested: string[]
  metadata?: Record<string, unknown>
}

// ─── RunResult ────────────────────────────────────────────────
export type RunStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface RunSummary {
  overall_score: number
  metric_scores: Record<string, number>
}

export interface RunResult {
  id: string
  created_at: string
  status: RunStatus
  agent_endpoint: string
  agent_version?: string
  dataset_id: string
  metrics_requested: string[]
  metric_config?: MetricConfig
  total_cases: number
  completed_cases: number
  failed_cases: number
  evaluation_ids: string[]
  summary: RunSummary
}

export interface RunListItem {
  id: string
  created_at: string
  status: RunStatus
  agent_endpoint: string
  agent_version?: string
  dataset_id: string
  total_cases: number
  summary: RunSummary
}

// ─── Dataset ──────────────────────────────────────────────────
export interface TestCase {
  id: string
  task: string
  reference?: Reference
}

export interface Dataset {
  id: string
  name: string
  description?: string
  created_at: string
  case_count: number
  cases: TestCase[]
}

export interface DatasetListItem {
  id: string
  name: string
  description?: string
  created_at: string
  case_count: number
}

// ─── MetricDescriptor ─────────────────────────────────────────
export interface MetricDescriptor {
  name: string
  ragas_metric?: string
  description: string
  requires_reference: boolean
  reference_fields?: string[]
  judger: 'rule' | 'llm' | 'statistical'
  eval_mode: 'result' | 'process'
  eval_dimension: 'effectiveness' | 'safety' | 'performance'
}

// ─── Paged response ───────────────────────────────────────────
export interface PagedResponse<T> {
  total: number
  page: number
  page_size: number
  items: T[]
}

// ─── Compare ──────────────────────────────────────────────────
export interface CompareResult {
  evaluations: Array<{
    id: string
    created_at: string
    task: string
    overall_score: number
    metadata?: Record<string, unknown>
    results: MetricResult[]
  }>
  summary: {
    best_overall: string
    metric_winners: Record<string, string>
  }
}
