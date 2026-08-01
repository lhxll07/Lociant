/* ── Lociant WebUI — Settings panels ── */

// ---- Locale ----
function loadLocaleSetting() {
  apiGet(localeStorePath)
    .then(result => {
      localeSetting = result && result.value ? result.value : { mode: 'system' }
      applyLocale()
    })
    .catch(() => applyLocale())
}

function saveLocaleSetting(mode) {
  localeSetting = { mode: mode || 'system' }
  apiPut(localeStorePath, { value: localeSetting }).catch(() => {})
  applyLocale()
}

function applyLocale() {
  currentLocale = resolveLocale(localeSetting)
  document.documentElement.lang = currentLocale === 'zh' ? 'zh-CN' : 'en'
  document.querySelectorAll('[data-i18n]').forEach(node => {
    node.textContent = t(node.dataset.i18n)
  })
  document.querySelectorAll('[data-i18n-placeholder]').forEach(node => {
    node.setAttribute('placeholder', t(node.dataset.i18nPlaceholder))
  })
  document.querySelectorAll('[data-i18n-aria-label]').forEach(node => {
    node.setAttribute('aria-label', t(node.dataset.i18nAriaLabel))
  })
  Array.from(languageControl.querySelectorAll('.segmented-option')).forEach(button => {
    button.classList.toggle('active', button.dataset.langMode === (localeSetting.mode || 'system'))
  })
  updateRuntimeServiceState(runtimeServiceState || {})
}

// ---- Sessions ----
function activeHomeSessions(state) {
  return Array.isArray(state && state.sessions) ? state.sessions : []
}

function homeSessionIdFromState(state) {
  return (state && state.currentSessionId) || ''
}

function markHomeSessionActive(state, sessionId) {
  const next = Object.assign({}, state || {})
  if (sessionId) next.currentSessionId = sessionId
  return next
}

function renderSessions(sessions) {
  if (!runtimeSessionList) return
  runtimeSessionList.innerHTML = ''
  const items = Array.isArray(sessions) ? sessions : []
  if (!items.length) {
    runtimeSessionList.appendChild(emptyCard(t('settings.noSessions')))
    return
  }
  items.forEach(session => {
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'model-choice-row pressable'
    row.classList.toggle('active', session.id === (runtimeServiceState && runtimeServiceState.currentSessionId))
    const body = document.createElement('div')
    body.className = 'model-choice-body'
    const title = el('div', 'model-choice-title', session.title || session.id || '--')
    const sub = el('div', 'model-choice-sub', (session.modelId || '--') + ' · ' + (session.messageCount || 0) + ' messages')
    body.appendChild(title)
    body.appendChild(sub)
    const check = el('div', 'model-choice-check', '✓')
    row.appendChild(body)
    row.appendChild(check)
    row.addEventListener('click', () => {
      selectRuntimeSession(session.id)
    })
    runtimeSessionList.appendChild(row)
  })
}

function renderHomeSessions(sessions) {
  if (!homeSessionList) return
  homeSessionList.innerHTML = ''
  const policyLimit = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.recentLimit)
  const filtered = activeHomeSessions({ sessions: Array.isArray(sessions) ? sessions : [] })
  const items = policyLimit > 0 ? filtered.slice(0, policyLimit) : filtered
  if (!items.length) return
  let lastGroup = ''
  items.forEach(session => {
    const group = sessionDateGroup(session.updatedAt)
    if (group !== lastGroup) {
      lastGroup = group
      homeSessionList.appendChild(el('div', 'chat-session-date', group))
    }
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'chat-session-item pressable'
    const active = session.id === homeCurrentSessionId()
    row.classList.toggle('active', active)
    const body = el('span', 'chat-session-body')
    const title = el('strong', '', session.title || session.id || '--')
    const sub = el('span', '', sessionDisplayMeta(session) + ' · ' + (session.messageCount || 0))
    const remove = el('span', 'chat-session-delete', '×')
    remove.setAttribute('role', 'button')
    remove.setAttribute('tabindex', '0')
    remove.setAttribute('aria-label', t('home.deleteChat'))
    body.appendChild(title)
    body.appendChild(sub)
    row.appendChild(body)
    row.appendChild(remove)
    row.addEventListener('click', () => {
      Promise.resolve(selectRuntimeSession(session.id))
        .then(state => {
          updateRuntimeServiceState(markHomeSessionActive(state, session.id))
          loadHomeConversation(session.id)
          if (homeSidebar && homeSidebar.classList.contains('open')) {
            homeSidebar.classList.remove('open')
            if (homeRailToggle) homeRailToggle.setAttribute('aria-expanded', 'false')
          }
        })
        .catch(error => showToast((error && error.message) || t('toast.modelImportFailed')))
    })
    const deleteSession = event => {
      event.preventDefault()
      event.stopPropagation()
      const deletingCurrent = session.id === homeCurrentSessionId()
      Promise.resolve(deleteRuntimeSession(session.id))
        .then(state => {
          updateRuntimeServiceState(markHomeSessionActive(state || {}, deletingCurrent ? '' : homeCurrentSessionId()))
          restoreHomeConversation({ forceLatest: deletingCurrent })
          showToast(t('home.deleteChat'))
        })
        .catch(error => showToast((error && error.message) || t('toast.modelDeleteFailed')))
    }
    remove.addEventListener('click', deleteSession)
    remove.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') deleteSession(event)
    })
    homeSessionList.appendChild(row)
  })
}

