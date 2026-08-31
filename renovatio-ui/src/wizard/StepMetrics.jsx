import MetricCard from '../dashboard/MetricCard'

function StepMetrics({ data, onNext, onBack }) {
  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Metrics & Complexity</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard title="Lines of Code" value="0" unit="LOC" icon="📝" />
        <MetricCard title="Cyclomatic Complexity" value="0" icon="🔄" />
        <MetricCard title="Copybooks" value="0" icon="📚" />
        <MetricCard title="Procedures" value="0" icon="📋" />
      </div>

      <div className="card mb-6">
        <h3 className="font-semibold mb-2">Analysis Summary</h3>
        <p className="text-gray-600">
          Analysis of the COBOL workspace has been completed. 
          Review the metrics above before proceeding to plan configuration.
        </p>
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
