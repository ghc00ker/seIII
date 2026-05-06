import axios from 'axios'
import type {
  PagedResponse, EvaluationResult, EvaluationListItem,
  RunResult, RunListItem, Dataset, DatasetListItem,
  MetricDescriptor, CompareResult, Trace, Reference, MetricConfig,
} from '@/types'

const http = axios.create({ baseURL: '/api/v1' })

// ─── Evaluations ──────────────────────────────────────────────
export const createEvaluation = (body: {
  trace: Trace
  reference?: Reference
  metrics?: string[]
  metric_config?: MetricConfig
}) => http.post<EvaluationResult>('/evaluations', body).then(r => r.data)

export const listEvaluations = (params: {
  page?: number; page_size?: number; agent_id?: string; run_id?: string
}) => http.get<PagedResponse<EvaluationListItem>>('/evaluations', { params }).then(r => r.data)

export const getEvaluation = (id: string) =>
  http.get<EvaluationResult>(`/evaluations/${id}`).then(r => r.data)

export const deleteEvaluation = (id: string) =>
  http.delete(`/evaluations/${id}`)

export const compareEvaluations = (ids: string[]) =>
  http.post<CompareResult>('/evaluations/compare', { ids }).then(r => r.data)

// ─── Runs ─────────────────────────────────────────────────────
export const createRun = (body: {
  agent_endpoint: string
  agent_version?: string
  dataset_id: string
  metrics?: string[]
  metric_config?: MetricConfig
}) => http.post<RunResult>('/runs', body).then(r => r.data)

export const listRuns = (params: { page?: number; page_size?: number }) =>
  http.get<PagedResponse<RunListItem>>('/runs', { params }).then(r => r.data)

export const getRun = (id: string) =>
  http.get<RunResult>(`/runs/${id}`).then(r => r.data)

// ─── Datasets ─────────────────────────────────────────────────
export const createDataset = (body: {
  name: string
  description?: string
  cases: Array<{ task: string; reference?: Reference }>
}) => http.post<DatasetListItem>('/datasets', body).then(r => r.data)

export const listDatasets = (params: { page?: number; page_size?: number }) =>
  http.get<PagedResponse<DatasetListItem>>('/datasets', { params }).then(r => r.data)

export const getDataset = (id: string) =>
  http.get<Dataset>(`/datasets/${id}`).then(r => r.data)

export const deleteDataset = (id: string) =>
  http.delete(`/datasets/${id}`)

// ─── Metrics ──────────────────────────────────────────────────
export const listMetrics = () =>
  http.get<MetricDescriptor[]>('/metrics').then(r => r.data)
