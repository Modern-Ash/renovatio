function StepDiff({ data, onNext, onBack }) {
  const dryRunResult = data.dryRunResult || {}
  const previewText = data.dryRunPreview || dryRunResult.preview || dryRunResult.diff || ''
  const selectedSteps = Array.isArray(dryRunResult.selectedSteps)
    ? dryRunResult.selectedSteps
    : Array.isArray(data.planSteps)
      ? data.planSteps.filter((step) => step.enabled).map((step) => step.description || step.type)
      : []
  const skippedSteps = Array.isArray(dryRunResult.skippedSteps)
    ? dryRunResult.skippedSteps
    : Array.isArray(data.planSteps)
      ? data.planSteps.filter((step) => !step.enabled).map((step) => step.description || step.type)
      : []
  const changes = dryRunResult.changes || {}

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Review Changes</h2>
      
      <div className="card mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h3 className="font-semibold mb-2 text-gray-700">Original</h3>
            <div className="bg-gray-100 p-4 rounded text-sm overflow-auto max-h-96 space-y-2">
              <p className="text-gray-600">
                Dry run mode does not write files. The original workspace remains unchanged.
              </p>
              {skippedSteps.length > 0 && (
                <div>
                  <p className="font-medium text-gray-700 mb-1">Skipped steps</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1">
                    {skippedSteps.map((step) => (
                      <li key={step}>{step}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
          <div>
            <h3 className="font-semibold mb-2 text-gray-700">Modified</h3>
            <div className="bg-gray-100 p-4 rounded text-sm overflow-auto max-h-96 space-y-3">
              {previewText ? (
                <pre className="whitespace-pre-wrap font-mono text-gray-800">{previewText}</pre>
              ) : (
                <p className="text-gray-600">No preview was generated for this dry run.</p>
              )}

              {selectedSteps.length > 0 && (
                <div>
                  <p className="font-medium text-gray-700 mb-1">Selected steps</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1">
                    {selectedSteps.map((step) => (
                      <li key={step}>{step}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="card">
          <p className="text-sm text-gray-500">Enabled steps</p>
          <p className="text-2xl font-semibold">{changes.enabledSteps ?? selectedSteps.length}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">Skipped steps</p>
          <p className="text-2xl font-semibold">{changes.skippedSteps ?? skippedSteps.length}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">Dry run</p>
          <p className="text-2xl font-semibold">{String(changes.dryRun ?? true)}</p>
        </div>
      </div>

      <div className="flex justify-between">
        <button onClick={onBack} className="btn btn-secondary">
          ← Back
        </button>
        <button onClick={onNext} className="btn btn-primary">
          Next: Review Action Items →
        </button>
      </div>
    </div>
  )
}

export default StepDiff
