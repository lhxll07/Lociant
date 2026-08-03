/* ── Lociant WebUI — UI components ── */

// ---- Sidebar ----
let sidebarBusy = false
let nativeKeyboardInset = 0
let homeChatInFlight = false
let homeChatFailure = null

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

let homeToolManifest = null
async function loadHomeToolManifest() {
  // Cache only a successful non-empty manifest. A failed or empty first fetch
  // (for example before the runtime is started) must not permanently disable
  // tool passing for later chats.
  if (Array.isArray(homeToolManifest) && homeToolManifest.length) return homeToolManifest
  try {
    const data = await apiGet('/api/v1/tools')
    const next = (data && Array.isArray(data.data)) ? data.data : []
    if (next.length) homeToolManifest = next
    return next
  } catch (error) {
    return homeToolManifest || []
  }
}

async function homeChatRequestBody(modelId, sessionId, prompt, image) {
  const body = {
    model: modelId,
    stream: true,
    stream_options: { include_usage: true },
    sessionId,
    messages: homeChatMessages(prompt, image)
  }
  const tools = await loadHomeToolManifest()
  if (Array.isArray(tools) && tools.length) {
    body.tools = tools
    body.execute_tools = true
  }
  return body
}

function submitHomeChat(text) {
  if (homeChatInFlight) return
  const prompt = String(text || '').trim()
  const image = homeAttachedImage
  if (!prompt && !image) return
  homeChatFailure = null
  appendChatBubble('user', image ? ((prompt || t('home.imageAttached')) + ' · ' + t('home.imageAttached')) : prompt)
  if (homeChatInput) homeChatInput.value = ''
  clearHomeImageAttachment()
  if (homeChatSendButton) homeChatSendButton.disabled = true
  const pending = appendAssistantRun()
  const modelId = (runtimeServiceState && runtimeServiceState.modelId) || ''
  const sessionId = homeCurrentSessionId()
  upsertHomeSessionPreview(sessionId, prompt || t('home.imageAttached'), 'user')
  homeChatInFlight = true
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
      const message = (error && error.message) || t('toast.modelImportFailed')
      // Remember interrupted turns so returning to the app restores them
      // instead of silently dropping the conversation.
      homeChatFailure = { sessionId: sessionId, message: message, prompt: prompt || t('home.imageAttached') }
      if (!document.hidden) {
        if (pending) renderChatMarkdown(chatTextTarget(pending), message)
        else appendChatBubble('assistant', message)
      }
    }).finally(() => {
      homeChatInFlight = false
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
  let reasoningDone = false
  let reasoningText = ''
  let reasoningNode = null
  let thinkingNode = null
  const toolAccumulator = createToolCallAccumulator()
  const toolCalls = []
  let toolSeq = 0
  const toolSeqByKey = new Map()
  // Live run status: the server emits lociant phase/ping events so the UI can
  // show progress instead of looking hung while a tool runs or the model thinks.
  let lastProgressAt = Date.now()
  let streamFinished = false
  const watchdog = window.setInterval(() => {
    if (streamFinished) return
    const idleMs = Date.now() - lastProgressAt
    if (idleMs > 45000) {
      updateRunStatus(target, runStatusText('home.runStatusStall', [Math.round(idleMs / 1000)]))
    }
  }, 1000)
  function ensureReasoningUi() {
    if (reasoningNode || !target) return
    const scope = chatTextTarget(target)
    if (!scope) return
    reasoningNode = el('div', 'chat-reasoning')
    thinkingNode = el('span', 'chat-thinking', t('home.thinking'))
    const body = el('span', 'chat-reasoning-text')
    reasoningNode.appendChild(thinkingNode)
    reasoningNode.appendChild(body)
    scope.insertBefore(reasoningNode, scope.firstChild)
  }
  function finishReasoningUi() {
    if (reasoningDone || !thinkingNode) return
    reasoningDone = true
    thinkingNode.textContent = t('home.thought')
    thinkingNode.classList.add('done')
  }
  try {
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
          if (json && json.error) throw new Error((json.error.message) || 'API request failed')
          if (json && json.lociant) {
            const info = json.lociant
            if (info.type === 'ping') {
              continue
            }
            if (info.type === 'phase') {
              lastProgressAt = Date.now()
              if (info.phase === 'tool_running') {
                updateRunStatus(target, runStatusText('home.runStatusTool', [info.tool || '', info.round || '']))
              } else if (info.phase === 'round') {
                updateRunStatus(target, runStatusText('home.runStatusRound', [info.round || '']))
              } else if (info.phase === 'retry') {
                updateRunStatus(target, runStatusText('home.runStatusRetry'))
              } else if (info.phase === 'tool_done') {
                updateRunStatus(target, '')
              }
              continue
            }
          }
          const delta = json.choices && json.choices[0] && json.choices[0].delta
          const reasoning = delta && typeof delta.reasoning_content === 'string' ? delta.reasoning_content : ''
          if (reasoning) {
            lastProgressAt = Date.now()
            reasoningText += reasoning
            ensureReasoningUi()
            if (reasoningNode) reasoningNode.querySelector('.chat-reasoning-text').textContent = reasoningText
          }
          if (delta && typeof delta.content === 'string') {
            lastProgressAt = Date.now()
            if (reasoningText) finishReasoningUi()
            text += delta.content
            if (writer) writer.push(delta.content)
          }
          const calls = delta ? normalizeToolCalls(delta.tool_calls) : []
          if (calls.length) lastProgressAt = Date.now()
          calls.forEach(call => {
            const merged = toolAccumulator.push(call)
            const key = merged._key
            if (!toolSeqByKey.has(key)) toolSeqByKey.set(key, toolSeq++)
            appendToolBubble(merged, target, toolSeqByKey.get(key))
          })
        }
      }
    }
  } finally {
    streamFinished = true
    window.clearInterval(watchdog)
    updateRunStatus(target, '')
  }
  if (reasoningText && !reasoningDone) finishReasoningUi()
  if (writer) writer.finish(text)
  toolCalls.push.apply(toolCalls, toolAccumulator.values())
  if (!text && target && !toolCalls.length) {
    renderChatMarkdown(chatTextTarget(target), t('home.emptyReply'))
  } else if (!text && target && toolCalls.length) {
    // Tools ran but the model produced no final text; show a completion note
    // instead of looking like the session died silently.
    renderChatMarkdown(chatTextTarget(target), t('home.toolRunDone'))
  }
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
      const partIndex = (part && part.index !== undefined && part.index !== null) ? part.index : calls.length
      const key = (part && part.id) || String(partIndex)
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
    const state = loadRuntimeSession(target)
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
  const state = runtimeState()
  if (homeChatInFlight && state && Array.isArray(state.sessions)) {
    // While a chat is streaming, the in-flight turn is not persisted yet;
    // keep the live session previews so the current session stays in the list.
    delete state.sessions
  }
  const signature = runtimeStateSignature(state)
  if (signature !== runtimePollSignature) {
    runtimePollSignature = signature
    updateRuntimeServiceState(state)
  }
  if (!runtimePollTimer) {
    runtimePollTimer = window.setInterval(() => {
      const next = runtimeState()
      if (homeChatInFlight && next && Array.isArray(next.sessions)) {
        // Do not let the two-second runtime poll replace the live preview while
        // the server is still processing the current turn.
        delete next.sessions
      }
      const nextSignature = runtimeStateSignature(next)
      if (nextSignature !== runtimePollSignature) {
        runtimePollSignature = nextSignature
        updateRuntimeServiceState(next)
      }
    }, 2000)
  }
}

// ---- Runtime message handler ----
function handleRuntimeMessage(message) {
  if (!message) return
  updateRuntimeServiceState(message)
  runtimePollSignature = runtimeStateSignature(runtimeServiceState)
}
