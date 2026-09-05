import { useEffect, useState } from 'react'
import { getLlmEvaluationHistory } from '../api/client'

export default function LlmEvaluationHistory({ projectId }) {
  const [items, setItems] = useState([]); const [error, setError] = useState('')
  useEffect(() => { let active = true; getLlmEvaluationHistory(projectId).then(value => active && setItems(value || [])).catch(e => active && setError(e?.message || 'No se pudo cargar el historial.')); return () => { active = false } }, [projectId])
  return <section className="architecture-preview" aria-label="LLM evaluation history"><h3>Historial de evaluación LLM</h3>{error && <div className="preview-error" role="alert">{error}</div>}{items.length === 0 && !error && <p className="preview-empty">Sin evaluaciones registradas.</p>}{items.length > 0 && <ul className="topology-list">{items.map(item => <li key={`${item.datasetId}-${item.createdAt}`}><strong>{item.datasetId}</strong> · {Math.round(item.acceptanceRate * 100)}% · {item.passes ? 'APROBADO' : 'REVISAR'}</li>)}</ul>}</section>
}
