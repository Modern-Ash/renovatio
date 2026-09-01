import { useState } from 'react'
import { createJob, getJobStatus, subscribeToJob } from '../api/client'

function StepApply({ projectId, data, onChange, onNext, onBack }) {
  const [jobId, setJobId] = useState(null)
  const [status, setStatus] = useState('idle')
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')

  const completionResult = (event) => {
    if (event?.result && typeof event.result === 'object') {
      return event.result
    }
    if (event?.data && typeof event.data === 'object') {
      return event.data
    }
    return {}
  }

  const completionMessage = (result) => {
    if (result?.message && result.message.trim()) {
      return result.message
    }
    if (result?.preview && result.preview.trim()) {
      return 'Dry run preview generated successfully!'
    }
    return 'Dry run completed successfully!'
  }

  const persistDryRunState = (result, fallbackId) => {
    if (typeof onChange !== 'function') {
      return
    }

    onChange({
      dryRunRunId: result.runId || fallbackId || null,
      dryRunResult: result,
      dryRunPreview: result.preview || result.diff || '',
      dryRunMessage: completionMessage(result)
    })
  }

  const startDryRun = async () => {
    try {
      setStatus('starting')
      setProgress(5)
      setMessage('Submitting dry run...')
      const job = await createJob(projectId || 'default', 'apply', {
        dryRun: true,
        planSteps: data.planSteps
      })
      setJobId(job.id)
      setStatus('running')
      setProgress(10)
      setMessage('Applying plan (dry run)...')

      let pollTimer = null
      let settled = false
      let unsubscribe = () => {}
      const stopPolling = () => {
        if (pollTimer) {
          clearInterval(pollTimer)
          pollTimer = null
        }
      }
      const stopSubscription = () => {
        stopPolling()
        unsubscribe()
        unsubscribe = () => {}
      }
      const complete = (event) => {
        if (settled) {
          return
        }
        settled = true
        const result = completionResult(event)
        stopSubscription()
        setStatus('completed')
        setProgress(100)
        setMessage(completionMessage(result))
        persistDryRunState(result, job.id)
      }
      const fail = (messageText) => {
        if (settled) {
          return
        }
        settled = true
        stopSubscription()
        setStatus('failed')
        setMessage(messageText || 'Dry run failed')
      }

      const refreshJobStatus = async () => {
        if (settled) {
          return
        }

        try {
          const current = await getJobStatus(job.id)
          if (current.status === 'COMPLETED') {
            complete(current)
            return
          }
          if (current.status === 'FAILED') {
            fail(current.error || 'Dry run failed')
            return
          }

          setProgress((currentProgress) => Math.min(currentProgress + 8, 95))
          setMessage('Applying plan (dry run)...')
        } catch {
          // Keep waiting; the job may still complete even if this poll blips.
        }
      }

      unsubscribe = subscribeToJob(
        job.id,
        (event) => {
          if (event.status === 'COMPLETED') {
            complete(event)
            unsubscribe()
          } else if (event.status === 'FAILED') {
            fail(event.error || 'Dry run failed')
            unsubscribe()
          } else if (event.progress !== undefined) {
            setProgress(Math.max(event.progress * 100, 10))
            setMessage(event.message || 'Applying plan (dry run)...')
          }
        },
        async () => {
          try {
            const current = await getJobStatus(job.id)
            if (current.status === 'COMPLETED') {
              complete(current)
              return
            }
            if (current.status === 'FAILED') {
              fail(current.error || 'Dry run failed')
              return
            }
          } catch (error) {
            // Keep polling below; the job may still complete.
          }

          setMessage('Waiting for dry run status...')
        }
      )

      pollTimer = setInterval(refreshJobStatus, 1000)
      void refreshJobStatus()
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
