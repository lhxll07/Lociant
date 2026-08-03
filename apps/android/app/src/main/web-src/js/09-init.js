/* ── Lociant WebUI — Event binding and initialization ── */

window.addEventListener('resize', syncKeyboardOffset)
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', syncKeyboardOffset)
  window.visualViewport.addEventListener('scroll', syncKeyboardOffset)
}
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
    const next = createRuntimeSession()
    Promise.resolve(next).then(state => {
      clearHomeMessages()
      const sessionId = homeSessionIdFromState(state) || homeCurrentSessionId()
      updateRuntimeServiceState(markHomeSessionActive(state || {}, sessionId))
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
// ---- Settings navigation ----
runtimeSettingsButton.addEventListener('click', openRuntimeSettings)
runtimeSettingsBack.addEventListener('click', closeRuntimeSettingsBack)
runtimeServerButton.addEventListener('click', openRuntimeServerSettings)
runtimeServerBack.addEventListener('click', backToRuntimeSettings)
runtimeModelButton.addEventListener('click', openRuntimeModelSettings)
runtimeModelBack.addEventListener('click', backToRuntimeSettings)
runtimeAdvancedButton.addEventListener('click', openRuntimeAdvancedSettings)
runtimeAdvancedBack.addEventListener('click', backToRuntimeSettings)
if (runtimeAboutButton) runtimeAboutButton.addEventListener('click', openAboutSettings)
aboutBack.addEventListener('click', closeAboutSettings)

document.querySelectorAll('[data-about-link]').forEach(link => {
  link.addEventListener('click', event => {
    event.preventDefault()
    native('openExternalUrl', link.href)
  })
})

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
  if (running) stopRuntime()
  else startRuntime({})
})
runtimeAutoStartInput.addEventListener('change', () => {
  updateRuntimeSettings({ autoStart: !!runtimeAutoStartInput.checked })
})
runtimeVisionButton.addEventListener('click', () => {
  const vision = visionState()
  if (vision && vision.running) stopRuntimeVision()
  else startRuntimeVision({})
})
runtimeWindowAutoInput.addEventListener('change', () => {
  updateRuntimeWindow({ autoShow: !!runtimeWindowAutoInput.checked })
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
  updateRuntimeSettings({ port: value })
})
runtimeMaxTokensInput.addEventListener('change', () => {
  const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
  const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeMaxTokensInput.value) || 512)))
  runtimeMaxTokensInput.value = String(value)
  updateRuntimeSettings({ maxOutputTokens: value })
})
runtimeAuthTokenInput.addEventListener('change', () => {
  updateRuntimeSettings({ authToken: runtimeAuthTokenInput.value.trim() })
})
runtimeAuthGenerateButton.addEventListener('click', () => {
  updateRuntimeSettings({ generateAuthToken: true })
})
runtimeAuthClearButton.addEventListener('click', () => {
  runtimeAuthTokenInput.value = ''
  updateRuntimeSettings({ authToken: '' })
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
  updateRuntimeSettings({ toolExposure: runtimeToolExposureInput.value || 'action' })
})
if (runtimePerformanceModeInput) {
  runtimePerformanceModeInput.addEventListener('change', () => {
    updateRuntimeSettings({ cpuThreads: threadsForPerformanceMode(runtimePerformanceModeInput.value) })
  })
}
if (runtimeBackendInput) {
  runtimeBackendInput.addEventListener('change', () => {
    updateRuntimeSettings({ inferenceBackend: runtimeBackendInput.value || 'model' })
  })
}
if (runtimeCloudEnabledInput) {
  runtimeCloudEnabledInput.addEventListener('change', () => {
    updateRuntimeSettings({ cloudEnabled: !!runtimeCloudEnabledInput.checked })
  })
}
function cloudSettingsPayload() {
  return {
    cloudBaseUrl: runtimeCloudBaseUrlInput ? runtimeCloudBaseUrlInput.value.trim() : '',
    cloudApiKey: runtimeCloudApiKeyInput ? runtimeCloudApiKeyInput.value.trim() : '',
    cloudModel: runtimeCloudModelInput ? runtimeCloudModelInput.value.trim() : ''
  }
}
if (runtimeCloudBaseUrlInput) {
  runtimeCloudBaseUrlInput.addEventListener('change', () => updateRuntimeSettings(cloudSettingsPayload()))
}
if (runtimeCloudApiKeyInput) {
  runtimeCloudApiKeyInput.addEventListener('change', () => updateRuntimeSettings(cloudSettingsPayload()))
}
if (runtimeCloudModelInput) {
  runtimeCloudModelInput.addEventListener('change', () => updateRuntimeSettings(cloudSettingsPayload()))
}
if (runtimeCloudResponseLengthInput) {
  runtimeCloudResponseLengthInput.addEventListener('change', () => {
    if (runtimeCloudResponseLengthInput.value === 'custom') {
      if (runtimeCloudResponseTokensInput) {
        runtimeCloudResponseTokensInput.classList.remove('is-hidden')
        runtimeCloudResponseTokensInput.focus()
      }
      return
    }
    if (runtimeCloudResponseTokensInput) runtimeCloudResponseTokensInput.classList.add('is-hidden')
    updateRuntimeSettings({ cloudMaxOutputTokens: Number(runtimeCloudResponseLengthInput.value) || 0 })
  })
}
if (runtimeCloudResponseTokensInput) {
  runtimeCloudResponseTokensInput.addEventListener('change', () => {
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(0, Math.min(hardMax, Math.round(Number(runtimeCloudResponseTokensInput.value) || 0)))
    runtimeCloudResponseTokensInput.value = String(value)
    updateRuntimeSettings({ cloudMaxOutputTokens: value })
  })
}
if (runtimeCloudContextWindowInput) {
  runtimeCloudContextWindowInput.addEventListener('change', () => {
    if (runtimeCloudContextWindowInput.value === 'custom') {
      if (runtimeCloudContextWindowTokensInput) {
        runtimeCloudContextWindowTokensInput.classList.remove('is-hidden')
        runtimeCloudContextWindowTokensInput.focus()
      }
      return
    }
    if (runtimeCloudContextWindowTokensInput) runtimeCloudContextWindowTokensInput.classList.add('is-hidden')
    updateRuntimeSettings({ cloudContextWindow: Number(runtimeCloudContextWindowInput.value) || 131072 })
  })
}
if (runtimeCloudContextWindowTokensInput) {
  runtimeCloudContextWindowTokensInput.addEventListener('change', () => {
    const value = Math.max(16384, Math.min(524288, Math.round(Number(runtimeCloudContextWindowTokensInput.value) || 131072)))
    runtimeCloudContextWindowTokensInput.value = String(value)
    updateRuntimeSettings({ cloudContextWindow: value })
  })
}
if (runtimeCloudHistoryLimitInput) {
  runtimeCloudHistoryLimitInput.addEventListener('change', () => {
    const value = Math.max(1, Math.min(1024, Math.round(Number(runtimeCloudHistoryLimitInput.value) || 256)))
    runtimeCloudHistoryLimitInput.value = String(value)
    updateRuntimeSettings({ cloudHistoryLimit: value })
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
    updateRuntimeSettings({ maxOutputTokens: value })
  })
}
if (runtimeResponseTokensInput) {
  runtimeResponseTokensInput.addEventListener('change', () => {
    const hardMax = Number(runtimeServiceState && runtimeServiceState.hardMaxOutputTokens) || 32768
    const value = Math.max(1, Math.min(hardMax, Math.round(Number(runtimeResponseTokensInput.value) || 512)))
    runtimeResponseTokensInput.value = String(value)
    updateRuntimeSettings({ maxOutputTokens: value })
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
    updateRuntimeSettings({ contextProfile: profile, historyLimit: historyLimitForContextProfile(profile) })
  })
}
if (runtimeHistoryLimitInput) {
  runtimeHistoryLimitInput.addEventListener('change', () => {
    const maxHistory = Number(runtimeServiceState && runtimeServiceState.sessionPolicy && runtimeServiceState.sessionPolicy.maxHistoryLimit) || 256
    const value = Math.max(1, Math.min(maxHistory, Math.round(Number(runtimeHistoryLimitInput.value) || 64)))
    runtimeHistoryLimitInput.value = String(value)
    updateRuntimeSettings({ contextProfile: contextPresetForHistoryLimit(value), historyLimit: value })
  })
}
if (runtimeReleaseModelButton) {
  runtimeReleaseModelButton.addEventListener('click', () => {
    releaseRuntimeModel()
    showToast(t('toast.modelReleased'))
  })
}

