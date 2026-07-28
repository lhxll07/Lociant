/* ── Lociant WebUI — Runtime state management and commands ── */

function invokeRuntimeBridge(method, ...args) {
  try {
    const raw = native(method, ...args)
    const next = raw ? JSON.parse(raw) : { running: false, message: 'Runtime bridge unavailable' }
    updateRuntimeServiceState(next)
    return next
  } catch (error) {
    updateRuntimeServiceState({ running: false, message: 'Runtime bridge call failed' })
    return Promise.reject(error)
  }
}

function startRuntime(payload) { return invokeRuntimeBridge('startRuntime', JSON.stringify(payload || {})) }
function stopRuntime() { return invokeRuntimeBridge('stopRuntime') }
function updateRuntimeSettings(payload) { return invokeRuntimeBridge('updateRuntimeSettings', JSON.stringify(payload || {})) }
function releaseRuntimeModel() { return invokeRuntimeBridge('releaseRuntimeModel') }
function createRuntimeSession() { return invokeRuntimeBridge('createSession') }
function selectRuntimeSession(sessionId) { return invokeRuntimeBridge('selectSession', String(sessionId || '')) }
function deleteRuntimeSession(sessionId) { return invokeRuntimeBridge('deleteSession', String(sessionId || '')) }
function loadRuntimeSession(sessionId) { return invokeRuntimeBridge('sessionDetails', String(sessionId || '')) }
function startRuntimeVision(payload) { return invokeRuntimeBridge('startVision', JSON.stringify(payload || {})) }
function stopRuntimeVision() { return invokeRuntimeBridge('stopVision') }
function updateRuntimeWindow(payload) { return invokeRuntimeBridge('updateRuntimeWindow', JSON.stringify(payload || {})) }

function runtimeWindowCommand(command) {
  const next = command || ((runtimeServiceState && runtimeServiceState.windowVisible) ? 'hide' : 'show')
  const state = invokeRuntimeBridge(next === 'hide' ? 'hideRuntimeWindow' : 'showRuntimeWindow')
  return state.window || state
}

function updateRuntimeServiceState(state) {
  runtimeServiceState = Object.assign({}, runtimeServiceState || {}, state || {})
  const running = !!runtimeServiceState.running
  const starting = !!runtimeServiceState.starting
  const cameraGranted = runtimeServiceState.cameraPermissionGranted === true
  const notificationGranted = runtimeServiceState.notificationPermissionGranted === true
  const overlayGranted = runtimeServiceState.windowAllowed === true
  const batteryGranted = !!runtimeServiceState.batteryOptimizationIgnored
  const accessibilityGranted = runtimeServiceState.accessibilityPermissionGranted === true
  syncTopStatus()
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
  if (accessibilityPermissionState) accessibilityPermissionState.textContent = devicePermissionLabel(accessibilityGranted, t('settings.accessibilityPermissionGranted'), t('settings.accessibilityPermissionNeeded'))
  setPermissionButton(cameraPermissionButton, cameraGranted)
  setPermissionButton(notificationPermissionButton, notificationGranted)
  setPermissionButton(overlayPermissionButton, overlayGranted)
  setPermissionButton(batteryPermissionButton, batteryGranted)
  setPermissionButton(accessibilityPermissionButton, accessibilityGranted)
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
  updateModelExperienceState()
  if (runtimeServiceState.authToken !== undefined) {
    if (runtimeAuthTokenInput.value !== runtimeServiceState.authToken) {
      runtimeAuthTokenInput.value = runtimeServiceState.authToken || ''
    }
  }
  if (runtimeServiceState.toolExposure) runtimeToolExposureInput.value = runtimeServiceState.toolExposure
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
  updateHomeState()
}

function syncTopStatus() {
  if (!stateText && !stateDot) return
  const running = !!(runtimeServiceState && runtimeServiceState.running)
  const starting = !!(runtimeServiceState && runtimeServiceState.starting)
  const active = running || starting
  if (stateText) {
    stateText.textContent = starting ? t('status.starting') : (running ? t('status.running') : t('state.idle'))
    stateText.classList.toggle('running', active)
  }
  if (stateDot) stateDot.classList.toggle('running', active)
}

