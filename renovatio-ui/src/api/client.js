const API_BASE = '/api';

async function apiResponse(path, options = {}) {
  const role = localStorage.getItem('userRole') || 'ADMIN';
  const { headers = {}, ...requestOptions } = options
  const response = await fetch(`${API_BASE}${path}`, {
    ...requestOptions,
    headers: {
      'X-Role': role,
      'Content-Type': 'application/json',
      ...headers
    }
  });
  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }
  if (!response.ok) {
    const error = new Error(payload?.message || `API error: ${response.status}`)
    error.status = response.status
    error.payload = payload
    throw error
  }
  return { response, payload }
}

async function apiCall(path, options = {}) {
  return (await apiResponse(path, options)).payload
}

export function getProjects() {
  return apiCall('/projects');
}

export function getProject(id) {
  return apiCall(`/projects/${id}`);
}

export function createProject(project) {
  return apiCall('/projects', {
    method: 'POST',
    body: JSON.stringify(project)
  });
}

export function createJob(projectId, operation, params = {}) {
  return apiCall(`/projects/${projectId}/jobs`, {
    method: 'POST',
    body: JSON.stringify({ operation, params })
  });
}

export async function createBrowserAnalyzeJob(projectId, files, workspaceLabel = '') {
  const role = localStorage.getItem('userRole') || 'ADMIN';
  const formData = new FormData();

  const cobolExtensions = ['.cob', '.cbl', '.cobol', '.cpy'];
  const selectedFiles = Array.from(files || []).filter((file) => {
    const name = (file.webkitRelativePath || file.name || '').toLowerCase();
    return cobolExtensions.some((extension) => name.endsWith(extension));
  });

  if (selectedFiles.length === 0) {
    throw new Error('No COBOL source or copybook files were found in the selected folder.')
  }

  selectedFiles.forEach((file) => {
    const filename = file.webkitRelativePath || file.name;
    formData.append('files', file, filename);
  });

  if (workspaceLabel) {
    formData.append('workspaceLabel', workspaceLabel);
  }

  const response = await fetch(`${API_BASE}/projects/${projectId}/jobs/browser-analyze`, {
    method: 'POST',
    headers: {
      'X-Role': role
    },
    body: formData
  });

  if (!response.ok) {
    if (response.status === 413) {
      throw new Error('The selected folder is too large to upload in one request. Try a smaller COBOL subtree or reduce non-source files.')
    }
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}

export function getJob(jobId) {
  return apiCall(`/jobs/${jobId}`);
}

export function getJobs() {
  return apiCall('/jobs');
}

export function getProjectJobs(projectId) {
  return apiCall(`/projects/${projectId}/jobs`);
}

export async function getJobStatus(jobId) {
  return getJob(jobId);
}

export function getActionItems(projectId) {
  return apiCall(`/projects/${projectId}/action-items`);
}

export function updateActionItemStatus(id, status) {
  return apiCall(`/action-items/${id}/status`, {
    method: 'POST',
    body: JSON.stringify({ status })
  });
}

export function getMetrics(projectId) {
  return apiCall(`/projects/${projectId}/metrics`);
}

export async function getProjectProfile(projectId) {
  const { response, payload } = await apiResponse(`/projects/${projectId}/profile`)
  return { profile: payload, etag: response.headers.get('ETag') }
}

export async function putProjectProfile(projectId, profile, etag) {
  const { response, payload } = await apiResponse(`/projects/${projectId}/profile`, {
    method: 'PUT',
    headers: { 'If-Match': etag },
    body: JSON.stringify(profile)
  })
  return { profile: payload, etag: response.headers.get('ETag') }
}

export function getEffectiveProfile(projectId) {
  return apiCall(`/projects/${projectId}/profile:effective`)
}

export function getProjectDecisions(projectId, filters = {}) {
  const query = new URLSearchParams()
  if (filters.category) query.set('category', filters.category)
  if (filters.minConfidence !== '' && filters.minConfidence !== undefined && filters.minConfidence !== null) {
    query.set('minConfidence', String(filters.minConfidence))
  }
  if (filters.status) query.set('status', filters.status)
  const suffix = query.toString() ? `?${query}` : ''
  return apiCall(`/projects/${projectId}/decisions${suffix}`)
}

export function patchProjectDecision(projectId, decisionId, chosenOption, revision) {
  return apiCall(`/projects/${projectId}/decisions/${decisionId}`, {
    method: 'PATCH',
    body: JSON.stringify({ chosenOption, revision })
  })
}

export function bulkConfirmProjectDecisions(projectId, minConfidence = 0.8) {
  return apiCall(`/projects/${projectId}/decisions:bulk-confirm`, {
    method: 'POST',
    body: JSON.stringify({ minConfidence: Number(minConfidence) })
  })
}

export function subscribeToJob(jobId, onEvent, onError) {
  const controller = new AbortController()
  const role = localStorage.getItem('userRole') || 'ADMIN'

  const parseChunk = (chunk, state) => {
    state.buffer += chunk

    let newlineIndex = state.buffer.indexOf('\n')
    while (newlineIndex !== -1) {
      const rawLine = state.buffer.slice(0, newlineIndex)
      state.buffer = state.buffer.slice(newlineIndex + 1)
      const line = rawLine.replace(/\r$/, '')

      if (line === '') {
        if (state.dataLines.length > 0) {
          const payload = state.dataLines.join('\n')
          state.dataLines = []
          state.eventName = 'message'
          try {
            onEvent(JSON.parse(payload))
          } catch {
            onEvent({ raw: payload })
          }
        }
      } else if (line.startsWith('event:')) {
        state.eventName = line.slice('event:'.length).trim()
      } else if (line.startsWith('data:')) {
        state.dataLines.push(line.slice('data:'.length).replace(/^ /, ''))
      }

      newlineIndex = state.buffer.indexOf('\n')
    }
  }

  ;(async () => {
    try {
      const response = await fetch(`${API_BASE}/jobs/${jobId}/events`, {
        method: 'GET',
        headers: {
          'X-Role': role,
          Accept: 'text/event-stream'
        },
        signal: controller.signal
      })

      if (!response.ok) {
        throw new Error(`API error: ${response.status}`)
      }

      if (!response.body) {
        throw new Error('Streaming response unavailable')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      const state = { buffer: '', dataLines: [], eventName: 'message' }

      while (true) {
        const { value, done } = await reader.read()
        if (done) {
          break
        }
        parseChunk(decoder.decode(value, { stream: true }), state)
      }

      if (state.dataLines.length > 0) {
        const payload = state.dataLines.join('\n')
        try {
          onEvent(JSON.parse(payload))
        } catch {
          onEvent({ raw: payload })
        }
      }
    } catch (error) {
      if (error?.name === 'AbortError') {
        return
      }
      if (onError) {
        onError(error)
      }
    }
  })()

  return () => controller.abort()
}
