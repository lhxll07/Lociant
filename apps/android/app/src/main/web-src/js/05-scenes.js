/* ── Lociant WebUI — Scene management ── */

function loadScenes() {
  retryApi(() => apiGet('/v1/scenes'), () => []).then(data => {
    scenes = Array.isArray(data) ? data : (Array.isArray(data && data.scenes) ? data.scenes : [])
    renderScenes(scenes)
  })
}

function renderScenes(scenes) {
  sceneList.innerHTML = ''
  if (!scenes.length) {
    sceneList.appendChild(emptyCard(t('empty.scenes')))
    return
  }
  scenes.forEach(scene => {
    const card = document.createElement('button')
    card.type = 'button'
    card.className = 'scene-card pressable'
    card.addEventListener('click', () => openScene(scene))

    const icon = el('div', 'scene-icon')
    icon.innerHTML = sceneIconSvg()
    const body = document.createElement('div')
    body.className = 'scene-body'
    const name = el('div', 'scene-name', scene.name || scene.id)
    const source = el('div', 'scene-source', scene.source || '')

    const actions = document.createElement('div')
    actions.className = 'scene-actions'
    if (scene.source === 'installed') {
      const uninstall = el('button', 'scene-uninstall', '✕')
      uninstall.type = 'button'
      uninstall.title = t('toast.sceneUninstalled')
      uninstall.addEventListener('click', event => {
        event.stopPropagation()
        uninstallScene(scene)
      })
      actions.appendChild(uninstall)
    }

    body.appendChild(name)
    body.appendChild(source)
    card.appendChild(icon)
    card.appendChild(body)
    card.appendChild(actions)
    sceneList.appendChild(card)
  })
}

function sceneIconSvg() {
  return '<svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H20v17H7.5A3.5 3.5 0 0 0 4 22V5.5z"/><path d="M4 5.5A3.5 3.5 0 0 1 7.5 9H20"/></svg>'
}

function sceneEntryUrl(scene) {
  return scene && scene.entryUrl
}

function sceneHasSettings(scene) {
  const capabilities = Array.isArray(scene && scene.capabilities) ? scene.capabilities : []
  return capabilities.includes('settings') || capabilities.includes('vision-settings')
}

function setSceneSettingsVisible(visible) {
  sceneSettingsButton.classList.toggle('visible', !!visible)
}

function openScene(scene) {
  if (!scene || !scene.entryUrl) return
  updateRuntimeServiceState(shellCommand('status', {}))
  backButton.classList.add('active')
  activeScene = scene
  setSceneSettingsVisible(sceneHasSettings(scene))
  showSceneHost()
  sceneHost.scrollTop = 0
  cameraPreviewRect = null
  sceneFrame.style.height = '100%'
  sceneFrame.src = sceneEntryUrl(scene)
  activateRuntime(scene)
  stateText.textContent = t('state.running')
  updateRuntimeStrip()
}

function goHome() {
  unloadSceneFrame()
  backButton.classList.remove('active')
  activeScene = null
  setSceneSettingsVisible(false)
  showPagePanel('scenes')
  const scenesPanel = document.getElementById('page-scenes')
  if (scenesPanel) scenesPanel.scrollTop = 0
  navItems.forEach(item => item.classList.toggle('active', item.dataset.page === 'scenes'))
  stateText.textContent = runtimeSnapshot && runtimeSnapshot.running ? t('state.background') : t('state.idle')
  updateRuntimeStrip()
}

function activateRuntime(scene) {
  const triggers = Array.isArray(scene && scene.triggers) ? scene.triggers : []
  if (triggers.length) {
    apiPost('/v1/scenes/' + encodeURIComponent(scene.id) + '/load', {}).catch(() => {})
  }
}

function uninstallScene(scene) {
  if (!scene || scene.source !== 'installed') return
  try {
    apiPost('/v1/scenes/' + encodeURIComponent(scene.id) + '/delete', {})
      .then(result => {
        if (result.ok) {
          showToast(t('toast.sceneUninstalled'))
          loadScenes()
        } else {
          showToast(result.message || t('toast.sceneUninstallFailed'))
        }
      })
      .catch(() => showToast(t('toast.sceneUninstallFailed')))
  } catch (error) {
    showToast(t('toast.sceneUninstallFailed'))
  }
}

