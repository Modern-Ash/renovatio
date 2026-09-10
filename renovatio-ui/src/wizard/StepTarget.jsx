import { useEffect, useMemo, useState } from 'react'
import {
  getArchitecturePreview,
  getEffectiveProfile,
  getProjectProfile,
  putProjectProfile
} from '../api/client'

const ARCHITECTURES = [
  { value: 'TRANSACTION_SCRIPT', label: 'Transaction script', active: true },
  { value: 'HEXAGONAL', label: 'Hexagonal', active: true },
  { value: 'LAYERED_MVC', label: 'Layered MVC · later', active: false }
]

const GROUPINGS = [
  { value: 'BY_PROGRAM', label: 'One module per program' },
  { value: 'BY_DOMAIN', label: 'Group by domain evidence' },
  { value: 'SINGLE_MODULE', label: 'Single project module' }
]

function ArchitectureDiagram({ preview }) {
  const layout = useMemo(() => {
    const columns = 2
    const width = 640
    const cellWidth = width / columns
    const rowHeight = 86
    const positions = new Map()
    preview.components.forEach((component, index) => {
      positions.set(component.id, {
        x: 18 + (index % columns) * cellWidth,
        y: 18 + Math.floor(index / columns) * rowHeight
      })
    })
    return { width, height: Math.max(122, 36 + Math.ceil(preview.components.length / columns) * rowHeight), positions }
  }, [preview])

  return (
    <svg className="architecture-diagram" viewBox={`0 0 ${layout.width} ${layout.height}`}
      role="img" aria-labelledby="architecture-diagram-title architecture-diagram-description">
      <title id="architecture-diagram-title">Architecture component diagram</title>
      <desc id="architecture-diagram-description">
        {preview.components.length} components and {preview.relations.length} relations from the canonical preview graph.
      </desc>
      {preview.relations.map((relation) => {
        const from = layout.positions.get(relation.fromComponentId)
        const to = layout.positions.get(relation.toComponentId)
        if (!from || !to) return null
        return <line key={relation.id} className="diagram-relation"
          x1={from.x + 142} y1={from.y + 25} x2={to.x + 142} y2={to.y + 25} />
      })}
      {preview.components.map((component) => {
        const position = layout.positions.get(component.id)
        return <g key={component.id} transform={`translate(${position.x} ${position.y})`}>
          <rect className="diagram-node" width="284" height="52" rx="7" />
          <text className="diagram-kind" x="12" y="17">{component.kind}</text>
          <text className="diagram-name" x="12" y="37">{component.name.slice(0, 34)}</text>
        </g>
      })}
    </svg>
  )
}

