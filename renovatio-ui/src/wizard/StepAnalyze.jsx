import { useState, useEffect } from 'react'
import { createJob, subscribeToJob } from '../api/client'

function StepAnalyze({ projectId, data, onNext, onBack }) {
  const [jobId, setJobId] = useState(null)
  const [status, setStatus] = useState('idle')
  const [progress, setProgress] = useState(0)
  const [message, setMessage] = useState('')

  const startAnalysis = async () => {
    try {
      setStatus('starting')
      const job = await createJob(projectId || 'default', 'analyze', {
        workspacePath: data.workspacePath
      })
      setJobId(job.id)
      setStatus('running')

      const unsubscribe = subscribeToJob(
        job.id,
        (event) => {
          if (event.status === 'COMPLETED') {
            setStatus('completed')
            unsubscribe()
          } else if (event.status === 'FAILED') {
            setStatus('failed')
            setMessage(event.error || 'Analysis failed')
            unsubscribe()
          } else if (event.progress !== undefined) {
            setProgress(event.progress * 100)
            setMessage(event.message || 'Analyzing...')
          }
        },
        (error) => {
          setStatus('failed')
          setMessage('Connection lost')
        }
      )
    } catch (error) {
      setStatus('failed')
      setMessage(error.message)
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
            <p className="text-green-800">Analysis completed successfully!</p>
          </div>
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
