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
  stateText.textContent = t('state.idle')
  updateRuntimeStrip()
}

// ---- Clock tick ----
function tick() {
  clock.textContent = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  syncRuntimeSnapshot({ notifyScene: false })
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