// ---- Session ----
runtimeSessionNewButton.addEventListener('click', () => {
  if (homeChatInFlight) return
  createRuntimeSession()
})
if (runtimeDiagRunButton) {
  runtimeDiagRunButton.addEventListener('click', runRuntimeDiagnostics)
}
// ---- Language ----
languageControl.addEventListener('click', event => {
  const button = event.target.closest('.segmented-option')
  if (button && button.dataset.langMode) saveLocaleSetting(button.dataset.langMode)
})

// ---- Model actions ----
modelReloadButton.addEventListener('click', () => {
  loadModels(true)
  showToast(t('toast.modelsReloaded'))
})
modelImportButton.addEventListener('click', () => {
  native('installModelPackage')
})
modelLocalButton.addEventListener('click', () => setModelView('local'))
modelLocalBack.addEventListener('click', () => setModelView('home'))
modelMarketButton.addEventListener('click', () => setModelView('market'))
modelMarketBack.addEventListener('click', () => setModelView('home'))
modelCloudButton.addEventListener('click', () => setModelView('cloud'))
modelCloudBack.addEventListener('click', () => setModelView('home'))
modelRuntimeButton.addEventListener('click', () => {
  navigateTo('settings')
  openRuntimeSettings()
})
modelMarketRefreshButton.addEventListener('click', () => loadModelMarket(true))
if (modelMarketStartButton) {
  modelMarketStartButton.addEventListener('click', startRuntimeForMarket)
}
modelMarketSearch.addEventListener('input', () => {
  window.clearTimeout(marketSearchTimer)
  marketSearchTimer = window.setTimeout(() => {
    marketQuery = modelMarketSearch.value.trim()
    loadModelMarket()
  }, 250)
})
// ---- PostMessage handlers ----
window.LociantEvents = {
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

// ---- Bootstrap ----
loadLocaleSetting()
refreshRuntimeServiceState()
restoreHomeConversation()
loadModels()
tick()
window.setInterval(tick, 1000)
