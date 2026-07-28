/* ── Lociant WebUI — API client and native bridge ── */

function native(method, ...args) {
  try {
    const bridge = window.MNNodeShell
    if (bridge && typeof bridge[method] === 'function') return bridge[method](...args)
  } catch (error) {}
  return null
}

function nativeJson(method, fallback, ...args) {
  const raw = native(method, ...args)
  if (!raw) return fallback
  try { return JSON.parse(raw) } catch (error) { return fallback }
}

function localApiBaseUrl() {
  const state = runtimeServiceState || {}
  const raw = state.url || 'http://127.0.0.1:11434'
  return String(raw).replace('0.0.0.0', '127.0.0.1').replace(/\/$/, '')
}

function publicRuntimeUrl(state) {
  const current = state || runtimeServiceState || {}
  const raw = current.lanUrl || current.url || ('http://127.0.0.1:' + (current.port || 11434))
  return String(raw).replace(/\/$/, '')
}

function openAiBaseUrl() {
  return publicRuntimeUrl(runtimeServiceState) + '/v1'
}

function mcpEndpointUrl() {
  return publicRuntimeUrl(runtimeServiceState) + '/mcp'
}

function runtimeAuthToken() {
  return (runtimeAuthTokenInput && runtimeAuthTokenInput.value.trim()) ||
    (runtimeServiceState && runtimeServiceState.authToken) ||
    ''
}

function authHeaderText() {
  const token = runtimeAuthToken()
  return token ? ('Authorization: Bearer ' + token) : 'Authorization disabled'
}

function mcpConfigText() {
  const server = {
    type: 'streamable-http',
    url: mcpEndpointUrl()
  }
  const token = runtimeAuthToken()
  if (token) server.headers = { Authorization: 'Bearer ' + token }
  return JSON.stringify({ mcpServers: { lociant: server } }, null, 2)
}

function testPromptText() {
  return 'Call runtime_status. Then call model_list. If vision is available, call vision_status.'
}

function copyText(text) {
  const value = String(text || '')
  if (navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(value)
  }
  const input = document.createElement('textarea')
  input.value = value
  input.setAttribute('readonly', '')
  input.style.position = 'fixed'
  input.style.left = '-9999px'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  input.setSelectionRange(0, input.value.length)
  const ok = document.execCommand('copy')
  document.body.removeChild(input)
  return ok ? Promise.resolve() : Promise.reject(new Error('copy failed'))
}

function copyConnectionText(factory) {
  try {
    copyText(factory()).then(() => {
      showToast(t('toast.copied'))
    }).catch(() => {
      showToast(t('toast.copyFailed'))
    })
  } catch (error) {
    showToast(t('toast.copyFailed'))
  }
}

function apiUrl(path) {
  return localApiBaseUrl() + path
}

async function apiGet(path) {
  return apiRequest('GET', path)
}

async function apiPost(path, body) {
  return apiRequest('POST', path, body)
}

async function apiRequest(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (runtimeServiceState && runtimeServiceState.authToken) headers.Authorization = 'Bearer ' + runtimeServiceState.authToken
  const response = await fetch(apiUrl(path), {
    method,
    headers,
    body: method === 'GET' ? undefined : JSON.stringify(body || {})
  })
  const json = await response.json()
  if (!response.ok) throw new Error(path + ': ' + ((json.error && json.error.message) || json.message || 'API request failed'))
  return json
}

function retryApi(task, fallback, attempts = 8) {
  let index = 0
  const run = () => task().catch(error => {
    index += 1
    if (index >= attempts) return fallback(error)
    return new Promise(resolve => window.setTimeout(resolve, 250 * index)).then(run)
  })
  return run()
}

function shellCommand(command, payload) {
  const raw = native('runtimeShellCommand', command, JSON.stringify(payload || {}))
  return raw ? JSON.parse(raw) : { running: false, message: 'Runtime shell unavailable' }
}

function runtimeState() {
  try {
    return shellCommand('status', {})
  } catch (error) {
    return runtimeServiceState || { running: false }
  }
}
