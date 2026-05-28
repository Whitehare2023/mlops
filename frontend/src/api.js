const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

async function request(path, options = {}) {
  const isFormData = options.body instanceof FormData
  const headers = isFormData
    ? { ...(options.headers || {}) }
    : {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }

  const response = await fetch(`${API_BASE}${path}`, {
    headers,
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

export function listAssets(keyword = '') {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  return request(`/assets${query}`)
}

export function createAsset(payload) {
  return request('/assets', {
    method: 'POST',
    body: assetFormData(payload)
  })
}

export function updateAsset(id, payload) {
  return request(`/assets/${id}`, {
    method: 'PUT',
    body: assetFormData(payload)
  })
}

export function deleteAsset(id) {
  return request(`/assets/${id}`, {
    method: 'DELETE'
  })
}

export function assetFileUrl(id) {
  return `${API_BASE}/assets/${id}/file`
}

function assetFormData(payload) {
  const formData = new FormData()
  formData.append('assetName', payload.assetName || '')
  formData.append('description', payload.description || '')
  if (payload.file) {
    formData.append('file', payload.file)
  }
  return formData
}
