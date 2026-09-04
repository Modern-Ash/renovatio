import { useEffect, useState } from 'react'
import { bulkConfirmProjectDecisions, getProjectDecisions, patchProjectDecision } from '../api/client'

const CATEGORIES = ['', 'NUMERIC', 'CONTROL_FLOW', 'DATA_SHAPE', 'PERSISTENCE', 'NAMING', 'ARCHITECTURE', 'BATCH']
const STATUSES = ['', 'AUTO', 'SUGGESTED', 'CONFIRMED', 'OVERRIDDEN']

function sourceLabel(source) {
  if (source === 'LLM') return 'IA'
  if (source === 'USER') return 'user'
  if (source === 'POLICY') return 'policy'
  return 'heuristic'
}

function StepDecisions({ projectId, onNext, onBack }) {
  const id = projectId || 'default'
  const [filters, setFilters] = useState({ category: '', minConfidence: '', status: '' })
  const [items, setItems] = useState([])
  const [selected, setSelected] = useState({})
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [bulkThreshold, setBulkThreshold] = useState('0.8')

  const load = async (nextFilters = filters) => {
    setLoading(true)
    setError('')
    try {
      const result = await getProjectDecisions(id, nextFilters)
      setItems(result.items || [])
      setSelected(Object.fromEntries((result.items || []).map((item) => [item.id, item.chosenOption])))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load({ category: '', minConfidence: '', status: '' }) }, [id])

  const confirm = async (decision, option) => {
    setBusyId(decision.id)
    setError('')
    setNotice('')
    try {
      const updated = await patchProjectDecision(id, decision.id, option, decision.revision)
      setItems((current) => current.map((item) => item.id === updated.id ? updated : item))
      setSelected((current) => ({ ...current, [updated.id]: updated.chosenOption }))
      setNotice(updated.status === 'OVERRIDDEN' ? 'Alternative saved as a user override.' : 'Decision confirmed.')
    } catch (reason) {
      if (reason.status === 409) {
        await load()
        setError('This decision changed in another session. The row was refreshed; review it before retrying.')
      } else {
        setError(reason.message)
      }
    } finally {
      setBusyId('')
    }
  }

  const bulkConfirm = async () => {
    setBusyId('bulk')
    setError('')
    setNotice('')
    try {
      const result = await bulkConfirmProjectDecisions(id, bulkThreshold)
      setNotice(`${result.confirmed} confirmed · ${result.skipped} skipped`)
      await load()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setBusyId('')
    }
  }

  const applyFilters = (event) => {
    event.preventDefault()
    load(filters)
  }

  return (
    <section aria-labelledby="decisions-heading">
      <div className="wizard-section-heading">
        <div><p className="wizard-kicker">Decision register</p><h2 id="decisions-heading" className="text-xl font-semibold">Review generated decisions</h2></div>
        <span className="decision-count">{items.length} visible</span>
      </div>
      <p className="text-gray-600 mb-5">Confirm a proposed default or choose one of its admitted alternatives. Unconfirmed items keep the compatibility default and never block the migration.</p>

      <form className="decision-filters" onSubmit={applyFilters} aria-label="Decision filters">
        <label><span>Category</span><select value={filters.category} onChange={(event) => setFilters({ ...filters, category: event.target.value })}>
          {CATEGORIES.map((value) => <option key={value || 'all'} value={value}>{value ? value.replaceAll('_', ' ') : 'All categories'}</option>)}
        </select></label>
        <label><span>Min. confidence</span><input type="number" min="0" max="1" step="0.1" placeholder="Any"
          value={filters.minConfidence} onChange={(event) => setFilters({ ...filters, minConfidence: event.target.value })} /></label>
        <label><span>Status</span><select value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
          {STATUSES.map((value) => <option key={value || 'all'} value={value}>{value || 'All statuses'}</option>)}
        </select></label>
        <button className="btn btn-secondary" type="submit">Apply filters</button>
      </form>

      <div className="bulk-confirm-bar">
        <label htmlFor="bulk-threshold">Bulk threshold</label>
        <input id="bulk-threshold" type="number" min="0" max="1" step="0.1" value={bulkThreshold}
          onChange={(event) => setBulkThreshold(event.target.value)} />
        <button type="button" className="btn btn-secondary" disabled={busyId === 'bulk'} onClick={bulkConfirm}>
          {busyId === 'bulk' ? 'Confirming…' : 'Confirm eligible'}
        </button>
      </div>

      <div aria-live="polite">
        {notice && <div className="decision-alert decision-alert-success">{notice}</div>}
        {error && <div className="decision-alert decision-alert-error" role="alert">{error}</div>}
      </div>

      {loading ? <p className="decision-empty" role="status">Loading decisions…</p> : items.length === 0 ? (
        <div className="decision-empty"><strong>No decisions match this view.</strong><span>Try clearing the filters or continue with the current defaults.</span></div>
      ) : (
        <div className="decision-list">
          {items.map((decision) => {
            const choice = selected[decision.id] ?? decision.chosenOption
            const changed = choice !== decision.chosenOption
            return <article key={decision.id} className="decision-card">
              <header>
                <div className="decision-labels">
                  <span className="decision-category">{decision.category.replaceAll('_', ' ')}</span>
                  <span className={`source-badge source-${decision.source.toLowerCase()}`}>{sourceLabel(decision.source)}</span>
                  <span className={`status-badge status-${decision.status.toLowerCase()}`}>{decision.status}</span>
                  {decision.llmFailed && <span className="fallback-badge">IA fallback · {decision.llmFailureCategory}</span>}
                </div>
                <span className="confidence-meter" title={`Confidence ${decision.confidence}`}><i style={{ width: `${Number(decision.confidence) * 100}%` }} />{Math.round(Number(decision.confidence) * 100)}%</span>
              </header>
              <h3>{decision.question}</h3>
              {decision.policyProvenance && <div className={`policy-provenance ${decision.policyProvenance.stale ? 'policy-provenance-stale' : ''}`}>
                <div><span>Inherited policy</span><strong>{decision.policyProvenance.catalogName} · v{decision.policyProvenance.catalogVersion}</strong></div>
                <div><span>Semantic match</span><strong>{Math.round(Number(decision.policyProvenance.matchConfidence) * 100)}%</strong></div>
                {decision.policyProvenance.stale && <p role="status">Review required: this policy was produced by a different analyzer version.</p>}
              </div>}
              <div className="decision-choice-row">
                <label><span>Chosen option</span><select value={choice}
                  onChange={(event) => setSelected({ ...selected, [decision.id]: event.target.value })}>
                  {decision.options.map((option) => <option key={option} value={option}>{option.replaceAll('_', ' ')}</option>)}
                </select></label>
                <p>Default <code>{decision.defaultOption}</code></p>
                <button type="button" className="btn btn-primary" disabled={busyId === decision.id}
                  onClick={() => confirm(decision, choice)}>
                  {busyId === decision.id ? 'Saving…' : changed ? 'Save override' : 'Confirm'}
                </button>
              </div>
              <details><summary>Rationale &amp; evidence</summary><p>{decision.rationale}</p>
                {decision.evidence.length > 0 && <ul>{decision.evidence.map((evidence) => <li key={evidence}>{evidence}</li>)}</ul>}
              </details>
            </article>
          })}
        </div>
      )}

      <div className="flex justify-between mt-6">
        <button onClick={onBack} className="btn btn-secondary">← Back</button>
        <button onClick={onNext} className="btn btn-primary">Next: View Metrics →</button>
      </div>
    </section>
  )
}

export default StepDecisions
