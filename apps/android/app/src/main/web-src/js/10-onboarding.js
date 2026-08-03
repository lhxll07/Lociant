/* Lociant WebUI - first-run setup guide */

const ONBOARDING_STORAGE_KEY = 'lociant.onboarding.1.1.0'
const ONBOARDING_STEP_TOTAL = 7
const DEEPSEEK_BASE_URL = 'https://api.deepseek.com/v1'
const DEEPSEEK_MODEL = 'deepseek-chat'
let onboardingStep = 0
let onboardingPollTimer = null
let onboardingTransitionTimer = null

function onboardingFormat(key, ...parts) {
  const values = parts.slice()
  return t(key).replace(/%s/g, () => (values.length ? String(values.shift()) : ''))
}

function onboardingHasSeen() {
  try {
    return window.localStorage.getItem(ONBOARDING_STORAGE_KEY) === 'done'
  } catch (error) {
    return false
  }
}

function markOnboardingSeen() {
  try {
    window.localStorage.setItem(ONBOARDING_STORAGE_KEY, 'done')
  } catch (error) {}
}

function onboardingCloudReady() {
  const state = runtimeServiceState || {}
  return !!(state.cloudEnabled && String(state.cloudBaseUrl || '').trim() &&
    String(state.cloudApiKey || '').trim() && String(state.cloudModel || '').trim())
}

function onboardingRuntimeRunning() {
  return !!(runtimeServiceState && runtimeServiceState.running)
}

function setOnboardingStep(step) {
  const previousStep = onboardingStep
  const nextStep = Math.max(0, Math.min(ONBOARDING_STEP_TOTAL - 1, Number(step) || 0))
  onboardingStep = nextStep
  if (onboardingDialog) {
    onboardingDialog.classList.remove('is-forward', 'is-backward')
    onboardingDialog.classList.add(nextStep < previousStep ? 'is-backward' : 'is-forward')
    window.clearTimeout(onboardingTransitionTimer)
    onboardingTransitionTimer = window.setTimeout(() => {
      onboardingDialog.classList.remove('is-forward', 'is-backward')
    }, 340)
  }
  onboardingSteps.forEach((node, index) => {
    const active = index === onboardingStep
    node.classList.toggle('active', active)
    node.setAttribute('aria-hidden', active ? 'false' : 'true')
  })
  onboardingProgress.forEach((node, index) => {
    node.classList.toggle('active', index <= onboardingStep)
  })
  if (onboardingStepCount) {
    onboardingStepCount.textContent = onboardingFormat('onboarding.stepCount', onboardingStep + 1, ONBOARDING_STEP_TOTAL)
  }
  if (onboardingBackButton) onboardingBackButton.classList.toggle('is-hidden', onboardingStep === 0)
  updateOnboardingState()
}

function updateOnboardingState() {
  if (!onboardingModal || onboardingModal.hidden) return
  const state = runtimeServiceState || {}
  const cloudReady = onboardingCloudReady()
  const running = onboardingRuntimeRunning()
  const starting = !!state.starting
  const accessibilityGranted = state.accessibilityPermissionGranted === true
  const overlayGranted = state.windowAllowed === true

  if (onboardingApiKeyInput && document.activeElement !== onboardingApiKeyInput) {
    onboardingApiKeyInput.value = String(state.cloudApiKey || '')
  }
  if (onboardingCloudStatus) {
    onboardingCloudStatus.textContent = cloudReady
      ? t('onboarding.cloudConfigured')
      : (state.cloudBaseUrl && state.cloudModel ? t('onboarding.cloudPresetApplied') : '')
    onboardingCloudStatus.classList.toggle('is-ready', cloudReady)
  }
  if (onboardingRuntimeStatus) {
    onboardingRuntimeStatus.textContent = running
      ? t('onboarding.runtimeRunning')
      : (starting ? t('onboarding.runtimeStarting') : t('onboarding.runtimeStopped'))
  }
  if (onboardingRuntimeDot) onboardingRuntimeDot.classList.toggle('is-ready', running)
  if (onboardingAccessibilityStatus) {
    onboardingAccessibilityStatus.textContent = accessibilityGranted ? t('onboarding.granted') : t('onboarding.needsGrant')
    onboardingAccessibilityStatus.classList.toggle('is-ready', accessibilityGranted)
  }
  if (onboardingOverlayStatus) {
    onboardingOverlayStatus.textContent = overlayGranted ? t('onboarding.granted') : t('onboarding.optional')
    onboardingOverlayStatus.classList.toggle('is-ready', overlayGranted)
  }
  if (onboardingMcpUrl) {
    onboardingMcpUrl.textContent = state.port ? mcpEndpointUrl() : '--'
  }
  updateOnboardingPrimaryButton(cloudReady, running, starting, accessibilityGranted)
}

function updateOnboardingPrimaryButton(cloudReady, running, starting, accessibilityGranted) {
  if (!onboardingPrimaryButton) return
  const label = onboardingStep === 0
    ? t('onboarding.start')
    : onboardingStep === 1
      ? (cloudReady ? t('onboarding.next') : t('common.save'))
      : onboardingStep === 2
        ? (running ? t('onboarding.next') : (starting ? t('onboarding.waitRuntime') : t('onboarding.startRuntime')))
        : onboardingStep === 3
          ? (accessibilityGranted ? t('onboarding.next') : t('onboarding.grantAccessibility'))
          : onboardingStep === 6 ? t('onboarding.finish') : t('onboarding.next')
  onboardingPrimaryButton.textContent = label
}

