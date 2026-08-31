import { useRef, useState } from 'react'

const COBOL_PROGRAM_EXTENSIONS = ['.cob', '.cbl', '.cobol']
const COBOL_COPYBOOK_EXTENSIONS = ['.cpy']
const COBOL_PROGRAM_MARKERS = [
  'IDENTIFICATION DIVISION',
  'PROGRAM-ID.',
  'DATA DIVISION',
  'PROCEDURE DIVISION',
  'WORKING-STORAGE SECTION',
  'LINKAGE SECTION',
  'ENVIRONMENT DIVISION'
]
const COBOL_COPYBOOK_MARKERS = ['COPY ', ' PIC ', ' REDEFINES ', ' COMP-3', ' VALUE ']

function getLowerName(file) {
  return (file?.name || '').toLowerCase()
}

function hasExtension(file, extensions) {
  const name = getLowerName(file)
  return extensions.some((extension) => name.endsWith(extension))
}

function isAbsoluteWorkspacePath(value) {
  if (!value || typeof value !== 'string') {
    return false
  }
  return /^(\/|[A-Za-z]:[\\/])/.test(value)
}

async function inspectFileContent(file) {
  try {
    return await file.text()
  } catch {
    return ''
  }
}

async function validateCobolWorkspace(files) {
  const selectedFiles = Array.from(files || [])
  if (selectedFiles.length === 0) {
    return {
      status: 'invalid',
      message: 'No files were selected from the folder.'
    }
  }

  const programFiles = selectedFiles.filter((file) => hasExtension(file, COBOL_PROGRAM_EXTENSIONS))
  const copybookFiles = selectedFiles.filter((file) => hasExtension(file, COBOL_COPYBOOK_EXTENSIONS))
  const cobolFiles = [...programFiles, ...copybookFiles]

  if (cobolFiles.length === 0) {
    return {
      status: 'invalid',
      message: 'No COBOL files found. Pick a folder with .cob, .cbl, .cobol, or .cpy files.'
    }
  }

  const sampleFiles = cobolFiles.slice(0, 4)
  const sampledContents = await Promise.all(
    sampleFiles.map(async (file) => ({
      file,
      text: (await inspectFileContent(file)).toUpperCase()
    }))
  )

  const looksLikeCobol = sampledContents.some(({ file, text }) => {
    if (hasExtension(file, COBOL_COPYBOOK_EXTENSIONS)) {
      return COBOL_COPYBOOK_MARKERS.some((marker) => text.includes(marker))
    }

    return COBOL_PROGRAM_MARKERS.some((marker) => text.includes(marker))
  })

  if (!looksLikeCobol) {
    return {
      status: 'invalid',
      message: 'The selected files use COBOL-like extensions, but their contents do not look like COBOL source.'
    }
  }

  if (programFiles.length === 0) {
    return {
      status: 'warning',
      message: 'Only copybooks were found. This looks like a COBOL include folder, but no program sources were selected.'
    }
  }

  return {
    status: 'valid',
    message: `Found ${programFiles.length} COBOL program file(s) and ${copybookFiles.length} copybook(s).`
  }
}

function StepFolder({ data, onChange, onNext }) {
  const folderInputRef = useRef(null)
  const [validation, setValidation] = useState({ status: 'idle', message: '' })

  const openFolderPicker = () => {
    folderInputRef.current?.click()
  }

  const handleFolderPick = async (event) => {
    const files = Array.from(event.target.files || [])
    if (files.length === 0) {
      setValidation({
        status: 'invalid',
        message: 'No files were selected from the folder.'
      })
      return
    }

    const relativePath = files[0].webkitRelativePath || files[0].name
    const pickedFolder = relativePath.includes('/')
      ? relativePath.split('/')[0]
      : files[0].name
    const currentWorkspacePath = isAbsoluteWorkspacePath(data.workspacePath) ? data.workspacePath : ''

    setValidation({ status: 'checking', message: 'Checking selected folder...' })
    const result = await validateCobolWorkspace(files)
    setValidation(result)

    onChange({
      workspacePath: currentWorkspacePath,
      workspaceFolderName: pickedFolder,
      workspaceSelectionMode: 'browser',
      workspaceValidationStatus: result.status,
      workspaceValidationMessage: result.message
    })
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Select COBOL Workspace</h2>
      <p className="text-gray-600 mb-4">
        Enter the absolute path to your COBOL workspace folder, or browse it to validate the folder contents first.
      </p>
      <div className="flex gap-2 mb-3">
        <input
          type="text"
          placeholder="/path/to/cobol/workspace"
          value={data.workspacePath || ''}
          onChange={(e) => {
            onChange({
              workspacePath: e.target.value,
              workspaceSelectionMode: 'manual',
              workspaceValidationStatus: 'idle',
              workspaceValidationMessage: ''
            })
            setValidation({ status: 'idle', message: '' })
          }}
          className="input flex-1"
        />
        <input
          ref={folderInputRef}
          type="file"
          className="hidden"
          webkitdirectory=""
          directory=""
          multiple
          onChange={handleFolderPick}
        />
        <button type="button" onClick={openFolderPicker} className="btn btn-secondary">
          Browse…
        </button>
      </div>
      {validation.status === 'checking' && (
        <p className="text-sm text-gray-500 mb-4">Checking selected folder...</p>
      )}
      {validation.status === 'valid' && (
        <p className="text-sm text-green-700 mb-4">
          ✓ {validation.message}
        </p>
      )}
      {validation.status === 'warning' && (
        <p className="text-sm text-gray-500 mb-4">
          {validation.message}
        </p>
      )}
      {validation.status === 'invalid' && (
        <p className="text-sm text-red-700 mb-4">
          {validation.message}
        </p>
      )}
      {data.workspaceSelectionMode === 'browser' && !isAbsoluteWorkspacePath(data.workspacePath) && (
        <p className="text-sm text-amber-700 mb-4">
          Folder browsing checks whether the contents look like COBOL, but analysis runs on the backend and still needs the full absolute filesystem path.
        </p>
      )}
      <div className="flex justify-end">
        <button
          onClick={onNext}
          disabled={validation.status === 'invalid'}
          className="btn btn-primary"
        >
          Next: Analyze →
        </button>
      </div>
    </div>
  )
}

export default StepFolder
