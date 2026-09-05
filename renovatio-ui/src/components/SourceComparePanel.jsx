import { useState } from 'react'
import { compareSourceEquivalence } from '../api/client'

export default function SourceComparePanel({ projectId }) {
  const [baseline, setBaseline] = useState('')
  const [candidate, setCandidate] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const run = async () => {
    setLoading(true); setError('')
    try { setResult(await compareSourceEquivalence(projectId, { caseId: `compare-${Date.now()}`, baselineSource: baseline, candidateSource: candidate, input: {}, files: {}, db2Responses: {}, ignoredFields: [] })) }
    catch (e) { setError(e?.message || 'No se pudo comparar las fuentes.') }
    finally { setLoading(false) }
  }
  return <section className="architecture-preview" aria-label="Comparación de fuentes COBOL">
    <div className="preview-header"><h3>Comparar baseline y candidate</h3>{result && <span className="preview-tag">{result.equivalent ? 'EQUIVALENTE' : 'DIVERGENCIA'}</span>}</div>
    <textarea className="source-editor" rows="6" value={baseline} onChange={e => setBaseline(e.target.value)} placeholder="Fuente COBOL baseline…" aria-label="Fuente baseline" />
    <textarea className="source-editor" rows="6" value={candidate} onChange={e => setCandidate(e.target.value)} placeholder="Fuente candidate…" aria-label="Fuente candidate" />
    {error && <div className="preview-error" role="alert">{error}</div>}
    {result?.divergences?.length > 0 && <ul className="topology-list">{result.divergences.map(item => <li key={item.field}><strong>{item.field}</strong>: {String(item.baseline)} → {String(item.candidate)}</li>)}</ul>}
    <button className="button button-primary" type="button" onClick={run} disabled={loading || !baseline.trim() || !candidate.trim()}>{loading ? 'Comparando…' : 'Comparar fuentes'}</button>
  </section>
}
