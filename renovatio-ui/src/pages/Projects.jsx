import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProjects, createProject, getProfileTemplates, getPolicyCatalogs } from '../api/client'

function Projects() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [newProject, setNewProject] = useState({
    name: '',
    workspacePath: '',
    javaOutputPath: 'generated-java-stubs',
    javaPackage: '',
    javaArchitecture: 'flat',
    profileTemplate: null,
    policyCatalog: null
  })
  const [templates, setTemplates] = useState([])
  const [policies, setPolicies] = useState([])
  const [folderMessage, setFolderMessage] = useState('')
  const [createError, setCreateError] = useState('')
  const [createInfo, setCreateInfo] = useState('')
  const [pickedFolder, setPickedFolder] = useState('')
  const [workspaceHint, setWorkspaceHint] = useState('')
  const [workspaceHintAbsolute, setWorkspaceHintAbsolute] = useState(false)
  const [isWorkspacePathValid, setIsWorkspacePathValid] = useState(true)

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
    Promise.all([getProfileTemplates(), getPolicyCatalogs()])
      .then(([nextTemplates, nextPolicies]) => { setTemplates(nextTemplates || []); setPolicies(nextPolicies || []) })
      .catch((error) => console.error('Failed to fetch reusable assets:', error))
  }, [])

  const resetCreateForm = () => {
    setNewProject({
      name: '',
      workspacePath: '',
      javaOutputPath: 'generated-java-stubs',
      javaPackage: '',
      javaArchitecture: 'flat',
      profileTemplate: null,
      policyCatalog: null
    })
    setFolderMessage('')
    setPickedFolder('')
    setWorkspaceHint('')
    setWorkspaceHintAbsolute(false)
    setIsWorkspacePathValid(true)
    setCreateError('')
  }

  const openCreateModal = () => {
    resetCreateForm()
    setShowCreate(true)
  }

  const closeCreateModal = () => {
    setShowCreate(false)
    resetCreateForm()
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreateError('')
    setCreateInfo('')
    try {
      if (!isWorkspacePathValid) {
        throw new Error('Ingrese una ruta absoluta para el workspace antes de guardar.')
      }
      const created = await createProject(newProject)
      setShowCreate(false)
      resetCreateForm()
      setCreateInfo(`Proyecto creado. Ruta del workspace guardada: ${created?.workspacePath || newProject.workspacePath}`)
      fetchProjects()
    } catch (error) {
      console.error('Failed to create project:', error)
      setCreateError(error.message || 'Failed to create project')
    }
  }

  const isAbsoluteWorkspacePath = (pathValue) => {
    if (!pathValue || typeof pathValue !== 'string') {
      return false
    }
    return /^(\/|[A-Za-z]:[\\/])/.test(pathValue.trim())
  }

  const validateWorkspacePath = (value) => {
    const trimmed = (value || '').trim()
    const valid = trimmed.length > 0 && isAbsoluteWorkspacePath(trimmed)
    setIsWorkspacePathValid(valid)
    return valid
  }

  const normalizeWorkspaceCandidate = (relativeOrAbsolutePath) => {
    const filePath = (relativeOrAbsolutePath || '').replace(/\\/g, '/')
    const lastSlash = filePath.lastIndexOf('/')
    if (lastSlash <= 0) {
      return { candidate: filePath, absolute: false }
    }
    const candidate = filePath.substring(0, lastSlash)
    return {
      candidate,
      absolute: isAbsoluteWorkspacePath(candidate)
    }
  }

  const isCobolWorkspaceSelection = (files) => {
    const selectedFiles = Array.from(files || [])
    if (selectedFiles.length === 0) {
      return {
        valid: false,
        message: 'No se seleccionaron archivos'
      }
    }
    const cobolExtensions = ['.cob', '.cbl', '.cobol', '.cpy']
    const hasCobol = selectedFiles.some((file) => {
      const name = (file.webkitRelativePath || file.name || '').toLowerCase()
      return cobolExtensions.some((ext) => name.endsWith(ext))
    })
    if (!hasCobol) {
      return {
        valid: false,
        message: 'La carpeta seleccionada no parece contener archivos COBOL (.cob, .cbl, .cobol, .cpy).'
      }
    }
    return {
      valid: true,
      message: 'Carpeta inspeccionada correctamente.'
    }
  }

  const handleFolderPick = (event) => {
    const files = event.target.files || []
    const result = isCobolWorkspaceSelection(files)
    const firstPath = files[0]?.webkitRelativePath || files[0]?.name || ''
    const absoluteGuess = normalizeWorkspaceCandidate(firstPath)
    const folderName = files[0]?.name || ''
    setPickedFolder(folderName)
    setWorkspaceHint(absoluteGuess.candidate)
    setWorkspaceHintAbsolute(absoluteGuess.absolute)
    if (absoluteGuess.absolute && !newProject.workspacePath) {
      setNewProject({ ...newProject, workspacePath: absoluteGuess.candidate })
      setIsWorkspacePathValid(true)
      setFolderMessage(`${result.message} Ruta candidata detectada: ${absoluteGuess.candidate}`)
      return
    }
    setFolderMessage(
      `${result.message} ` +
      (absoluteGuess.absolute
        ? `Ruta candidata detectada: ${absoluteGuess.candidate}`
        : 'No se detectó ruta absoluta desde el navegador; completa la ruta completa manualmente.')
    )
    setIsWorkspacePathValid(false)
    if (!absoluteGuess.absolute) {
      setWorkspaceHint('')
    }
    if (!result.valid) {
      return
    }
  }

  const applyWorkspaceHint = () => {
    if (!workspaceHint) {
      return
    }
    setNewProject({ ...newProject, workspacePath: workspaceHint })
    validateWorkspacePath(workspaceHint)
  }

  if (loading) {
    return <div className="text-center p-8">Loading...</div>
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Projects</h1>
          <button onClick={openCreateModal} className="btn btn-primary">
          New Project
        </button>
      </div>

      {showCreate && (
        <div className="card mb-6">
          <h2 className="text-lg font-semibold mb-4">Create Project</h2>
          <form onSubmit={handleCreate}>
            <p className="text-sm text-gray-600 mb-2">
              En este paso no se cargan fuentes aún: solo definimos nombre del proyecto y la carpeta donde vive el COBOL.
              La lectura/análisis de COBOL ocurre al iniciar Analyze en el wizard.
            </p>
            <p className="text-sm text-gray-600 mb-4">
              En Workspace Path pegá una ruta absoluta (ej: <code>/home/usuario/proyecto</code> o <code>C:\proyecto</code>).
              Podés validar la carpeta con <strong>Browse…</strong> para detectar si tiene COBOL.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <label className="flex flex-col gap-1">
                <span className="text-xs text-gray-600">Project Name</span>
                <input
                  type="text"
                  placeholder="Project Name"
                  value={newProject.name}
                  onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
                  className="input"
                  required
                />
              </label>
              <div className="col-span-2 flex gap-2">
                <label className="flex-1 flex flex-col gap-1">
                  <span className="text-xs text-gray-600">Workspace Path</span>
                  <input
                    type="text"
                    placeholder="/ruta/absoluta/al/workspace"
                    value={newProject.workspacePath}
                    onChange={(e) => {
                      const value = e.target.value
                      setNewProject({ ...newProject, workspacePath: value })
                      validateWorkspacePath(value)
                    }}
                    className="input flex-1"
                    required
                  />
                </label>
                <input
                  id="project-folder-picker"
                  type="file"
                  className="hidden"
                  webkitdirectory=""
                  directory=""
                  multiple
                  onChange={handleFolderPick}
                />
                <label htmlFor="project-folder-picker" className="btn btn-secondary inline-flex items-center">
                  Browse…
                </label>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-3">
              <label className="flex flex-col gap-1">
                <span className="text-xs text-gray-600">Java output path</span>
                <input
                  type="text"
                  placeholder="generated-java-stubs"
                  value={newProject.javaOutputPath}
                  onChange={(e) => setNewProject({ ...newProject, javaOutputPath: e.target.value })}
                  className="input"
                />
                <p className="text-xs text-gray-500">
                  Folder inside the workspace where generated Java files are written. Absolute paths are also supported.
                </p>
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-xs text-gray-600">Base Java package</span>
                <input
                  type="text"
                  placeholder="com.example.generated"
                  value={newProject.javaPackage}
                  onChange={(e) => setNewProject({ ...newProject, javaPackage: e.target.value })}
                  className="input"
                />
                <p className="text-xs text-gray-500">
                  Base package for generated sources (for example: com.mycompany.transformation).
                </p>
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-xs text-gray-600">Java architecture</span>
                <select
                  value={newProject.javaArchitecture}
                  onChange={(e) => setNewProject({ ...newProject, javaArchitecture: e.target.value })}
                  className="input"
                >
                  <option value="flat">Flat package</option>
                  <option value="layered">Layered (entity/service/repository)</option>
                  <option value="program-package">Package per COBOL program</option>
                  <option value="feature-package">Package by feature</option>
                </select>
                <p className="text-xs text-gray-500">
                  Controls how generated classes are organized into Java packages (single package, layered, by program, or by feature).
                </p>
              </label>
            </div>
            <fieldset className="project-inheritance mt-4">
              <legend>Reusable starting point <span>optional · explicit versions</span></legend>
              <label><span>Profile template</span><select value={newProject.profileTemplate ? `${newProject.profileTemplate.name}@${newProject.profileTemplate.version}` : ''}
                onChange={(event) => { const item = templates.find((value) => `${value.name}@${value.version}` === event.target.value); setNewProject({ ...newProject, profileTemplate: item ? { name: item.name, version: item.version } : null }) }}>
                <option value="">Start from defaults</option>{templates.map((item) => <option key={`${item.name}@${item.version}`} value={`${item.name}@${item.version}`}>{item.name} · v{item.version}</option>)}
              </select><small>Seeds the migration profile while local changes stay independent.</small></label>
              <label><span>Policy catalog</span><select value={newProject.policyCatalog ? `${newProject.policyCatalog.name}@${newProject.policyCatalog.version}` : ''}
                onChange={(event) => { const item = policies.find((value) => `${value.name}@${value.version}` === event.target.value); setNewProject({ ...newProject, policyCatalog: item ? { name: item.name, version: item.version } : null }) }}>
                <option value="">Review every decision</option>{policies.map((item) => <option key={`${item.name}@${item.version}`} value={`${item.name}@${item.version}`}>{item.name} · v{item.version}</option>)}
              </select><small>High-confidence semantic matches arrive confirmed with provenance.</small></label>
            </fieldset>
            {folderMessage && (
              <p className="text-sm mt-2 text-gray-700">{folderMessage}</p>
            )}
            {pickedFolder && (
              <p className="text-sm mt-1 text-gray-500">Última carpeta inspeccionada: {pickedFolder}</p>
            )}
            {workspaceHint && workspaceHintAbsolute && (
              <button type="button" className="btn btn-secondary mt-2" onClick={applyWorkspaceHint}>
                Usar ruta detectada
              </button>
            )}
            <p className="text-sm mt-2 text-gray-600">
              Al crear el proyecto, si la ruta no existe, Renovatio crea el directorio automáticamente.
            </p>
            <p className="text-sm mt-2 text-amber-700">
              Tip: el navegador no siempre expone ruta absoluta al seleccionar carpeta.
              Si el picker trae solo nombre, pegá la ruta completa manualmente antes de guardar.
            </p>
            {createError && (
              <p className="text-sm text-red-700 mt-2">{createError}</p>
            )}
            <div className="mt-4 flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={!isWorkspacePathValid}>
                Create
              </button>
              <button type="button" onClick={closeCreateModal} className="btn btn-secondary">
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {createInfo && (
        <p className="text-sm text-green-700 mb-4">{createInfo}</p>
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
              {project.javaOutputPath && (
                <p className="text-xs text-gray-400 mt-1">Java output: {project.javaOutputPath}</p>
              )}
              {project.javaPackage && (
                <p className="text-xs text-gray-400 mt-1">Package: {project.javaPackage}</p>
              )}
              {project.javaArchitecture && (
                <p className="text-xs text-gray-400 mt-1">Architecture: {project.javaArchitecture}</p>
              )}
              {(project.profileTemplate || project.policyCatalog) && <div className="project-binding-tags">
                {project.profileTemplate && <span>Template {project.profileTemplate.name}@{project.profileTemplate.version}</span>}
                {project.policyCatalog && <span>Policy {project.policyCatalog.name}@{project.policyCatalog.version}</span>}
              </div>}
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
