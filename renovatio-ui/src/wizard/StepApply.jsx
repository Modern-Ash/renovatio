import { useState } from 'react'
import { createJob, getJobStatus, subscribeToJob } from '../api/client'

function StepApply({ projectId, data, onNext, onBack }) {
  const [jobId, setJobId] = useState(null)
  const [status, setStatus] = useState('idle')
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')

  const startDryRun = async () => {
    try {
      setStatus('starting')
      const job = await createJob(projectId || 'default', 'apply', {
        dryRun: true,
        planSteps: data.planSteps
      })
      setJobId(job.id)
      setStatus('running')

      const unsubscribe = subscribeToJob(
        job.id,
        (event) => {
          if (event.status === 'COMPLETED') {
            setStatus('completed')
            setProgress(100)
            setMessage('Dry run completed successfully!')
            unsubscribe()
          } else if (event.status === 'FAILED') {
            setStatus('failed')
            setMessage(event.error || 'Dry run failed')
            unsubscribe()
          } else if (event.progress !== undefined) {
            setProgress(event.progress * 100)
            setMessage(event.message || 'Applying plan (dry run)...')
          }
        },
        async () => {
          try {
            const current = await getJobStatus(job.id)
            if (current.status === 'COMPLETED') {
              setStatus('completed')
              setProgress(100)
              setMessage('Dry run completed successfully!')
              return
            }
            if (current.status === 'FAILED') {
              setStatus('failed')
              setMessage(current.error || 'Dry run failed')
              return
            }
          } catch (error) {
            // Fall through to the generic connection error below.
          }

          setStatus('failed')
          setMessage('Connection lost while waiting for apply updates')
        }
      )
    } catch (error) {
      setStatus('failed')
      setMessage(error.message)
    }
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Dry Run Apply</h2>
      
      {status === 'idle' && (
        <div>
          <p className="text-gray-600 mb-4">
            Execute the migration plan in dry-run mode to preview changes without modifying files.
          </p>
          <div className="flex justify-between">
            <button onClick={onBack} className="btn btn-secondary">
              ← Back
            </button>
            <button onClick={startDryRun} className="btn btn-primary">
              Start Dry Run
            </button>
          </div>
        </div>
      )}

      {(status === 'starting' || status === 'running') && (
        <div>
          <div className="mb-4">
            <div className="flex justify-between text-sm text-gray-600 mb-1">
              <span>{message || 'Applying plan...'}</span>
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
            <p className="text-green-800">Dry run completed successfully!</p>
          </div>
          <div className="flex justify-between">
            <button onClick={onBack} className="btn btn-secondary">
              ← Back
            </button>
            <button onClick={onNext} className="btn btn-primary">
              Next: View Diff →
            </button>
          </div>
        </div>
      )}

      {status === 'failed' && (
        <div>
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-800">Dry run failed: {message}</p>
          </div>
          <div className="flex justify-between">
            <button onClick={onBack} className="btn btn-secondary">
              ← Back
            </button>
            <button onClick={startDryRun} className="btn btn-primary">
              Retry
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default StepApply
