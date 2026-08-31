const API_BASE = '/api';

async function apiCall(path, options = {}) {
  const role = localStorage.getItem('userRole') || 'ADMIN';
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'X-Role': role,
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  });
  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }
  return response.json();
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

  Array.from(files || []).forEach((file) => {
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
