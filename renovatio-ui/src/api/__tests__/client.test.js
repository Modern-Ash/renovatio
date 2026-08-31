import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createBrowserAnalyzeJob } from '../client'

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
