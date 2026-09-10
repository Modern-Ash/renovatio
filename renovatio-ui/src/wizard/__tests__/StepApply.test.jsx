import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import StepApply from '../StepApply'

const createJobMock = vi.fn()
const subscribeToJobMock = vi.fn()
const getJobStatusMock = vi.fn()

vi.mock('../../api/client', () => ({
  createJob: (...args) => createJobMock(...args),
  subscribeToJob: (...args) => subscribeToJobMock(...args),
  getJobStatus: (...args) => getJobStatusMock(...args)
}))

describe('StepApply', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    createJobMock.mockResolvedValue({ id: 'job-apply' })
    subscribeToJobMock.mockReturnValue(vi.fn())
    getJobStatusMock
      .mockResolvedValueOnce({ status: 'RUNNING' })
      .mockResolvedValueOnce({ status: 'COMPLETED' })
    vi.spyOn(globalThis, 'setInterval').mockImplementation((callback) => {
      queueMicrotask(() => {
        callback()
      })
      return 1
    })
    vi.spyOn(globalThis, 'clearInterval').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('polls job status so a quiet dry run still completes', async () => {
    render(
      <StepApply
        projectId="project-1"
        data={{ planSteps: [] }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: /start dry run/i }))

    await waitFor(() => {
      expect(screen.getByText(/dry run completed successfully/i)).toBeTruthy()
    })
    expect(getJobStatusMock).toHaveBeenCalledWith('job-apply')
  })
})
