/* ── Lociant WebUI — Runtime state management and commands ── */

function runtimeApiCommand(command, payload) {
  try {
    const body = Object.assign({}, payload || {}, {
      sceneId: (payload && payload.sceneId) || (activeScene && activeScene.id) || ''
    })
    const runShell = ['start', 'stop', 'status', 'battery.requestExemption',
      'window.show', 'window.hide', 'window.collapse', 'window.expand',
      'window.settings', 'window.permission', 'vision.start', 'vision.stop', 'vision.status'
    ].includes(command)
    if (runShell) {
      updateRuntimeServiceState(shellCommand(command, body))
      return
    }
    const promise = apiPost('/v1/runtime/' + encodeURIComponent(command), body)
    promise.then(state => {
      updateRuntimeServiceState(state)
    }).catch(() => updateRuntimeServiceState({ running: false, message: 'API server command failed' }))
  } catch (error) {
    updateRuntimeServiceState({ running: false, message: 'API server command failed' })
  }
}

function runtimeServiceCommand(command, payload) {
  try {
    updateRuntimeServiceState(shellCommand(command, payload))
  } catch (error) {
    updateRuntimeServiceState({ running: false, message: 'Runtime service command failed' })
  }
}

function runtimeWindowCommand(command) {
  const next = command || ((runtimeServiceState && runtimeServiceState.windowVisible) ? 'hide' : 'show')
  const state = shellCommand('window.' + next, {})
  updateRuntimeServiceState(state)
  return state.window || state
}

function updateRuntimeServiceState(state) {
  runtimeServiceState = Object.assign({}, runtimeServiceState || {}, state || {})
  publishSceneApiClient()
  const running = !!runtimeServiceState.running
  const starting = !!runtimeServiceState.starting
  if (stateDot) stateDot.classList.toggle('running', running || starting)
  if (stateText) stateText.classList.toggle('running', running || starting)
  runtimeSettingsState.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
  runtimeSettingsState.classList.toggle('running', running || starting)
  runtimeServiceToggle.textContent = running || starting ? t('common.stop') : t('common.start')
  runtimeServiceMessage.textContent = runtimeServiceState.message || t('settings.runtimeDefaultMessage')
  runtimeServerState.textContent = runtimeServiceState.port ? String(runtimeServiceState.port) : '--'
  const vision = visionState()
  const visionRunning = !!vision.running
  const visionStarting = String(vision.state || '').toLowerCase() === 'starting'
  const visionLabel = visionStateLabel(vision)
  runtimeCapabilitiesState.textContent = visionLabel + ' · ' + runtimeWindowLabel()
  runtimeCapabilitiesState.classList.toggle('running', visionRunning || visionStarting)
  runtimeVisionText.textContent = vision.message || (
    visionRunning
      ? (Math.round(Number(vision.fps) || 0) + ' fps · ' + ((((vision.lastDetection || {}).detections) || []).length || 0) + ' detections')
      : t('settings.visionSub')
  )
  runtimeVisionButton.textContent = visionRunning ? t('common.stop') : t('common.start')
  runtimeWindowAutoInput.checked = !!runtimeServiceState.windowAutoShow
  runtimeWindowAutoInput.disabled = !running || !runtimeServiceState.port
  runtimeWindowButton.textContent =
    runtimeServiceState.windowVisible ? t('settings.windowHide') : t('settings.windowShow')
  if (runtimeServiceState.windowAllowed !== undefined) {
    runtimeWindowPermissionButton.style.display = runtimeServiceState.windowAllowed ? 'none' : ''
  }
  runtimeModelState.textContent = (runtimeServiceState && runtimeServiceState.modelId) || '--'
  runtimePortInput.value = runtimeServiceState.port || ''
  runtimeMaxTokensInput.value = runtimeServiceState.maxOutputTokens || ''
  if (runtimeServiceState.authToken !== undefined) {
    if (runtimeAuthTokenInput.value !== runtimeServiceState.authToken) {
      runtimeAuthTokenInput.value = runtimeServiceState.authToken || ''
    }
  }
  if (runtimeServiceState.toolExposure) runtimeToolExposureInput.value = runtimeServiceState.toolExposure
  if (runtimeServiceState.cpuThreads) runtimeCpuThreadsInput.value = runtimeServiceState.cpuThreads
  if (runtimeServiceState.currentSessionId) {
    runtimeSessionCurrent.textContent = runtimeServiceState.currentSessionId
  }
  if (runtimeServiceState.port && runtimeServiceState.url) {
    diagUrl.textContent = runtimeServiceState.url.replace('0.0.0.0', localApiBaseUrl().split(':')[0] || '127.0.0.1')
  }
  if (runtimeServiceState.running !== undefined) {
    diagApi.textContent = running ? t('status.running') : (starting ? t('status.starting') : t('status.stopped'))
  }
  const hard = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
  const effective = runtimeServiceState.effectiveMaxOutputTokens || '--'
  const defaultVal = runtimeServiceState.defaultOutputTokens || '--'
  const modelVal = runtimeServiceState.modelMaxOutputTokens || '--'
  if (runtimeDefaultTokens) runtimeDefaultTokens.textContent = String(defaultVal)
  if (runtimeModelTokens) runtimeModelTokens.textContent = String(modelVal)
  if (runtimeEffectiveTokens) runtimeEffectiveTokens.textContent = String(effective)
  if (runtimeServiceState.sessions) renderSessions(runtimeServiceState.sessions)
  if (runtimeServiceState.requestCount !== undefined || runtimeServiceState.recentRequests) {
    updateDiagnostics(runtimeServiceState)
  }
  batteryAction(runtimeServiceState)
  updateRuntimeStrip()
}

