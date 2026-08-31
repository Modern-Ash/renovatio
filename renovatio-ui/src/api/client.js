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

export function getJob(jobId) {
  return apiCall(`/jobs/${jobId}`);
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
  const eventSource = new EventSource(`${API_BASE}/jobs/${jobId}/events`);
  
  eventSource.addEventListener('progress', (e) => {
    onEvent(JSON.parse(e.data));
  });
  
  eventSource.addEventListener('status', (e) => {
    onEvent(JSON.parse(e.data));
  });
  
  eventSource.addEventListener('error', (e) => {
    if (onError) onError(e);
  });
  
  return () => eventSource.close();
}
