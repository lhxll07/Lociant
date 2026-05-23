/* ── Lociant WebUI — Runtime state management and commands ── */

function runtimeApiCommand(command, payload) {
  try {
    const body = Object.assign({}, payload || {}, {
      sceneId: (payload && payload.sceneId) || (activeScene && activeScene.id) || ''
    })
    const runShell = ['start', 'stop', 'status', 'settings', 'battery.requestExemption',
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
  const cameraGranted = runtimeServiceState.cameraPermissionGranted === true
  const notificationGranted = runtimeServiceState.notificationPermissionGranted === true
  const overlayGranted = runtimeServiceState.windowAllowed === true
  const batteryGranted = !!runtimeServiceState.batteryOptimizationIgnored
  if (stateDot) stateDot.classList.toggle('running', running || starting)
  if (stateText) stateText.classList.toggle('running', running || starting)
  runtimeSettingsState.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('status.stopped'))
  runtimeSettingsState.classList.toggle('running', running || starting)
  runtimeServiceToggle.textContent = running || starting ? t('common.stop') : t('common.start')
  runtimeServiceMessage.textContent = runtimeServiceStatusText(runtimeServiceState, running, starting)
  if (runtimeAutoStartInput) runtimeAutoStartInput.checked = !!runtimeServiceState.autoStart
  runtimeServerState.textContent = runtimeServiceState.port ? String(runtimeServiceState.port) : '--'
  if (cameraPermissionState) cameraPermissionState.textContent = devicePermissionLabel(cameraGranted, t('settings.cameraPermissionGranted'), t('settings.cameraPermissionNeeded'))
  if (notificationPermissionState) notificationPermissionState.textContent = devicePermissionLabel(notificationGranted, t('settings.notificationPermissionGranted'), t('settings.notificationPermissionNeeded'))
  if (overlayPermissionState) overlayPermissionState.textContent = devicePermissionLabel(overlayGranted, t('settings.overlayPermissionGranted'), t('settings.overlayPermissionNeeded'))
  if (batteryPermissionState) batteryPermissionState.textContent = devicePermissionLabel(batteryGranted, t('settings.batteryAllowed'), t('settings.batteryRestricted'))
  setPermissionButton(cameraPermissionButton, cameraGranted)
  setPermissionButton(notificationPermissionButton, notificationGranted)
  setPermissionButton(overlayPermissionButton, overlayGranted)
  setPermissionButton(batteryPermissionButton, batteryGranted)
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
  if (runtimeServiceState.port && (runtimeServiceState.lanUrl || runtimeServiceState.url)) {
    diagUrl.textContent = runtimeServiceState.lanUrl || publicRuntimeUrl(runtimeServiceState)
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
  if (runtimeDeviceState) {
    const device = runtimeServiceState.device || {}
    const parts = []
    parts.push(device.interactive ? 'interactive' : 'idle')
    if (device.keyguardLocked) parts.push('locked')
    if (!device.activityForeground) parts.push('background')
    runtimeDeviceState.textContent = parts.join(' · ')
  }
  if (runtimeServiceState.sessions) renderSessions(runtimeServiceState.sessions)
  if (runtimeAdvancedState) {
    const sessions = Array.isArray(runtimeServiceState.sessions) ? runtimeServiceState.sessions.length : 0
    const requests = Number(runtimeServiceState.requestCount) || 0
    runtimeAdvancedState.textContent = sessions + ' / ' + requests
  }
  if (runtimeServiceState.requestCount !== undefined || runtimeServiceState.recentRequests) {
    updateDiagnostics(runtimeServiceState)
  }
  updateRuntimeStrip()
}

function runtimeServiceStatusText(state, running, starting) {
  if (starting) return t('status.starting')
  if (running) return publicRuntimeUrl(state)
  return t('settings.runtimeDefaultMessage')
}

function visionState() {
  return (runtimeServiceState && runtimeServiceState.vision) || {}
}

function devicePermissionLabel(granted, okText, badText) {
  return granted ? okText : badText
}

function setPermissionButton(button, granted) {
  if (!button) return
  button.textContent = granted ? t('settings.manage') : t('settings.grant')
  button.dataset.permissionGranted = granted ? 'true' : 'false'
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
