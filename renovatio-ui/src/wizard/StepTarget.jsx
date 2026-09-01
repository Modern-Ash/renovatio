import { useEffect, useMemo, useState } from 'react'
import { getEffectiveProfile, getProjectProfile, putProjectProfile } from '../api/client'

const ARCHITECTURES = [
  { value: 'TRANSACTION_SCRIPT', label: 'Transaction script', layers: ['Program service', 'Generated models'] },
  { value: 'LAYERED_MVC', label: 'Layered MVC', layers: ['Controller', 'Application service', 'Repository'] },
  { value: 'HEXAGONAL', label: 'Hexagonal', layers: ['Inbound ports', 'Domain core', 'Outbound adapters'] }
]

function StepTarget({ projectId, data, onChange, onNext, onBack }) {
  const id = projectId || 'default'
  const [stored, setStored] = useState(null)
  const [etag, setEtag] = useState(null)
  const [form, setForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [violations, setViolations] = useState([])
  const [error, setError] = useState('')

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

  const architecture = useMemo(
    () => ARCHITECTURES.find((item) => item.value === form?.architecture?.style) || ARCHITECTURES[0],
    [form]
  )

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
      <p className="text-gray-600 mb-6">These settings become the versioned input to Analyze. Architecture choices are previews in F1 and do not change emitted Java yet.</p>

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
            <legend>Architecture preview</legend>
            <div className="choice-stack">
              {ARCHITECTURES.map((item) => (
                <label key={item.value} className="choice-card">
                  <input type="radio" name="architecture" value={item.value}
                    checked={form.architecture.style === item.value}
                    onChange={() => update('architecture', 'style', item.value)} />
                  <span>{item.label}</span>
                </label>
              ))}
            </div>
            <p className="preview-note">Preview only until the architecture implementation phase.</p>
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

        <aside className="architecture-preview" aria-label={`${architecture.label} static layout preview`}>
          <span className="preview-tag">STATIC PREVIEW</span>
          <h3>{architecture.label}</h3>
          <div className="architecture-rail">
            {architecture.layers.map((layer, index) => (
              <div key={layer} className="architecture-layer"><span>{String(index + 1).padStart(2, '0')}</span>{layer}</div>
            ))}
          </div>
          <dl>
            <div><dt>Grouping</dt><dd>{form.architecture.moduleGrouping.replaceAll('_', ' ')}</dd></div>
            <div><dt>Framework</dt><dd>{form.runtime.framework.replaceAll('_', ' ')}</dd></div>
            <div><dt>Persistence</dt><dd>{form.persistence.defaultStrategy.replaceAll('_', ' ')}</dd></div>
          </dl>
        </aside>
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