function ArchitecturePreview({ preview, loading, error, stale }) {
  return (
    <aside className="architecture-preview" aria-label="Generated architecture preview" aria-busy={loading}>
      <div className="preview-header">
        <div>
          <span className="preview-tag">CANONICAL MANIFEST</span>
          <h3>Target topology</h3>
        </div>
        {preview && <code className="preview-hash" title={preview.requestHash}>#{preview.requestHash.slice(0, 10)}</code>}
      </div>

      <div className="preview-status" aria-live="polite">
        {loading && <span><i className="status-pulse" />Recalculating from source…</span>}
        {!loading && stale && <span>Draft changed; refreshing preview…</span>}
      </div>

      {error && <div className="preview-error" role="alert">
        <strong>Preview unavailable</strong>
        <span>{error}</span>
      </div>}

      {!error && !preview && !loading && <p className="preview-empty">No architecture data is available.</p>}

      {preview && <div className={stale ? 'preview-content preview-content-stale' : 'preview-content'}>
        {preview.hasFallback && <div className="fallback-banner" role="status">
          One or more programs use a safe transaction-script fallback.
        </div>}

        <section className="preview-section" aria-labelledby="artifact-tree-heading">
          <div className="preview-section-title">
            <h4 id="artifact-tree-heading">Artifact tree</h4>
            <span>{preview.artifacts.length} files</span>
          </div>
          <div className="artifact-tree">
            {preview.modules.map((module) => <section key={module.id} className="artifact-module">
              <h5><span aria-hidden="true">◇</span>{module.name}</h5>
              {module.programIds.map((programId) => {
                const artifacts = preview.artifacts.filter((artifact) => artifact.moduleId === module.id && artifact.programId === programId)
                return <div key={programId} className="artifact-program">
                  <strong>{programId}</strong>
                  {artifacts.length > 0
                    ? <ul>{artifacts.map((artifact) => <li key={artifact.id}><code>{artifact.path}</code></li>)}</ul>
                    : <p>No planned artifacts.</p>}
                </div>
              })}
            </section>)}
          </div>
        </section>

        <section className="preview-section" aria-labelledby="diagram-heading">
          <div className="preview-section-title">
            <h4 id="diagram-heading">Architecture map</h4>
            <span>{preview.components.length} nodes · {preview.relations.length} edges</span>
          </div>
          {preview.components.length > 0
            ? <ArchitectureDiagram preview={preview} />
            : <p className="preview-empty">No components were projected.</p>}
        </section>

        <div className="topology-lists">
          <section aria-labelledby="component-list-heading">
            <h4 id="component-list-heading">Components</h4>
            <ul className="topology-list">
              {preview.components.map((component) => <li key={component.id}>
                <span>{component.kind}</span><strong>{component.name}</strong>
              </li>)}
            </ul>
          </section>
          <section aria-labelledby="relation-list-heading">
            <h4 id="relation-list-heading">Relations</h4>
            {preview.relations.length > 0
              ? <ul className="topology-list">{preview.relations.map((relation) => <li key={relation.id}>
                <span>{relation.kind}</span><code>{relation.fromComponentId.slice(0, 6)} → {relation.toComponentId.slice(0, 6)}</code>
              </li>)}</ul>
              : <p className="preview-empty">No proven relations.</p>}
          </section>
        </div>

        {preview.diagnostics.length > 0 && <section className="preview-diagnostics" aria-labelledby="diagnostics-heading">
          <h4 id="diagnostics-heading">Diagnostics</h4>
          <ul>{preview.diagnostics.map((diagnostic) => <li key={`${diagnostic.code}-${diagnostic.programId}`}>
            <code>{diagnostic.code}</code> · {diagnostic.programId}: {diagnostic.message}
          </li>)}</ul>
        </section>}
      </div>}
    </aside>
  )
}

function StepTarget({ projectId, data, onChange, onNext, onBack }) {
  const id = projectId || 'default'
  const [stored, setStored] = useState(null)
  const [etag, setEtag] = useState(null)
  const [form, setForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [violations, setViolations] = useState([])
  const [error, setError] = useState('')
  const [preview, setPreview] = useState(null)
  const [previewSelection, setPreviewSelection] = useState('')
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([getProjectProfile(id), getEffectiveProfile(id)])
      .then(([profileResult, effective]) => {
        if (!active) return
        setStored(profileResult.profile)
        setEtag(profileResult.etag)
        setForm(effective.profile)
        onChange({ effectiveProfile: effective.profile, profileHash: effective.profileHash })
      })
      .catch((reason) => active && setError(reason.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [id])

  const selectionKey = form
    ? `${form.architecture.style}:${form.architecture.moduleGrouping}`
    : ''

  useEffect(() => {
    if (!form) return undefined
    let active = true
    const controller = new AbortController()
    setPreviewLoading(true)
    setPreviewError('')
    const timer = window.setTimeout(() => {
      getArchitecturePreview(id, {
        style: form.architecture.style,
        moduleGrouping: form.architecture.moduleGrouping,
        signal: controller.signal
      }).then((result) => {
        if (!active) return
        setPreview(result)
        setPreviewSelection(selectionKey)
      }).catch((reason) => {
        if (active && reason?.name !== 'AbortError') setPreviewError(reason.message)
      }).finally(() => {
        if (active) setPreviewLoading(false)
      })
    }, 120)
    return () => {
      active = false
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [id, selectionKey])

  const update = (section, key, value) => {
    setForm((current) => ({
      ...current,
      [section]: { ...current[section], [key]: value }
    }))
    setViolations([])
    setError('')
  }

  const toggleSuggestions = (enabled) => {
    setForm((current) => ({
      ...current,
      llm: { enabled, suggestDecisions: enabled, maxSuggestionsPerRun: enabled ? 10 : 0 }
    }))
  }

  const saveAndContinue = async () => {
    setSaving(true)
    setViolations([])
    setError('')
    const overlay = {
      ...stored,
      schemaVersion: '1',
      extensions: stored?.extensions || {},
      target: form.target,
      architecture: form.architecture,
      runtime: form.runtime,
      persistence: form.persistence,
      llm: form.llm
    }
    try {
      const saved = await putProjectProfile(id, overlay, etag)
      const effective = await getEffectiveProfile(id)
      setStored(saved.profile)
      setEtag(saved.etag)
      onChange({
        migrationProfile: saved.profile,
        profileEtag: saved.etag,
        effectiveProfile: effective.profile,
        profileHash: effective.profileHash
      })
      onNext()
    } catch (reason) {
      if (reason.status === 422 && Array.isArray(reason.payload?.violations)) {
        setViolations(reason.payload.violations)
      } else {
        setError(reason.status === 409 ? 'The profile changed in another session. Go back and reload before saving.' : reason.message)
      }
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p className="text-sm text-gray-600" role="status">Loading migration profile…</p>
  if (!form) return <div role="alert" className="decision-alert decision-alert-error">{error || 'Profile unavailable.'}</div>

  return (
    <section aria-labelledby="target-heading">
      <div className="wizard-section-heading">
        <div>
          <p className="wizard-kicker">Migration contract</p>
          <h2 id="target-heading" className="text-xl font-semibold">Choose the target shape</h2>
        </div>
        {data.profileHash && <code className="profile-hash" title={data.profileHash}>#{data.profileHash.slice(0, 10)}</code>}
      </div>
      <p className="text-gray-600 mb-6">Preview and Apply share this canonical manifest. Draft choices recalculate the topology without writing generated files.</p>

      <div className="target-grid">
        <div className="target-controls">
          <fieldset className="control-panel">
            <legend>Target language</legend>
            <div className="choice-row">
              {['JAVA', 'NODE', 'PYTHON'].map((language) => (
                <label key={language} className={`choice-chip ${language !== 'JAVA' ? 'choice-chip-disabled' : ''}`}>
                  <input type="radio" name="language" value={language}
                    checked={form.target.language === language} disabled={language !== 'JAVA'}
                    onChange={() => update('target', 'language', language)} />
                  {language === 'JAVA' ? 'Java' : `${language[0]}${language.slice(1).toLowerCase()} · later`}
                </label>
              ))}
            </div>
            <label className="field-label" htmlFor="language-version">Language version</label>
            <input id="language-version" className="input" value={form.target.languageVersion}
              onChange={(event) => update('target', 'languageVersion', event.target.value)} />
          </fieldset>

          <fieldset className="control-panel">
            <legend>Architecture</legend>
            <div className="choice-stack">
              {ARCHITECTURES.map((item) => (
                <label key={item.value} className={`choice-card ${!item.active ? 'choice-chip-disabled' : ''}`}>
                  <input type="radio" name="architecture" value={item.value}
                    checked={form.architecture.style === item.value} disabled={!item.active}
                    onChange={() => update('architecture', 'style', item.value)} />
                  <span>{item.label}</span>
                </label>
              ))}
            </div>
            <label className="field-label mt-4" htmlFor="module-grouping">Module grouping</label>
            <select id="module-grouping" className="input" value={form.architecture.moduleGrouping}
              onChange={(event) => update('architecture', 'moduleGrouping', event.target.value)}>
              {GROUPINGS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
            <p className="preview-note">Only source-proven components and relations appear in the topology.</p>
          </fieldset>

          <div className="control-panel compact-control-grid">
            <label><span className="field-label">Framework</span>
              <select className="input" value={form.runtime.framework}
                onChange={(event) => update('runtime', 'framework', event.target.value)}>
                <option value="SPRING_BOOT">Spring Boot</option><option value="NONE">Plain Java</option>
              </select>
            </label>
            <label><span className="field-label">Persistence</span>
              <select className="input" value={form.persistence.defaultStrategy}
                onChange={(event) => update('persistence', 'defaultStrategy', event.target.value)}>
                <option value="IN_MEMORY">In memory</option><option value="JPA">JPA</option>
                <option value="SPRING_DATA_JDBC">Spring Data JDBC</option>
              </select>
            </label>
          </div>

          <label className="ai-toggle">
            <input type="checkbox" checked={form.llm.suggestDecisions}
              onChange={(event) => toggleSuggestions(event.target.checked)} />
            <span><strong>IA suggestions</strong><small>Use governed suggestions only for eligible low-confidence decisions.</small></span>
          </label>
        </div>

        <ArchitecturePreview preview={preview} loading={previewLoading} error={previewError}
          stale={Boolean(preview && previewSelection !== selectionKey)} />
      </div>

      {violations.length > 0 && <div className="decision-alert decision-alert-error" role="alert">
        <strong>Fix the profile before continuing.</strong>
        <ul>{violations.map((item) => <li key={`${item.path}-${item.code}`}><code>{item.path}</code> — {item.message}</li>)}</ul>
      </div>}
      {error && <div className="decision-alert decision-alert-error" role="alert">{error}</div>}
      <div className="flex justify-between mt-6">
        <button onClick={onBack} className="btn btn-secondary">← Back</button>
        <button onClick={saveAndContinue} disabled={saving} className="btn btn-primary">
          {saving ? 'Saving profile…' : 'Save & Analyze →'}
        </button>
      </div>
    </section>
  )
}

export default StepTarget
