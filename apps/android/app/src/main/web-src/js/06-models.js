/* ── Lociant WebUI — Model management ── */

function setModelView(view) {
  modelView = view || 'home'
  const views = { home: modelHomeView, local: modelLocalView, market: modelMarketPanel, cloud: modelCloudView }
  Object.keys(views).forEach(key => {
    const node = views[key]
    if (!node) return
    const active = key === modelView
    node.classList.toggle('active', active)
    node.setAttribute('aria-hidden', active ? 'false' : 'true')
  })
  if (modelView === 'local') loadModels()
  if (modelView === 'market') {
    updateModelMarketHint()
    renderModelMarket(marketModels)
    if (!marketModels.length) loadModelMarket()
  }
  if (modelView === 'cloud') {
    updateModelCloudState()
  }
}

function updateModelCloudState() {
  if (!modelCloudState || !modelCloudHintText) return
  const state = runtimeServiceState || {}
  const enabled = !!state.cloudEnabled && !!state.cloudModel
  modelCloudState.textContent = enabled ? String(state.cloudModel) : '--'
  modelCloudState.classList.toggle('running', enabled)
  modelCloudHintText.textContent = enabled
    ? t('models.cloudHintEnabled')
    : t('models.cloudHint')
}

function loadModels(refresh) {
  const path = refresh ? '/api/v1/models?refresh=true' : '/api/v1/models'
  return retryApi(() => apiGet(path), () => ({ models: [] })).then(data => {
    runtimeModels = data && Array.isArray(data.models) ? data.models : []
    renderModels(runtimeModels)
    updateModelHomeState()
    return runtimeModels
  })
}

function renderModels(models) {
  modelList.innerHTML = ''
  if (!Array.isArray(models) || !models.length) {
    modelList.appendChild(emptyCard(t('empty.models')))
    return
  }
  models.forEach(model => {
    const name = model.name || model.id || '--'
    const id = model.id || ''
    const runtime = model.runtime || ''
    const type = model.type || ''
    const ready = model.ready
    const installed = model.installed

    const row = document.createElement('div')
    row.className = 'model-row'

    const icon = el('div', 'model-icon', ready ? '✓' : '○')
    const body = document.createElement('div')
    body.className = 'model-body'
    const title = el('div', 'model-title', name)
    const tags = document.createElement('div')
    tags.className = 'model-tags'
    if (runtime) tags.appendChild(el('span', 'model-tag', runtime))
    if (type) tags.appendChild(el('span', 'model-tag', type))
    if (!installed) tags.appendChild(el('span', 'model-tag', 'built-in'))

    const actions = document.createElement('div')
    actions.className = 'model-actions'
    if (ready) {
      actions.appendChild(el('span', 'model-ready', t('settings.visionReady')))
    } else if (installed) {
      const missing = model.missingFiles
      if (Array.isArray(missing) && missing.length) {
        actions.appendChild(el('span', 'model-missing', missing.join(', ')))
      } else {
        actions.appendChild(el('span', 'model-missing', t('empty.models')))
      }
    } else {
      actions.appendChild(el('span', 'model-not-ready', ''))
    }
    if (installed) {
      const del = document.createElement('button')
      del.type = 'button'
      del.className = 'model-delete pressable'
      del.textContent = t('models.delete')
      del.addEventListener('click', event => {
        event.stopPropagation()
        if (model.id) deleteModel(model.id)
      })
      row.appendChild(del)
    }

    body.appendChild(title)
    body.appendChild(tags)
    row.appendChild(icon)
    row.appendChild(body)
    row.appendChild(actions)
    modelList.appendChild(row)
  })
}

function updateModelHomeState() {
  const readyModels = runtimeModels.filter(model => model && model.ready)
  if (modelLocalState) modelLocalState.textContent = String(readyModels.length)
  if (modelRuntimeState) modelRuntimeState.textContent = (runtimeServiceState && runtimeServiceState.modelId) || '--'
  updateHomeState()
}

function deleteModel(modelId) {
  if (!modelId) return
  apiDelete('/api/v1/models/' + encodeURIComponent(modelId))
    .then(() => {
      loadModels()
      showToast(t('toast.modelDeleted'))
    })
    .catch(() => showToast(t('toast.modelDeleteFailed')))
}

// ---- Model Market ----
function loadModelMarket(forceRefresh) {
  const query = marketQuery
  const url = '/api/v1/catalog/models' + (query ? '?q=' + encodeURIComponent(query) : '') + (forceRefresh ? (query ? '&refresh=true' : '?refresh=true') : '')
  apiGet(url)
    .then(data => {
      marketModels = (data && Array.isArray(data.models)) ? data.models : []
      renderModelMarket(marketModels)
      updateModelMarketHint()
      showToast(t('toast.modelMarketLoaded'))
    })
    .catch(() => {
      updateModelMarketHint()
      const state = runtimeServiceState || {}
      if (state.running || state.starting) showToast(t('toast.modelMarketFailed'))
    })
}

