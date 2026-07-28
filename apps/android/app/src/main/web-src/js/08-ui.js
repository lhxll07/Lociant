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
  showPagePanel(page)
  const activePanel = document.getElementById('page-' + page)
  if (activePanel) activePanel.scrollTop = 0
  if (page === 'models') {
    setModelView(modelView)
    loadModels()
  }
  syncTopStatus()
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
    runRuntimeDiagnostics()
    return
  }
  if (action === 'copy-config') {
    openRuntimeServerFromHome()
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

function homeChatRequestBody(modelId, sessionId, prompt, image) {
  return {
    model: modelId,
    stream: true,
    stream_options: { include_usage: true },
    sessionId,
    messages: homeChatMessages(prompt, image)
  }
}

function submitHomeChat(text) {
  const prompt = String(text || '').trim()
  const image = homeAttachedImage
  if (!prompt && !image) return
  appendChatBubble('user', image ? ((prompt || t('home.imageAttached')) + ' · ' + t('home.imageAttached')) : prompt)
  if (homeChatInput) homeChatInput.value = ''
  clearHomeImageAttachment()
  if (homeChatSendButton) homeChatSendButton.disabled = true
  const pending = appendAssistantRun()
  const modelId = (runtimeServiceState && runtimeServiceState.modelId) || ''
  const sessionId = homeCurrentSessionId()
  upsertHomeSessionPreview(sessionId, prompt || t('home.imageAttached'), 'user')
  Promise.resolve(homeChatRequestBody(modelId, sessionId, prompt, image))
    .then(body => streamOpenAiHomeChat(body, pending))
    .then(result => {
      const toolCalls = Array.isArray(result && result.toolCalls) ? result.toolCalls : []
      const text = (result && result.text) || ''
      const reply = text || t('home.emptyReply')
      if (pending && !text && !toolCalls.length) renderChatMarkdown(chatTextTarget(pending), reply)
      upsertHomeSessionPreview(sessionId, reply || prompt || t('home.imageAttached'), 'assistant')
      refreshRuntimeServiceState()
    }).catch(error => {
      if (pending) renderChatMarkdown(chatTextTarget(pending), (error && error.message) || t('toast.modelImportFailed'))
      else appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
    }).finally(() => {
      if (homeChatSendButton) homeChatSendButton.disabled = false
    })
}

async function streamOpenAiHomeChat(body, target) {
  const headers = { 'Content-Type': 'application/json' }
  if (runtimeServiceState && runtimeServiceState.authToken) headers.Authorization = 'Bearer ' + runtimeServiceState.authToken
  const response = await fetch(apiUrl('/v1/chat/completions'), {
    method: 'POST',
    headers,
    body: JSON.stringify(body || {})
  })
  if (!response.ok) {
    const errorJson = await response.json().catch(() => ({}))
    throw new Error((errorJson.error && errorJson.error.message) || errorJson.message || 'API request failed')
  }
  if (!response.body || !response.body.getReader) {
    const json = await response.json()
    const text = chatResponseText(json)
    const toolCalls = collectToolCallsFromMessage(json)
    toolCalls.forEach(call => appendToolBubble(call, target))
    if (target) renderChatMarkdown(chatTextTarget(target), text || t('home.emptyReply'))
    return { text, toolCalls }
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  const writer = target ? createChatTextStream(chatTextTarget(target)) : null
  let buffer = ''
  let text = ''
  const toolAccumulator = createToolCallAccumulator()
  const toolCalls = []
  while (true) {
    const read = await reader.read()
    if (read.done) break
    buffer += decoder.decode(read.value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    for (const event of events) {
      const lines = event.split('\n').map(line => line.trim()).filter(Boolean)
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (!data || data === '[DONE]') continue
        const json = JSON.parse(data)
        const delta = json.choices && json.choices[0] && json.choices[0].delta
        if (delta && typeof delta.content === 'string') {
          text += delta.content
          if (writer) writer.push(delta.content)
        }
        const calls = delta ? normalizeToolCalls(delta.tool_calls) : []
        calls.forEach(call => {
          const merged = toolAccumulator.push(call)
          appendToolBubble(merged, target)
        })
      }
    }
  }
  if (writer) writer.finish(text)
  toolCalls.push.apply(toolCalls, toolAccumulator.values())
  if (!text && target && !toolCalls.length) renderChatMarkdown(chatTextTarget(target), t('home.emptyReply'))
  return { text, toolCalls }
}

function normalizeToolCalls(raw) {
  if (!Array.isArray(raw)) return []
  return raw.map((call, index) => {
    const fn = call && call.function ? call.function : {}
    return {
      id: (call && call.id) || String((call && call.index) !== undefined ? call.index : index),
      index: (call && call.index) !== undefined ? call.index : index,
      type: call && call.type,
      name: fn.name || (call && call.name) || 'tool',
      arguments: fn.arguments || (call && call.arguments) || ''
    }
  })
}

function createToolCallAccumulator() {
  const calls = []
  return {
    push(part) {
      const key = (part && part.id) || String((part && part.index) || calls.length)
      let current = calls.find(call => call._key === key)
      if (!current) {
        current = { _key: key, id: part && part.id, index: part && part.index, type: part && part.type, name: '', arguments: '' }
        calls.push(current)
      }
      if (part && part.id) current.id = part.id
      if (part && part.type) current.type = part.type
      if (part && part.name) current.name = part.name
      if (part && part.arguments) current.arguments += part.arguments
      return current
    },
    values() {
      return calls.map(call => ({
        id: call.id || call._key,
        index: call.index,
        type: call.type,
        name: call.name || 'tool',
        arguments: call.arguments || ''
      }))
    }
  }
}

function collectToolCallsFromMessage(result) {
  const choice = result && result.choices && result.choices[0]
  const message = choice && choice.message
  return normalizeToolCalls(message && message.tool_calls)
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
    updateRuntimeServiceState(markHomeSessionActive(state || {}, target))
    renderHomeConversation(target, messages)
    if (typeof updateHomeChatContext === 'function') updateHomeChatContext()
  } catch (error) {
    clearHomeMessages()
    if (!silent) appendChatBubble('assistant', (error && error.message) || t('toast.modelImportFailed'))
  }
}

// ---- Clock tick ----
function tick() {
  clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

// ---- Runtime polling ----
let runtimePollTimer = null
let runtimePollSignature = ''

function runtimeStateSignature(state) {
  const current = state || {}
  const vision = current.vision || {}
  const detectionCount = Array.isArray(vision.lastDetection && vision.lastDetection.detections)
    ? vision.lastDetection.detections.length
    : 0
  const device = current.device || {}
  const sessions = Array.isArray(current.sessions) ? current.sessions : []
  const requests = Array.isArray(current.recentRequests) ? current.recentRequests : []
  const sessionSignature = sessions.map(item => [item.id, item.updatedAt, item.messageCount].join(':')).join(',')
  const requestSignature = requests.map(item => [item.method, item.endpoint, item.status, item.elapsedMs].join(':')).join(',')
  return [
    current.running, current.starting, current.message, current.port, current.lanUrl, current.url,
    current.modelId, current.modelLoaded, current.maxOutputTokens, current.cpuThreads,
    current.contextProfile, current.historyLimit, current.toolExposure, current.authToken,
    current.autoStart, current.currentSessionId, current.requestCount,
    current.cameraPermissionGranted, current.notificationPermissionGranted,
    current.windowAllowed, current.windowVisible, current.windowState, current.windowAutoShow,
    current.batteryOptimizationIgnored, current.accessibilityPermissionGranted,
    vision.state, vision.running, vision.message, vision.fps, detectionCount,
    device.interactive, device.keyguardLocked, device.activityForeground,
    sessionSignature, requestSignature,
  ].join('\x1f')
}

function refreshRuntimeServiceState() {
  const state = shellCommand('status', {})
  const signature = runtimeStateSignature(state)
  if (signature !== runtimePollSignature) {
    runtimePollSignature = signature
    updateRuntimeServiceState(state)
  }
  if (!runtimePollTimer) {
    runtimePollTimer = window.setInterval(() => {
      const next = shellCommand('status', {})
      const nextSignature = runtimeStateSignature(next)
      if (nextSignature !== runtimePollSignature) {
        runtimePollSignature = nextSignature
        updateRuntimeServiceState(next)
      }
    }, 4000)
  }
}

// ---- Runtime message handler ----
function handleRuntimeMessage(message) {
  if (!message) return
  updateRuntimeServiceState(message)
  runtimePollSignature = runtimeStateSignature(runtimeServiceState)
}
