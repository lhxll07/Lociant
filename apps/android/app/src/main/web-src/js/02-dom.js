/* ── Lociant WebUI — DOM references and state variables ── */

// ---- DOM refs ----
const app = document.getElementById('app')
const clock = document.getElementById('clock')
const stateText = document.getElementById('stateText')
const stateDot = document.getElementById('stateDot')
const navItems = Array.from(document.querySelectorAll('.nav-item'))
const panels = Array.from(document.querySelectorAll('.panel'))
const sceneHost = document.getElementById('sceneHost')
const sceneFrame = document.getElementById('sceneFrame')
const sceneList = document.getElementById('sceneList')
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
const installButton = document.getElementById('installButton')
const reloadButton = document.getElementById('reloadButton')
const modelReloadButton = document.getElementById('modelReloadButton')
const modelImportButton = document.getElementById('modelImportButton')
const modelMarketButton = document.getElementById('modelMarketButton')
const backButton = document.getElementById('backButton')
const sceneSettingsButton = document.getElementById('sceneSettingsButton')
const runtimeStrip = document.getElementById('runtimeStrip')
const runtimeSceneName = document.getElementById('runtimeSceneName')
const runtimeEventText = document.getElementById('runtimeEventText')
const runtimeWindowText = document.getElementById('runtimeWindowText')
const runtimeStateText = document.getElementById('runtimeStateText')
const runtimeElapsedText = document.getElementById('runtimeElapsedText')
const alertBanner = document.getElementById('alertBanner')
const alertSceneName = document.getElementById('alertSceneName')
const alertTitle = document.getElementById('alertTitle')
const alertMessage = document.getElementById('alertMessage')
const alertOpenButton = document.getElementById('alertOpenButton')
const alertCloseButton = document.getElementById('alertCloseButton')
const settingsList = document.getElementById('settingsList')
const runtimeSettingsButton = document.getElementById('runtimeSettingsButton')
const runtimeSettingsState = document.getElementById('runtimeSettingsState')
const runtimeSettingsPanel = document.getElementById('runtimeSettingsPanel')
const runtimeSettingsBack = document.getElementById('runtimeSettingsBack')
const runtimeServiceToggle =
  document.getElementById('runtimeServiceToggle')
const runtimeServiceMessage =
  document.getElementById('runtimeServiceMessage')
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
const runtimeCapabilitiesButton = document.getElementById('runtimeCapabilitiesButton')
const runtimeCapabilitiesState = document.getElementById('runtimeCapabilitiesState')
const runtimeCapabilitiesPanel = document.getElementById('runtimeCapabilitiesPanel')
const runtimeCapabilitiesBack = document.getElementById('runtimeCapabilitiesBack')
const runtimeVisionText = document.getElementById('runtimeVisionText')
const runtimeVisionButton = document.getElementById('runtimeVisionButton')
const runtimeToolExposureInput = document.getElementById('runtimeToolExposureInput')
const runtimeWindowAutoInput = document.getElementById('runtimeWindowAutoInput')
const runtimeWindowButton = document.getElementById('runtimeWindowButton')
const runtimeWindowPermissionButton = document.getElementById('runtimeWindowPermissionButton')
const runtimeModelButton = document.getElementById('runtimeModelButton')
const runtimeModelPanel = document.getElementById('runtimeModelPanel')
const runtimeModelBack = document.getElementById('runtimeModelBack')
const runtimeModelList = document.getElementById('runtimeModelList')
const runtimeModelState = document.getElementById('runtimeModelState')
const runtimeModelNote = document.getElementById('runtimeModelNote')
const runtimeSessionsButton = document.getElementById('runtimeSessionsButton')
const runtimeSessionsState = document.getElementById('runtimeSessionsState')
const runtimeSessionsPanel = document.getElementById('runtimeSessionsPanel')
const runtimeSessionsBack = document.getElementById('runtimeSessionsBack')
const runtimeSessionCurrent = document.getElementById('runtimeSessionCurrent')
const runtimeSessionList = document.getElementById('runtimeSessionList')
const runtimeSessionNewButton = document.getElementById('runtimeSessionNewButton')
const runtimeDiagnosticsButton = document.getElementById('runtimeDiagnosticsButton')
const runtimeDiagnosticsState = document.getElementById('runtimeDiagnosticsState')
const runtimeDiagnosticsPanel = document.getElementById('runtimeDiagnosticsPanel')
const runtimeDiagnosticsBack = document.getElementById('runtimeDiagnosticsBack')
const runtimeCpuThreadsInput = document.getElementById('runtimeCpuThreadsInput')
const languageControl = document.getElementById('languageControl')
const batteryOptimizationButton = document.getElementById('batteryOptimizationButton')
const batteryOptimizationText = document.getElementById('batteryOptimizationText')
const toast = document.getElementById('toast')
const runtimeDefaultTokens = document.getElementById('runtimeDefaultTokens')
const runtimeModelTokens = document.getElementById('runtimeModelTokens')
const runtimeEffectiveTokens = document.getElementById('runtimeEffectiveTokens')

// ---- State variables ----
let runtimeServiceState = null
let runtimeSnapshot = null
let runtimeModels = []
let modelView = 'home'
let marketModels = []
let marketQuery = ''
let marketInstallTimer = null
let marketInstallingModelId = ''
let marketSearchTimer = null
let localeSetting = { mode: 'system' }
let currentLocale = 'en'
let activeAlert = null
const localeStorePath = '/v1/store/runtime-settings/locale'
let scenes = []
let activeScene = null
let cameraPreviewRect = null

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

const reliableTimers = new Map()

// ---- Surface switching ----
function animateSurface(node) {
  if (!node) return
  node.classList.remove('animate-in')
  void node.offsetWidth
  node.classList.add('animate-in')
}

function showPanel(panel) {
  if (!panel) return
  sceneHost.classList.remove('active', 'animate-in')
  panels.forEach(item => {
    const active = item === panel
    item.classList.toggle('active', active)
    if (!active) item.classList.remove('animate-in')
  })
  animateSurface(panel)
}

function showPagePanel(page) {
  showPanel(document.getElementById('page-' + page))
}

function hidePanels() {
  panels.forEach(panel => panel.classList.remove('active', 'animate-in'))
}

function showSceneHost() {
  hidePanels()
  sceneHost.classList.add('active')
  animateSurface(sceneHost)
}
