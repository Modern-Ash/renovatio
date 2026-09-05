import { useState } from 'react'
import { compareEquivalenceFixture } from '../api/client'

export default function ReplayComparisonPanel({ projectId, fixture }) {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const run = async () => {
    setLoading(true); setError('')
    try { setResult(await compareEquivalenceFixture(projectId, fixture)) }
    catch (reason) { setError(reason?.message || 'No se pudo comparar el replay.') }
    finally { setLoading(false) }
  }
  return <section className="architecture-preview" aria-label="Replay comparison">
    <div className="preview-header"><h3>Replay / shadow</h3>
      {result && <span className="preview-tag">{result.equivalent ? 'EQUIVALENTE' : 'DIVERGENCIA'}</span>}</div>
    {error && <div className="preview-error" role="alert">{error}</div>}
    {result && <p>{result.divergences?.length || 0} divergencias observables</p>}
    {result?.divergences?.length > 0 && <ul className="topology-list">
      {result.divergences.map(item => <li key={item.field}><strong>{item.field}</strong>: {String(item.baseline)} → {String(item.candidate)}</li>)}
    </ul>}
    <button className="button button-primary" type="button" onClick={run} disabled={loading || !fixture}>
      {loading ? 'Comparando…' : 'Ejecutar comparación'}
    </button>
  </section>
}
