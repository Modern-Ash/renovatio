import { useState, useEffect } from 'react'
import { getProjects, getActionItems } from '../api/client'
import MetricCard from '../dashboard/MetricCard'
import ActionItems from '../dashboard/ActionItems'
import JobTimeline from '../dashboard/JobTimeline'

function Dashboard() {
  const [projects, setProjects] = useState([])
  const [actionItems, setActionItems] = useState([])
  const [loading, setLoading] = useState(true)

  const fetchData = async () => {
    try {
      const [projectsData] = await Promise.all([
        getProjects()
      ])
      setProjects(projectsData)
      
      if (projectsData.length > 0) {
        const items = await getActionItems(projectsData[0].id)
        setActionItems(items)
      }
    } catch (error) {
      console.error('Failed to fetch data:', error)
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
        <MetricCard title="Lines of Code" value="0" unit="LOC" icon="📝" />
        <MetricCard title="Cyclomatic Complexity" value="0" icon="🔄" />
        <MetricCard title="Action Items" value={actionItems.length} icon="📋" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ActionItems 
          items={actionItems} 
          onStatusChange={fetchData}
        />
        <JobTimeline jobs={[]} />
      </div>
    </div>
  )
}

export default Dashboard
