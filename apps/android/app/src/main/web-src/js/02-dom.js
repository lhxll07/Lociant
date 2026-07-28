/* ── Lociant WebUI — DOM references and state variables ── */

// ---- DOM refs ----
const app = document.getElementById('app')
const clock = document.getElementById('clock')
const stateText = document.getElementById('stateText')
const stateDot = document.getElementById('stateDot')
const navItems = Array.from(document.querySelectorAll('.nav-item'))
const panels = Array.from(document.querySelectorAll('.panel'))
const modelHomeView = document.getElementById('modelHomeView')
const modelLocalView = document.getElementById('modelLocalView')
const modelLocalButton = document.getElementById('modelLocalButton')
const modelLocalBack = document.getElementById('modelLocalBack')
const modelLocalState = document.getElementById('modelLocalState')
const modelRuntimeButton = document.getElementById('modelRuntimeButton')
const modelRuntimeState = document.getElementById('modelRuntimeState')
const modelList = document.getElementById('modelList')
const modelMarketPanel = document.getElementById('modelMarketPanel')
const modelMarketBack = document.getElementById('modelMarketBack')
const modelMarketList = document.getElementById('modelMarketList')
const modelMarketSearch = document.getElementById('modelMarketSearch')
const modelMarketRefreshButton = document.getElementById('modelMarketRefreshButton')
const modelProgress = document.getElementById('modelProgress')
const modelProgressText = document.getElementById('modelProgressText')
const modelProgressPercent = document.getElementById('modelProgressPercent')
const modelProgressFill = document.getElementById('modelProgressFill')
const modelReloadButton = document.getElementById('modelReloadButton')
const modelImportButton = document.getElementById('modelImportButton')
const modelMarketButton = document.getElementById('modelMarketButton')
const settingsList = document.getElementById('settingsList')
const runtimeSettingsButton = document.getElementById('runtimeSettingsButton')
const runtimeSettingsState = document.getElementById('runtimeSettingsState')
const runtimeSettingsPanel = document.getElementById('runtimeSettingsPanel')
const runtimeSettingsBack = document.getElementById('runtimeSettingsBack')
const runtimeServiceToggle =
  document.getElementById('runtimeServiceToggle')
const runtimeServiceMessage =
  document.getElementById('runtimeServiceMessage')
