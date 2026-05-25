/* ── Lociant WebUI — UI components ── */

// ---- Sidebar ----
let sidebarBusy = false

function clearPress(target) {
  if (target) target.classList.remove('is-pressed')
}

function toggleSidebar(event) {
  if (event) event.preventDefault()
  if (window.matchMedia('(orientation: portrait), (max-width: 760px)').matches) return
  if (sidebarBusy) return
  sidebarBusy = true
  menuButton.classList.add('is-busy')
  app.classList.toggle('collapsed')
  window.setTimeout(() => {
    sidebarBusy = false
    menuButton.classList.remove('is-busy')
  }, 290)
}

// ---- Toast ----
function showToast(text) {
  toast.textContent = text
  toast.classList.add('active')
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => toast.classList.remove('active'), 2200)
}

// ---- Navigation ----
function navigateTo(page) {
  navItems.forEach(i => i.classList.toggle('active', i.dataset.page === page))
  unloadSceneFrame()
  backButton.classList.remove('active')
  activeScene = null
  setSceneSettingsVisible(false)
  showPagePanel(page)
  const activePanel = document.getElementById('page-' + page)
  if (activePanel) activePanel.scrollTop = 0
  if (page === 'models') {
    setModelView(modelView)
    loadModels()
  }
  stateText.textContent = runtimeSnapshot && runtimeSnapshot.running ? t('state.background') : t('state.idle')
  updateRuntimeStrip()
}

function openRuntimeServerFromHome() {
  navigateTo('settings')
  openRuntimeServerSettings()
}

function showHomeConversationLoading(text) {
  clearHomeMessages()
  appendChatBubble('assistant', text || t('home.thinking'))
}

function handleHomeAction(action) {
  if (action === 'diagnostics') {
    navigateTo('settings')
    openRuntimeAdvancedSettings()
    runAgentDiagnostics()
    return
  }
  if (action === 'copy-config') {
    openRuntimeServerFromHome()
    return
  }
  if (action === 'scenes') {
    navigateTo('scenes')
    return
  }
}

function submitHomeChat(text) {
  const prompt = String(text || '').trim()
  if (!prompt) return
  appendChatBubble('user', prompt)
  if (homeChatInput) homeChatInput.value = ''
  if (homeChatSendButton) homeChatSendButton.disabled = true
  const pending = appendChatBubble('assistant', t('home.thinking'))
  const modelId = (runtimeServiceState && runtimeServiceState.modelId) || ''
  const sessionId = homeCurrentSessionId()
  upsertHomeSessionPreview(sessionId, prompt, 'user')
  apiPost('/v1/chat/completions', {
    model: modelId,
    stream: false,
    sessionId,
    messages: [{ role: 'user', content: prompt }]
  }).then(result => {
    const reply = chatResponseText(result)
    if (pending) pending.textContent = reply || t('home.emptyReply')
    else appendChatBubble('assistant', reply || t('home.emptyReply'))
    if (result && result.sessionId) {
      runtimeServiceState = Object.assign({}, runtimeServiceState || {}, { currentSessionId: result.sessionId })
    }
    upsertHomeSessionPreview((result && result.sessionId) || sessionId, reply || prompt, 'assistant')
    refreshRuntimeServiceState()
  }).catch(error => {
    if (pending) pending.textContent = (error && error.message) || t('toast.modelImportFailed')
    else appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
  }).finally(() => {
    if (homeChatSendButton) homeChatSendButton.disabled = false
  })
}

function chatResponseText(result) {
  if (!result) return ''
  const choice = result.choices && result.choices[0]
  const message = choice && choice.message
  return String(
    (message && message.content) ||
    (result.message && result.message.content) ||
    result.response ||
    result.text ||
    ''
  ).trim()
}

function loadHomeConversation(sessionId) {
  const target = sessionId || homeCurrentSessionId()
  showHomeConversationLoading(t('home.thinking'))
  try {
    const state = runtimeApiCommand('session.details', { sessionId: target })
    const payload = state && state.session ? state.session : state
    const messages = payload && Array.isArray(payload.messages) ? payload.messages : []
    renderHomeConversation(target, messages)
  } catch (error) {
    clearHomeMessages()
    appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
  }
}

// ---- Clock tick ----
function tick() {
  clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  updateRuntimeStrip()
}

// ---- Runtime snapshot polling ----
let runtimePollTimer = null

function refreshRuntimeServiceState() {
  const state = shellCommand('status', {})
  updateRuntimeServiceState(state)
  runtimeSnapshot = state
  if (!runtimePollTimer) {
    runtimePollTimer = window.setInterval(() => {
      const next = shellCommand('status', {})
      const changed = JSON.stringify(next) !== JSON.stringify(runtimeSnapshot)
      if (changed || activeScene) {
        updateRuntimeServiceState(next)
        runtimeSnapshot = next
      }
    }, 4000)
  }
}

function syncRuntimeSnapshot(options) {
  const notify = options && options.notifyScene !== false
  const next = shellCommand('status', {})
  const changed = JSON.stringify(next) !== JSON.stringify(runtimeSnapshot)
  if (changed) {
    updateRuntimeServiceState(next)
    runtimeSnapshot = next
  }
  if (notify && activeScene) {
    postToSceneReliable({ type: 'runtime.snapshot', snapshot: runtimeSnapshot }, 3, 'runtime-snapshot')
  }
}

// ---- Runtime message handler ----
function handleRuntimeMessage(message) {
  if (!message) return
  if (message.type === 'alert') {
    showAlert(message)
    return
  }
  if (message.type === 'runtime.snapshot' && message.snapshot) {
    runtimeSnapshot = message.snapshot
    updateRuntimeServiceState(runtimeSnapshot)
    return
  }
  updateRuntimeServiceState(message)
}

function sendRuntimeCommand(sceneId, command, payload) {
  runtimeServiceCommand(command, Object.assign({}, payload, { sceneId: sceneId || undefined }))
}