function visionState() {
  return (runtimeServiceState && runtimeServiceState.vision) || {}
}

function visionStateLabel(vision) {
  const current = vision || {}
  const state = String(current.state || '').toLowerCase()
  if (current.running || state === 'running') return t('status.running')
  if (state === 'starting') return t('settings.visionStarting')
  if (state === 'unavailable') return t('settings.visionUnavailable')
  if (state === 'idle') return t('status.stopped')
  return state || t('status.stopped')
}

function runtimeWindowLabel(state) {
  const value = state || (runtimeServiceState && runtimeServiceState.windowState) || 'hidden'
  return ({
    expanded: t('settings.windowStateVisible'),
    collapsed: t('settings.windowStateCollapsed'),
    hidden: t('settings.windowStateHidden'),
    error: t('settings.windowStateError')
  })[value] || t('settings.windowStateWindow')
}

function runtimeStateLabel(state) {
  if (state === 'away') return 'Away'
  if (state === 'distracted' || state === 'phone') return 'Distracted'
  if (state === 'focused') return 'Focused'
  return state || t('state.idle')
}

function fmtDuration(ms) {
  const total = Math.max(0, Math.floor((Number(ms) || 0) / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  if (h > 0) return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
  return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
}

function sceneById(sceneId) {
  return scenes.find(scene => scene.id === sceneId) || null
}

function runtimeScene() {
  if (!runtimeSnapshot || !runtimeSnapshot.sceneId) return null
  return scenes.find(scene => scene.id === runtimeSnapshot.sceneId) || null
}

function updateRuntimeStrip() {
  const scene = runtimeScene()
  const visible = !!(runtimeSnapshot && runtimeSnapshot.active && scene && !activeScene)
  runtimeStrip.classList.toggle('visible', visible)
  runtimeStrip.setAttribute('aria-hidden', visible ? 'false' : 'true')
  if (!visible) return

  const events = Array.isArray(runtimeSnapshot.events) ? runtimeSnapshot.events : []
  const latest = events[0]
  runtimeSceneName.textContent = scene.name || scene.id
  runtimeWindowText.textContent = runtimeWindowLabel()
  runtimeWindowText.classList.toggle('active', !!(runtimeServiceState && runtimeServiceState.windowVisible))
  runtimeStateText.textContent = runtimeStateLabel(runtimeSnapshot.state)
  runtimeElapsedText.textContent = fmtDuration(runtimeSnapshot.elapsedMs)
  runtimeEventText.textContent = latest
    ? ((latest.name || latest.ruleId || 'event') + ' · ' + new Date(latest.timestamp || Date.now()).toLocaleTimeString('zh-CN', { hour12: false }))
    : ((runtimeSnapshot.running || runtimeSnapshot.sessionState === 'running') ? t('state.background') : 'Paused')
}

// ---- Battery ----
function batteryAction(state) {
  if (!batteryOptimizationText) return
  if (!state || !state.running) {
    batteryOptimizationText.textContent = t('settings.checking')
    batteryOptimizationButton.style.display = 'none'
    return
  }
  if (state.batteryExempted) {
    batteryOptimizationText.textContent = t('settings.batteryAllowed')
    batteryOptimizationButton.style.display = 'none'
    return
  }
  batteryOptimizationText.textContent = t('settings.batteryRestricted')
  batteryOptimizationButton.style.display = ''
}