function updateModelMarketHint() {
  if (!modelMarketRuntimeHint) return
  const state = runtimeServiceState || {}
  const needsRuntime = !(state.running || state.starting)
  modelMarketRuntimeHint.classList.toggle('is-hidden', !needsRuntime)
}

function startRuntimeForMarket() {
  startRuntime({})
  updateModelMarketHint()
  let attempts = 0
  const timer = window.setInterval(() => {
    const state = runtimeServiceState || {}
    attempts += 1
    if (state.running) {
      window.clearInterval(timer)
      updateModelMarketHint()
      loadModelMarket(true)
    } else if (attempts >= 30) {
      window.clearInterval(timer)
      updateModelMarketHint()
    } else {
      updateModelMarketHint()
    }
  }, 1000)
}

function renderModelMarket(models) {
  modelMarketList.innerHTML = ''
  if (!Array.isArray(models) || !models.length) {
    modelMarketList.appendChild(emptyCard(t('empty.models')))
    return
  }
  models.forEach(model => {
    const row = document.createElement('div')
    row.className = 'model-market-row'
    const body = document.createElement('div')
    body.className = 'model-market-body'
    const name = el('div', 'model-market-name', model.name || model.id || '--')
    const desc = el('div', 'model-market-desc', (model.description || '').slice(0, 120))
    const actions = document.createElement('div')
    actions.className = 'model-market-actions'
    const install = document.createElement('button')
    install.type = 'button'
    install.className = 'model-market-install pressable'
    install.textContent = isMarketModelInstalled(model)
      ? t('models.installed')
      : (marketInstallingModelId === model.id ? t('models.installing') : t('models.install'))
    install.disabled = isMarketModelInstalled(model) || marketInstallingModelId === model.id
    install.addEventListener('click', () => installMarketModel(model))
    actions.appendChild(install)
    body.appendChild(name)
    body.appendChild(desc)
    row.appendChild(body)
    row.appendChild(actions)
    modelMarketList.appendChild(row)
  })
}

function isMarketModelInstalled(model) {
  return runtimeModels.some(item => item && item.id === model.id && item.ready)
}

function installMarketModel(model) {
  if (!model || !model.id || isMarketModelInstalled(model)) return
  marketInstallingModelId = model.id
  modelProgressLastPercent = 0
  setModelProgress({ state: 'installing', message: t('models.installing') + ': ' + (model.name || model.id) })
  apiPost('/api/v1/model-installations', { modelId: model.id })
    .then(result => {
      pollMarketInstall(result.jobId, model.id)
      loadModels()
    })
    .catch(() => {
      marketInstallingModelId = ''
      setModelProgress({ state: 'error', message: t('toast.modelImportFailed') })
      showToast(t('toast.modelImportFailed'))
    })
}

function pollMarketInstall(jobId, modelId) {
  if (marketInstallTimer) window.clearInterval(marketInstallTimer)
  let retries = 0
  marketInstallTimer = window.setInterval(() => {
    apiGet('/api/v1/model-installations/' + encodeURIComponent(jobId))
      .then(data => {
        const next = normalizeMarketProgress(data, modelId)
        if (next.state === 'done') {
          marketInstallingModelId = ''
          setModelProgress(next)
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
          loadModels()
          showToast(t('toast.modelImported'))
          return
        }
        if (next.state === 'error') {
          marketInstallingModelId = ''
          setModelProgress(next)
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
          return
        }
        retries = 0
        setModelProgress(next)
      })
      .catch(() => {
        retries += 1
        setModelProgress({
          state: 'installing',
          active: true,
          modelId: modelId,
          progress: modelProgressLastPercent || 0,
          message: t('models.installing') + ': ' + (marketInstallingModelId || modelId),
        })
        if (retries >= 20) {
          marketInstallingModelId = ''
          setModelProgress({ state: 'error', message: t('toast.modelImportFailed'), progress: modelProgressLastPercent || null })
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
        }
      })
  }, 800)
}

