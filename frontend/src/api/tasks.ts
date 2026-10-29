export type TaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'NEEDS_REVIEW'
export type TaskRole = '기획' | '개발' | '검토'

export type Task = {
  id: string
  title: string
  content: string
  role: TaskRole
  status: TaskStatus
  requestId: string
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  resultSummary: string | null
  errorSummary: string | null
  errorCode: string | null
  aiModel: string | null
  inputTokens: number | null
  cachedInputTokens: number | null
  outputTokens: number | null
}

export type CreateTask = Pick<Task, 'title' | 'content' | 'role'>
export type AiStatus = {
  enabled: boolean
  configured: boolean
  available: boolean
  model: string
  reasoningEffort: string
  message: string
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '')

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
  } catch {
    throw new Error('백엔드에 연결할 수 없습니다. 로컬 서버 실행 상태를 확인해 주세요.')
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(body?.message || `요청을 처리하지 못했습니다. (HTTP ${response.status})`)
  }
  return response.json() as Promise<T>
}

export function listTasks() {
  return request<Task[]>('/api/tasks')
}

export function getTask(id: string) {
  return request<Task>(`/api/tasks/${encodeURIComponent(id)}`)
}

export function createTask(task: CreateTask, requestId: string) {
  return request<Task>('/api/tasks', {
    method: 'POST',
    headers: { 'Idempotency-Key': requestId },
    body: JSON.stringify(task),
  })
}

export function getAiStatus() {
  return request<AiStatus>('/api/ai/status')
}

export function executeTask(id: string) {
  return request<Task>(`/api/tasks/${encodeURIComponent(id)}/execute`, { method: 'POST' })
}
