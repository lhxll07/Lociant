/* ── Lociant WebUI — Event binding and initialization ── */

// ---- Scene frame events ----
sceneFrame.addEventListener('load', () => {
  installSceneApiClient()
  resizeSceneFrame()
  window.setTimeout(resizeSceneFrame, 80)
  window.setTimeout(resizeSceneFrame, 300)
  window.setTimeout(syncRuntimeSnapshot, 120)
})

window.addEventListener('resize', resizeSceneFrame)
sceneHost.addEventListener('scroll', () => syncCameraPreviewRect(), { passive: true })
app.addEventListener('transitionend', () => syncCameraPreviewRect())

// ---- Sidebar ----
menuButton.addEventListener('pointerdown', event => {
  event.preventDefault()
  menuButton.classList.add('is-pressed')
  toggleSidebar(event)
})
menuButton.addEventListener('pointerup', () => clearPress(menuButton))
menuButton.addEventListener('pointercancel', () => clearPress(menuButton))
menuButton.addEventListener('mouseleave', () => clearPress(menuButton))

document.addEventListener('pointerdown', event => {
  const target = event.target.closest('.pressable')
  if (target && target !== menuButton) target.classList.add('is-pressed')
}, { passive: true })

document.addEventListener('pointerup', event => {
  const target = event.target.closest('.pressable')
  clearPress(target)
}, { passive: true })

document.addEventListener('pointercancel', event => {
  const target = event.target.closest('.pressable')
  clearPress(target)
}, { passive: true })

// ---- Navigation ----
navItems.forEach(item => {
  item.addEventListener('click', () => {
    navigateTo(item.dataset.page)
  })
})

// ---- Buttons ----
backButton.addEventListener('click', goHome)
sceneSettingsButton.addEventListener('click', toggleSceneSettings)
alertOpenButton.addEventListener('click', openAlertScene)
alertCloseButton.addEventListener('click', clearAlert)
runtimeStrip.addEventListener('click', () => {
  const scene = runtimeScene()
  if (scene) openScene(scene)
})
runtimeWindowText.addEventListener('click', event => {
  event.stopPropagation()
  runtimeWindowCommand()
})

// ---- Settings navigation ----
runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
runtimeSettingsBack.addEventListener('click', closeRuntimeSettingsBack)
runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
runtimeServerBack.addEventListener('click', backToRuntimeSettings)
runtimeCapabilitiesButton.addEventListener('click', openRuntimeCapabilitiesSettings)
runtimeCapabilitiesBack.addEventListener('click', backToRuntimeSettings)
runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
runtimeModelBack.addEventListener('click', backToRuntimeSettings)
runtimeSessionsButton.addEventListener('click', openRuntimeSessionsSettings)
runtimeSessionsBack.addEventListener('click', backToRuntimeSettings)
runtimeDiagnosticsButton.addEventListener('click', openRuntimeDiagnosticsSettings)
runtimeDiagnosticsBack.addEventListener('click', backToRuntimeSettings)

// ---- Runtime controls ----
runtimeServiceToggle.addEventListener('click', () => {
  const running = runtimeServiceState && (runtimeServiceState.running || runtimeServiceState.starting)
  runtimeApiCommand(running ? 'stop' : 'start', {})
})
runtimeVisionButton.addEventListener('click', () => {
  const vision = visionState()
  runtimeServiceCommand(vision && vision.running ? 'vision.stop' : 'vision.start', {})
})
runtimeWindowAutoInput.addEventListener('change', () => {
  runtimeServiceCommand('window.settings', { autoShow: !!runtimeWindowAutoInput.checked })
})
runtimeWindowButton.addEventListener('click', () => {
  runtimeWindowCommand()
})
runtimeWindowPermissionButton.addEventListener('click', () => {
  runtimeServiceCommand('window.permission', {})
})

// ---- Server settings ----
runtimePortInput.addEventListener('change', () => {
  const value = Math.max(1024, Math.min(65535, Math.round(Number(runtimePortInput.value) || 11434)))
  runtimePortInput.value = String(value)
  runtimeApiCommand('settings', { port: value })
})
runtimeMaxTokensInput.addEventListener('change', () => {
  const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
  const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeMaxTokensInput.value) || 512)))
  runtimeMaxTokensInput.value = String(value)
  runtimeApiCommand('settings', { maxOutputTokens: value })
})
runtimeAuthTokenInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { authToken: runtimeAuthTokenInput.value.trim() })
})
runtimeAuthGenerateButton.addEventListener('click', () => {
  runtimeApiCommand('settings', { generateAuthToken: true })
})
runtimeAuthClearButton.addEventListener('click', () => {
  runtimeAuthTokenInput.value = ''
  runtimeApiCommand('settings', { authToken: '' })
})

