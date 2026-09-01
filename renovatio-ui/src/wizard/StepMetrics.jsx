import MetricCard from '../dashboard/MetricCard'

function StepMetrics({ data, onNext, onBack }) {
  const summary = data.analysisSummary || data.summary || {}
  const sourceFiles = Number(summary.sourceFiles ?? 0)
  const copybooks = Number(summary.copybooks ?? 0)
  const programs = Number(summary.programs ?? 0)
  const scannedFiles = sourceFiles + copybooks
  const hasAnalysis = sourceFiles > 0 || copybooks > 0 || programs > 0

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Metrics & Complexity</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard title="COBOL Source Files" value={sourceFiles} icon="📝" />
        <MetricCard title="Copybooks" value={copybooks} icon="📚" />
        <MetricCard title="Parsed Programs" value={programs} icon="🔄" />
        <MetricCard title="Files Scanned" value={scannedFiles} icon="📋" />
      </div>

      <div className="card mb-6">
        <h3 className="font-semibold mb-2">Analysis Summary</h3>
        {hasAnalysis ? (
          <div className="text-gray-600 space-y-1">
            <p>Analysis of the COBOL workspace has been completed.</p>
            {data.analysisWorkspace && (
              <p>
                Workspace: <span className="font-medium text-gray-900">{data.analysisWorkspace}</span>
              </p>
            )}
            {data.analysisMessage && (
              <p>
                Latest message: <span className="font-medium text-gray-900">{data.analysisMessage}</span>
              </p>
            )}
            <p>
              Review the metrics above before proceeding to plan configuration.
            </p>
          </div>
        ) : (
          <p className="text-gray-600">
            Run analysis first to populate real metrics for this workspace.
          </p>
        )}
      </div>

      <div className="flex justify-between">
        <button onClick={onBack} className="btn btn-secondary">
          ← Back
        </button>
        <button onClick={onNext} className="btn btn-primary">
          Next: Configure Plan →
        </button>
      </div>
    </div>
  )
}

export default StepMetrics