function updateModelExperienceState() {
  const state = runtimeServiceState || {}
  const maxThreads = Number(state.maxCpuThreads) || 16
  const threads = Number(state.cpuThreads) || 4
  const performance = performanceModeFromThreads(threads, maxThreads)
  if (runtimePerformanceModeInput) runtimePerformanceModeInput.value = performance
  if (runtimePerformanceText) runtimePerformanceText.textContent = performanceSubText(performance)

  const tokens = Number(state.maxOutputTokens) || 512
  const responseMode = responsePresetForTokens(tokens)
  if (runtimeResponseLengthInput) runtimeResponseLengthInput.value = responseMode || 'custom'
  if (runtimeResponseLengthText) runtimeResponseLengthText.textContent = tokens + ' tokens'
  if (runtimeResponseTokensInput && document.activeElement !== runtimeResponseTokensInput) {
    runtimeResponseTokensInput.max = String(Number(state.hardMaxOutputTokens) || 32768)
    runtimeResponseTokensInput.value = String(tokens)
  }
  if (runtimeResponseTokensInput) {
    runtimeResponseTokensInput.classList.toggle('is-hidden', !!responseMode)
  }

  const historyLimit = Number((state.sessionPolicy && state.sessionPolicy.historyLimit) || state.historyLimit) || 64
  const contextProfile = state.contextProfile || (state.sessionPolicy && state.sessionPolicy.contextProfile) || contextPresetForHistoryLimit(historyLimit)
  const contextMode = contextSelectValue(contextProfile, historyLimit)
  if (runtimeContextMemoryInput) runtimeContextMemoryInput.value = contextMode
  if (runtimeContextText) runtimeContextText.textContent = contextSubText(contextProfile) + (historyLimit ? (' / ' + historyLimit) : '')
  if (runtimeHistoryLimitInput && document.activeElement !== runtimeHistoryLimitInput) {
    const maxHistory = Number(state.sessionPolicy && state.sessionPolicy.maxHistoryLimit) || 256
    runtimeHistoryLimitInput.max = String(maxHistory)
    runtimeHistoryLimitInput.value = String(historyLimit)
  }
  if (runtimeHistoryLimitInput) {
    runtimeHistoryLimitInput.classList.toggle('is-hidden', contextMode !== 'custom')
  }

  const cache = state.sessionPolicy && state.sessionPolicy.cache
  const cacheOn = !!(cache && cache.promptCache)
  if (runtimeCacheState) runtimeCacheState.textContent = cacheOn ? t('settings.promptCacheOnSub') : t('settings.promptCacheOffSub')
  if (runtimeCacheBadge) {
    runtimeCacheBadge.textContent = cacheOn ? t('diagnostics.ready') : t('status.stopped')
    runtimeCacheBadge.classList.toggle('running', cacheOn)
  }
}

function performanceModeFromThreads(threads, maxThreads) {
  const max = Math.max(1, maxThreads)
  if (threads <= Math.max(1, Math.floor(max / 3))) return 'eco'
  if (threads >= Math.max(2, Math.ceil(max * 0.75))) return 'fast'
  return 'balanced'
}

function threadsForPerformanceMode(mode) {
  const max = Number(runtimeServiceState && runtimeServiceState.maxCpuThreads) || 16
  if (mode === 'eco') return Math.max(1, Math.floor(max / 3))
  if (mode === 'fast') return Math.max(1, max)
  return Math.max(1, Math.min(max, Math.round(max / 2)))
}

function responsePresetForTokens(tokens) {
  const value = Number(tokens)
  if (value === 256 || value === 512 || value === 1024) return String(value)
  return ''
}

function contextPresetForHistoryLimit(limit) {
  if (limit <= 24) return 'light'
  if (limit >= 96) return 'deep'
  return 'balanced'
}

function historyLimitForContextProfile(profile) {
  return ({ light: 16, balanced: 64, deep: 128 })[profile] || 64
}

function contextSelectValue(profile, historyLimit) {
  const normalized = ['light', 'balanced', 'deep'].includes(profile) ? profile : contextPresetForHistoryLimit(historyLimit)
  return Number(historyLimit) === historyLimitForContextProfile(normalized) ? normalized : 'custom'
}

function performanceSubText(mode) {
  return ({
    eco: t('settings.performanceEcoSub'),
    fast: t('settings.performanceFastSub'),
  })[mode] || t('settings.performanceBalancedSub')
}

function contextSubText(profile) {
  return ({
    light: t('settings.contextLightSub'),
    deep: t('settings.contextDeepSub'),
  })[profile] || t('settings.contextBalancedSub')
}

function updateHomeState() {
  const sessions = Array.isArray(runtimeServiceState && runtimeServiceState.sessions) ? runtimeServiceState.sessions : []
  const visibleSessions = typeof activeHomeSessions === 'function' ? activeHomeSessions(runtimeServiceState) : sessions
  syncTopStatus()
  if (homeSessionCount) homeSessionCount.textContent = String(visibleSessions.length)
  renderHomeSessions(sessions)
  if (typeof updateHomeChatContext === 'function') updateHomeChatContext()
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
