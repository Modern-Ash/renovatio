import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import StepFolder from '../wizard/StepFolder'
import StepAnalyze from '../wizard/StepAnalyze'
import StepMetrics from '../wizard/StepMetrics'
import StepPlan from '../wizard/StepPlan'
import StepApply from '../wizard/StepApply'
import StepDiff from '../wizard/StepDiff'
import StepReview from '../wizard/StepReview'
import StepExport from '../wizard/StepExport'

const steps = [
  { id: 'folder', label: 'Select Folder' },
  { id: 'analyze', label: 'Analyze' },
  { id: 'metrics', label: 'Metrics' },
  { id: 'plan', label: 'Plan' },
  { id: 'apply', label: 'Dry Run' },
  { id: 'diff', label: 'Diff' },
  { id: 'review', label: 'Review' },
  { id: 'export', label: 'Export' }
]

function Wizard() {
  const { projectId } = useParams()
  const [currentStep, setCurrentStep] = useState(0)
  const [data, setData] = useState({})

  const updateData = (newData) => {
    setData((current) => ({ ...current, ...newData }))
  }

  const goNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1)
    }
  }

  const goBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1)
    }
  }

  const renderStep = () => {
    switch (currentStep) {
      case 0:
        return <StepFolder data={data} onChange={updateData} onNext={goNext} />
      case 1:
        return (
          <StepAnalyze
            projectId={projectId}
            data={data}
            onChange={updateData}
            onNext={goNext}
            onBack={goBack}
          />
        )
      case 2:
        return <StepMetrics data={data} onNext={goNext} onBack={goBack} />
      case 3:
        return <StepPlan data={data} onChange={updateData} onNext={goNext} onBack={goBack} />
      case 4:
        return (
          <StepApply
            projectId={projectId}
            data={data}
            onChange={updateData}
            onNext={goNext}
            onBack={goBack}
          />
        )
      case 5:
        return <StepDiff data={data} onNext={goNext} onBack={goBack} />
      case 6:
        return (
          <StepReview
            projectId={projectId}
            data={data}
            onNext={goNext}
            onBack={goBack}
          />
        )
      case 7:
        return (
          <StepExport
            projectId={projectId}
            data={data}
            onBack={goBack}
          />
        )
      default:
        return (
          <div>
            <h2 className="text-xl font-semibold mb-4">
              Step {currentStep + 1}: {steps[currentStep].label}
            </h2>
            <p className="text-gray-600 mb-4">This step will be implemented soon.</p>
            <div className="flex justify-between">
              <button onClick={goBack} className="btn btn-secondary">
                ← Back
              </button>
              {currentStep < steps.length - 1 && (
                <button onClick={goNext} className="btn btn-primary">
                  Next →
                </button>
              )}
            </div>
          </div>
        )
    }
  }

  return (
    <div>
      <div className="mb-6">
        <Link to={projectId ? `/projects/${projectId}` : '/projects'} className="text-primary-600 hover:underline">
          ← Back
        </Link>
      </div>

      <h1 className="text-2xl font-bold mb-6">Migration Wizard</h1>

      <div className="mb-8">
        <div className="flex justify-between">
          {steps.map((step, index) => (
            <div
              key={step.id}
              className={`flex items-center ${
                index < steps.length - 1 ? 'flex-1' : ''
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  index < currentStep
                    ? 'bg-green-500 text-white'
                    : index === currentStep
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-200 text-gray-600'
                }`}
              >
                {index < currentStep ? '✓' : index + 1}
              </div>
              {index < steps.length - 1 && (
                <div
                  className={`flex-1 h-1 mx-2 ${
                    index < currentStep ? 'bg-green-500' : 'bg-gray-200'
                  }`}
                />
              )}
            </div>
          ))}
        </div>
        <div className="flex justify-between mt-2">
          {steps.map((step) => (
            <div key={step.id} className="text-xs text-gray-500 w-12 text-center">
              {step.label}
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        {renderStep()}
      </div>
    </div>
  )
}

export default Wizard
