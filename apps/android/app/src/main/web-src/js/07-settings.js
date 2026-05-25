/* ── Lociant WebUI — Settings panels ── */

// ---- Locale ----
function loadLocaleSetting() {
  apiGet(localeStorePath)
    .then(result => {
      localeSetting = result && result.ok && result.value ? result.value : { mode: 'system' }
      applyLocale()
    })
    .catch(() => applyLocale())
}

function saveLocaleSetting(mode) {
  localeSetting = { mode: mode || 'system' }
  apiPost(localeStorePath, { value: localeSetting }).catch(() => {})
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
  Array.from(languageControl.querySelectorAll('.segmented-option')).forEach(button => {
    button.classList.toggle('active', button.dataset.langMode === (localeSetting.mode || 'system'))
  })
  updateRuntimeServiceState(runtimeServiceState || {})
  broadcastLocale()
}

function broadcastLocale() {
  postToScene({
    type: 'runtime.locale',
    language: currentLocale,
    mode: localeSetting.mode || 'system',
    fallback: 'en'
  })
}

// ---- Sessions ----
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
      runtimeApiCommand('session.select', { sessionId: session.id })
    })
    runtimeSessionList.appendChild(row)
  })
}

function renderHomeSessions(sessions) {
  if (!homeSessionList) return
  homeSessionList.innerHTML = ''
  const items = Array.isArray(sessions) ? sessions.slice(0, 8) : []
  if (!items.length) {
    homeSessionList.appendChild(el('div', 'chat-session-empty', t('home.noChats')))
    return
  }
  items.forEach(session => {
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'chat-session-item pressable'
    row.classList.toggle('active', session.id === (runtimeServiceState && runtimeServiceState.currentSessionId))
    const body = el('span', 'chat-session-body')
    const title = el('strong', '', session.title || session.id || '--')
    const sub = el('span', '', (session.modelId || '--') + ' · ' + (session.messageCount || 0))
    const remove = el('span', 'chat-session-delete', 'x')
    remove.setAttribute('role', 'button')
    remove.setAttribute('tabindex', '0')
    remove.setAttribute('aria-label', t('home.deleteChat'))
    body.appendChild(title)
    body.appendChild(sub)
    row.appendChild(body)
    row.appendChild(remove)
    row.addEventListener('click', () => {
      Promise.resolve(runtimeApiCommand('session.select', { sessionId: session.id }))
        .then(state => {
          updateRuntimeServiceState(Object.assign({}, state || {}, { currentSessionId: session.id }))
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
      Promise.resolve(runtimeApiCommand('session.delete', { sessionId: session.id }))
        .then(state => {
          updateRuntimeServiceState(state || {})
          if (state && state.currentSessionId) loadHomeConversation(state.currentSessionId)
          else clearHomeMessages()
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

function upsertHomeSessionPreview(sessionId, titleText, lastRole) {
  if (!sessionId) return
  const now = Date.now()
  const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions)
    ? runtimeServiceState.sessions.slice()
    : []
  const index = sessions.findIndex(session => session && session.id === sessionId)
  const existing = index >= 0 ? sessions[index] : {}
  const next = Object.assign({}, existing, {
    id: sessionId,
    title: existing.title || String(titleText || '').trim() || sessionId,
    modelId: existing.modelId || (runtimeServiceState && runtimeServiceState.modelId) || '--',
    updatedAt: now,
    messageCount: Math.max(Number(existing.messageCount) || 0, 1) + (lastRole === 'assistant' ? 1 : 0),
    lastRole: lastRole || existing.lastRole || 'user',
    lastText: String(titleText || existing.lastText || '').slice(0, 120),
  })
  if (index >= 0) sessions.splice(index, 1)
  sessions.unshift(next)
  runtimeServiceState = Object.assign({}, runtimeServiceState || {}, {
    currentSessionId: sessionId,
    sessions,
  })
  updateHomeState()
}

function homeCurrentSessionId() {
  return (runtimeServiceState && runtimeServiceState.currentSessionId) || 'model-server/chat/default'
}

function appendChatBubble(role, text, meta) {
  if (!homeChatFeed || !text) return null
  const node = document.createElement('div')
  node.className = 'chat-message ' + (role || 'assistant')
  if (meta && meta.active) node.dataset.activeSession = 'true'
  node.textContent = text
  homeChatFeed.appendChild(node)
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
  return node
}

function clearHomeMessages() {
  if (!homeChatFeed) return
  Array.from(homeChatFeed.querySelectorAll('.chat-message')).forEach(node => node.remove())
}

function renderHomeConversation(sessionId, messages) {
  if (!homeChatFeed) return
  clearHomeMessages()
  const items = Array.isArray(messages) ? messages : []
  if (!items.length) {
    appendChatBubble('assistant', t('home.noChats'))
    return
  }
  items.forEach(item => {
    const role = item && item.role ? item.role : 'assistant'
    const text = item && (item.text || item.content || item.message || '')
    if (text) appendChatBubble(role, text, { active: sessionId === homeCurrentSessionId() })
  })
  homeChatFeed.scrollTop = homeChatFeed.scrollHeight
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
    card.innerHTML = '<span class="settings-section-main">' +
      '<span class="settings-section-title">' + item.title + '</span>' +
      '<span class="settings-section-sub diagnostic-card-text">' + (item.text || '--') + '</span>' +
      '</span>' +
      '<span class="status-pill diagnostic-card-state">' + (item.ok ? t('diagnostics.ready') : t('diagnostics.issue')) + '</span>'
    runtimeDiagSummary.appendChild(card)
  })
}

function toolResultText(result, fallback) {
  if (!result) return fallback
  if (result.ok === false) return result.message || fallback
  return fallback
}

function runAgentDiagnostics() {
  if (!runtimeDiagRunButton) return
  runtimeDiagRunButton.disabled = true
  runtimeDiagRunButton.textContent = t('diagnostics.running')
  renderDiagnosticsSummary({ running: true })
  const state = shellCommand('status', {})
  updateRuntimeServiceState(state)
  Promise.allSettled([
    apiGet('/v1/tools'),
    apiPost('/v1/tools/runtime_status/call', { arguments: {} }),
    apiPost('/v1/tools/model_list/call', { arguments: {} }),
    apiPost('/v1/tools/vision_status/call', { arguments: {} })
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
    row.innerHTML = '<span class="diag-method">' + (req.method || '') + '</span>' +
      '<span class="diag-path">' + (req.endpoint || '') + '</span>' +
      '<span class="diag-status diag-' + (req.status < 400 ? 'ok' : 'err') + '">' + (req.status || '') + '</span>' +
      '<span class="diag-time">' + (req.elapsedMs || 0) + 'ms</span>'
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
