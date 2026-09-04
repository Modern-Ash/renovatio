import { useMemo, useState } from 'react'

/** Review gate for the target-neutral business model before architecture projection. */
export default function DomainModelReview({ domainModel, onProject, disabled = false }) {
  const [confirmed, setConfirmed] = useState(false)
  const [projecting, setProjecting] = useState(false)
  const [error, setError] = useState('')
  const nodes = domainModel?.nodes || []
  const relations = domainModel?.relations || []
  const invariants = domainModel?.invariants || []
  const confidence = useMemo(() => nodes.length
    ? Math.round(nodes.reduce((sum, node) => sum + Number(node.confidence || 0), 0) / nodes.length * 100)
    : 0, [nodes])

  if (!domainModel) return <section className="preview-empty">No hay DomainModel para revisar.</section>

  return <section className="architecture-preview" aria-label="Domain model review">
    <div className="preview-header">
      <div><span className="preview-tag">DOMAIN MODEL · v{domainModel.schemaVersion}</span>
        <h3>Modelo de negocio</h3></div>
      <code className="preview-hash">#{String(domainModel.projectId || '').slice(0, 10)}</code>
    </div>
    <p>{nodes.length} nodos · {relations.length} relaciones · {invariants.length} invariantes · confianza media {confidence}%</p>
    <ul className="topology-list">
      {nodes.map(node => <li key={node.id}><strong>{node.name}</strong> <span>{node.kind}</span>
        <small> · {Math.round(Number(node.confidence || 0) * 100)}% · {node.origin}</small></li>)}
    </ul>
    <label className="preview-status"><input type="checkbox" checked={confirmed} disabled={disabled}
      onChange={event => setConfirmed(event.target.checked)} /> Confirmo que este modelo representa el negocio</label>
    {error && <div className="preview-error" role="alert">{error}</div>}
    <button className="button button-primary" type="button" disabled={!confirmed || disabled || projecting}
      onClick={async () => {
        setProjecting(true); setError('')
        try { await onProject?.(domainModel) } catch (reason) { setError(reason?.message || 'No se pudo proyectar la arquitectura.') }
        finally { setProjecting(false) }
      }}>{projecting ? 'Proyectando…' : 'Proyectar arquitectura'}</button>
  </section>
}