function sessionDateGroup(updatedAt) {
  const value = Number(updatedAt) || 0
  if (!value) return t('home.earlier')
  const date = new Date(value)
  const today = new Date()
  const startToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime()
  const startDate = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const diffDays = Math.round((startToday - startDate) / 86400000)
  if (diffDays <= 0) return t('home.today')
  if (diffDays === 1) return t('home.yesterday')
  return t('home.earlier')
}

function sessionDisplayMeta(session) {
  if (!session) return '--'
  return session.modelId || t('home.localChatMeta')
}

function currentHomeSession() {
  const currentId = homeCurrentSessionId()
  const sessions = activeHomeSessions(runtimeServiceState)
  return sessions.find(session => session && session.id === currentId) || latestHomeSession(runtimeServiceState)
}

function updateHomeChatContext() {
  const session = currentHomeSession()
  const model = (runtimeServiceState && runtimeServiceState.modelId) || t('home.localChatMeta')
  if (homeChatTitle) homeChatTitle.textContent = (session && session.title) || t('home.newChat')
  if (homeChatMeta) homeChatMeta.textContent = model
  if (homeChatState) {
    const active = !!(runtimeServiceState && runtimeServiceState.running)
    homeChatState.textContent = active ? t('status.running') : t('status.stopped')
    homeChatState.classList.toggle('running', active)
  }
}

function upsertHomeSessionPreview(sessionId, titleText, lastRole) {
  if (!sessionId) return
  const now = Date.now()
  const policyLastTextLimit = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.lastTextLimit)
  const lastTextLimit = policyLastTextLimit > 0 ? policyLastTextLimit : Number.POSITIVE_INFINITY
  const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions)
    ? runtimeServiceState.sessions.slice()
    : []
  const index = sessions.findIndex(session => session && session.id === sessionId)
  const existing = index >= 0 ? sessions[index] : {}
  const next = Object.assign({}, existing, {
    id: sessionId,
    title: existing.title || String(titleText || '').trim() || sessionId,
    kind: existing.kind || 'model-chat',
    modelId: existing.modelId || ((runtimeServiceState && runtimeServiceState.modelId) || '--'),
    updatedAt: now,
    messageCount: Math.max(Number(existing.messageCount) || 0, 1) + (lastRole === 'assistant' ? 1 : 0),
    lastRole: lastRole || existing.lastRole || 'user',
    lastText: String(titleText || existing.lastText || '').slice(0, lastTextLimit),
  })
  if (index >= 0) sessions.splice(index, 1)
  sessions.unshift(next)
  runtimeServiceState = markHomeSessionActive(Object.assign({}, runtimeServiceState || {}, { sessions }), sessionId)
  updateHomeState()
  updateHomeChatContext()
}

function homeCurrentSessionId() {
  const current = runtimeServiceState && runtimeServiceState.currentSessionId
  if (current) return current
  return (runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.defaultSessionId) || ''
}

function appendChatBubble(role, text, meta) {
  if (!homeChatFeed) return null
  const node = document.createElement('div')
  node.className = 'chat-message ' + (role || 'assistant')
  if (meta && meta.active) node.dataset.activeSession = 'true'
  renderChatMarkdown(node, text)
  homeChatFeed.appendChild(node)
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return node
}

function appendAssistantRun() {
  if (!homeChatFeed) return null
  const node = document.createElement('div')
  node.className = 'chat-message assistant assistant-run'
  const chain = el('div', 'chat-tool-chain')
  const content = el('div', 'chat-run-content')
  node.appendChild(chain)
  node.appendChild(content)
  homeChatFeed.appendChild(node)
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return { node, chain, content }
}

function chatTextTarget(target) {
  return target && target.content ? target.content : target
}

