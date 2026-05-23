/* ── Lociant WebUI — Model management ── */

function setModelView(view) {
  modelView = view || 'home'
  const views = { home: modelHomeView, local: modelLocalView, market: modelMarketPanel }
  Object.keys(views).forEach(key => {
    const node = views[key]
    if (!node) return
    const active = key === modelView
    node.classList.toggle('active', active)
    node.setAttribute('aria-hidden', active ? 'false' : 'true')
  })
  if (modelView === 'local') loadModels()
  if (modelView === 'market') {
    renderModelMarket(marketModels)
    if (!marketModels.length) loadModelMarket()
  }
}

function loadModels() {
  retryApi(() => apiGet('/v1/models/full'), []).then(data => {
    runtimeModels = Array.isArray(data) ? data : []
    renderModels(runtimeModels)
    updateModelHomeState()
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
}

function deleteModel(modelId) {
  if (!modelId) return
  apiPost('/v1/models/' + encodeURIComponent(modelId) + '/delete', {})
    .then(result => {
      if (result.ok) {
        loadModels()
        showToast(t('toast.modelDeleted'))
      } else {
        showToast(result.message || t('toast.modelDeleteFailed'))
      }
    })
    .catch(() => showToast(t('toast.modelDeleteFailed')))
}

// ---- Model Market ----
function loadModelMarket(forceRefresh) {
  const query = marketQuery
  const url = '/v1/models/market' + (query ? '?q=' + encodeURIComponent(query) : '') + (forceRefresh ? (query ? '&refresh=true' : '?refresh=true') : '')
  apiGet(url)
    .then(data => {
      marketModels = (data && Array.isArray(data.models)) ? data.models : []
      renderModelMarket(marketModels)
      showToast(t('toast.modelMarketLoaded'))
    })
    .catch(() => showToast(t('toast.modelMarketFailed')))
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
  setModelProgress({ state: 'installing', message: t('models.installing') + ': ' + (model.name || model.id) })
  apiPost('/v1/models/market/' + encodeURIComponent(model.id) + '/install', {})
    .then(result => {
      if (result && result.ok) {
        pollMarketInstall(model.id)
        loadModels()
      } else {
        marketInstallingModelId = ''
        setModelProgress(Object.assign({ state: 'error' }, result || {}))
        showToast((result && result.message) || t('toast.modelImportFailed'))
      }
    })
    .catch(() => {
      marketInstallingModelId = ''
      setModelProgress({ state: 'error', message: t('toast.modelImportFailed') })
      showToast(t('toast.modelImportFailed'))
    })
}

function pollMarketInstall(modelId) {
  if (marketInstallTimer) window.clearInterval(marketInstallTimer)
  marketInstallTimer = window.setInterval(() => {
    apiGet('/v1/models/market/' + encodeURIComponent(modelId) + '/progress')
      .then(data => {
        if (data && data.active) {
          setModelProgress(data)
        } else {
          marketInstallingModelId = ''
          setModelProgress({ state: 'done', message: t('toast.modelImported') })
          window.clearInterval(marketInstallTimer)
          marketInstallTimer = null
          loadModels()
          showToast(t('toast.modelImported'))
        }
      })
      .catch(() => {
        marketInstallingModelId = ''
        window.clearInterval(marketInstallTimer)
        marketInstallTimer = null
      })
  }, 2000)
}

function setModelProgress(data) {
  if (!modelProgress) return
  const state = data && data.state
  modelProgress.classList.toggle('active', state === 'installing' || state === 'downloading')
  modelProgress.classList.toggle('error', state === 'error')
  modelProgress.classList.toggle('done', state === 'done')
  modelProgressText.textContent = (data && data.message) || ''
  modelProgressPercent.textContent = data && data.percent ? Math.round(data.percent) + '%' : ''
  modelProgressFill.style.width = (data && data.percent ? Math.min(100, Math.round(data.percent)) : 0) + '%'
}

// ---- Model choice (settings panel) ----
function renderRuntimeModelChoices(models) {
  if (!runtimeModelList) return
  runtimeModelList.innerHTML = ''
  const readyModels = (Array.isArray(models) ? models : []).filter(model => model && model.ready && isRuntimeModel(model))
  const currentModelId = (runtimeServiceState && runtimeServiceState.modelId) || (readyModels[0] && readyModels[0].id) || '--'
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
      runtimeApiCommand('settings', { modelId: model.id })
    })
    runtimeModelList.appendChild(row)
  })
}

function isRuntimeModel(model) {
  const type = (model.type || '').toLowerCase()
  const runtime = (model.runtime || '').toLowerCase()
  return runtime === 'mnn' || type === 'vlm' || type === 'chat' || type === 'llm'
}
