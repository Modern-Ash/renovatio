import { useState, useEffect } from 'react'
import { getProjects, getActionItems, getJobs } from '../api/client'
import MetricCard from '../dashboard/MetricCard'
import ActionItems from '../dashboard/ActionItems'
import JobTimeline from '../dashboard/JobTimeline'

function Dashboard() {
  const [projects, setProjects] = useState([])
  const [actionItems, setActionItems] = useState([])
  const [jobs, setJobs] = useState([])
  const [latestAnalysis, setLatestAnalysis] = useState(null)
  const [loading, setLoading] = useState(true)

  const analysisCounts = latestAnalysis || {
    sourceFiles: 0,
    copybooks: 0,
    programs: 0,
    message: 'No completed analysis yet.',
    workspacePath: '',
    projectId: ''
  }

  const extractSummary = (job) => {
    const result = job?.result || {}
    const summary = result.summary || result.analysis?.summary || result.data?.summary
    if (!summary) {
      return null
    }
    return {
      message: result.message || job.message || 'Analysis completed',
      sourceFiles: Number(summary.sourceFiles ?? 0),
      copybooks: Number(summary.copybooks ?? 0),
      programs: Number(summary.programs ?? 0),
      workspacePath: result.workspacePath || job.workspacePath || '',
      projectId: job.projectId || ''
    }
  }

  const fetchData = async () => {
    try {
      const [projectsData, jobsData] = await Promise.all([
        getProjects(),
        getJobs()
      ])
      setProjects(projectsData)
      setJobs(jobsData)

      const completedAnalysis = jobsData.find(
        (job) => job.operation === 'analyze' && job.status === 'COMPLETED'
      )
      setLatestAnalysis(completedAnalysis ? extractSummary(completedAnalysis) : null)

      const actionItemProjectId = completedAnalysis?.projectId || projectsData[0]?.id
      if (actionItemProjectId) {
        const items = await getActionItems(actionItemProjectId)
        setActionItems(items)
      } else {
        setActionItems([])
      }
    } catch (error) {
      console.error('Failed to fetch data:', error)
      setActionItems([])
      setJobs([])
      setLatestAnalysis(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  if (loading) {
    return <div className="text-center p-8">Loading...</div>
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <MetricCard title="Total Projects" value={projects.length} icon="📁" />
        <MetricCard title="COBOL Source Files" value={analysisCounts.sourceFiles} icon="📝" />
        <MetricCard title="Copybooks" value={analysisCounts.copybooks} icon="📚" />
        <MetricCard title="Parsed Programs" value={analysisCounts.programs} icon="🔄" />
      </div>

      <div className="card mb-8">
        <h3 className="font-semibold mb-3">Latest Analysis</h3>
        {latestAnalysis ? (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-gray-500">Status</p>
              <p className="font-medium text-green-700">Completed</p>
            </div>
            <div>
              <p className="text-gray-500">Sources</p>
              <p className="font-medium">{latestAnalysis.sourceFiles} COBOL file(s)</p>
            </div>
            <div>
              <p className="text-gray-500">Copybooks</p>
              <p className="font-medium">{latestAnalysis.copybooks}</p>
            </div>
            <div>
              <p className="text-gray-500">Programs</p>
              <p className="font-medium">{latestAnalysis.programs}</p>
            </div>
            {latestAnalysis.projectId && (
              <div className="md:col-span-4">
                <p className="text-gray-500">Project</p>
                <p className="font-medium">
                  {projects.find((project) => project.id === latestAnalysis.projectId)?.name || latestAnalysis.projectId}
                </p>
              </div>
            )}
            <div className="md:col-span-4">
              <p className="text-gray-500">Summary</p>
              <p className="font-medium">{latestAnalysis.message}</p>
            </div>
            {latestAnalysis.workspacePath && (
              <div className="md:col-span-4">
                <p className="text-gray-500">Workspace</p>
                <p className="font-medium break-all">{latestAnalysis.workspacePath}</p>
              </div>
            )}
          </div>
        ) : (
          <p className="text-gray-500">No completed analysis yet.</p>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ActionItems 
          items={actionItems} 
          onStatusChange={fetchData}
        />
        <JobTimeline jobs={jobs} />
      </div>
    </div>
  )
}

export default Dashboard
