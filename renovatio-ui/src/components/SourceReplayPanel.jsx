import { useState } from 'react'
import { runSourceEquivalenceReplay } from '../api/client'

export default function SourceReplayPanel({ projectId }) {
  const [source, setSource] = useState('')
  const [input, setInput] = useState('{}')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const run = async () => {
    setLoading(true); setError('')
    try { setResult(await runSourceEquivalenceReplay(projectId, { caseId: `source-${Date.now()}`, source, input: JSON.parse(input) })) }
    catch (e) { setError(e?.message || 'No se pudo ejecutar el replay.') }
    finally { setLoading(false) }
  }
  return <section className="architecture-preview" aria-label="Replay desde fuente COBOL">
    <div className="preview-header"><h3>Replay desde fuente COBOL</h3>{result && <span className="preview-tag">{result.status}</span>}</div>
    <textarea className="source-editor" rows="8" value={source} onChange={e => setSource(e.target.value)} placeholder="Pegá aquí el programa COBOL…" aria-label="Fuente COBOL" />
    <input className="source-input" value={input} onChange={e => setInput(e.target.value)} aria-label="Entrada JSON" />
    {error && <div className="preview-error" role="alert">{error}</div>}
    {result && <pre className="source-output">{JSON.stringify(result.output, null, 2)}</pre>}
    <button className="button button-primary" type="button" onClick={run} disabled={loading || !source.trim()}>{loading ? 'Ejecutando…' : 'Ejecutar replay'}</button>
  </section>
}