const runtimeAutoStartInput = document.getElementById('runtimeAutoStartInput')
const cameraPermissionState = document.getElementById('cameraPermissionState')
const notificationPermissionState = document.getElementById('notificationPermissionState')
const overlayPermissionState = document.getElementById('overlayPermissionState')
const batteryPermissionState = document.getElementById('batteryPermissionState')
const accessibilityPermissionState = document.getElementById('accessibilityPermissionState')
const cameraPermissionButton = document.getElementById('cameraPermissionButton')
const notificationPermissionButton = document.getElementById('notificationPermissionButton')
const overlayPermissionButton = document.getElementById('overlayPermissionButton')
const batteryPermissionButton = document.getElementById('batteryPermissionButton')
const accessibilityPermissionButton = document.getElementById('accessibilityPermissionButton')
const runtimeServerButton = document.getElementById('runtimeServerButton')
const runtimeServerState = document.getElementById('runtimeServerState')
const runtimeServerPanel = document.getElementById('runtimeServerPanel')
const runtimeServerBack = document.getElementById('runtimeServerBack')
const runtimePortInput = document.getElementById('runtimePortInput')
const runtimeMaxTokensInput = document.getElementById('runtimeMaxTokensInput')
const runtimeAuthTokenInput = document.getElementById('runtimeAuthTokenInput')
const runtimeAuthGenerateButton = document.getElementById('runtimeAuthGenerateButton')
const runtimeAuthClearButton = document.getElementById('runtimeAuthClearButton')
const diagUrl = document.getElementById('diagUrl')
const diagApi = document.getElementById('diagApi')
const copyOpenAiUrlButton = document.getElementById('copyOpenAiUrlButton')
const copyMcpUrlButton = document.getElementById('copyMcpUrlButton')
const copyAuthHeaderButton = document.getElementById('copyAuthHeaderButton')
const copyMcpConfigButton = document.getElementById('copyMcpConfigButton')
const copyTestPromptButton = document.getElementById('copyTestPromptButton')
const runtimeCapabilitiesButton = document.getElementById('runtimeCapabilitiesButton')
const runtimeCapabilitiesState = document.getElementById('runtimeCapabilitiesState')
const runtimeCapabilitiesPanel = document.getElementById('runtimeCapabilitiesPanel')
const runtimeCapabilitiesBack = document.getElementById('runtimeCapabilitiesBack')
const runtimeVisionText = document.getElementById('runtimeVisionText')
const runtimeVisionButton = document.getElementById('runtimeVisionButton')
const runtimeToolExposureInput = document.getElementById('runtimeToolExposureInput')
const runtimeWindowAutoInput = document.getElementById('runtimeWindowAutoInput')
const runtimeWindowButton = document.getElementById('runtimeWindowButton')
const runtimeModelButton = document.getElementById('runtimeModelButton')
const runtimeModelPanel = document.getElementById('runtimeModelPanel')
const runtimeModelBack = document.getElementById('runtimeModelBack')
const runtimeModelList = document.getElementById('runtimeModelList')
const runtimeModelState = document.getElementById('runtimeModelState')
const runtimeModelNote = document.getElementById('runtimeModelNote')
const runtimePerformanceModeInput = document.getElementById('runtimePerformanceModeInput')
const runtimePerformanceText = document.getElementById('runtimePerformanceText')
const runtimeResponseLengthInput = document.getElementById('runtimeResponseLengthInput')
const runtimeResponseLengthText = document.getElementById('runtimeResponseLengthText')
const runtimeResponseTokensInput = document.getElementById('runtimeResponseTokensInput')
const runtimeContextMemoryInput = document.getElementById('runtimeContextMemoryInput')
const runtimeContextText = document.getElementById('runtimeContextText')
const runtimeHistoryLimitInput = document.getElementById('runtimeHistoryLimitInput')
const runtimeCacheState = document.getElementById('runtimeCacheState')
const runtimeCacheBadge = document.getElementById('runtimeCacheBadge')
const runtimeReleaseModelButton = document.getElementById('runtimeReleaseModelButton')
const runtimePerModelButton = document.getElementById('runtimePerModelButton')
const runtimeAdvancedButton = document.getElementById('runtimeAdvancedButton')
const runtimeAdvancedState = document.getElementById('runtimeAdvancedState')
const runtimeAdvancedPanel = document.getElementById('runtimeAdvancedPanel')
const runtimeAdvancedBack = document.getElementById('runtimeAdvancedBack')
const runtimeSessionCurrent = document.getElementById('runtimeSessionCurrent')
const runtimeSessionList = document.getElementById('runtimeSessionList')
const runtimeSessionNewButton = document.getElementById('runtimeSessionNewButton')
const runtimeDiagRunButton = document.getElementById('runtimeDiagRunButton')
const runtimeDiagSummary = document.getElementById('runtimeDiagSummary')
const runtimeDiagSummaryText = document.getElementById('runtimeDiagSummaryText')
const languageControl = document.getElementById('languageControl')
const toast = document.getElementById('toast')
const runtimeDefaultTokens = document.getElementById('runtimeDefaultTokens')
const runtimeModelTokens = document.getElementById('runtimeModelTokens')
const runtimeEffectiveTokens = document.getElementById('runtimeEffectiveTokens')
const runtimeDeviceState = document.getElementById('runtimeDeviceState')
const settingsHomePage = document.getElementById('settingsList')
const homeRailToggle = document.getElementById('homeRailToggle')
const homeSidebar = document.getElementById('homeSidebar')
const homeSessionCount = document.getElementById('homeSessionCount')
const homeNewChatButton = document.getElementById('homeNewChatButton')
const homeSessionList = document.getElementById('homeSessionList')
const homeChatForm = document.getElementById('homeChatForm')
const homeChatInput = document.getElementById('homeChatInput')
const homeChatSendButton = document.getElementById('homeChatSendButton')
const homeChatFeed = document.getElementById('homeChatFeed')
const homeChatTitle = document.getElementById('homeChatTitle')
const homeChatMeta = document.getElementById('homeChatMeta')
const homeChatState = document.getElementById('homeChatState')
const homeImageInput = document.getElementById('homeImageInput')
const homeImagePreview = document.getElementById('homeImagePreview')
const homeImagePreviewImg = document.getElementById('homeImagePreviewImg')
const homeImagePreviewName = document.getElementById('homeImagePreviewName')
const homeImageRemoveButton = document.getElementById('homeImageRemoveButton')
// ---- State variables ----
let runtimeServiceState = null
let runtimeModels = []
let modelView = 'home'
let marketModels = []
let marketQuery = ''
let marketInstallTimer = null
let marketInstallingModelId = ''
let marketSearchTimer = null
let modelProgressLastPercent = 0
let modelProgressHideTimer = null
let localeSetting = { mode: 'system' }
let currentLocale = 'en'
const localeStorePath = '/api/v1/store/runtime-settings/locale'
let homeAttachedImage = null
let activePage = 'home'

// ---- DOM helpers ----
function el(tag, className, text) {
  const node = document.createElement(tag)
  if (className) node.className = className
  if (text !== undefined) node.textContent = text
  return node
}

function emptyCard(text) {
  return el('div', 'empty-card', text)
}

function runtimeDetails() {
  return [
    runtimeSettingsPanel,
    runtimeServerPanel,
    runtimeCapabilitiesPanel,
    runtimeModelPanel,
    runtimeAdvancedPanel,
  ].filter(Boolean)
}

// ---- Surface switching ----
function animateSurface(node) {
  if (!node) return
  node.classList.remove('animate-in')
  void node.offsetWidth
  node.classList.add('animate-in')
}

function showPanel(panel) {
  if (!panel) return
  panels.forEach(item => {
    const active = item === panel
    item.classList.toggle('active', active)
    if (!active) item.classList.remove('animate-in')
  })
  animateSurface(panel)
}

function showPagePanel(page) {
  activePage = page || 'home'
  if (page !== 'settings') {
    closeSettingsDetails()
    if (settingsHomePage) {
      settingsHomePage.classList.remove('active')
      settingsHomePage.setAttribute('aria-hidden', 'true')
    }
  }
  showPanel(document.getElementById('page-' + page))
  if (page === 'settings' && settingsHomePage) {
    settingsHomePage.classList.add('active')
    settingsHomePage.setAttribute('aria-hidden', 'false')
  }
}
