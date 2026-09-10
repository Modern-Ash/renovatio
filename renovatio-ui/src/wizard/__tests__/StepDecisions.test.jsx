import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import StepDecisions from '../StepDecisions'

const getDecisions = vi.fn()
const patchDecision = vi.fn()
const bulkConfirm = vi.fn()

vi.mock('../../api/client', () => ({
  getProjectDecisions: (...args) => getDecisions(...args),
  patchProjectDecision: (...args) => patchDecision(...args),
  bulkConfirmProjectDecisions: (...args) => bulkConfirm(...args)
}))

const decision = {
  id: 'd1', category: 'NAMING', decisionKey: 'java.accessor-convention',
  question: 'Which accessor convention?', options: ['JAVA_BEANS', 'FLUENT'],
  defaultOption: 'JAVA_BEANS', chosenOption: 'JAVA_BEANS', source: 'LLM', confidence: 0.72,
  rationale: 'The surrounding model uses bean accessors.', evidence: ['program: CUSTOMER'],
  status: 'SUGGESTED', revision: 3, llmFailed: false, llmFailureCategory: null
}

describe('StepDecisions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getDecisions.mockResolvedValue({ items: [decision], total: 1 })
    patchDecision.mockResolvedValue({ ...decision, status: 'CONFIRMED', revision: 4 })
    bulkConfirm.mockResolvedValue({ confirmed: 1, skipped: 0, items: [{ ...decision, status: 'CONFIRMED' }] })
  })

  it('renders source badges, rationale, evidence, and LLM fallback without blocking Next', async () => {
    getDecisions.mockResolvedValue({ items: [
      decision,
      { ...decision, id: 'd2', source: 'HEURISTIC', llmFailed: true, llmFailureCategory: 'TIMEOUT' }
    ], total: 2 })
    render(<StepDecisions projectId="p1" onNext={vi.fn()} onBack={vi.fn()} />)

    expect(await screen.findByText('IA')).toBeTruthy()
    expect(screen.getByText('heuristic')).toBeTruthy()
    expect(screen.getByText(/ia fallback · timeout/i)).toBeTruthy()
    fireEvent.click(screen.getAllByText(/rationale & evidence/i)[0])
    expect(screen.getAllByText(/surrounding model uses bean/i)).toHaveLength(2)
    expect(screen.getAllByText(/program: customer/i)).toHaveLength(2)
    expect(screen.getByRole('button', { name: /next: view metrics/i }).disabled).toBe(false)
  })

  it('applies exact filters, confirms, edits, and bulk confirms at the default threshold', async () => {
    render(<StepDecisions projectId="p1" onNext={vi.fn()} onBack={vi.fn()} />)
    await screen.findByText(/which accessor convention/i)
    fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'NAMING' } })
    fireEvent.change(screen.getByLabelText(/min. confidence/i), { target: { value: '0.7' } })
    fireEvent.change(screen.getByLabelText(/^status$/i), { target: { value: 'SUGGESTED' } })
    fireEvent.click(screen.getByRole('button', { name: /apply filters/i }))
    await waitFor(() => expect(getDecisions).toHaveBeenLastCalledWith('p1', {
      category: 'NAMING', minConfidence: '0.7', status: 'SUGGESTED'
    }))

    fireEvent.click(screen.getByRole('button', { name: /^confirm$/i }))
    await waitFor(() => expect(patchDecision).toHaveBeenCalledWith('p1', 'd1', 'JAVA_BEANS', 3))
    fireEvent.change(screen.getByLabelText(/chosen option/i), { target: { value: 'FLUENT' } })
    fireEvent.click(screen.getByRole('button', { name: /save override/i }))
    await waitFor(() => expect(patchDecision).toHaveBeenLastCalledWith('p1', 'd1', 'FLUENT', 4))

    fireEvent.click(screen.getByRole('button', { name: /confirm eligible/i }))
    await waitFor(() => expect(bulkConfirm).toHaveBeenCalledWith('p1', '0.8'))
    expect(await screen.findByText(/1 confirmed · 0 skipped/i)).toBeTruthy()
  })

  it('refreshes a stale row and surfaces a conflict message', async () => {
    patchDecision.mockRejectedValue(Object.assign(new Error('stale'), { status: 409 }))
    render(<StepDecisions projectId="p1" onNext={vi.fn()} onBack={vi.fn()} />)
    await screen.findByText(/which accessor convention/i)
    fireEvent.click(screen.getByRole('button', { name: /^confirm$/i }))
    expect(await screen.findByText(/changed in another session/i)).toBeTruthy()
    expect(getDecisions).toHaveBeenCalledTimes(2)
  })

  it('renders an empty filtered result as a valid state', async () => {
    getDecisions.mockResolvedValue({ items: [], total: 0 })
    render(<StepDecisions projectId="p1" onNext={vi.fn()} onBack={vi.fn()} />)
    expect(await screen.findByText(/no decisions match/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /next: view metrics/i }).disabled).toBe(false)
  })

  it('shows policy version, confidence, staleness, and keeps override available', async () => {
    getDecisions.mockResolvedValue({ items: [{ ...decision, source: 'POLICY', status: 'CONFIRMED',
      confidence: 1, policyProvenance: { catalogName: 'bank', catalogVersion: '1',
        matchConfidence: 0.98, stale: true } }], total: 1 })
    render(<StepDecisions projectId="p1" onNext={vi.fn()} onBack={vi.fn()} />)

    expect(await screen.findByText('policy')).toBeTruthy()
    expect(screen.getByText(/bank · v1/i)).toBeTruthy()
    expect(screen.getByText('98%')).toBeTruthy()
    expect(screen.getByText(/different analyzer version/i)).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/chosen option/i), { target: { value: 'FLUENT' } })
    expect(screen.getByRole('button', { name: /save override/i })).toBeTruthy()
  })
})
