import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  bulkConfirmProjectDecisions,
  createBrowserAnalyzeJob,
  getArchitecturePreview,
  getProjectDecisions,
  getProjectProfile,
  patchProjectDecision,
  putProjectProfile
} from '../client'

describe('client browser analyze upload', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    localStorage.setItem('userRole', 'ADMIN')
  })

  it('uploads only COBOL sources and copybooks from the selected folder tree', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: 'job-1' })
    })
    vi.stubGlobal('fetch', fetchMock)

    const cobolFile = new File(['IDENTIFICATION DIVISION.'], 'sample.cbl')
    Object.defineProperty(cobolFile, 'webkitRelativePath', {
      value: 'app/src/cbl/sample.cbl'
    })

    const copybookFile = new File(['01 SAMPLE PIC X(10).'], 'copybook.cpy')
    Object.defineProperty(copybookFile, 'webkitRelativePath', {
      value: 'app/includes/copybook.cpy'
    })

    const ignoredFile = new File(['hello world'], 'notes.txt')
    Object.defineProperty(ignoredFile, 'webkitRelativePath', {
      value: 'app/docs/notes.txt'
    })

    await createBrowserAnalyzeJob('project-1', [cobolFile, copybookFile, ignoredFile], 'app')

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [, request] = fetchMock.mock.calls[0]
    const body = request.body
    expect(body.getAll('files')).toHaveLength(2)
    expect(body.getAll('files').map((file) => file.name)).toEqual([
      'app/src/cbl/sample.cbl',
      'app/includes/copybook.cpy'
    ])
    expect(body.get('workspaceLabel')).toBe('app')
  })

  it('shows a friendly message when the upload exceeds the request limit', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 413
    })
    vi.stubGlobal('fetch', fetchMock)

    const cobolFile = new File(['IDENTIFICATION DIVISION.'], 'sample.cbl')
    Object.defineProperty(cobolFile, 'webkitRelativePath', {
      value: 'app/src/cbl/sample.cbl'
    })

    await expect(
      createBrowserAnalyzeJob('project-1', [cobolFile], 'app')
    ).rejects.toThrow(/too large to upload in one request/i)
  })
})

describe('decision layer client', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    localStorage.setItem('userRole', 'MANAGER')
  })

  it('uses exact profile paths and forwards the quoted ETag with the role', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, headers: new Headers({ ETag: '"3"' }), json: async () => ({ schemaVersion: '1', extensions: {} }) })
      .mockResolvedValueOnce({ ok: true, headers: new Headers({ ETag: '"4"' }), json: async () => ({ schemaVersion: '1', extensions: {} }) })
    vi.stubGlobal('fetch', fetchMock)

    expect((await getProjectProfile('p 1')).etag).toBe('"3"')
    await putProjectProfile('p1', { schemaVersion: '1', extensions: {} }, '"3"')

    expect(fetchMock.mock.calls[0][0]).toBe('/api/projects/p 1/profile')
    expect(fetchMock.mock.calls[1]).toEqual(['/api/projects/p1/profile', expect.objectContaining({
      method: 'PUT', body: '{"schemaVersion":"1","extensions":{}}',
      headers: expect.objectContaining({ 'If-Match': '"3"', 'X-Role': 'MANAGER', 'Content-Type': 'application/json' })
    })])
  })

  it('encodes independent filters and exact decision mutation bodies', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, headers: new Headers(), json: async () => ({ items: [], total: 0 }) })
    vi.stubGlobal('fetch', fetchMock)

    await getProjectDecisions('p1', { category: 'DATA_SHAPE', minConfidence: 0.8, status: 'AUTO' })
    await patchProjectDecision('p1', 'd1', 'FIELD_INITIALIZER', 2)
    await bulkConfirmProjectDecisions('p1', 0.8)

    expect(fetchMock.mock.calls[0][0]).toBe('/api/projects/p1/decisions?category=DATA_SHAPE&minConfidence=0.8&status=AUTO')
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({ method: 'PATCH', body: '{"chosenOption":"FIELD_INITIALIZER","revision":2}' }))
    expect(fetchMock.mock.calls[2][1]).toEqual(expect.objectContaining({ method: 'POST', body: '{"minConfidence":0.8}' }))
  })

  it('requests a read-only architecture preview for the draft selection', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true, headers: new Headers(), json: async () => ({ schemaVersion: '1', artifacts: [] })
    })
    vi.stubGlobal('fetch', fetchMock)

    await getArchitecturePreview('p1', { style: 'HEXAGONAL', moduleGrouping: 'BY_DOMAIN' })

    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/projects/p1/architecture-preview?style=HEXAGONAL&moduleGrouping=BY_DOMAIN'
    )
    expect(fetchMock.mock.calls[0][1].headers).toEqual(expect.objectContaining({ 'X-Role': 'MANAGER' }))
  })

  it('propagates structured status and profile violations', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false, status: 422, headers: new Headers(),
      json: async () => ({ code: 'PROFILE_VALIDATION_FAILED', violations: [{ path: '/llm/enabled' }] })
    }))

    await expect(putProjectProfile('p1', {}, '"0"')).rejects.toMatchObject({
      status: 422,
      payload: { code: 'PROFILE_VALIDATION_FAILED' }
    })
  })
})
