import { useEffect, useState } from 'react'

function StepPlan({ data, onChange, onNext, onBack }) {
  const defaultSteps = [
    { type: 'PARSE_COBOL', description: 'Parse COBOL programs', enabled: true },
    { type: 'GENERATE_JAVA_DTOS', description: 'Generate Java DTOs', enabled: true },
    { type: 'GENERATE_JAVA_STUBS', description: 'Generate Java stubs', enabled: true },
    { type: 'CREATE_MAPPINGS', description: 'Create data mappings', enabled: true },
    { type: 'GENERATE_TESTS', description: 'Generate tests', enabled: false }
  ]
  const [steps, setSteps] = useState(
    Array.isArray(data?.planSteps) && data.planSteps.length > 0
      ? data.planSteps.map((step) => ({ ...step }))
      : defaultSteps
  )

  useEffect(() => {
    onChange({ planSteps: steps })
  }, [])

  const toggleStep = (index) => {
    const newSteps = [...steps]
    newSteps[index].enabled = !newSteps[index].enabled
    setSteps(newSteps)
    onChange({ planSteps: newSteps })
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Configure Migration Plan</h2>
      <p className="text-gray-600 mb-4">
        Select which migration steps to include in the plan.
      </p>

      <div className="space-y-3 mb-6">
        {steps.map((step, index) => (
          <div key={step.type} className="flex items-center justify-between p-3 border rounded-lg">
            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                checked={step.enabled}
                onChange={() => toggleStep(index)}
                className="w-4 h-4"
              />
              <div>
                <p className="font-medium">{step.description}</p>
                <p className="text-sm text-gray-500">{step.type}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="flex justify-between">
        <button onClick={onBack} className="btn btn-secondary">
          ← Back
        </button>
        <button onClick={onNext} className="btn btn-primary">
          Next: Dry Run →
        </button>
      </div>
    </div>
  )
}

export default StepPlan
