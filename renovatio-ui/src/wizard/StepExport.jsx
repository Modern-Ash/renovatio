import { useState } from 'react'
import { createJob, getJobStatus, subscribeToJob } from '../api/client'

function StepExport({ projectId, data, onBack }) {
  const [applying, setApplying] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [status, setStatus] = useState(null)

  const handleApply = async () => {
    setApplying(true)
    try {
      const job = await createJob(projectId || 'default', 'apply', {
        dryRun: false,
        planSteps: data.planSteps
      })

      const unsubscribe = subscribeToJob(
        job.id,
        (event) => {
          if (event.status === 'COMPLETED') {
            setStatus('applied')
            setApplying(false)
            unsubscribe()
          } else if (event.status === 'FAILED') {
            setStatus('failed')
            setApplying(false)
            unsubscribe()
          }
        },
        async () => {
          try {
            const current = await getJobStatus(job.id)
            if (current.status === 'COMPLETED') {
              setStatus('applied')
              setApplying(false)
              return
            }
            if (current.status === 'FAILED') {
              setStatus('failed')
              setApplying(false)
              return
            }
          } catch (error) {
            // Fall through to the generic connection error below.
          }

          setStatus('failed')
          setApplying(false)
        }
      )
    } catch (error) {
      setStatus('failed')
      setApplying(false)
    }
  }

  const handleExport = async (format) => {
    setExporting(true)
    try {
      const response = await fetch(`/api/projects/${projectId}/report/${format}`, {
        headers: { 'X-Role': 'ADMIN' }
      })
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `report.${format}`
      a.click()
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('Export failed:', error)
    } finally {
      setExporting(false)
    }
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Export or Apply</h2>
      
      {status === 'applied' && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
          <p className="text-green-800">Migration applied successfully!</p>
        </div>
      )}

      {status === 'failed' && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">Apply failed. Please try again.</p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div className="card">
          <h3 className="font-semibold mb-4">Apply Migration</h3>
          <p className="text-gray-600 mb-4">
            Apply the migration plan to actually modify files in your workspace.
          </p>
          <button
            onClick={handleApply}
            disabled={applying || status === 'applied'}
            className="btn btn-primary w-full"
          >
            {applying ? 'Applying...' : 'Apply for Real'}
          </button>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-4">Export Report</h3>
          <p className="text-gray-600 mb-4">
            Download a report of the migration plan and analysis.
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => handleExport('html')}
              disabled={exporting}
              className="btn btn-secondary flex-1"
            >
              {exporting ? 'Exporting...' : 'HTML'}
            </button>
            <button
              onClick={() => handleExport('pdf')}
              disabled={exporting}
              className="btn btn-secondary flex-1"
            >
              {exporting ? 'Exporting...' : 'PDF'}
            </button>
          </div>
        </div>
      </div>

      <div className="flex justify-between">
        <button onClick={onBack} className="btn btn-secondary">
          ← Back
        </button>
      </div>
    </div>
  )
}

export default StepExport
