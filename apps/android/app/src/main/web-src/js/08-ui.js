/* ── Lociant WebUI — UI components ── */

// ---- Sidebar ----
let sidebarBusy = false
let nativeKeyboardInset = 0

function clearPress(target) {
  if (target) target.classList.remove('is-pressed')
}

function toggleSidebar(event) {
  if (event) event.preventDefault()
  if (window.matchMedia('(orientation: portrait), (max-width: 760px)').matches) {
    app.classList.toggle('mobile-nav-open')
    menuButton.classList.toggle('is-active', app.classList.contains('mobile-nav-open'))
    if (app.classList.contains('mobile-nav-open') && homeSidebar && homeRailToggle) {
      homeSidebar.classList.remove('open')
      homeRailToggle.setAttribute('aria-expanded', 'false')
      homeRailToggle.classList.remove('is-active')
    }
    return
  }
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
  app.classList.remove('mobile-nav-open')
  menuButton.classList.remove('is-active')
  setKeyboardOffset(0)
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

function syncKeyboardOffset() {
  const chatFocused = document.activeElement === homeChatInput
  if (!document.documentElement || !chatFocused) {
    setKeyboardOffset(0)
    return
  }
  const viewport = window.visualViewport
  const viewportHidden = viewport
    ? Math.max(0, window.innerHeight - viewport.height - viewport.offsetTop)
    : 0
  const hidden = Math.max(nativeKeyboardInset, viewportHidden)
  const offset = hidden > 80 ? Math.min(hidden, 320) : 0
  setKeyboardOffset(offset)
  if (offset && homeChatFeed) homeChatFeed.scrollTop = homeChatFeed.scrollHeight
}

function setKeyboardOffset(offset) {
  document.documentElement.style.setProperty('--keyboard-offset', offset + 'px')
  app.classList.toggle('keyboard-active', offset > 0)
}

window.__lociantKeyboardInset = function(insetPx) {
  nativeKeyboardInset = Math.max(0, Number(insetPx) || 0)
  syncKeyboardOffset()
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

function setHomeImageAttachment(file, dataUrl) {
  homeAttachedImage = file && dataUrl ? {
    name: file.name || t('home.imageAttached'),
    url: dataUrl,
  } : null
  if (!homeImagePreview) return
  const active = !!homeAttachedImage
  homeImagePreview.classList.toggle('active', active)
  homeImagePreview.setAttribute('aria-hidden', active ? 'false' : 'true')
  if (homeImagePreviewImg) homeImagePreviewImg.src = active ? homeAttachedImage.url : ''
  if (homeImagePreviewName) homeImagePreviewName.textContent = active ? homeAttachedImage.name : ''
}

function clearHomeImageAttachment() {
  setHomeImageAttachment(null, '')
  if (homeImageInput) homeImageInput.value = ''
}

function readHomeImage(file) {
  if (!file || !file.type || !file.type.startsWith('image/')) return
  const reader = new FileReader()
  reader.onload = () => setHomeImageAttachment(file, String(reader.result || ''))
  reader.onerror = () => showToast(t('toast.modelImportFailed'))
  reader.readAsDataURL(file)
}

function homeChatMessages(prompt, image) {
  if (!image) return [{ role: 'user', content: prompt }]
  const content = []
  if (prompt) content.push({ type: 'text', text: prompt })
  content.push({ type: 'image_url', image_url: { url: image.url } })
  return [{ role: 'user', content }]
}

function submitHomeChat(text) {
  const prompt = String(text || '').trim()
  const image = homeAttachedImage
  if (!prompt && !image) return
  appendChatBubble('user', image ? ((prompt || t('home.imageAttached')) + ' · ' + t('home.imageAttached')) : prompt)
  if (homeChatInput) homeChatInput.value = ''
  clearHomeImageAttachment()
  if (homeChatSendButton) homeChatSendButton.disabled = true
  const pending = appendChatBubble('assistant', t('home.thinking'))
  const modelId = (runtimeServiceState && runtimeServiceState.modelId) || ''
  const sessionId = homeCurrentSessionId()
  upsertHomeSessionPreview(sessionId, prompt || t('home.imageAttached'), 'user')
  apiPost('/v1/chat/completions', {
    model: modelId,
    stream: false,
    sessionId,
    messages: homeChatMessages(prompt, image)
  }).then(result => {
    const reply = chatResponseText(result)
    if (pending) renderChatMarkdown(pending, reply || t('home.emptyReply'))
    else appendChatBubble('assistant', reply || t('home.emptyReply'))
    if (result && result.sessionId) {
      runtimeServiceState = Object.assign({}, runtimeServiceState || {}, { currentSessionId: result.sessionId })
    }
    upsertHomeSessionPreview((result && result.sessionId) || sessionId, reply || prompt || t('home.imageAttached'), 'assistant')
    refreshRuntimeServiceState()
  }).catch(error => {
    if (pending) renderChatMarkdown(pending, (error && error.message) || t('toast.modelImportFailed'))
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

function loadHomeConversation(sessionId, options) {
  const target = sessionId || homeCurrentSessionId()
  const silent = !!(options && options.silent)
  if (!silent) showHomeConversationLoading(t('home.thinking'))
  try {
    const state = runtimeApiCommand('session.details', { sessionId: target })
    const payload = state && state.session ? state.session : state
    const messages = payload && Array.isArray(payload.messages) ? payload.messages : []
    updateRuntimeServiceState(Object.assign({}, state || {}, { currentSessionId: target }))
    renderHomeConversation(target, messages)
  } catch (error) {
    clearHomeMessages()
    if (!silent) appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
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