function chatNodeTarget(target) {
  return target && target.node ? target.node : target
}

function appendToolBubble(toolCall, target) {
  if (!homeChatFeed || !toolCall) return null
  const scope = target && target.chain ? target.chain : homeChatFeed
  const id = toolCall.id || toolCall.toolCallId || ''
  const existing = id ? scope.querySelector('.tool-call-item[data-tool-call-id="' + cssEscape(id) + '"]') : null
  const node = existing || document.createElement('div')
  node.className = 'tool-call-item'
  if (id) node.dataset.toolCallId = id
  const info = normalizeToolBubbleInfo(toolCall)
  node.classList.toggle('done', info.status === 'completed')
  node.classList.toggle('error', info.status === 'failed')
  node.replaceChildren()
  node.appendChild(el('span', 'tool-call-dot'))
  const main = el('span', 'tool-call-main')
  main.appendChild(el('span', 'tool-call-label', info.name))
  main.appendChild(el('span', 'tool-call-meta', info.meta))
  if (info.args) main.appendChild(el('code', '', info.args))
  node.appendChild(main)
  node.appendChild(el('span', 'tool-call-state', info.statusLabel))
  if (!existing) scope.appendChild(node)
  if (target && target.chain) {
    chatNodeTarget(target).classList.add('has-tools')
  }
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return node
}

function normalizeToolBubbleInfo(toolCall) {
  const rawName = toolCall.name || toolCall.functionName || toolCall.title || toolCall.kind || 'tool'
  const status = String(toolCall.status || '').toLowerCase()
  const args = toolCall.arguments || toolCall.args || ''
  const metaParts = []
  if (toolCall.kind && toolCall.kind !== rawName) metaParts.push(toolCall.kind)
  if (toolCall.type && toolCall.type !== toolCall.kind) metaParts.push(toolCall.type)
  return {
    name: rawName,
    status,
    statusLabel: toolStatusLabel(status),
    meta: metaParts.join(' · ') || 'tool call',
    args: compactJsonText(args)
  }
}

function toolStatusLabel(status) {
  if (status === 'completed' || status === 'success' || status === 'done') return 'done'
  if (status === 'failed' || status === 'error') return 'error'
  if (status === 'pending') return 'pending'
  return status || 'call'
}

function renderChatMarkdown(target, text) {
  if (!target) return
  const source = String(text || '')
  if (target) target.dataset.rawText = source
  if (!window.marked || !window.DOMPurify) {
    target.textContent = source
    return
  }
  window.marked.setOptions({
    breaks: true,
    gfm: true,
    mangle: false,
    headerIds: false,
  })
  const html = window.marked.parse(source)
  target.innerHTML = window.DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code', 'pre', 'ul', 'ol', 'li', 'blockquote', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ALLOWED_ATTR: ['href', 'target', 'rel'],
  })
  target.querySelectorAll('a[href]').forEach(link => {
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
  })
}

function createChatTextStream(target) {
  let source = ''
  let shown = ''
  let timer = null
  let closed = false
  const step = () => {
    timer = null
    if (!target || closed) return
    if (shown.length >= source.length) return
    shown = source.slice(0, shown.length + 1)
    renderChatMarkdown(target, shown)
    if (homeChatFeed) homeChatFeed.scrollTop = homeChatFeed.scrollHeight
    if (shown.length < source.length) timer = window.setTimeout(step, 12)
  }
  return {
    push(text) {
      source += String(text || '')
      if (!timer) timer = window.setTimeout(step, 8)
    },
    finish(text) {
      if (text !== undefined && String(text) !== source) source = String(text || '')
      if (timer) window.clearTimeout(timer)
      timer = null
      shown = source
      if (target) renderChatMarkdown(target, shown)
      closed = true
    },
    text() {
      return source
    }
  }
}

function compactJsonText(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  try { return JSON.stringify(JSON.parse(text)) } catch (error) { return text }
}

