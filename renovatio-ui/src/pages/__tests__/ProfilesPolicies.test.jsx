import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import ProfilesPolicies from '../ProfilesPolicies'

const api = vi.hoisted(() => ({
  templates: vi.fn(), policies: vi.fn(), projects: vi.fn(), saveTemplate: vi.fn(),
  exportPolicy: vi.fn(), bindTemplate: vi.fn(), bindPolicy: vi.fn()
}))

vi.mock('../../api/client', () => ({
  getProfileTemplates: api.templates,
  getPolicyCatalogs: api.policies,
  getProjects: api.projects,
  saveProfileTemplate: api.saveTemplate,
  exportPolicyCatalog: api.exportPolicy,
  bindProfileTemplate: api.bindTemplate,
  bindPolicyCatalog: api.bindPolicy
}))

describe('ProfilesPolicies', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.templates.mockResolvedValue([{ name: 'bank', version: '1', description: 'Bank profile', contentHash: 'a'.repeat(64), projects: [] }])
    api.policies.mockResolvedValue([{ name: 'bank', version: '1', entries: 7, contentHash: 'b'.repeat(64), projects: ['p2'] }])
    api.projects.mockResolvedValue([{ id: 'p1', name: 'Project A' }])
    api.saveTemplate.mockResolvedValue({})
    api.exportPolicy.mockResolvedValue({})
    api.bindTemplate.mockResolvedValue({})
    api.bindPolicy.mockResolvedValue({ autoConfirmed: 6, suggested: 1, unmatched: 0 })
  })

  it('lists immutable versions and creates a profile template from a project', async () => {
    render(<ProfilesPolicies />)
    expect(await screen.findByText('Bank profile')).toBeTruthy()
    expect(screen.getByText('7 reusable decisions')).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/^name$/i), { target: { value: 'payments' } })
    fireEvent.click(screen.getByRole('button', { name: /create immutable version/i }))
    await waitFor(() => expect(api.saveTemplate).toHaveBeenCalledWith(expect.objectContaining({
      projectId: 'p1', name: 'payments', version: '1.0.0'
    })))
  })

  it('binds explicit versions and reports policy match counts', async () => {
    render(<ProfilesPolicies />)
    await screen.findByText('Bank profile')
    fireEvent.click(screen.getByRole('button', { name: /bind bank@1/i }))
    await waitFor(() => expect(api.bindTemplate).toHaveBeenCalledWith('p1', expect.objectContaining({ name: 'bank', version: '1' })))
    fireEvent.click(screen.getByRole('button', { name: /apply bank@1/i }))
    expect(await screen.findByText(/6 auto-confirmed · 1 suggested/i)).toBeTruthy()
  })
})