// ---- Capabilities settings ----
runtimeToolExposureInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { toolExposure: runtimeToolExposureInput.value || 'action' })
})
runtimeCpuThreadsInput.addEventListener('change', () => {
  const max = Number(runtimeServiceState && runtimeServiceState.maxCpuThreads) || 16
  const value = Math.max(1, Math.min(max, Math.round(Number(runtimeCpuThreadsInput.value) || 4)))
  runtimeCpuThreadsInput.value = String(value)
  runtimeApiCommand('settings', { cpuThreads: value })
})

// ---- Session ----
runtimeSessionNewButton.addEventListener('click', () => {
  runtimeApiCommand('session.create', {})
})

// ---- Battery ----
batteryOptimizationButton.addEventListener('click', () => {
  runtimeServiceCommand('battery.requestExemption', {})
})

// ---- Language ----
languageControl.addEventListener('click', event => {
  const button = event.target.closest('.segmented-option')
  if (button && button.dataset.langMode) saveLocaleSetting(button.dataset.langMode)
})

// ---- Model actions ----
modelReloadButton.addEventListener('click', () => {
  loadModels()
  showToast(t('toast.modelsReloaded'))
})
modelImportButton.addEventListener('click', () => {
  native('installModelPackage')
})
modelLocalButton.addEventListener('click', () => setModelView('local'))
modelLocalBack.addEventListener('click', () => setModelView('home'))
modelMarketButton.addEventListener('click', () => setModelView('market'))
modelMarketBack.addEventListener('click', () => setModelView('home'))
modelRuntimeButton.addEventListener('click', () => {
  navigateTo('settings')
  openRuntimeSettings()
})
modelMarketRefreshButton.addEventListener('click', () => loadModelMarket(true))
modelMarketSearch.addEventListener('input', () => {
  window.clearTimeout(marketSearchTimer)
  marketSearchTimer = window.setTimeout(() => {
    marketQuery = modelMarketSearch.value.trim()
    loadModelMarket()
  }, 250)
})
reloadButton.addEventListener('click', () => {
  loadScenes()
  showToast(t('toast.scenesReloaded'))
})

// ---- Scene install ----
installButton.addEventListener('click', () => {
  native('installScenePack')
})

// ---- Expose helpers ----
window.MNNodeShellUi = { goHome }

// ---- PostMessage handlers ----
window.MNNodeEvents = {
  onSceneInstallResult(result) {
    if (result && result.ok) {
      loadScenes()
      showToast(t('toast.sceneInstalled'))
      stateText.textContent = t('state.idle')
    } else if (result) {
      showToast(result.message || t('toast.installFailed'))
    }
  },
  onModelInstallResult(result) {
    if (result && result.state === 'installing') {
      setModelProgress(result)
    } else if (result && result.ok) {
      setModelProgress(result)
      loadModels()
      showToast(t('toast.modelImported'))
    } else if (result) {
      setModelProgress(Object.assign({ state: 'error' }, result))
      showToast(result.message || t('toast.modelImportFailed'))
    }
  },
  onVisionState(result) {
    postToScene(Object.assign({ type: 'vision.state' }, result || {}))
    if (result && result.state === 'running') showToast(t('toast.visionStarted'))
    if (result && result.state === 'error') showToast(result.message || 'Vision failed')
  },
  onVisionFrame(frame) {
    postToScene(Object.assign({ type: 'vision.frame' }, frame || {}))
  },
  onRuntimeMessage(message) {
    handleRuntimeMessage(message)
  }
}

document.addEventListener('visibilitychange', () => {
  if (!document.hidden) refreshRuntimeServiceState()
})

const messageHandlers = {
  'scene.ready': () => {
    stateText.textContent = t('state.running')
    broadcastLocale()
    syncRuntimeSnapshot()
  },
  'runtime.subscribe': () => syncRuntimeSnapshot(),
  'runtime.command': data => sendRuntimeCommand(data.sceneId || (activeScene && activeScene.id) || '', data.command || '', data.payload || {})
}

window.addEventListener('message', event => {
  const data = event.data || {}
  const handler = messageHandlers[data.type]
  if (handler) handler(data)
})

// ---- Bootstrap ----
refreshRuntimeServiceState()
loadScenes()
loadModels()
loadLocaleSetting()
tick()
