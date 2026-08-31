import { useState, useEffect } from 'react'
import { createJob, getJobStatus, subscribeToJob } from '../api/client'

function StepAnalyze({ projectId, data, onNext, onBack }) {
  const [jobId, setJobId] = useState(null)
  const [status, setStatus] = useState('idle')
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')
  const [summary, setSummary] = useState(null)

  const completionMessage = (event) => {
    if (event?.message && event.message.trim()) {
      return event.message
    }
    if (event?.result?.message && event.result.message.trim()) {
      return event.result.message
    }
    if (event?.result?.data?.message && event.result.data.message.trim()) {
      return event.result.data.message
    }
    return 'Analysis completed successfully!'
  }

  const completionSummary = (event) => {
    const candidate = event?.summary || event?.result?.summary || event?.result?.data?.summary
    if (candidate && typeof candidate === 'object') {
      return {
        sourceFiles: Number(candidate.sourceFiles ?? 0),
        copybooks: Number(candidate.copybooks ?? 0),
        programs: Number(candidate.programs ?? 0)
      }
    }

    const dataBlock = event?.result?.data || event?.result || {}
    const sourceFiles = Array.isArray(dataBlock.sourceFiles) ? dataBlock.sourceFiles.length : 0
    const copybooks = Array.isArray(dataBlock.copybooks) ? dataBlock.copybooks.length : 0
    const programs = Array.isArray(dataBlock.programs) ? dataBlock.programs.length : 0

    if (sourceFiles || copybooks || programs) {
      return { sourceFiles, copybooks, programs }
    }

    return null
  }

  const isAbsoluteWorkspacePath = (value) => {
    if (!value || typeof value !== 'string') {
      return false
    }
    return /^(\/|[A-Za-z]:[\\/])/.test(value)
  }

  const startAnalysis = async () => {
    try {
      if (!isAbsoluteWorkspacePath(data.workspacePath)) {
        setStatus('failed')
        setProgress(0)
        setSummary(null)
        setMessage(
          data.workspaceSelectionMode === 'browser'
            ? `The browser-selected folder "${data.workspaceFolderName || data.workspacePath}" does not provide an absolute filesystem path. Paste the full path in the field above, then retry analysis.`
            : 'Please enter the absolute filesystem path to the COBOL workspace before starting analysis.'
        )
        return
      }

      setStatus('starting')
      const job = await createJob(projectId || 'default', 'analyze', {
        workspacePath: data.workspacePath
      })
      setJobId(job.id)
      setStatus('running')
      setSummary(null)

      let unsubscribe = () => {}
      unsubscribe = subscribeToJob(
        job.id,
        (event) => {
          if (event.status === 'COMPLETED') {
            setStatus('completed')
            setProgress(100)
            setMessage(completionMessage(event))
            setSummary(completionSummary(event))
            unsubscribe()
          } else if (event.status === 'FAILED') {
            setStatus('failed')
            setMessage(event.error || 'Analysis failed')
            setSummary(null)
            unsubscribe()
          } else if (event.progress !== undefined) {
            setProgress(event.progress * 100)
            setMessage(event.message || 'Analyzing...')
          }
        },
        async () => {
          try {
            const current = await getJobStatus(job.id)
            if (current.status === 'COMPLETED') {
              setStatus('completed')
              setProgress(100)
              setMessage(completionMessage(current))
              setSummary(completionSummary(current))
              return
            }
            if (current.status === 'FAILED') {
              setStatus('failed')
              setMessage(current.error || 'Analysis failed')
              setSummary(null)
              return
            }
          } catch (error) {
            // Fall through to the generic connection error below.
          }

          setStatus('failed')
          setMessage('Connection lost while waiting for analysis updates')
          setSummary(null)
        }
      )
    } catch (error) {
      setStatus('failed')
      setMessage(error.message)
      setSummary(null)
    }
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Analyze COBOL Programs</h2>
      
      {status === 'idle' && (
        <div>
          <p className="text-gray-600 mb-4">
            Ready to analyze COBOL programs in: <code>{data.workspacePath}</code>
          </p>
          <div className="flex justify-between">
            <button onClick={onBack} className="btn btn-secondary">
              ← Back
            </button>
            <button onClick={startAnalysis} className="btn btn-primary">
              Start Analysis
            </button>
          </div>
        </div>
      )}

      {(status === 'starting' || status === 'running') && (
        <div>
          <div className="mb-4">
            <div className="flex justify-between text-sm text-gray-600 mb-1">
              <span>{message || 'Analyzing...'}</span>
              <span>{Math.round(progress)}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-primary-600 h-2 rounded-full transition-all"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        </div>
      )}

      {status === 'completed' && (
        <div>
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
            <p className="text-green-800">
              {message || 'Analysis completed successfully!'}
            </p>
          </div>
          {summary && (
            <div className="bg-white border border-gray-200 rounded-lg p-4 mb-4">
              <p className="text-sm font-medium text-gray-700 mb-2">Parsing summary</p>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-sm text-gray-600">
                <div>
                  <span className="font-semibold text-gray-900">{summary.sourceFiles}</span>{' '}
                  COBOL source file(s)
                </div>
                <div>
                  <span className="font-semibold text-gray-900">{summary.copybooks}</span>{' '}
                  copybook(s)
                </div>
                <div>
                  <span className="font-semibold text-gray-900">{summary.programs}</span>{' '}
                  parsed program(s)
                </div>
              </div>
            </div>
          )}
          <div className="flex justify-between">
            <button onClick={onBack} className="btn btn-secondary">
              ← Back
            </button>
            <button onClick={onNext} className="btn btn-primary">
              Next: View Metrics →
            </button>
          </div>
        </div>
      )}

      {status === 'failed' && (
        <div>
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-800">Analysis failed: {message}</p>
          </div>
          <div className="flex justify-between">
            <button onClick={onBack} className="btn btn-secondary">
              ← Back
            </button>
            <button onClick={startAnalysis} className="btn btn-primary">
              Retry
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default StepAnalyze
