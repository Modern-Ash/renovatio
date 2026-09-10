import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getProject, createJob, getActionItems, getProfileTemplateDiff } from '../api/client'
import MetricCard from '../dashboard/MetricCard'
import ActionItems from '../dashboard/ActionItems'

function ProjectDetail() {
  const { id } = useParams()
  const [project, setProject] = useState(null)
  const [actionItems, setActionItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [creatingJob, setCreatingJob] = useState(false)
  const [profileDiff, setProfileDiff] = useState([])

  const fetchData = async () => {
    try {
      const [projectData, itemsData, diffData] = await Promise.all([
        getProject(id),
        getActionItems(id),
        getProfileTemplateDiff(id)
      ])
      setProject(projectData)
      setActionItems(itemsData)
      setProfileDiff(diffData || [])
    } catch (error) {
      console.error('Failed to fetch project:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [id])

  const handleAnalyze = async () => {
    setCreatingJob(true)
    try {
      await createJob(id, 'analyze')
      alert('Analysis job created!')
    } catch (error) {
      console.error('Failed to create job:', error)
      alert('Failed to create job')
    } finally {
      setCreatingJob(false)
    }
  }

  if (loading) {
    return <div className="text-center p-8">Loading...</div>
  }

  if (!project) {
    return <div className="text-center p-8">Project not found</div>
  }

  return (
    <div>
      <div className="mb-6">
        <Link to="/projects" className="text-primary-600 hover:underline">
          ← Back to Projects
        </Link>
      </div>

      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">{project.name}</h1>
          <p className="text-gray-500">{project.workspacePath}</p>
          {project.javaOutputPath && (
            <p className="text-gray-500 text-sm">Java output: {project.javaOutputPath}</p>
          )}
          {project.javaPackage && (
            <p className="text-gray-500 text-sm">Base package: {project.javaPackage}</p>
          )}
          {project.javaArchitecture && (
            <p className="text-gray-500 text-sm">Architecture: {project.javaArchitecture}</p>
          )}
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleAnalyze}
            disabled={creatingJob}
            className="btn btn-primary"
          >
            {creatingJob ? 'Creating...' : 'Analyze'}
          </button>
          <Link to={`/wizard/${id}`} className="btn btn-secondary">
            Start Wizard
          </Link>
        </div>
      </div>

      <section className="binding-overview" aria-labelledby="binding-heading">
        <div><p className="wizard-kicker">Inheritance map</p><h2 id="binding-heading">Reusable configuration</h2></div>
        <div className="binding-cards">
          <article><span>Profile template</span><strong>{project.profileTemplate ? `${project.profileTemplate.name}@${project.profileTemplate.version}` : 'Project defaults'}</strong><small>{profileDiff.length} local deviation{profileDiff.length === 1 ? '' : 's'}</small></article>
          <article><span>Policy catalog</span><strong>{project.policyCatalog ? `${project.policyCatalog.name}@${project.policyCatalog.version}` : 'No catalog bound'}</strong><small>{project.policyCatalog ? 'Exact version pinned' : 'Decisions require review'}</small></article>
        </div>
        {profileDiff.length > 0 && <details className="profile-diff"><summary>Inspect profile deviations</summary><ul>{profileDiff.map((item) => <li key={item.path}><code>{item.path}</code><span>{item.changeKind.toLowerCase()}</span></li>)}</ul></details>}
      </section>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <MetricCard title="Lines of Code" value="0" unit="LOC" icon="📝" />
        <MetricCard title="Cyclomatic Complexity" value="0" icon="🔄" />
        <MetricCard title="Copybooks" value="0" icon="📚" />
        <MetricCard title="Action Items" value={actionItems.length} icon="📋" />
      </div>

      <ActionItems items={actionItems} onStatusChange={fetchData} />
    </div>
  )
}

export default ProjectDetail
