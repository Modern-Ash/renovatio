import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import StepAnalyze from '../StepAnalyze'

const createJobMock = vi.fn()
const createBrowserAnalyzeJobMock = vi.fn()
const subscribeToJobMock = vi.fn()
const getJobStatusMock = vi.fn()

vi.mock('../../api/client', () => ({
  createJob: (...args) => createJobMock(...args),
  createBrowserAnalyzeJob: (...args) => createBrowserAnalyzeJobMock(...args),
  subscribeToJob: (...args) => subscribeToJobMock(...args),
  getJobStatus: (...args) => getJobStatusMock(...args)
}))

describe('StepAnalyze', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    createJobMock.mockResolvedValue({ id: 'job-1' })
    createBrowserAnalyzeJobMock.mockResolvedValue({ id: 'job-browser' })
    getJobStatusMock.mockResolvedValue({ status: 'COMPLETED' })
    subscribeToJobMock.mockImplementation((_jobId, _onEvent, onError) => {
      onError(new Event('error'))
      return vi.fn()
    })
  })

  it('keeps a completed analysis successful even if the stream closes', async () => {
    render(
      <StepAnalyze
        projectId="project-1"
        data={{ workspacePath: '/workspace/demo' }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: /start analysis/i }))

    expect(await screen.findByText(/analysis completed successfully/i)).toBeTruthy()
    expect(screen.queryByText(/connection lost/i)).toBeNull()
    expect(createJobMock).toHaveBeenCalledWith('project-1', 'analyze', {
      workspacePath: '/workspace/demo'
    })
  })

  it('shows the completion message and parsing summary returned by the job when available', async () => {
    const onChangeMock = vi.fn()
    subscribeToJobMock.mockImplementation((_jobId, onEvent) => {
      onEvent({
        status: 'COMPLETED',
        message: 'Parsed 44 COBOL source file(s) and 62 copybook(s) from /workspace/demo',
        summary: {
          sourceFiles: 44,
          copybooks: 62,
          programs: 44
        }
      })
      return vi.fn()
    })

    render(
      <StepAnalyze
        projectId="project-1"
        data={{ workspacePath: '/workspace/demo' }}
        onChange={onChangeMock}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: /start analysis/i }))

    expect(await screen.findByText(/parsed 44 cobol source file\(s\) and 62 copybook\(s\) from \/workspace\/demo/i)).toBeTruthy()
    expect(await screen.findByText(/44 cobol source file\(s\)/i)).toBeTruthy()
    expect(screen.getByText(/62 copybook\(s\)/i)).toBeTruthy()
    const summaryBlock = screen.getByText(/parsing summary/i).parentElement
    expect(summaryBlock?.textContent).toContain('44')
    expect(summaryBlock?.textContent).toContain('62')
    expect(summaryBlock?.textContent).toContain('parsed program(s)')
    expect(onChangeMock).toHaveBeenCalledWith({
      analysisSummary: {
        sourceFiles: 44,
        copybooks: 62,
        programs: 44
      },
      analysisMessage: 'Parsed 44 COBOL source file(s) and 62 copybook(s) from /workspace/demo',
      analysisWorkspace: '/workspace/demo'
    })
  })

  it('uploads a browser-selected folder and analyzes the uploaded subdirectory tree', async () => {
    const file = new File(
      ['       IDENTIFICATION DIVISION.\n       PROGRAM-ID. SAMPLE.\n'],
      'sample.cbl'
    )
    Object.defineProperty(file, 'webkitRelativePath', {
      value: 'app/src/cbl/sample.cbl'
    })

    render(
      <StepAnalyze
        projectId="project-1"
        data={{
          workspaceSelectionMode: 'browser',
          workspaceFolderName: 'app',
          workspaceFiles: [file]
        }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: /start analysis/i }))

    expect(await screen.findByText(/analysis completed successfully/i)).toBeTruthy()
    expect(createBrowserAnalyzeJobMock).toHaveBeenCalledWith('project-1', [file], 'app')
    expect(createJobMock).not.toHaveBeenCalled()
  })
})
