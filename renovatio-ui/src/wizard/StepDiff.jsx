function StepDiff({ data, onNext, onBack }) {
  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Review Changes</h2>
      
      <div className="card mb-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <h3 className="font-semibold mb-2 text-gray-700">Original</h3>
            <pre className="bg-gray-100 p-4 rounded text-sm overflow-auto max-h-96">
              {/* Original code will be displayed here */}
              <code>No changes to display</code>
            </pre>
          </div>
          <div>
            <h3 className="font-semibold mb-2 text-gray-700">Modified</h3>
            <pre className="bg-gray-100 p-4 rounded text-sm overflow-auto max-h-96">
              {/* Modified code will be displayed here */}
              <code>No changes to display</code>
            </pre>
          </div>
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
