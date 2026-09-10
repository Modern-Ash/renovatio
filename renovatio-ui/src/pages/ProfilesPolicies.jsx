import { useEffect, useMemo, useState } from 'react'
import {
  bindPolicyCatalog,
  bindProfileTemplate,
  exportPolicyCatalog,
  getPolicyCatalogs,
  getProfileTemplates,
  getProjects,
  saveProfileTemplate
} from '../api/client'

const EMPTY_FORM = { projectId: '', name: '', version: '1.0.0' }

function AssetRail({ title, eyebrow, items, empty, accent }) {
  return <section className={`asset-rail asset-rail-${accent}`} aria-labelledby={`${accent}-heading`}>
    <header><div><p>{eyebrow}</p><h2 id={`${accent}-heading`}>{title}</h2></div><span>{items.length} versions</span></header>
    {items.length === 0 ? <div className="asset-empty">{empty}</div> : <div className="asset-stack">
      {items.map((item) => <article key={`${item.name}@${item.version}`} className="asset-card">
        <div className="asset-version"><strong>{item.name}</strong><code>v{item.version}</code></div>
        <p>{item.description || (item.entries !== undefined ? `${item.entries} reusable decisions` : 'Migration profile template')}</p>
        <div className="asset-meta"><span title={item.contentHash}>#{item.contentHash.slice(0, 10)}</span><span>{item.projects.length} linked project{item.projects.length === 1 ? '' : 's'}</span></div>
        {item.projects.length > 0 && <ul aria-label="Linked projects">{item.projects.map((project) => <li key={project}>{project.slice(0, 8)}</li>)}</ul>}
      </article>)}
    </div>}
  </section>
}

function ProfilesPolicies() {
  const [templates, setTemplates] = useState([])
  const [policies, setPolicies] = useState([])
  const [projects, setProjects] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [mode, setMode] = useState('template')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')

  const refresh = async () => {
    setLoading(true)
    try {
      const [nextTemplates, nextPolicies, nextProjects] = await Promise.all([
        getProfileTemplates(), getPolicyCatalogs(), getProjects()
      ])
      setTemplates(nextTemplates || [])
      setPolicies(nextPolicies || [])
      setProjects(nextProjects || [])
      setForm((current) => ({ ...current, projectId: current.projectId || nextProjects?.[0]?.id || '' }))
    } catch (reason) {
      setError(reason.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { refresh() }, [])

  const selectedProject = useMemo(() => projects.find((project) => project.id === form.projectId), [projects, form.projectId])

  const createVersion = async (event) => {
    event.preventDefault()
    setBusy(true); setError(''); setNotice('')
    try {
      const payload = { ...form }
      if (mode === 'template') await saveProfileTemplate(payload)
      else await exportPolicyCatalog(payload)
      setNotice(`${mode === 'template' ? 'Profile template' : 'Policy catalog'} ${form.name}@${form.version} created from ${selectedProject?.name}.`)
      setForm((current) => ({ ...current, name: '' }))
      await refresh()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setBusy(false)
    }
  }

  const applySelected = async (kind, item) => {
    if (!form.projectId) return
    setBusy(true); setError(''); setNotice('')
    try {
      const reference = { name: item.name, version: item.version }
      if (kind === 'template') await bindProfileTemplate(form.projectId, reference)
      else {
        const report = await bindPolicyCatalog(form.projectId, reference)
        setNotice(`${report.autoConfirmed} auto-confirmed · ${report.suggested} suggested · ${report.unmatched} unmatched`)
      }
      if (kind === 'template') setNotice(`Bound ${item.name}@${item.version} to ${selectedProject?.name}.`)
      await refresh()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setBusy(false)
    }
  }

  return <main className="assets-page">
    <header className="assets-hero">
      <div><p className="wizard-kicker">Reusable intelligence</p><h1>Profiles &amp; policies</h1>
        <p>Turn one reviewed migration into a controlled starting point for the next—without losing version provenance or local choice.</p></div>
      <div className="assets-stat"><strong>{templates.length + policies.length}</strong><span>immutable versions</span></div>
    </header>

    <div aria-live="polite">{notice && <p className="decision-alert decision-alert-success">{notice}</p>}{error && <p className="decision-alert decision-alert-error" role="alert">{error}</p>}</div>

    <section className="asset-workbench" aria-labelledby="asset-workbench-heading">
      <div><p className="wizard-kicker">Version workshop</p><h2 id="asset-workbench-heading">Create from a reviewed project</h2></div>
      <form onSubmit={createVersion}>
        <label><span>Asset type</span><select value={mode} onChange={(event) => setMode(event.target.value)}><option value="template">Profile template</option><option value="policy">Policy catalog</option></select></label>
        <label><span>Source project</span><select value={form.projectId} onChange={(event) => setForm({ ...form, projectId: event.target.value })} required><option value="">Select project</option>{projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></label>
        <label><span>Name</span><input value={form.name} pattern="[A-Za-z0-9][A-Za-z0-9._-]{0,63}" onChange={(event) => setForm({ ...form, name: event.target.value })} required /></label>
        <label><span>Version</span><input value={form.version} pattern="[A-Za-z0-9][A-Za-z0-9._-]{0,63}" onChange={(event) => setForm({ ...form, version: event.target.value })} required /></label>
        <button className="btn btn-primary" disabled={busy || projects.length === 0}>{busy ? 'Working…' : 'Create immutable version'}</button>
      </form>
    </section>

    {loading ? <p className="asset-loading" role="status">Loading reusable assets…</p> : <div className="assets-grid">
      <div><AssetRail title="Profile templates" eyebrow="Configuration DNA" items={templates} empty="No templates yet. Save a reviewed project profile to create the first version." accent="profile" />
        <div className="asset-actions">{templates.map((item) => <button key={`${item.name}@${item.version}`} disabled={busy || !form.projectId} onClick={() => applySelected('template', item)}>Bind {item.name}@{item.version}</button>)}</div></div>
      <div><AssetRail title="Policy catalogs" eyebrow="Decision memory" items={policies} empty="No policy catalogs yet. Export confirmed decisions from a reviewed project." accent="policy" />
        <div className="asset-actions">{policies.map((item) => <button key={`${item.name}@${item.version}`} disabled={busy || !form.projectId} onClick={() => applySelected('policy', item)}>Apply {item.name}@{item.version}</button>)}</div></div>
    </div>}
  </main>
}

export default ProfilesPolicies