function cssEscape(value) {
  if (window.CSS && typeof window.CSS.escape === 'function') return window.CSS.escape(String(value || ''))
  return String(value || '').replace(/["\\]/g, '\\$&')
}

function clearHomeMessages() {
  if (!homeChatFeed) return
  Array.from(homeChatFeed.querySelectorAll('.chat-message')).forEach(node => node.remove())
}

function renderHomeConversation(sessionId, messages) {
  if (!homeChatFeed) return
  clearHomeMessages()
  const items = Array.isArray(messages) ? messages : []
  if (!items.length) return
  items.forEach(item => {
    const role = item && item.role ? item.role : 'assistant'
    const text = item && (item.text || item.content || item.message || '')
    if (text) appendChatBubble(role, text, { active: sessionId === homeCurrentSessionId() })
  })
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
}

function latestHomeSession(state) {
  const sessions = activeHomeSessions(state)
  return sessions.find(session => session && session.id && Number(session.messageCount || 0) > 0) ||
    sessions.find(session => session && session.id) ||
    null
}

function shouldPreferLatestHomeSession(state, currentId) {
  if (!currentId) return true
  const sessions = activeHomeSessions(state)
  const current = sessions.find(session => session && session.id === currentId)
  return !current || Number(current.messageCount || 0) <= 0
}

function restoreHomeConversation(options) {
  const forceLatest = !!(options && options.forceLatest)
  const state = runtimeServiceState || runtimeState()
  updateRuntimeServiceState(state)
  const latest = latestHomeSession(state)
  const currentId = homeCurrentSessionId()
  const target = forceLatest || shouldPreferLatestHomeSession(state, currentId)
    ? (latest && latest.id)
    : currentId
  if (!target) {
    clearHomeMessages()
    return null
  }
  if (target !== currentId) {
    const selected = selectRuntimeSession(target)
    updateRuntimeServiceState(markHomeSessionActive(selected || state, target))
  }
  loadHomeConversation(target, { silent: true })
  return target
}

// ---- Diagnostics ----
let runtimeDiagLastResults = null

function diagnosticCardsFromState(state, check) {
  const current = state || {}
  const vision = current.vision || {}
  const tools = check && check.tools
  const model = check && check.model
  return [
    {
      key: 'runtime',
      title: t('diagnostics.runtime'),
      ok: !!current.running,
      text: current.running ? publicRuntimeUrl(current) : (current.message || t('status.stopped'))
    },
    {
      key: 'tools',
      title: t('diagnostics.tools'),
      ok: tools ? tools.ok : current.toolExposure !== undefined,
      text: tools ? tools.text : ((current.toolExposure || 'action') + ' exposure')
    },
    {
      key: 'model',
      title: t('diagnostics.model'),
      ok: model ? model.ok : !!current.modelLoaded,
      text: model ? model.text : ((current.modelId || '--') + (current.modelLoaded ? ' ready' : ' not loaded'))
    },
    {
      key: 'vision',
      title: t('diagnostics.vision'),
      ok: vision.running || String(vision.state || '').toLowerCase() === 'idle',
      text: vision.message || visionStateLabel(vision)
    },
    {
      key: 'mcp',
      title: t('diagnostics.mcp'),
      ok: !!current.running,
      text: current.running ? (publicRuntimeUrl(current) + '/mcp') : t('status.stopped')
    }
  ]
}

function renderDiagnosticsSummary(check) {
  if (!runtimeDiagSummary) return
  runtimeDiagSummary.innerHTML = ''
  const cards = diagnosticCardsFromState(runtimeServiceState || {}, check || runtimeDiagLastResults)
  const issues = cards.filter(item => !item.ok).length
  if (runtimeDiagSummaryText) {
    runtimeDiagSummaryText.textContent = check && check.running
      ? t('diagnostics.running')
      : (issues ? (issues + ' ' + t('diagnostics.issue')) : t('diagnostics.ready'))
  }
  cards.forEach(item => {
    const card = document.createElement('div')
    card.className = 'settings-section diagnostic-card' + (item.ok ? ' ok' : ' issue')
    const main = document.createElement('span')
    main.className = 'settings-section-main'
    main.appendChild(el('span', 'settings-section-title', item.title))
    main.appendChild(el('span', 'settings-section-sub diagnostic-card-text', item.text || '--'))
    card.appendChild(main)
    card.appendChild(el('span', 'status-pill diagnostic-card-state', item.ok ? t('diagnostics.ready') : t('diagnostics.issue')))
    runtimeDiagSummary.appendChild(card)
  })
}

function toolResultText(result, fallback) {
  if (!result) return fallback
  if (result.ok === false) return result.message || fallback
  return fallback
}

function runRuntimeDiagnostics() {
  if (!runtimeDiagRunButton) return
  runtimeDiagRunButton.disabled = true
  runtimeDiagRunButton.textContent = t('diagnostics.running')
  renderDiagnosticsSummary({ running: true })
  const state = runtimeState()
  updateRuntimeServiceState(state)
  Promise.allSettled([
    apiGet('/api/v1/tools'),
    apiPost('/api/v1/tools/runtime_status/calls', { arguments: {} }),
    apiPost('/api/v1/tools/model_list/calls', { arguments: {} }),
    apiPost('/api/v1/tools/vision_status/calls', { arguments: {} })
  ]).then(results => {
    const toolsResponse = results[0].status === 'fulfilled' ? results[0].value : null
    const runtimeResponse = results[1].status === 'fulfilled' ? results[1].value : null
    const modelResponse = results[2].status === 'fulfilled' ? results[2].value : null
    const visionResponse = results[3].status === 'fulfilled' ? results[3].value : null
    const toolCount = toolsResponse && toolsResponse.data ? toolsResponse.data.length : 0
    const models = modelResponse && modelResponse.result && Array.isArray(modelResponse.result.models)
      ? modelResponse.result.models
      : []
    const readyModels = models.filter(model => model.ready !== false).length
    const vision = visionResponse && visionResponse.result ? visionResponse.result : null
    runtimeDiagLastResults = {
      tools: {
        ok: toolCount > 0 && !!runtimeResponse,
        text: toolCount ? (toolCount + ' tools exposed') : 'No tools visible'
      },
      model: {
        ok: readyModels > 0 || !!(runtimeServiceState && runtimeServiceState.modelLoaded),
        text: readyModels ? (readyModels + ' ready models') : ((runtimeServiceState && runtimeServiceState.modelId) || '--')
      },
      vision: {
        ok: !!vision,
        text: toolResultText(vision, vision ? visionStateLabel(vision) : '--')
      }
    }
    renderDiagnosticsSummary(runtimeDiagLastResults)
  }).catch(() => {
    runtimeDiagLastResults = {
      tools: { ok: false, text: 'Tool check failed' },
      model: { ok: false, text: 'Model check failed' }
    }
    renderDiagnosticsSummary(runtimeDiagLastResults)
  }).finally(() => {
    runtimeDiagRunButton.disabled = false
    runtimeDiagRunButton.textContent = t('diagnostics.run')
  })
}

function updateDiagnostics(state) {
  const log = document.getElementById('runtimeDiagLog')
  renderDiagnosticsSummary()
  if (!log) return
  log.innerHTML = ''
  const requests = Array.isArray(state.recentRequests) ? state.recentRequests : []
  if (!requests.length) {
    log.appendChild(emptyCard(t('settings.noRequests')))
    return
  }
  requests.forEach(req => {
    const row = document.createElement('div')
    row.className = 'diag-row'
    row.appendChild(el('span', 'diag-method', req.method || ''))
    row.appendChild(el('span', 'diag-path', req.endpoint || ''))
    row.appendChild(el('span', 'diag-status diag-' + (req.status < 400 ? 'ok' : 'err'), req.status || ''))
    row.appendChild(el('span', 'diag-time', (req.elapsedMs || 0) + 'ms'))
    log.appendChild(row)
  })
}

// ---- Settings navigation ----
function showSettingsHome() {
  closeSettingsDetails()
  if (settingsList) {
    settingsList.classList.add('active')
    settingsList.setAttribute('aria-hidden', 'false')
  }
}

function showSettingsDetail(panel) {
  closeSettingsDetails()
  if (settingsList) {
    settingsList.classList.remove('active')
    settingsList.setAttribute('aria-hidden', 'true')
  }
  if (panel) {
    panel.classList.add('active')
    panel.setAttribute('aria-hidden', 'false')
  }
}

function openRuntimeSettings() {
  showSettingsDetail(runtimeSettingsPanel)
}

function closeRuntimeSettings() {
  showSettingsHome()
  updateRuntimeServiceState(runtimeServiceState || {})
}

function closeRuntimeSettingsBack() {
  closeRuntimeSettings()
}

function ensureSettingsDetailClosed() {
  closeSettingsDetails()
  if (settingsList) {
    settingsList.classList.add('active')
    settingsList.setAttribute('aria-hidden', 'false')
  }
}

function closeSettingsDetails() {
  runtimeDetails().forEach(p => { p.classList.remove('active'); p.setAttribute('aria-hidden', 'true') })
}

function openRuntimeServerSettings() {
  showSettingsDetail(runtimeServerPanel)
}

function backToRuntimeSettings() {
  showSettingsHome()
  updateRuntimeServiceState(runtimeServiceState || {})
}

function openRuntimeCapabilitiesSettings() {
  showSettingsDetail(runtimeCapabilitiesPanel)
}

function openRuntimeModelSettings() {
  showSettingsDetail(runtimeModelPanel)
  renderRuntimeModelChoices(runtimeModels)
}

function openRuntimeAdvancedSettings() {
  showSettingsDetail(runtimeAdvancedPanel)
}
