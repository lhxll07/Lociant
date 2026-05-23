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

function sceneApiClient() {
  const baseUrl = localApiBaseUrl()
  const headers = () => {
    const output = { 'Content-Type': 'application/json' }
    if (runtimeServiceState && runtimeServiceState.authToken) output.Authorization = 'Bearer ' + runtimeServiceState.authToken
    return output
  }
  const request = (path, body) => fetch(baseUrl + path, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify(body || {})
  }).then(response => response.json().then(json => {
    if (!response.ok || !json.ok) throw new Error((json.error && json.error.message) || json.message || 'API request failed')
    return json.result || json
  }))
  return {
    baseUrl,
    get authToken() {
      return (runtimeServiceState && runtimeServiceState.authToken) || ''
    },
    get(path) {
      return fetch(baseUrl + path, { headers: headers() }).then(response => response.json())
    },
    post(path, body) {
      return request(path, body)
    },
    tool(name, args) {
      return request('/v1/tools/' + encodeURIComponent(name) + '/call', { arguments: args || {} })
    },
    chat(requestBody) {
      return fetch(baseUrl + '/v1/chat/completions', {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify(requestBody || {})
      }).then(response => response.json())
    },
    runtime(command, payload) {
      return shellCommand(command, payload || {})
    },
    window(command) {
      return runtimeWindowCommand(command || 'show')
    }
  }
}

function publishSceneApiClient() {
  const api = sceneApiClient()
  window.MNNodeAPI = api
  return api
}
