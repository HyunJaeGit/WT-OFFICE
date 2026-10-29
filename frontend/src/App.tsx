import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { createTask as saveTask, getTask, listTasks, type Task, type TaskRole, type TaskStatus } from './api/tasks'

type View = 'office' | 'request' | 'detail' | 'history'

const staff: { role: TaskRole; name: string; initial: string; color: string; description: string }[] = [
  { role: '기획', name: '기획 담당', initial: '기', color: 'lilac', description: '요구사항과 방향을 정리해요' },
  { role: '개발', name: '개발 담당', initial: '개', color: 'mint', description: '구현 계획을 살펴봐요' },
  { role: '검토', name: '검토 담당', initial: '검', color: 'peach', description: '결과와 세부 사항을 확인해요' },
]

function App() {
  const [view, setView] = useState<View>('office')
  const [tasks, setTasks] = useState<Task[]>([])
  const [selectedTask, setSelectedTask] = useState<Task | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [role, setRole] = useState<TaskRole>('기획')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const pendingRequest = useRef<{ fingerprint: string; id: string } | null>(null)

  const refreshTasks = useCallback(async () => {
    setLoading(true)
    try {
      setTasks(await listTasks())
      setError('')
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refreshTasks()
  }, [refreshTasks])

  async function openTask(taskId: string) {
    setError('')
    setLoading(true)
    setView('detail')
    try {
      setSelectedTask(await getTask(taskId))
    } catch (requestError) {
      setError(errorMessage(requestError))
      setSelectedTask(null)
    } finally {
      setLoading(false)
    }
  }

  async function submitTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!title.trim() || !content.trim()) {
      setError('작업 제목과 내용을 모두 입력해 주세요.')
      return
    }
    setSubmitting(true)
    setError('')
    const payload = { title: title.trim(), content: content.trim(), role }
    const fingerprint = JSON.stringify(payload)
    const requestId = pendingRequest.current?.fingerprint === fingerprint
      ? pendingRequest.current.id
      : crypto.randomUUID()
    pendingRequest.current = { fingerprint, id: requestId }
    try {
      const task = await saveTask(payload, requestId)
      pendingRequest.current = null
      setSelectedTask(task)
      setTitle('')
      setContent('')
      setView('detail')
      await refreshTasks()
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  function navigate(nextView: View) {
    setView(nextView)
    setError('')
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => navigate('office')} aria-label="WT-OFFICE 홈">
          <span className="brand-mark">W</span>
          <span>WT-OFFICE<small>AI TEAM SPACE</small></span>
        </button>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="main-nav" aria-label="주 메뉴">
          <button className={view === 'office' ? 'nav-item active' : 'nav-item'} onClick={() => navigate('office')}><span>▦</span> 사무실</button>
          <button className={view === 'request' ? 'nav-item active' : 'nav-item'} onClick={() => navigate('request')}><span>＋</span> 새 작업 요청</button>
          <button className={view === 'history' ? 'nav-item active' : 'nav-item'} onClick={() => { navigate('history'); void refreshTasks() }}><span>◷</span> 작업 이력 <span className="nav-count">{tasks.length}</span></button>
        </nav>
        <div className="sidebar-bottom">
          <div className="connection"><span className="connection-dot" /> 로컬 SQLite 저장</div>
          <div className="profile"><div className="profile-avatar">나</div><div><strong>내 워크스페이스</strong><small>관리자</small></div><span className="profile-more">···</span></div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb">내 워크스페이스 <span>/</span> <strong>{viewTitle(view)}</strong></div>
          <div className="topbar-right"><span className="preview-chip"><i /> 실제 저장 데이터</span><button className="help-button" aria-label="도움말">?</button></div>
        </header>

        {error && view !== 'request' && <div className="global-error" role="alert"><span>{error}</span><button className="text-button" onClick={() => void refreshTasks()}>다시 시도</button></div>}

        {view === 'office' && <section className="page office-page">
          <div className="page-heading"><div><div className="eyebrow">THURSDAY, OCTOBER 29</div><h1>오늘도 좋은 하루예요 <span className="wave">✳</span></h1><p>팀의 상태를 살펴보고, 새로운 작업을 맡겨 보세요.</p></div><button className="button primary" onClick={() => navigate('request')}><span>＋</span> 새 작업 요청</button></div>
          <div className="office-banner">
            <div className="banner-copy"><span className="banner-kicker">YOUR AI TEAM</span><h2>작은 아이디어도<br />함께 시작해 볼까요?</h2><p>역할에 맞는 팀원에게 작업을 요청하고<br className="desktop-break" /> 결과를 한곳에서 확인할 수 있어요.</p><button className="button dark" onClick={() => navigate('request')}>작업 맡기기 <span>→</span></button></div>
            <div className="office-scene" aria-label="책상과 화분, 조명이 있는 사무실 일러스트" role="img"><div className="scene-window"><i /><i /><i /></div><div className="scene-shelf"><b /><b /><b /></div><div className="scene-lamp"><i /><b /></div><div className="scene-plant"><i /><i /><i /><b /></div><div className="scene-desk"><div className="scene-laptop"><i /></div><div className="scene-cup" /></div><div className="scene-floor" /></div>
          </div>
          <div className="section-heading"><div><h2>우리 팀</h2><p>AI 연결은 준비 중이며 아직 작업을 실행하지 않습니다</p></div><span className="soft-label">3 TEAM MEMBERS</span></div>
          <div className="staff-grid">{staff.map((member) => <article className="staff-card" key={member.role}><div className={`staff-avatar ${member.color}`}>{member.initial}<span className="avatar-spark">✦</span></div><div className="staff-info"><div className="staff-name-row"><h3>{member.name}</h3><span className="status-dot idle" /></div><div className="role-label">{member.role} · AI 팀원</div><p>{member.description}</p></div><div className="staff-status"><span className="status-pill idle">대기 중</span></div></article>)}</div>
          <div className="section-heading task-section-heading"><div><h2>최근 작업</h2><p>SQLite에 저장된 요청과 상태</p></div><button className="text-button" onClick={() => { navigate('history'); void refreshTasks() }}>전체 보기 <span>→</span></button></div>
          {loading && <LoadingState />}
          {!loading && !error && <TaskList tasks={tasks.slice(0, 3)} onOpen={openTask} />}
          <StorageNote />
        </section>}

        {view === 'request' && <section className="page form-page"><div className="page-heading compact"><div><div className="eyebrow">NEW TASK</div><h1>어떤 일을 맡겨 볼까요?</h1><p>요청을 SQLite에 저장합니다. AI 실행은 준비 중입니다.</p></div></div><form className="request-layout" onSubmit={submitTask} noValidate><div className="form-card"><label htmlFor="task-title">작업 제목</label><input id="task-title" value={title} onChange={(event) => setTitle(event.target.value)} maxLength={120} placeholder="예: 새 기능 아이디어 정리" /><div className="field-meta"><span>작업을 한눈에 알아볼 수 있게 적어 주세요.</span><span>{title.length}/120</span></div><label htmlFor="task-content" className="content-label">작업 내용</label><textarea id="task-content" value={content} onChange={(event) => setContent(event.target.value)} maxLength={5000} placeholder="무엇을 원하는지, 참고할 내용이나 조건을 자유롭게 적어 주세요." rows={9} /><div className="field-meta"><span>구체적으로 적을수록 요청을 이해하기 쉬워요.</span><span>{content.length}/5000</span></div>{error && <p className="form-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="button secondary" onClick={() => navigate('office')}>취소</button><button type="submit" className="button primary" disabled={submitting}>{submitting ? '저장 중…' : <>작업 저장 <span>→</span></>}</button></div></div><aside className="role-card"><h2>담당 역할</h2><p>어떤 관점이 필요한지 선택해 주세요.</p><div className="role-options">{staff.map((member) => <label className={role === member.role ? 'role-option selected' : 'role-option'} key={member.role}><input type="radio" name="role" checked={role === member.role} onChange={() => setRole(member.role)} /><span className={`mini-avatar ${member.color}`}>{member.initial}</span><span><strong>{member.role} 담당</strong><small>{member.description}</small></span>{role === member.role && <span className="checkmark">✓</span>}</label>)}</div><div className="role-tip"><span>✦</span> 저장된 작업은 AI에 전달되지 않으며 상태는 대기로 유지됩니다.</div></aside></form><StorageNote /></section>}

        {view === 'detail' && <section className="page detail-page"><div className="back-row"><button className="text-button" onClick={() => { navigate('history'); void refreshTasks() }}>← 작업 이력</button><span className="detail-id">{selectedTask ? `TASK-${selectedTask.id.slice(0, 8).toUpperCase()}` : ''}</span></div>{loading ? <LoadingState /> : selectedTask ? <><div className="detail-layout"><article className="detail-main"><div className="detail-topline"><StatusPill status={selectedTask.status} /><span className="sample-inline">SQLite 저장 데이터</span></div><h1>{selectedTask.title}</h1><p className="detail-subtitle">{selectedTask.role} 담당 · {formatDate(selectedTask.createdAt)}</p><div className="detail-divider" /><h2>요청 내용</h2><p className="detail-content">{selectedTask.content}</p>{selectedTask.resultSummary && <section className="result-panel"><div className="result-icon">✦</div><div><h2>저장된 작업 결과</h2><p>{selectedTask.resultSummary}</p></div></section>}{selectedTask.errorSummary && <section className="error-panel"><strong>오류·확인 사항</strong><p>{selectedTask.errorSummary}</p></section>}<TaskStatusMessage status={selectedTask.status} /></article><aside className="detail-side"><h2>작업 정보</h2><dl><div><dt>담당 역할</dt><dd><span className={`mini-avatar ${staff.find((member) => member.role === selectedTask.role)?.color}`}>{selectedTask.role[0]}</span>{selectedTask.role} 담당</dd></div><div><dt>상태</dt><dd><StatusPill status={selectedTask.status} /></dd></div><div><dt>요청 시각</dt><dd>{formatDate(selectedTask.createdAt)}</dd></div><div><dt>시작 시각</dt><dd>{formatDate(selectedTask.startedAt)}</dd></div><div><dt>종료 시각</dt><dd>{formatDate(selectedTask.finishedAt)}</dd></div></dl><button className="button secondary full" onClick={() => navigate('office')}>사무실로 돌아가기</button></aside></div><StorageNote /></> : <div className="empty-state"><h3>작업을 불러오지 못했어요</h3><p>작업 이력에서 다시 선택해 주세요.</p></div>}</section>}

        {view === 'history' && <section className="page history-page"><div className="page-heading compact"><div><div className="eyebrow">TASK HISTORY</div><h1>작업 이력</h1><p>요청한 작업과 저장된 상태를 확인합니다.</p></div><button className="button primary" onClick={() => navigate('request')}><span>＋</span> 새 작업 요청</button></div><div className="history-toolbar"><span>{tasks.length}개의 저장된 작업</span><span className="soft-label">최근 요청순</span></div>{loading ? <LoadingState /> : !error && <TaskList tasks={tasks} onOpen={openTask} />}<StorageNote /></section>}
      </main>
    </div>
  )
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

function viewTitle(view: View) {
  return { office: '사무실', request: '새 작업 요청', detail: '작업 상세', history: '작업 이력' }[view]
}

function formatDate(value: string | null) {
  return value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
}

function StatusPill({ status }: { status: TaskStatus }) {
  const labels: Record<TaskStatus, string> = {
    PENDING: '대기', RUNNING: '진행 중', COMPLETED: '완료', FAILED: '실패', NEEDS_REVIEW: '확인 필요',
  }
  return <span className={`status-pill ${status.toLowerCase()}`}>{labels[status]}</span>
}

function TaskStatusMessage({ status }: { status: TaskStatus }) {
  const messages: Record<TaskStatus, { title: string; detail: string }> = {
    PENDING: { title: 'AI 실행 준비 중', detail: '작업은 저장되었지만 실제 AI 작업은 실행되지 않았습니다.' },
    RUNNING: { title: '작업 진행 중', detail: '현재 저장된 상태가 진행 중입니다. 실행 이벤트와 테스트 결과는 별도로 확인해야 합니다.' },
    COMPLETED: { title: '작업 완료', detail: '저장된 결과를 확인해 주세요. 이 상태만으로 테스트 통과를 의미하지는 않습니다.' },
    FAILED: { title: '작업 실패', detail: '저장된 오류 요약을 확인해 주세요.' },
    NEEDS_REVIEW: { title: '실행 상태 확인 필요', detail: '서버 재시작 시 진행 중이던 작업입니다. 자동 재실행하지 않았으며 실제 결과를 확인할 수 없습니다.' },
  }
  const message = messages[status]
  return <section className="result-panel"><div className="result-icon">✦</div><div><h2>{message.title}</h2><p>{message.detail}</p></div></section>
}

function TaskList({ tasks, onOpen }: { tasks: Task[]; onOpen: (id: string) => void }) {
  if (tasks.length === 0) return <div className="empty-state"><div>＋</div><h3>아직 저장된 작업이 없어요</h3><p>새 작업을 요청하면 SQLite에 저장됩니다.</p></div>
  return <div className="task-list">{tasks.map((task) => <button className="task-row" key={task.id} onClick={() => void onOpen(task.id)}><span className={`task-role-icon ${staff.find((member) => member.role === task.role)?.color}`}>{task.role[0]}</span><span className="task-copy"><strong>{task.title}</strong><small>{task.role} 담당 <i /> {formatDate(task.createdAt)}</small></span><span className="task-row-status"><StatusPill status={task.status} /></span><span className="row-arrow">→</span></button>)}</div>
}

function LoadingState() {
  return <div className="empty-state" role="status"><h3>저장된 작업을 불러오는 중…</h3></div>
}

function StorageNote() {
  return <p className="sample-note"><span>ⓘ</span> 실제 작업 데이터는 로컬 SQLite에 저장됩니다. AI 연결은 준비 중이며 아직 실행되지 않습니다.</p>
}

export default App
