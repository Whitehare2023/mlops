const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message || 'Request failed')
  }

  if (response.status === 204) {
    return null
  }
  return response.json()
}

export function listTasks() {
  return request('/tasks')
}

export function createTask(payload) {
  return request('/tasks', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function getTask(id) {
  return request(`/tasks/${id}`)
}

export function getTaskCsv(id) {
  return request(`/tasks/${id}/csv`)
}

export function getTaskImages(id) {
  return request(`/tasks/${id}/images`)
}

export function listAssets() {
  return request('/assets')
}

export function createAsset(payload) {
  return request('/assets', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

