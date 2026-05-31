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
window.addEventListener('resize', syncKeyboardOffset)
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', syncKeyboardOffset)
  window.visualViewport.addEventListener('scroll', syncKeyboardOffset)
}
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
  if (app.classList.contains('mobile-nav-open') && !event.target.closest('.sidebar')) {
    app.classList.remove('mobile-nav-open')
    menuButton.classList.remove('is-active')
  }
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
if (topNodeButton) {
  topNodeButton.addEventListener('click', () => navigateTo('nodes'))
}
if (homeRailToggle && homeSidebar) {
  homeRailToggle.setAttribute('aria-expanded', 'false')
  homeRailToggle.addEventListener('click', () => {
    const open = homeSidebar.classList.toggle('open')
    homeRailToggle.setAttribute('aria-expanded', open ? 'true' : 'false')
    homeRailToggle.classList.toggle('is-active', open)
    if (open) {
      app.classList.remove('mobile-nav-open')
      menuButton.classList.remove('is-active')
    }
    if (open) refreshRuntimeServiceState()
  })
  document.addEventListener('pointerdown', event => {
    if (!homeSidebar.classList.contains('open')) return
    if (!homeSidebar.contains(event.target) && !homeRailToggle.contains(event.target)) {
      homeSidebar.classList.remove('open')
      homeRailToggle.setAttribute('aria-expanded', 'false')
      homeRailToggle.classList.remove('is-active')
    }
  })
}
if (homeNewChatButton) {
  homeNewChatButton.addEventListener('click', () => {
    const next = activeNodeKind() === 'acp'
      ? apiPost('/v1/runtime/agent.session.create', {})
      : runtimeApiCommand('session.create', {})
    Promise.resolve(next).then(state => {
      const sessionId = state && state.currentSessionId ? state.currentSessionId : homeCurrentSessionId()
      clearHomeMessages()
      updateRuntimeServiceState(state || {})
      loadHomeConversation(sessionId)
    }).catch(error => {
      showToast((error && error.message) || t('toast.modelImportFailed'))
    })
  })
}
if (homeChatForm) {
  homeChatForm.addEventListener('submit', event => {
    event.preventDefault()
    submitHomeChat(homeChatInput && homeChatInput.value)
  })
}
if (homeChatInput) {
  homeChatInput.addEventListener('focus', () => {
    window.setTimeout(syncKeyboardOffset, 80)
    window.setTimeout(syncKeyboardOffset, 260)
  })
  homeChatInput.addEventListener('blur', () => {
    setKeyboardOffset(0)
    window.setTimeout(syncKeyboardOffset, 120)
  })
}
if (homeImageInput) {
  homeImageInput.addEventListener('change', () => {
    const file = homeImageInput.files && homeImageInput.files[0]
    readHomeImage(file)
  })
}
if (homeImageRemoveButton) {
  homeImageRemoveButton.addEventListener('click', clearHomeImageAttachment)
}
if (homeChatFeed) {
  homeChatFeed.addEventListener('click', event => {
    const button = event.target.closest('[data-home-action]')
    if (button) handleHomeAction(button.dataset.homeAction)
  })
}

// ---- Settings navigation ----
runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
runtimeSettingsBack.addEventListener('click', closeRuntimeSettingsBack)
runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
runtimeServerBack.addEventListener('click', backToRuntimeSettings)
runtimeCapabilitiesButton.addEventListener('click', openRuntimeCapabilitiesSettings)
runtimeCapabilitiesBack.addEventListener('click', backToRuntimeSettings)
runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
runtimeModelBack.addEventListener('click', backToRuntimeSettings)
runtimeAdvancedButton.addEventListener('click', openRuntimeAdvancedSettings)
runtimeAdvancedBack.addEventListener('click', backToRuntimeSettings)

// ---- Runtime controls ----
function isPermissionGranted(button) {
  return button && button.dataset.permissionGranted === 'true'
}

function handlePermissionAction(button, requestMethod, settingsKind) {
  if (isPermissionGranted(button)) native('openPermissionSettings', settingsKind || 'app')
  else native(requestMethod)
}

runtimeServiceToggle.addEventListener('click', () => {
  const running = runtimeServiceState && (runtimeServiceState.running || runtimeServiceState.starting)
  runtimeApiCommand(running ? 'stop' : 'start', {})
})
runtimeAutoStartInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { autoStart: !!runtimeAutoStartInput.checked })
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
cameraPermissionButton.addEventListener('click', () => {
  handlePermissionAction(cameraPermissionButton, 'requestCameraPermission', 'app')
})
notificationPermissionButton.addEventListener('click', () => {
  handlePermissionAction(notificationPermissionButton, 'requestNotificationPermission', 'app')
})
overlayPermissionButton.addEventListener('click', () => {
  handlePermissionAction(overlayPermissionButton, 'requestOverlayPermission', 'overlay')
})
batteryPermissionButton.addEventListener('click', () => {
  handlePermissionAction(batteryPermissionButton, 'requestBatteryOptimizationExemption', 'battery')
})
if (accessibilityPermissionButton) {
  accessibilityPermissionButton.addEventListener('click', () => {
    handlePermissionAction(accessibilityPermissionButton, 'requestAccessibilityPermission', 'accessibility')
  })
}

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
if (copyOpenAiUrlButton) {
  copyOpenAiUrlButton.addEventListener('click', () => copyConnectionText(openAiBaseUrl))
}
if (copyMcpUrlButton) {
  copyMcpUrlButton.addEventListener('click', () => copyConnectionText(mcpEndpointUrl))
}
if (copyAuthHeaderButton) {
  copyAuthHeaderButton.addEventListener('click', () => copyConnectionText(authHeaderText))
}
if (copyMcpConfigButton) {
  copyMcpConfigButton.addEventListener('click', () => copyConnectionText(mcpConfigText))
}
if (copyTestPromptButton) {
  copyTestPromptButton.addEventListener('click', () => copyConnectionText(testPromptText))
}