function toggleSceneSettings() {
  if (!activeScene || !sceneHasSettings(activeScene)) return
  postToScene({ type: 'scene.settings.toggle', sceneId: activeScene.id })
}

// ---- Alert ----
function showAlert(event) {
  const alert = event && event.alert
  if (!alert) return
  const scene = sceneById(event.sceneId)
  activeAlert = { event, alert }
  alertSceneName.textContent = (scene ? scene.name : event.sceneId || 'Scene') + ' · ' + (alert.level || 'alert')
  alertTitle.textContent = alert.title || event.name || 'Alert'
  alertMessage.textContent = alert.message || 'A scene alert was triggered.'
  alertBanner.classList.add('visible')
  alertBanner.setAttribute('aria-hidden', 'false')
}

function clearAlert() {
  activeAlert = null
  alertBanner.classList.remove('visible')
  alertBanner.setAttribute('aria-hidden', 'true')
}

function openAlertScene() {
  const sceneId = activeAlert && activeAlert.event && activeAlert.event.sceneId
  const scene = sceneById(sceneId)
  if (scene) openScene(scene)
  clearAlert()
}

// ---- Scene iframe communication ----
function postToScene(message) {
  try {
    if (!sceneFrame.contentWindow) return false
    sceneFrame.contentWindow.postMessage(message, '*')
    return true
  } catch (error) {
    return false
  }
}

function postToSceneReliable(message, attempts = 6, key = '') {
  if (!activeScene) return
  if (key && reliableTimers.has(key)) {
    reliableTimers.get(key).forEach(timer => window.clearTimeout(timer))
    reliableTimers.delete(key)
  }
  let count = 0
  const timers = []
  const send = () => {
    count += 1
    postToScene(message)
    if (count < attempts) {
      timers.push(window.setTimeout(send, count === 1 ? 80 : 220))
    } else if (key) {
      reliableTimers.delete(key)
    }
  }
  if (key) reliableTimers.set(key, timers)
  send()
}

function installSceneApiClient() {
  const api = publishSceneApiClient()
  try {
    if (sceneFrame.contentWindow) {
      sceneFrame.contentWindow.MNNodeAPI = api
      postToScene({ type: 'api.ready', baseUrl: api.baseUrl, sameOrigin: true })
    }
  } catch (error) {
    postToScene({ type: 'api.ready', baseUrl: api.baseUrl, sameOrigin: false })
  }
}

function unloadSceneFrame() {
  postToScene({ type: 'scene.dispose', keepVision: true })
  sceneFrame.removeAttribute('src')
  sceneFrame.style.height = '100%'
}

function resizeSceneFrame() {
  try {
    const doc = sceneFrame.contentDocument || sceneFrame.contentWindow.document
    if (!doc) return
    const height = Math.max(
      sceneHost.clientHeight,
      doc.documentElement.scrollHeight,
      doc.body ? doc.body.scrollHeight : 0
    )
    sceneFrame.style.height = height + 'px'
    syncCameraPreviewRect()
  } catch (error) {
    sceneFrame.style.height = '100%'
  }
}

function syncCameraPreviewRect(rect) {
  if (rect) cameraPreviewRect = rect
  if (!cameraPreviewRect || !sceneHost.classList.contains('active')) return
  const frameRect = sceneFrame.getBoundingClientRect()
  const cssX = Math.max(0, frameRect.left + cameraPreviewRect.x)
  const cssY = Math.max(0, frameRect.top + cameraPreviewRect.y - sceneHost.scrollTop)
  const scale = nativeViewportScale()
  void { x: Math.round(cssX * scale.x), y: Math.round(cssY * scale.y),
         width: Math.max(1, Math.round(cameraPreviewRect.width * scale.x)),
         height: Math.max(1, Math.round(cameraPreviewRect.height * scale.y)) }
}

function nativeViewportScale() {
  const ratio = window.devicePixelRatio || 1
  return { x: ratio, y: ratio }
}
