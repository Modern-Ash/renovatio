import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import Dashboard from '../Dashboard'

const getProjectsMock = vi.fn()
const getActionItemsMock = vi.fn()
const getJobsMock = vi.fn()

vi.mock('../../api/client', () => ({
  getProjects: (...args) => getProjectsMock(...args),
  getActionItems: (...args) => getActionItemsMock(...args),
  getJobs: (...args) => getJobsMock(...args)
}))

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProjectsMock.mockResolvedValue([
      { id: 'project-1', name: 'CardDemo One', workspacePath: '/workspace/one', createdAt: new Date().toISOString() },
      { id: 'project-2', name: 'CardDemo Two', workspacePath: '/workspace/two', createdAt: new Date().toISOString() }
    ])
    getActionItemsMock.mockResolvedValue([])
    getJobsMock.mockResolvedValue([
      {
        id: 'job-1',
        projectId: 'project-1',
        operation: 'plan',
        status: 'COMPLETED',
        createdAt: new Date(Date.now() - 10000).toISOString(),
        result: {
          message: 'Planning complete',
          workspacePath: '/workspace/one'
        }
      },
      {
        id: 'job-2',
        projectId: 'project-2',
        operation: 'analyze',
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
        result: {
          message: 'Parsed 44 COBOL source file(s) and 62 copybook(s) from /workspace/demo',
          workspacePath: '/workspace/demo',
          summary: {
            sourceFiles: 44,
            copybooks: 62,
            programs: 44
          }
        }
      }
    ])
  })

  it('shows the latest analysis summary on the dashboard', async () => {
    render(<Dashboard />)

    expect(await screen.findByText(/^cobol source files$/i)).toBeTruthy()
    expect(screen.getAllByText(/^copybooks$/i).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/^parsed programs$/i).length).toBeGreaterThan(0)
    const heading = await screen.findByText(/latest analysis/i)
    const summaryCard = heading.parentElement
    expect(summaryCard?.textContent).toContain('CardDemo Two')
    expect(summaryCard?.textContent).toContain('Parsed 44 COBOL source file(s) and 62 copybook(s) from /workspace/demo')
    expect(summaryCard?.textContent).toContain('44 COBOL file(s)')
    expect(summaryCard?.textContent).toContain('62')
    expect(summaryCard?.textContent).toContain('44')
    expect(getActionItemsMock).toHaveBeenCalledWith('project-2')
  })
})