// ---- Capabilities settings ----
runtimeToolExposureInput.addEventListener('change', () => {
  runtimeApiCommand('settings', { toolExposure: runtimeToolExposureInput.value || 'action' })
})
if (runtimePerformanceModeInput) {
  runtimePerformanceModeInput.addEventListener('change', () => {
    runtimeApiCommand('settings', { cpuThreads: threadsForPerformanceMode(runtimePerformanceModeInput.value) })
  })
}
if (runtimeResponseLengthInput) {
  runtimeResponseLengthInput.addEventListener('change', () => {
    if (runtimeResponseLengthInput.value === 'custom') {
      if (runtimeResponseTokensInput) {
        runtimeResponseTokensInput.classList.remove('is-hidden')
        runtimeResponseTokensInput.focus()
      }
      return
    }
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(1, Math.min(hardMax, Number(runtimeResponseLengthInput.value) || 512))
    runtimeApiCommand('settings', { maxOutputTokens: value })
  })
}
if (runtimeResponseTokensInput) {
  runtimeResponseTokensInput.addEventListener('change', () => {
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeResponseTokensInput.value) || 512)))
    runtimeResponseTokensInput.value = String(value)
    runtimeApiCommand('settings', { maxOutputTokens: value })
  })
}
if (runtimeContextMemoryInput) {
  runtimeContextMemoryInput.addEventListener('change', () => {
    if (runtimeContextMemoryInput.value === 'custom') {
      if (runtimeHistoryLimitInput) {
        runtimeHistoryLimitInput.classList.remove('is-hidden')
        runtimeHistoryLimitInput.focus()
      }
      return
    }
    const profile = runtimeContextMemoryInput.value || 'balanced'
    runtimeApiCommand('settings', { contextProfile: profile, historyLimit: historyLimitForContextProfile(profile) })
  })
}
if (runtimeHistoryLimitInput) {
  runtimeHistoryLimitInput.addEventListener('change', () => {
    const maxHistory = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.maxHistoryLimit) || 256
    const value = Math.max(1, Math.min(maxHistory, Math.round(Number(runtimeHistoryLimitInput.value) || 64)))
    runtimeHistoryLimitInput.value = String(value)
    runtimeApiCommand('settings', { contextProfile: contextPresetForHistoryLimit(value), historyLimit: value })
  })
}
if (runtimeReleaseModelButton) {
  runtimeReleaseModelButton.addEventListener('click', () => {
    runtimeApiCommand('model.release', {})
    showToast(t('toast.modelReleased'))
  })
}
if (runtimePerModelButton) {
  runtimePerModelButton.addEventListener('click', () => showToast(t('settings.perModelConfigSub')))
}

// ---- Session ----
runtimeSessionNewButton.addEventListener('click', () => {
  runtimeApiCommand('session.create', {})
})
if (runtimeDiagRunButton) {
  runtimeDiagRunButton.addEventListener('click', runAgentDiagnostics)
}
if (nodeCopyMcpButton) {
  nodeCopyMcpButton.addEventListener('click', () => copyConnectionText(() => t('nodes.starterCommand')))
}
if (nodeLocalButton) {
  nodeLocalButton.addEventListener('click', () => {
    updateRuntimeServiceState(runtimeApiCommand('agent.selectNode', { nodeId: 'local' }) || {})
    showToast(t('nodes.localActive'))
  })
}
if (nodeOpenServerButton) {
  nodeOpenServerButton.addEventListener('click', openRuntimeServerFromHome)
}
if (nodeSaveCodexButton) {
  nodeSaveCodexButton.addEventListener('click', () => {
    const node = {
      id: 'desktop-codex',
      kind: 'acp',
      name: t('nodes.codexNode'),
      url: nodeCodexUrlInput ? nodeCodexUrlInput.value.trim() : '',
      cwd: nodeCodexCwdInput ? nodeCodexCwdInput.value.trim() : '',
      token: ''
    }
    updateRuntimeServiceState(runtimeApiCommand('agent.saveNode', { node, active: true }) || {})
    showToast(t('common.save'))
  })
}
if (nodeConnectCodexButton) {
  nodeConnectCodexButton.addEventListener('click', () => {
    const network = runtimeServiceState && runtimeServiceState.agentNetwork
    const active = network && network.activeNode && network.activeNode.kind === 'acp'
    const connected = network && network.agent && network.agent.connected
    if (active && connected) {
      updateRuntimeServiceState(runtimeApiCommand('agent.disconnect', {}) || {})
      return
    }
    const node = {
      id: 'desktop-codex',
      kind: 'acp',
      name: t('nodes.codexNode'),
      url: nodeCodexUrlInput ? nodeCodexUrlInput.value.trim() : '',
      cwd: nodeCodexCwdInput ? nodeCodexCwdInput.value.trim() : '',
      token: ''
    }
    updateRuntimeServiceState(runtimeApiCommand('agent.saveNode', { node, active: true }) || {})
    Promise.resolve(apiPost('/v1/runtime/agent.connect', {}))
      .then(state => updateRuntimeServiceState(state || {}))
      .catch(error => showToast((error && error.message) || t('toast.modelImportFailed')))
  })
}
if (nodePairQrButton) {
  nodePairQrButton.addEventListener('click', () => showToast(t('nodes.qrTodo')))
}

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
  if (!document.hidden) {
    refreshRuntimeServiceState()
    restoreHomeConversation()
  }
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
restoreHomeConversation()
loadScenes()
loadModels()
loadLocaleSetting()
tick()
window.setInterval(tick, 1000)
