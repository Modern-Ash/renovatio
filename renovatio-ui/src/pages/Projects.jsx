import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getProjects, createProject } from '../api/client'

function Projects() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [newProject, setNewProject] = useState({ name: '', workspacePath: '', branch: 'main' })

  const fetchProjects = async () => {
    try {
      const data = await getProjects()
      setProjects(data)
    } catch (error) {
      console.error('Failed to fetch projects:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProjects()
  }, [])

  const handleCreate = async (e) => {
    e.preventDefault()
    try {
      await createProject(newProject)
      setShowCreate(false)
      setNewProject({ name: '', workspacePath: '', branch: 'main' })
      fetchProjects()
    } catch (error) {
      console.error('Failed to create project:', error)
    }
  }

  if (loading) {
    return <div className="text-center p-8">Loading...</div>
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Projects</h1>
        <button onClick={() => setShowCreate(true)} className="btn btn-primary">
          New Project
        </button>
      </div>

      {showCreate && (
        <div className="card mb-6">
          <h2 className="text-lg font-semibold mb-4">Create Project</h2>
          <form onSubmit={handleCreate}>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <input
                type="text"
                placeholder="Project Name"
                value={newProject.name}
                onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
                className="input"
                required
              />
              <input
                type="text"
                placeholder="Workspace Path"
                value={newProject.workspacePath}
                onChange={(e) => setNewProject({ ...newProject, workspacePath: e.target.value })}
                className="input"
                required
              />
              <input
                type="text"
                placeholder="Branch (optional)"
                value={newProject.branch}
                onChange={(e) => setNewProject({ ...newProject, branch: e.target.value })}
                className="input"
              />
            </div>
            <div className="mt-4 flex gap-2">
              <button type="submit" className="btn btn-primary">
                Create
              </button>
              <button type="button" onClick={() => setShowCreate(false)} className="btn btn-secondary">
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {projects.length === 0 ? (
        <div className="card text-center py-8">
          <p className="text-gray-500">No projects yet. Create one to get started.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {projects.map(project => (
            <Link
              key={project.id}
              to={`/projects/${project.id}`}
              className="card hover:shadow-lg transition-shadow"
            >
              <h3 className="font-semibold">{project.name}</h3>
              <p className="text-sm text-gray-500 mt-1">{project.workspacePath}</p>
              {project.branch && (
                <p className="text-xs text-gray-400 mt-1">Branch: {project.branch}</p>
              )}
              <p className="text-xs text-gray-400 mt-2">
                Created: {new Date(project.createdAt).toLocaleDateString()}
              </p>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

export default Projects