function openOnboarding() {
  if (!onboardingModal) return
  onboardingStep = 0
  onboardingModal.hidden = false
  onboardingModal.setAttribute('aria-hidden', 'false')
  app.classList.add('onboarding-open')
  setOnboardingStep(0)
  window.clearInterval(onboardingPollTimer)
  onboardingPollTimer = window.setInterval(() => {
    refreshRuntimeServiceState()
    updateOnboardingState()
  }, 1000)
  window.setTimeout(() => {
    if (onboardingStep === 0 && onboardingPrimaryButton) onboardingPrimaryButton.focus()
  }, 80)
}

function closeOnboarding(markSeen) {
  if (!onboardingModal) return
  if (markSeen) markOnboardingSeen()
  window.clearInterval(onboardingPollTimer)
  onboardingPollTimer = null
  onboardingModal.hidden = true
  onboardingModal.setAttribute('aria-hidden', 'true')
  app.classList.remove('onboarding-open')
}

function finishOnboarding() {
  markOnboardingSeen()
  closeOnboarding(false)
  navigateTo('home')
}

function saveOnboardingCloudSettings() {
  const key = onboardingApiKeyInput ? onboardingApiKeyInput.value.trim() : ''
  const baseUrl = (runtimeCloudBaseUrlInput && runtimeCloudBaseUrlInput.value.trim()) ||
    String(runtimeServiceState && runtimeServiceState.cloudBaseUrl || '').trim() || DEEPSEEK_BASE_URL
  const model = (runtimeCloudModelInput && runtimeCloudModelInput.value.trim()) ||
    String(runtimeServiceState && runtimeServiceState.cloudModel || '').trim() || DEEPSEEK_MODEL
  if (!key) {
    if (onboardingCloudStatus) onboardingCloudStatus.textContent = t('onboarding.cloudRequired')
    if (onboardingApiKeyInput) onboardingApiKeyInput.focus()
    return false
  }
  updateRuntimeSettings({
    cloudEnabled: true,
    cloudBaseUrl: baseUrl,
    cloudApiKey: key,
    cloudModel: model,
  })
  return onboardingCloudReady()
}

function useDeepSeekPreset() {
  if (runtimeCloudBaseUrlInput) runtimeCloudBaseUrlInput.value = DEEPSEEK_BASE_URL
  if (runtimeCloudModelInput) runtimeCloudModelInput.value = DEEPSEEK_MODEL
  updateRuntimeSettings({ cloudBaseUrl: DEEPSEEK_BASE_URL, cloudModel: DEEPSEEK_MODEL })
  if (onboardingCloudStatus) onboardingCloudStatus.textContent = t('onboarding.cloudPresetApplied')
  if (onboardingApiKeyInput) onboardingApiKeyInput.focus()
}

function runOnboardingPrimaryAction() {
  if (onboardingStep === 0) {
    setOnboardingStep(1)
    return
  }
  if (onboardingStep === 1) {
    if (saveOnboardingCloudSettings()) setOnboardingStep(2)
    return
  }
  if (onboardingStep === 2) {
    if (onboardingRuntimeRunning()) {
      setOnboardingStep(3)
    } else if (!(runtimeServiceState && runtimeServiceState.starting)) {
      startRuntime({})
    }
    updateOnboardingState()
    return
  }
  if (onboardingStep === 3) {
    if (runtimeServiceState && runtimeServiceState.accessibilityPermissionGranted === true) {
      setOnboardingStep(4)
    } else {
      native('requestAccessibilityPermission')
    }
    updateOnboardingState()
    return
  }
  if (onboardingStep === 4) {
    setOnboardingStep(5)
    return
  }
  if (onboardingStep === 5) {
    setOnboardingStep(6)
    return
  }
  finishOnboarding()
}

function leaveOnboardingToModels(view) {
  closeOnboarding(false)
  navigateTo('models')
  setModelView(view)
}

if (onboardingSettingsButton) onboardingSettingsButton.addEventListener('click', openOnboarding)
if (onboardingCloseButton) onboardingCloseButton.addEventListener('click', () => closeOnboarding(true))
if (onboardingSkipButton) onboardingSkipButton.addEventListener('click', () => closeOnboarding(true))
if (onboardingBackButton) onboardingBackButton.addEventListener('click', () => setOnboardingStep(onboardingStep - 1))
if (onboardingPrimaryButton) onboardingPrimaryButton.addEventListener('click', runOnboardingPrimaryAction)
if (onboardingDeepSeekButton) onboardingDeepSeekButton.addEventListener('click', useDeepSeekPreset)
if (onboardingApiKeyInput) onboardingApiKeyInput.addEventListener('input', updateOnboardingState)
if (onboardingCloudSkipButton) onboardingCloudSkipButton.addEventListener('click', () => setOnboardingStep(2))
if (onboardingMcpCopyButton) onboardingMcpCopyButton.addEventListener('click', () => copyConnectionText(mcpConfigText))
if (onboardingAccessibilityButton) {
  onboardingAccessibilityButton.addEventListener('click', () => {
    if (runtimeServiceState && runtimeServiceState.accessibilityPermissionGranted === true) {
      native('openPermissionSettings', 'accessibility')
    } else {
      native('requestAccessibilityPermission')
    }
  })
}
if (onboardingAppPermissionButton) {
  onboardingAppPermissionButton.addEventListener('click', () => native('openPermissionSettings', 'app'))
}
if (onboardingLocalButton) onboardingLocalButton.addEventListener('click', () => leaveOnboardingToModels('local'))
if (onboardingMarketButton) onboardingMarketButton.addEventListener('click', () => leaveOnboardingToModels('market'))

document.addEventListener('visibilitychange', () => {
  if (!document.hidden && onboardingModal && !onboardingModal.hidden) {
    refreshRuntimeServiceState()
    updateOnboardingState()
  }
})

window.setTimeout(() => {
  if (!onboardingHasSeen()) openOnboarding()
}, 180)
