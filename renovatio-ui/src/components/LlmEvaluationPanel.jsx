import { useState } from 'react'
import { evaluateLlmDataset } from '../api/client'

export default function LlmEvaluationPanel({ projectId, summary }) {
  const [result, setResult] = useState(null); const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  const run = async () => { setLoading(true); setError(''); try { setResult(await evaluateLlmDataset(projectId, summary)) } catch (e) { setError(e?.message || 'No se pudo evaluar el dataset.') } finally { setLoading(false) } }
  return <section className="architecture-preview" aria-label="LLM evaluation">
    <div className="preview-header"><h3>Evaluación LLM</h3>{result && <span className="preview-tag">{result.passes ? 'APROBADO' : 'REVISAR'}</span>}</div>
    {result && <p>Tasa de aceptación: {Math.round(result.evaluation.acceptanceRate * 100)}% · schema: {result.evaluation.schemaFailures} · provenance: {result.evaluation.provenanceFailures}</p>}
    {error && <div className="preview-error" role="alert">{error}</div>}
    <button className="button button-primary" type="button" disabled={loading || !summary} onClick={run}>{loading ? 'Evaluando…' : 'Evaluar dataset'}</button>
  </section>
}
