import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import StepTarget from '../StepTarget'

const getProfile = vi.fn()
const getEffective = vi.fn()
const putProfile = vi.fn()

vi.mock('../../api/client', () => ({
  getProjectProfile: (...args) => getProfile(...args),
  getEffectiveProfile: (...args) => getEffective(...args),
  putProjectProfile: (...args) => putProfile(...args)
}))

const effectiveProfile = {
  schemaVersion: '1', extensions: {},
  target: { language: 'JAVA', languageVersion: '17' },
  architecture: { style: 'TRANSACTION_SCRIPT', moduleGrouping: 'BY_PROGRAM' },
  runtime: { framework: 'SPRING_BOOT' },
  persistence: { defaultStrategy: 'IN_MEMORY', transactionBoundary: 'METHOD' },
  style: { numericPolicy: 'BIGDECIMAL', nullability: 'NON_NULL_BY_DEFAULT', errorHandling: 'EXCEPTIONS', naming: 'JAVA_BEANS' },
  llm: { enabled: false, suggestDecisions: false, maxSuggestionsPerRun: 0 }
}

describe('StepTarget', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProfile.mockResolvedValue({ profile: { schemaVersion: '1', extensions: {} }, etag: '"0"' })
    getEffective.mockResolvedValue({ profile: effectiveProfile, appliedDecisionIds: [], profileHash: 'a'.repeat(64) })
    putProfile.mockResolvedValue({ profile: effectiveProfile, etag: '"1"' })
  })

  it('loads defaults, disables inactive languages, and updates the architecture preview', async () => {
    render(<StepTarget projectId="p1" data={{}} onChange={vi.fn()} onNext={vi.fn()} onBack={vi.fn()} />)

    expect(await screen.findByRole('heading', { name: /choose the target shape/i })).toBeTruthy()
    expect(screen.getByLabelText(/node · later/i).disabled).toBe(true)
    expect(screen.getByLabelText(/python · later/i).disabled).toBe(true)
    fireEvent.click(screen.getByLabelText(/hexagonal/i))
    expect(screen.getByLabelText(/hexagonal static layout preview/i)).toBeTruthy()
    expect(screen.getByText('Inbound ports')).toBeTruthy()
  })

  it('saves the profile with If-Match semantics and stores effective state before continuing', async () => {
    const onChange = vi.fn()
    const onNext = vi.fn()
    render(<StepTarget projectId="p1" data={{}} onChange={onChange} onNext={onNext} onBack={vi.fn()} />)
    await screen.findByRole('heading', { name: /choose the target shape/i })
    fireEvent.click(screen.getByLabelText(/ia suggestions/i))
    fireEvent.click(screen.getByRole('button', { name: /save & analyze/i }))

    await waitFor(() => expect(putProfile).toHaveBeenCalled())
    expect(putProfile.mock.calls[0][0]).toBe('p1')
    expect(putProfile.mock.calls[0][2]).toBe('"0"')
    expect(putProfile.mock.calls[0][1].llm).toEqual({ enabled: true, suggestDecisions: true, maxSuggestionsPerRun: 10 })
    await waitFor(() => expect(onNext).toHaveBeenCalled())
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ profileHash: 'a'.repeat(64) }))
  })

  it('keeps ordered server validation errors on the step', async () => {
    putProfile.mockRejectedValue(Object.assign(new Error('validation'), {
      status: 422,
      payload: { violations: [
        { path: '/llm/maxSuggestionsPerRun', code: 'OUT_OF_RANGE', message: 'must be between 0 and 100' },
        { path: '/llm/suggestDecisions', code: 'REQUIRES_ENABLED', message: 'requires llm.enabled=true' }
      ] }
    }))
    render(<StepTarget projectId="p1" data={{}} onChange={vi.fn()} onNext={vi.fn()} onBack={vi.fn()} />)
    await screen.findByRole('heading', { name: /choose the target shape/i })
    fireEvent.click(screen.getByRole('button', { name: /save & analyze/i }))

    expect(await screen.findByText(/fix the profile/i)).toBeTruthy()
    const alert = screen.getByRole('alert')
    expect(alert.textContent.indexOf('/llm/maxSuggestionsPerRun')).toBeLessThan(alert.textContent.indexOf('/llm/suggestDecisions'))
  })
})