function setModelProgress(data) {
  if (!modelProgress) return
  const normalized = normalizeMarketProgress(data, marketInstallingModelId)
  const state = normalized.state
  window.clearTimeout(modelProgressHideTimer)
  modelProgress.classList.toggle('active', state === 'installing' || state === 'downloading' || state === 'done')
  modelProgress.classList.toggle('error', state === 'error')
  modelProgress.classList.toggle('done', state === 'done')
  modelProgressText.textContent = normalized.message || ''
  const percent = Number(normalized.percent)
  if (Number.isFinite(percent)) {
    modelProgressLastPercent = Math.max(modelProgressLastPercent, percent)
  }
  const showPercent = Number.isFinite(percent) && percent > 0
  modelProgressPercent.textContent = showPercent ? Math.round(percent) + '%' : ''
  const width = Number.isFinite(percent) ? percent : modelProgressLastPercent
  modelProgressFill.style.width = Math.max(0, Math.min(100, Math.round(width || 0))) + '%'
  if (state === 'installing' || state === 'downloading') {
    marketInstallingModelId = marketInstallingModelId || normalized.modelId || ''
  } else if (state === 'done') {
    modelProgressHideTimer = window.setTimeout(() => {
      if (!modelProgress) return
      modelProgress.classList.remove('active')
      modelProgress.classList.remove('done')
    }, 1400)
  } else if (state === 'error') {
    modelProgressHideTimer = window.setTimeout(() => {
      if (!modelProgress) return
      modelProgress.classList.remove('active')
      modelProgress.classList.remove('error')
    }, 2400)
  }
}

function normalizeMarketProgress(data, fallbackModelId) {
  const payload = data || {}
  const rawState = String(payload.state || '').toLowerCase()
  const active = payload.active !== undefined ? !!payload.active : null
  const modelId = payload.modelId || fallbackModelId || marketInstallingModelId || ''
  const rawPercent = Number(payload.percent ?? payload.progress)
  const hasPercent = Number.isFinite(rawPercent)
  const percent = hasPercent ? (rawPercent <= 1 ? rawPercent * 100 : rawPercent) : null

  let state = rawState
  if (!state) {
    if (active === false && hasPercent && percent >= 100) state = 'done'
    else if (active === false && !hasPercent) state = 'installing'
    else state = 'installing'
  }

  if (state === 'done' || (active === false && percent !== null && percent >= 100)) {
    state = 'done'
  } else if (!['error', 'done', 'downloading', 'installing'].includes(state)) {
    state = 'installing'
  }

  return {
    state,
    active: state === 'installing' || state === 'downloading',
    modelId,
    message: payload.message || (state === 'done' ? t('toast.modelImported') : t('models.installing') + ': ' + (modelId || marketInstallingModelId || '')),
    percent: percent,
  }
}

// ---- Model choice (settings panel) ----
function renderRuntimeModelChoices(models) {
  if (!runtimeModelList) return
  runtimeModelList.innerHTML = ''
  const readyModels = (Array.isArray(models) ? models : []).filter(model => model && model.ready && isRuntimeModel(model))
  const state = runtimeServiceState || {}
  const cloudId = String(state.cloudModel || '').trim()
  const cloudIdLower = cloudId.toLowerCase()
  if (state.cloudEnabled && cloudId && !readyModels.some(m => m && String(m.id || '').toLowerCase() === cloudIdLower)) {
    readyModels.push({ id: cloudId, name: cloudId, runtime: 'cloud', type: 'chat', ready: true, installed: true, cloud: true })
  }
  const currentModelId = state.modelId || (readyModels[0] && readyModels[0].id) || '--'
  runtimeModelState.textContent = currentModelId
  runtimeModelNote.textContent = readyModels.length ? t('settings.defaultModelNote') : t('empty.models')
  if (!readyModels.length) {
    runtimeModelList.appendChild(emptyCard(t('empty.models')))
    return
  }
  readyModels.forEach(model => {
    const row = document.createElement('button')
    row.type = 'button'
    row.className = 'model-choice-row pressable'
    row.classList.toggle('active', (runtimeServiceState && runtimeServiceState.modelId) === model.id)
    const body = document.createElement('div')
    body.className = 'model-choice-body'
    const name = el('div', 'model-choice-title', model.name || model.id)
    const meta = el('div', 'model-choice-sub')
    meta.textContent = [model.runtime, model.type].filter(Boolean).join(' · ')
    body.appendChild(name)
    body.appendChild(meta)
    const check = el('div', 'model-choice-check', '✓')
    row.appendChild(body)
    row.appendChild(check)
    row.addEventListener('click', () => {
      updateRuntimeSettings({ modelId: model.id })
    })
    runtimeModelList.appendChild(row)
  })
}

function isRuntimeModel(model) {
  const type = (model.type || '').toLowerCase()
  const runtime = (model.runtime || '').toLowerCase()
  return runtime === 'mnn' || type === 'vlm' || type === 'chat' || type === 'llm'
}
