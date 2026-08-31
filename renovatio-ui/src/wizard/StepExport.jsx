import { useState } from 'react'
import { createJob, getJobStatus, subscribeToJob } from '../api/client'

function StepExport({ projectId, data, onBack }) {
  const [applying, setApplying] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [status, setStatus] = useState(null)
  const [reportHtmlPreview, setReportHtmlPreview] = useState('')
  const [reportPreviewLoading, setReportPreviewLoading] = useState(false)
  const [reportPreviewError, setReportPreviewError] = useState('')
  const [reportPreviewLoaded, setReportPreviewLoaded] = useState(false)
  const [reportSummary, setReportSummary] = useState(null)

  const handleApply = async () => {
    setApplying(true)
    setStatus(null)
    try {
      const job = await createJob(projectId || 'default', 'apply', {
        dryRun: false,
        planSteps: data.planSteps
      })

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
      const complete = () => {
        if (settled) {
          return
        }
        settled = true
        stopSubscription()
        setStatus('applied')
        setApplying(false)
      }
      const fail = () => {
        if (settled) {
          return
        }
        settled = true
        stopSubscription()
        setStatus('failed')
        setApplying(false)
      }
      const refreshJobStatus = async () => {
        if (settled) {
          return
        }

        try {
          const current = await getJobStatus(job.id)
          if (current.status === 'COMPLETED') {
            complete()
            return
          }
          if (current.status === 'FAILED') {
            fail()
            return
          }
        } catch {
          // Keep waiting; the job may still complete.
        }
      }

      unsubscribe = subscribeToJob(
        job.id,
        (event) => {
          if (event.status === 'COMPLETED') {
            complete()
          } else if (event.status === 'FAILED') {
            fail()
          }
        },
        async () => {
          await refreshJobStatus()

          if (!settled) {
            setStatus('failed')
            setApplying(false)
            setTimeout(() => {
              if (!settled) {
                setStatus(null)
                setApplying(true)
              }
            }, 250)
            stopSubscription()
          }
        }
      )

      pollTimer = setInterval(refreshJobStatus, 1000)
      void refreshJobStatus()
    } catch (error) {
      setApplying(false)
      setStatus('failed')
    }
  }

  const handleExport = async (format) => {
    setExporting(true)
    try {
      setReportPreviewError('')
      const response = await fetch(`/api/projects/${projectId}/report/${format}`, {
        headers: { 'X-Role': 'ADMIN' }
      })
      if (!response.ok) {
        throw new Error(`API error: ${response.status}`)
      }
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `report.${format}`
      a.click()
      window.URL.revokeObjectURL(url)
    } catch (error) {
      setReportPreviewError(error.message || 'Failed to export report')
      console.error('Export failed:', error)
    } finally {
      setExporting(false)
    }
  }

  const handleReportPreview = async () => {
    setReportPreviewLoading(true)
    setReportPreviewError('')
    try {
      const [htmlResponse, summaryResponse] = await Promise.all([
        fetch(`/api/projects/${projectId}/report/html`, { headers: { 'X-Role': 'ADMIN' } }),
        fetch(`/api/projects/${projectId}/report/json`, { headers: { 'X-Role': 'ADMIN' } })
      ])

      if (!htmlResponse.ok) {
        throw new Error(`API error: ${htmlResponse.status}`)
      }
      if (!summaryResponse.ok) {
        throw new Error(`API error: ${summaryResponse.status}`)
      }

      const [html, summary] = await Promise.all([
        htmlResponse.text(),
        summaryResponse.json()
      ])

      setReportHtmlPreview(html)
      setReportSummary(summary)
      setReportPreviewLoaded(true)
    } catch (error) {
      setReportHtmlPreview('')
      setReportSummary(null)
      setReportPreviewLoaded(false)
      setReportPreviewError(error.message || 'No se pudo generar el preview del reporte.')
    } finally {
      setReportPreviewLoading(false)
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
          <button
            onClick={handleReportPreview}
            disabled={reportPreviewLoading}
            className="btn btn-secondary w-full mt-3"
          >
            {reportPreviewLoading ? 'Cargando preview...' : 'Previsualizar reporte (HTML)'}
          </button>
          {reportPreviewError && (
            <p className="text-sm text-red-700 mt-2">{reportPreviewError}</p>
          )}
          {reportPreviewLoaded && reportHtmlPreview && (
            <div className="mt-4 border rounded-lg p-2 bg-white">
              <p className="text-sm text-gray-600 mb-2">Vista previa del reporte</p>
              <iframe
                title="Report preview"
                srcDoc={reportHtmlPreview}
                className="w-full border rounded"
                style={{ height: '260px' }}
              />
            </div>
          )}
              {reportSummary && (
            <div className="mt-3 border rounded-lg p-3 bg-gray-50 space-y-3">
              <p className="font-semibold text-gray-700">Resumen del reporte</p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-700">
                <div>
                  <p className="text-xs font-semibold text-gray-500 uppercase mb-1">Estados</p>
                  <ul className="list-disc list-inside space-y-1">
                    {Object.entries(reportSummary.statuses || {}).map(([name, value]) => (
                      <li key={name}>
                        {name}: <span className="font-medium">{String(value)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
                <div>
                  <p className="text-xs font-semibold text-gray-500 uppercase mb-1">Métricas</p>
                  <ul className="list-disc list-inside space-y-1">
                    {Object.entries(reportSummary.metrics || {}).map(([name, value]) => (
                      <li key={name}>
                        {name}: <span className="font-medium">{typeof value === 'number' ? value.toFixed(2) : String(value)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div className="bg-white border rounded p-2">
                  <p className="text-xs text-gray-500">Acciones habilitadas</p>
                  <p className="text-lg font-semibold">
                    {Array.isArray((data.dryRunResult && data.dryRunResult.selectedSteps) || [])
                      ? (data.dryRunResult && data.dryRunResult.selectedSteps).length
                      : 0}
                  </p>
                </div>
                <div className="bg-white border rounded p-2">
                  <p className="text-xs text-gray-500">Acciones omitidas</p>
                  <p className="text-lg font-semibold">
                    {Array.isArray((data.dryRunResult && data.dryRunResult.skippedSteps) || [])
                      ? (data.dryRunResult && data.dryRunResult.skippedSteps).length
                      : 0}
                  </p>
                </div>
                <div className="bg-white border rounded p-2">
                  <p className="text-xs text-gray-500">Action items pendientes</p>
                  <p className="text-lg font-semibold">
                    {Array.isArray((data.review && data.review.actionItems) || [])
                      ? data.review.actionItems.length
                      : 0}
                  </p>
                </div>
              </div>

              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase mb-1">Plan aplicado</p>
                <p className="text-sm text-gray-700">
                  Seleccionados: {Array.isArray(data.dryRunResult?.selectedSteps) && data.dryRunResult.selectedSteps.length > 0
                    ? data.dryRunResult.selectedSteps.join(', ')
                    : '—'}
                </p>
                <p className="text-sm text-gray-700">
                  Omitidos: {Array.isArray(data.dryRunResult?.skippedSteps) && data.dryRunResult.skippedSteps.length > 0
                    ? data.dryRunResult.skippedSteps.join(', ')
                    : '—'}
                </p>
              </div>
            </div>
          )}
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
