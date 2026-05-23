/* ── Lociant WebUI — Settings panels ── */

// ---- Locale ----
function loadLocaleSetting() {
  apiGet(localeStorePath)
    .then(result => {
      localeSetting = result && result.ok && result.value ? result.value : { mode: 'system' }
      applyLocale()
    })
    .catch(() => applyLocale())
}

function saveLocaleSetting(mode) {
  localeSetting = { mode: mode || 'system' }
  apiPost(localeStorePath, { value: localeSetting }).catch(() => {})
  applyLocale()
}

function applyLocale() {
  currentLocale = resolveLocale(localeSetting)
  document.documentElement.lang = currentLocale === 'zh' ? 'zh-CN' : 'en'
  document.querySelectorAll('[data-i18n]').forEach(node => {
    node.textContent = t(node.dataset.i18n)
  })
  Array.from(languageControl.querySelectorAll('.segmented-option')).forEach(button => {
    button.classList.toggle('active', button.dataset.langMode === (localeSetting.mode || 'system'))
  })
  updateRuntimeServiceState(runtimeServiceState || {})
  broadcastLocale()
}

function broadcastLocale() {
  postToScene({
    type: 'runtime.locale',
    language: currentLocale,
    mode: localeSetting.mode || 'system',
    fallback: 'en'
  })
}

// ---- Sessions ----
function renderSessions(sessions) {
  if (!runtimeSessionList) return
  runtimeSessionList.innerHTML = ''
  const items = Array.isArray(sessions) ? sessions : []
  if (!items.length) {
    runtimeSessionList.appendChild(emptyCard(t('settings.noSessions')))
    return
  }
  items.forEach(session => {
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'model-choice-row pressable'
    row.classList.toggle('active', session.id === (runtimeServiceState && runtimeServiceState.currentSessionId))
    const body = document.createElement('div')
    body.className = 'model-choice-body'
    const title = el('div', 'model-choice-title', session.title || session.id || '--')
    const sub = el('div', 'model-choice-sub', (session.modelId || '--') + ' · ' + (session.messageCount || 0) + ' messages')
    body.appendChild(title)
    body.appendChild(sub)
    const check = el('div', 'model-choice-check', '✓')
    row.appendChild(body)
    row.appendChild(check)
    row.addEventListener('click', () => {
      runtimeApiCommand('session.select', { sessionId: session.id })
    })
    runtimeSessionList.appendChild(row)
  })
}

// ---- Diagnostics ----
function updateDiagnostics(state) {
  const log = document.getElementById('runtimeDiagLog')
  if (!log) return
  log.innerHTML = ''
  const requests = Array.isArray(state.recentRequests) ? state.recentRequests : []
  if (!requests.length) {
    log.appendChild(emptyCard(t('settings.noRequests')))
    return
  }
  requests.forEach(req => {
    const row = document.createElement('div')
    row.className = 'diag-row'
    row.innerHTML = '<span class="diag-method">' + (req.method || '') + '</span>' +
      '<span class="diag-path">' + (req.endpoint || '') + '</span>' +
      '<span class="diag-status diag-' + (req.status < 400 ? 'ok' : 'err') + '">' + (req.status || '') + '</span>' +
      '<span class="diag-time">' + (req.elapsedMs || 0) + 'ms</span>'
    log.appendChild(row)
  })
}

// ---- Settings navigation ----
function showSettingsHome() {
  ensureSettingsDetailClosed()
  settingsList.setAttribute('aria-hidden', 'false')
}

function showSettingsDetail(panel) {
  ensureSettingsDetailClosed()
  settingsList.setAttribute('aria-hidden', 'true')
  panel.classList.add('active')
  panel.setAttribute('aria-hidden', 'false')
}

function openRuntimeSettings() {
  showSettingsDetail(runtimeSettingsPanel)
}

function closeRuntimeSettings() {
  showSettingsHome()
  updateRuntimeServiceState(runtimeServiceState || {})
}

function closeRuntimeSettingsBack() {
  closeRuntimeSettings()
}

function ensureSettingsDetailClosed() {
  runtimeDetails().forEach(p => { p.classList.remove('active'); p.setAttribute('aria-hidden', 'true') })
}

function openRuntimeServerSettings() {
  showSettingsDetail(runtimeServerPanel)
}

function backToRuntimeSettings() {
  showSettingsHome()
  updateRuntimeServiceState(runtimeServiceState || {})
}

function openRuntimeCapabilitiesSettings() {
  showSettingsDetail(runtimeCapabilitiesPanel)
}

function openRuntimeModelSettings() {
  showSettingsDetail(runtimeModelPanel)
  renderRuntimeModelChoices(runtimeModels)
}

function openRuntimeSessionsSettings() {
  showSettingsDetail(runtimeSessionsPanel)
}

function openRuntimeDiagnosticsSettings() {
  showSettingsDetail(runtimeDiagnosticsPanel)
}
